package com.dealio.app.data

import android.app.Activity
import com.dealio.app.BuildConfig
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Firebase phone-number OTP for Android.
 *
 * Firebase sends the SMS and checks the code — the backend never sees either.
 * A successful check yields a Firebase ID token, which is exchanged for a Dealio
 * session at POST /api/auth/firebase (see [AuthRepository.firebaseExchange]).
 * This mirrors the web flow in Dealio_frontend/src/lib/firebase.ts.
 *
 * Note this bypasses the backend's own OTP rate limiting in authService.sendOtp —
 * abuse protection here is Firebase's (Play Integrity app-check plus its own
 * per-number quotas).
 */
sealed class PhoneVerification {
    /**
     * Firebase resolved the number without the user typing anything — SMS
     * auto-retrieval, or instant validation of a number the device already owns.
     * Sign in with this credential directly; there is no code to collect.
     */
    data class Completed(val credential: PhoneAuthCredential) : PhoneVerification()

    /** A code was sent. Keep [verificationId] to pair with the code the user types. */
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken,
    ) : PhoneVerification()

    data class Failed(val message: String) : PhoneVerification()
}

object FirebasePhoneAuth {

    private const val TIMEOUT_SECONDS = 60L

    /**
     * When true, debug builds skip Firebase's app verification — which also limits
     * them to the fictional numbers registered in the console, since a real number
     * then has no app identifier to send. Flip to false to exercise the real Play
     * Integrity path on a debug build.
     */
    private const val DISABLE_APP_VERIFICATION_ON_DEBUG = true

    /**
     * Debug builds are sideloaded, never distributed through Play, so Play
     * Integrity cannot attest them — it returns UNRECOGNIZED_VERSION and Firebase
     * rejects the request with "Invalid app info in play_integrity_token". The
     * package name and both SHA fingerprints are registered correctly; there is
     * nothing to fix in the console for a local build.
     *
     * Turning app verification off on debug lets the emulator run the flow
     * against the test numbers configured in Firebase Console → Authentication →
     * Sign-in method → Phone. Release builds keep full verification.
     */
    private fun applyDebugAppVerification(auth: FirebaseAuth) {
        if (BuildConfig.DEBUG && DISABLE_APP_VERIFICATION_ON_DEBUG) {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
    }

    /**
     * Warm Firebase up before anyone asks for a code.
     *
     * The first touch of [FirebaseAuth] loads the SDK and reads its config, and
     * `initializeRecaptchaConfig` fetches the app-verification configuration
     * that [sendCode] would otherwise fetch as part of the send itself. Doing
     * both while the user is still picking a role and typing takes that work
     * off the path between the button and the SMS.
     *
     * Best-effort and idempotent: if it fails, the send does the work exactly as
     * it did before. Call off the main thread — the first `getInstance()` reads
     * from disk.
     */
    fun prewarm() {
        runCatching {
            val auth = FirebaseAuth.getInstance()
            applyDebugAppVerification(auth)
            auth.initializeRecaptchaConfig()
        }
    }

    /**
     * Start verification for [e164Phone] (e.g. "+919502320615").
     *
     * [onEvent] may fire more than once: a [PhoneVerification.CodeSent] can be
     * followed by a [PhoneVerification.Completed] when the SMS is auto-retrieved,
     * so this is a callback rather than a suspend function.
     *
     * Pass the [resendToken] from a previous [PhoneVerification.CodeSent] to
     * force a genuine resend instead of reusing the in-flight verification.
     */
    fun sendCode(
        activity: Activity,
        e164Phone: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onEvent: (PhoneVerification) -> Unit,
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onEvent(PhoneVerification.Completed(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onEvent(PhoneVerification.Failed(errorMessage(e)))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                onEvent(PhoneVerification.CodeSent(verificationId, token))
            }
        }

        val auth = FirebaseAuth.getInstance()
        applyDebugAppVerification(auth)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply { if (resendToken != null) setForceResendingToken(resendToken) }
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /** Pair a typed code with the verification it belongs to. */
    fun credentialFor(verificationId: String, code: String): PhoneAuthCredential =
        PhoneAuthProvider.getCredential(verificationId, code)

    /**
     * Exchange a verified credential for a Firebase ID token.
     *
     * The Firebase user is signed out immediately afterwards: the token is a
     * one-shot proof of phone ownership for our backend, and leaving a live
     * Firebase session around serves no purpose once we hold a Dealio session.
     */
    suspend fun idTokenFor(credential: PhoneAuthCredential): Result<String> =
        suspendCancellableCoroutine { cont ->
            val auth = FirebaseAuth.getInstance()
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        cont.resume(Result.failure(IllegalStateException("Verification succeeded but returned no user")))
                        return@addOnSuccessListener
                    }
                    user.getIdToken(false)
                        .addOnSuccessListener { tokenResult ->
                            val token = tokenResult.token
                            auth.signOut()
                            if (token.isNullOrBlank()) {
                                cont.resume(Result.failure(IllegalStateException("Verification returned an empty token")))
                            } else {
                                cont.resume(Result.success(token))
                            }
                        }
                        .addOnFailureListener { e ->
                            auth.signOut()
                            cont.resume(Result.failure(e))
                        }
                }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    /** Turn a Firebase phone-auth failure into something worth showing a user. */
    fun errorMessage(e: Throwable): String = when {
        e is FirebaseAuthInvalidCredentialsException ->
            "Incorrect or expired code. Please check and try again."
        e is FirebaseTooManyRequestsException ->
            "Too many attempts. Please wait a bit and try again."
        e.message?.contains("BILLING_NOT_ENABLED", ignoreCase = true) == true ->
            "Phone sign-in is not enabled for this app. Please contact support."
        e.message?.contains("quota", ignoreCase = true) == true ->
            "SMS limit reached. Please try again later."
        else -> e.message ?: "Verification failed. Please try again."
    }
}
