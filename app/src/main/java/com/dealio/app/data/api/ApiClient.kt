package com.dealio.app.data.api

import android.content.Context
import com.dealio.app.BuildConfig
import com.dealio.app.data.Session
import com.dealio.app.data.TokenStore
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * Holds the JWT for the Authorization header, and is wiped when the server
     * rejects it. Wired up in [init] so the authed builder/customer endpoints
     * work after login.
     */
    @Volatile
    private var tokenStore: TokenStore? = null

    /** Call once (from MainActivity) so authed requests carry the JWT. */
    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }

    /**
     * Sign-in endpoints. Their 401s mean "that code/number is no good", not
     * "your session is over", so they must never tear the session down — the
     * Firebase exchange in particular answers 401 for a rejected ID token.
     */
    private val SIGN_IN_PATHS = listOf(
        "auth/login", "auth/signup", "auth/firebase", "auth/phone/lookup",
    )

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = tokenStore?.accessToken
                val authed = !token.isNullOrBlank()
                val request = if (!authed) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                }
                val response = chain.proceed(request)

                // A 401 on a request we signed means this access token is spent.
                // Try the refresh token before giving up on the session: that is
                // the difference between a week-old app renewing itself in the
                // background and dumping its user at the sign-in screen.
                val signIn = SIGN_IN_PATHS.any { request.url.encodedPath.contains(it) }
                if (!authed || response.code != 401 || signIn) {
                    response
                } else {
                    val renewed = renewAccessToken(staleToken = token!!)
                    if (renewed == null) {
                        // Nothing left to sign with. Ending the session here, in
                        // the one place every call passes through, is what stops
                        // each screen from stranding the user on the raw message.
                        tokenStore?.clear()
                        Session.end()
                        response
                    } else {
                        response.close()
                        chain.proceed(
                            request.newBuilder()
                                .header("Authorization", "Bearer $renewed")
                                .build()
                        )
                    }
                }
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

    /**
     * Spends the refresh token for a new access token, returning it, or null if
     * the session is truly over.
     *
     * Runs on its own bare client: the refresh call must not pass back through
     * the interceptor above, or a 401 on the refresh itself would recurse.
     *
     * Synchronized, and takes the token that failed, so that several requests
     * failing at once produce one refresh rather than a stampede — the others
     * find the token already changed and reuse the new one. That matters because
     * the server rotates refresh tokens: a second concurrent refresh would spend
     * a token the first one had already invalidated and needlessly end the
     * session.
     */
    @Synchronized
    private fun renewAccessToken(staleToken: String): String? {
        val store = tokenStore ?: return null

        // Another thread already renewed it while this one waited on the lock.
        val current = store.accessToken
        if (!current.isNullOrBlank() && current != staleToken) return current

        val refreshToken = store.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        val body = gson.toJson(mapOf("refreshToken" to refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "auth/refresh")
            .post(body)
            .build()

        return runCatching {
            bareHttp.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val envelope = gson.fromJson(
                    res.body?.string(), RefreshEnvelope::class.java,
                )
                val data = envelope?.data ?: return@use null
                if (data.accessToken.isBlank() || data.refreshToken.isBlank()) return@use null
                store.saveTokens(data.accessToken, data.refreshToken)
                data.accessToken
            }
        }.getOrNull()
    }

    private val gson by lazy { Gson() }

    /** No interceptors — see [renewAccessToken]. */
    private val bareHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private data class RefreshEnvelope(val ok: Boolean, val data: RefreshData?)

    private data class RefreshData(val accessToken: String, val refreshToken: String)

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    val builderApi: BuilderApi by lazy { retrofit.create(BuilderApi::class.java) }

    val customerApi: CustomerApi by lazy { retrofit.create(CustomerApi::class.java) }

    val cpApi: CpApi by lazy { retrofit.create(CpApi::class.java) }

    // Not role-scoped — the caller comes from the token, so every portal shares it.
    val threadApi: ThreadApi by lazy { retrofit.create(ThreadApi::class.java) }
}
