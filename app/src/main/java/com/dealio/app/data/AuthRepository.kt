package com.dealio.app.data

import com.dealio.app.data.api.ApiEnvelope
import com.dealio.app.data.api.AuthApi
import com.dealio.app.data.api.AuthData
import com.dealio.app.data.api.AuthUser
import com.dealio.app.data.api.FirebaseAuthRequest
import com.dealio.app.data.api.PhoneLookupData
import com.dealio.app.data.api.PhoneLookupRequest
import com.dealio.app.data.api.SendOtpData
import com.dealio.app.data.api.SendOtpRequest
import com.dealio.app.data.api.VerifyLoginRequest
import com.dealio.app.data.api.VerifySignupRequest
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * [code] is the HTTP status when the failure came from a response, and null
     * when it came from the transport (no connection, malformed body). Callers
     * that need to tell "the server said no" apart from "this build of the
     * server has never heard of that route" read it — see the phone-lookup
     * pre-flight in AuthViewModel.
     */
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
) {
    private val gson = Gson()

    suspend fun sendOtp(isSignup: Boolean, phone: String, countryCode: String): ApiResult<SendOtpData> =
        call {
            val body = SendOtpRequest(phone = phone, countryCode = countryCode)
            if (isSignup) api.sendSignupOtp(body) else api.sendLoginOtp(body)
        }

    /**
     * Sign-in pre-flight — whether [phone] has an account, and its role.
     * Firebase sends the OTP from the device, so this is the only chance to
     * reject an unknown number before an SMS is spent on it.
     */
    suspend fun phoneLookup(phone: String): ApiResult<PhoneLookupData> =
        call { api.phoneLookup(PhoneLookupRequest(phone = phone)) }

    suspend fun verifyLogin(phone: String, otp: String): ApiResult<AuthData> =
        call { api.verifyLoginOtp(VerifyLoginRequest(phone = phone, otp = otp)) }
            .also { if (it is ApiResult.Success) tokenStore.save(it.data) }

    suspend fun verifySignup(
        phone: String,
        otp: String,
        fullName: String,
        role: String,
        referralCode: String?,
    ): ApiResult<AuthData> =
        call {
            api.verifySignupOtp(
                VerifySignupRequest(
                    phone = phone,
                    otp = otp,
                    fullName = fullName,
                    role = role,
                    referralCode = referralCode?.takeIf { it.isNotBlank() },
                )
            )
        }.also { if (it is ApiResult.Success) tokenStore.save(it.data) }

    /**
     * Trades a Firebase ID token (proof the caller controls the phone number)
     * for a Dealio session. Used by the Firebase OTP flow in place of
     * [verifyLogin] / [verifySignup].
     */
    suspend fun firebaseExchange(
        idToken: String,
        isSignup: Boolean,
        fullName: String? = null,
        role: String? = null,
        referralCode: String? = null,
    ): ApiResult<AuthData> =
        call {
            api.firebaseAuth(
                FirebaseAuthRequest(
                    idToken = idToken,
                    mode = if (isSignup) "signup" else "login",
                    fullName = fullName?.takeIf { it.isNotBlank() },
                    role = role?.takeIf { it.isNotBlank() },
                    referralCode = referralCode?.takeIf { it.isNotBlank() },
                )
            )
        }.also { if (it is ApiResult.Success) tokenStore.save(it.data) }

    // ── Profile picture ──────────────────────────────────────────────────────
    // Both write straight back into the token store: it holds the app's only
    // copy of the signed-in user between logins, so a picture that changed on
    // the server but not here would come back the moment a screen re-read it.

    suspend fun uploadAvatar(bytes: ByteArray, fileName: String, mime: String): ApiResult<AuthUser> {
        val part = MultipartBody.Part.createFormData(
            "file", fileName, bytes.toRequestBody(mime.toMediaTypeOrNull()),
        )
        return call { api.uploadAvatar(part) }
            .also { if (it is ApiResult.Success) tokenStore.avatarUrl = it.data.avatarUrl }
    }

    suspend fun removeAvatar(): ApiResult<AuthUser> =
        call { api.removeAvatar() }
            .also { if (it is ApiResult.Success) tokenStore.avatarUrl = null }

    /** Unwraps the `{ ok, message, data }` envelope and normalizes failures. */
    private suspend fun <T> call(block: suspend () -> Response<ApiEnvelope<T>>): ApiResult<T> {
        return try {
            val response = block()
            val envelope = if (response.isSuccessful) {
                response.body()
            } else {
                // Error responses carry the envelope too: { ok: false, message }
                response.errorBody()?.string()?.let { raw ->
                    runCatching { gson.fromJson(raw, ApiEnvelope::class.java) }.getOrNull()
                }?.let { ApiEnvelope<T>(ok = it.ok, message = it.message) }
            }
            val status = response.code()
            when {
                envelope == null -> ApiResult.Error("Unexpected server response (HTTP $status)", status)
                !envelope.ok -> ApiResult.Error(envelope.message ?: "Request failed", status)
                envelope.data == null -> ApiResult.Error(envelope.message ?: "Empty response from server", status)
                else -> ApiResult.Success(envelope.data)
            }
        } catch (e: IOException) {
            ApiResult.Error("Can't reach the Dealio server. Check your connection and try again.")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Something went wrong")
        }
    }
}
