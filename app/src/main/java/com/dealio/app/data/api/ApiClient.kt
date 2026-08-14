package com.dealio.app.data.api

import android.content.Context
import com.dealio.app.BuildConfig
import com.dealio.app.data.Session
import com.dealio.app.data.TokenStore
import okhttp3.OkHttpClient
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

                // A 401 on a request we signed means the token is spent — expired,
                // or its session revoked from another device. Ending it here, in
                // the one place every call passes through, is what stops each
                // screen from stranding the user on the server's raw message.
                val signIn = SIGN_IN_PATHS.any { request.url.encodedPath.contains(it) }
                if (authed && response.code == 401 && !signIn) {
                    tokenStore?.clear()
                    Session.end()
                }
                response
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
