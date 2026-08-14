package com.dealio.app.ui.auth

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.AuthRepository
import com.dealio.app.data.FirebasePhoneAuth
import com.dealio.app.data.PhoneVerification
import com.dealio.app.data.TokenStore
import com.dealio.app.data.api.ApiClient
import com.dealio.app.data.api.AuthUser
import com.dealio.app.data.api.PhoneLookupData
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { DETAILS, OTP }

data class AuthUiState(
    val step: AuthStep = AuthStep.DETAILS,
    val loading: Boolean = false,
    val error: String? = null,
    val maskedPhone: String? = null,
    val demoCode: String? = null,
    val resendSecondsLeft: Int = 0,
    /**
     * Wire value of the account's real role, set when sign-in was attempted under
     * a different one. Lets the screen offer a one-tap correction instead of
     * making the user hunt for the right pill.
     */
    val mismatchedRole: String? = null,
    /** Non-null once OTP verification succeeded — the screen navigates away. */
    val loggedInUser: AuthUser? = null,
)

/**
 * Drives both the login and signup flows: send OTP → verify OTP.
 * Each screen gets its own instance (scoped to its nav back-stack entry).
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AuthRepository(ApiClient.authApi, TokenStore(app))

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    /** Set once Firebase has sent a code; pairs with whatever the user types. */
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /** Remembered from [sendOtp] so an auto-retrieved code can finish signup itself. */
    private var pendingSignup: PendingSignup? = null

    /** Guards against two credentials being redeemed at once — see [exchange]. */
    private var exchanging = false

    /** The last account pre-flight, reused by [sendOtp] — see [prefetchLookup]. */
    private var lookup: CachedLookup? = null

    private data class PendingSignup(
        val fullName: String,
        val role: String,
        val referralCode: String,
    )

    private class CachedLookup(
        val key: String,
        val startedAt: Long,
        val result: Deferred<ApiResult<PhoneLookupData>>,
    )

    init {
        // Firebase's first touch loads the SDK and fetches its app-verification
        // config — work the send used to pay for. Do it now, while the user is
        // still picking a role and typing their number.
        viewModelScope.launch(Dispatchers.IO) { FirebasePhoneAuth.prewarm() }
    }

    /**
     * Look the number up, reusing a recent answer for the same one.
     *
     * [sendOtp] awaits this; [prefetchLookup] starts it as soon as a plausible
     * number has been typed, so the round trip is usually already done by the
     * time the button is pressed.
     */
    private fun lookupFor(e164: String): Deferred<ApiResult<PhoneLookupData>> {
        val cached = lookup
        if (cached != null &&
            cached.key == e164 &&
            SystemClock.elapsedRealtime() - cached.startedAt < LOOKUP_TTL_MS
        ) {
            return cached.result
        }
        val started = viewModelScope.async { repository.phoneLookup(e164) }
        lookup = CachedLookup(e164, SystemClock.elapsedRealtime(), started)
        return started
    }

    /**
     * Start the account pre-flight for a number the user is still typing. Costs
     * one request and saves a full round trip off the send; safe to call on
     * every keystroke, since a repeat of the same number reuses the first call.
     */
    fun prefetchLookup(phone: String, countryCode: String) {
        if (phone.isBlank()) return
        lookupFor(toE164(phone, countryCode))
    }

    /**
     * Start Firebase phone verification. Needs the hosting [activity] because
     * Firebase attaches its app-verification (Play Integrity / reCAPTCHA
     * fallback) to it.
     *
     * Pass [signup] on the signup flow so an auto-retrieved SMS can complete the
     * account without the user typing anything.
     *
     * On login, [role] is the account type the user picked and is checked against
     * the number's real role before any SMS is spent.
     */
    fun sendOtp(
        activity: Activity,
        isSignup: Boolean,
        phone: String,
        countryCode: String,
        fullName: String = "",
        role: String = "",
        referralCode: String = "",
        isResend: Boolean = false,
    ) {
        if (phone.isBlank()) {
            _state.update { it.copy(error = "Enter your phone number") }
            return
        }
        if (isSignup && fullName.isBlank()) {
            _state.update { it.copy(error = "Enter your full name") }
            return
        }

        pendingSignup = if (isSignup) PendingSignup(fullName.trim(), role, referralCode.trim()) else null
        val e164 = toE164(phone, countryCode)
        _state.update { it.copy(loading = true, error = null, mismatchedRole = null) }

        viewModelScope.launch {
            // Firebase sends the code from the device, so the backend never sees
            // the number first. On login, ask it whether an account exists before
            // spending an SMS — otherwise an unregistered number only finds out
            // after typing a code it can never redeem.
            if (!isSignup) {
                // Usually already answered: prefetchLookup started this while the
                // number was being typed.
                val preflight = lookupFor(e164).await()
                // A failure is never reused — the next attempt has to retry it.
                if (preflight is ApiResult.Error) lookup = null
                when (preflight) {
                    is ApiResult.Error -> {
                        // A 404 is the one failure that isn't about this number:
                        // the server is up but predates /auth/phone/lookup. An
                        // installed app outlives any single backend deploy, so
                        // treat the pre-flight as the optimisation it is and go
                        // on to send the code — /auth/firebase still rejects
                        // unregistered and suspended numbers after the OTP, so
                        // this costs an avoidable SMS, not a security check.
                        // Every other failure fails closed: the token exchange
                        // would not have worked either.
                        if (preflight.code != 404) {
                            _state.update { it.copy(loading = false, error = preflight.message) }
                            return@launch
                        }
                    }
                    is ApiResult.Success -> {
                        // Only enforce roles the picker can actually express. The
                        // backend also has VENDOR/LANDOWNER/REFERRAL, which have no
                        // pill — blocking those would lock them out of the app.
                        val actual = roleFor(preflight.data.role)
                        val mismatched = actual != null &&
                            role.isNotBlank() &&
                            !actual.value.equals(role, ignoreCase = true)
                        val error = when {
                            !preflight.data.exists ->
                                "No account found for this number. Create an account first."
                            preflight.data.suspended ->
                                "Account suspended. Please contact support."
                            mismatched ->
                                "This number is registered as a ${actual!!.label} account."
                            else -> null
                        }
                        if (error != null) {
                            _state.update {
                                it.copy(
                                    loading = false,
                                    error = error,
                                    mismatchedRole = if (mismatched) actual!!.value else null,
                                )
                            }
                            return@launch
                        }
                    }
                }
            }

            FirebasePhoneAuth.sendCode(
                activity = activity,
                e164Phone = e164,
                resendToken = if (isResend) resendToken else null,
            ) { event ->
                when (event) {
                    is PhoneVerification.CodeSent -> {
                        verificationId = event.verificationId
                        resendToken = event.resendToken
                        _state.update {
                            it.copy(loading = false, step = AuthStep.OTP, maskedPhone = maskPhone(e164))
                        }
                        startResendCountdown()
                    }
                    // Firebase resolved the number on its own — no code to type.
                    is PhoneVerification.Completed -> exchange(event.credential, isSignup)
                    is PhoneVerification.Failed ->
                        _state.update { it.copy(loading = false, error = event.message) }
                }
            }
        }
    }

    fun verifyLogin(otp: String) = submitCode(otp, isSignup = false)

    fun verifySignup(otp: String) = submitCode(otp, isSignup = true)

    private fun submitCode(otp: String, isSignup: Boolean) {
        val id = verificationId
        if (id == null) {
            _state.update { it.copy(error = "Request a code first") }
            return
        }
        if (otp.isBlank()) {
            _state.update { it.copy(error = "Enter the 6-digit code") }
            return
        }
        exchange(FirebasePhoneAuth.credentialFor(id, otp.trim()), isSignup)
    }

    /** Check the credential with Firebase, then trade its ID token for a session. */
    private fun exchange(credential: PhoneAuthCredential, isSignup: Boolean) {
        // Auto-retrieval races the user: onVerificationCompleted can land while
        // the code they typed is already in flight, and on signup a second
        // exchange is a second account-creation attempt. First one wins.
        if (exchanging) return
        exchanging = true
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val tokenResult = FirebasePhoneAuth.idTokenFor(credential)
            val idToken = tokenResult.getOrElse { e ->
                exchanging = false
                _state.update { it.copy(loading = false, error = FirebasePhoneAuth.errorMessage(e)) }
                return@launch
            }
            val signup = pendingSignup
            verify {
                repository.firebaseExchange(
                    idToken = idToken,
                    isSignup = isSignup,
                    fullName = signup?.fullName,
                    role = signup?.role,
                    referralCode = signup?.referralCode,
                )
            }
        }
    }

    private fun toE164(phone: String, countryCode: String): String {
        val digits = phone.filter { it.isDigit() }
        if (phone.trim().startsWith("+")) return "+$digits"
        val cc = countryCode.filter { it.isDigit() }.ifBlank { "91" }
        return "+$cc$digits"
    }

    private fun maskPhone(e164: String): String =
        if (e164.length <= 4) e164
        else e164.take(3) + "*".repeat((e164.length - 7).coerceAtLeast(0)) + e164.takeLast(4)

    /** Already inside a coroutine — suspends rather than launching its own. */
    private suspend fun verify(block: suspend () -> ApiResult<com.dealio.app.data.api.AuthData>) {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = block()) {
            is ApiResult.Success -> {
                // Register this device for push now that we're authenticated.
                com.dealio.app.push.Push.ensureRegistered(getApplication())
                _state.update { it.copy(loading = false, loggedInUser = result.data.user) }
            }
            // Released only on failure. After a success the screen navigates away
            // and a late auto-retrieval must not open a second session.
            is ApiResult.Error -> {
                exchanging = false
                _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun backToDetails() {
        countdownJob?.cancel()
        // Drop the in-flight verification too — editing the number must not let a
        // code minted for the previous one still be submitted.
        verificationId = null
        resendToken = null
        exchanging = false
        _state.update {
            it.copy(
                step = AuthStep.DETAILS,
                error = null,
                demoCode = null,
                resendSecondsLeft = 0,
                mismatchedRole = null,
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null, mismatchedRole = null) }
    }

    private fun startResendCountdown(seconds: Int = 30) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _state.update { it.copy(resendSecondsLeft = seconds) }
            repeat(seconds) {
                delay(1_000)
                _state.update { s -> s.copy(resendSecondsLeft = (s.resendSecondsLeft - 1).coerceAtLeast(0)) }
            }
        }
    }

    private companion object {
        /**
         * How long a prefetched lookup stays reusable. Long enough to cover the
         * typing-to-tap gap it exists for, short enough that an account created
         * or suspended meanwhile isn't remembered wrongly.
         */
        const val LOOKUP_TTL_MS = 60_000L
    }
}
