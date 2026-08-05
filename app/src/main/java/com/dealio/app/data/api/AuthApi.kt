package com.dealio.app.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Auth endpoints of the Dealio backend (under /api/auth).
 * All responses use the `{ ok, message, data }` envelope.
 */
interface AuthApi {

    @POST("auth/login/phone/send-otp")
    suspend fun sendLoginOtp(@Body body: SendOtpRequest): Response<ApiEnvelope<SendOtpData>>

    @POST("auth/login/phone/verify-otp")
    suspend fun verifyLoginOtp(@Body body: VerifyLoginRequest): Response<ApiEnvelope<AuthData>>

    @POST("auth/signup/phone/send-otp")
    suspend fun sendSignupOtp(@Body body: SendOtpRequest): Response<ApiEnvelope<SendOtpData>>

    @POST("auth/signup/phone/verify-otp")
    suspend fun verifySignupOtp(@Body body: VerifySignupRequest): Response<ApiEnvelope<AuthData>>

    /**
     * Sign-in pre-flight: is this number registered, and under which role?
     * Firebase sends the OTP from the device, so this is the only chance to
     * reject an unknown number before an SMS is spent on it.
     */
    @POST("auth/phone/lookup")
    suspend fun phoneLookup(@Body body: PhoneLookupRequest): Response<ApiEnvelope<PhoneLookupData>>

    /**
     * Exchanges a Firebase ID token (from the phone-OTP flow) for a Dealio
     * session. `mode = "signup"` may create the account and needs a role;
     * anything else is treated as a login and requires an existing account.
     */
    @POST("auth/firebase")
    suspend fun firebaseAuth(@Body body: FirebaseAuthRequest): Response<ApiEnvelope<AuthData>>

    /** Registers this device's FCM token for push notifications (requires auth). */
    @POST("auth/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): Response<ApiEnvelope<Unit>>

    // ── Profile picture ──────────────────────────────────────────────────────
    // Whoever holds the token; there is no user id in the path by design.

    @Multipart
    @POST("auth/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<ApiEnvelope<AuthUser>>

    @DELETE("auth/me/avatar")
    suspend fun removeAvatar(): Response<ApiEnvelope<AuthUser>>
}

// ── Requests ─────────────────────────────────────────────────────────────────

data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android",
)

data class SendOtpRequest(
    val phone: String,
    val countryCode: String? = null,
)

data class VerifyLoginRequest(
    val phone: String,
    val otp: String,
)

data class PhoneLookupRequest(
    val phone: String,
)

data class VerifySignupRequest(
    val phone: String,
    val otp: String,
    val fullName: String,
    val role: String,
    val referralCode: String? = null,
)

data class FirebaseAuthRequest(
    val idToken: String,
    /** "login" or "signup". */
    val mode: String,
    val fullName: String? = null,
    val role: String? = null,
    val referralCode: String? = null,
)

// ── Responses ────────────────────────────────────────────────────────────────

data class ApiEnvelope<T>(
    val ok: Boolean,
    val message: String? = null,
    val data: T? = null,
)

data class SendOtpData(
    val message: String? = null,
    val maskedPhone: String? = null,
    /** Echoed by the backend only outside production — handy on the emulator. */
    val demoCode: String? = null,
)

data class PhoneLookupData(
    val exists: Boolean,
    val suspended: Boolean = false,
    /** The account's role, or null when unregistered or suspended. */
    val role: String? = null,
)

data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: AuthUser,
)

data class AuthUser(
    val id: Long,
    val fullName: String?,
    val phone: String,
    val role: String,
    val email: String? = null,
    /**
     * Profile picture, absolute. Null for anyone who has not set one — every
     * screen that draws it falls back to initials rather than a grey silhouette.
     */
    val avatarUrl: String? = null,
)
