package com.dealio.app.push

import android.content.Context
import android.util.Log
import com.dealio.app.data.TokenStore
import com.dealio.app.data.api.ApiClient
import com.dealio.app.data.api.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Notification channel id — must match the backend's android.notification.channelId. */
const val DEFAULT_CHANNEL_ID = "dealio_default"

private const val TAG = "DealioPush"

/**
 * Registers this device's FCM token with the backend so the user can receive
 * pushes. Best-effort and only when logged in (the endpoint requires auth).
 */
object Push {

    /**
     * Fetches the current FCM token (logging it for debugging) and registers it
     * with the backend if the user is logged in. A token-fetch failure here means
     * Firebase/Google Play Services couldn't mint a token — pushes can't work.
     */
    fun ensureRegistered(context: Context) {
        val app = context.applicationContext
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM registration token: $token")
                if (token.isNullOrBlank()) return@addOnSuccessListener
                if (TokenStore(app).isLoggedIn) registerToken(app, token)
                else Log.d(TAG, "Not logged in yet — token will be registered after login")
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to fetch FCM token", e) }
    }

    /** Sends a specific FCM token to the backend (used by onNewToken and after login). */
    fun registerToken(context: Context, token: String) {
        val app = context.applicationContext
        ApiClient.init(app) // ensure the auth header is wired even if MainActivity hasn't run
        if (!TokenStore(app).isLoggedIn) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ApiClient.authApi.registerDeviceToken(DeviceTokenRequest(token, "android")) }
        }
    }
}
