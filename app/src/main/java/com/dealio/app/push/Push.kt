package com.dealio.app.push

import android.content.Context
import com.dealio.app.data.TokenStore
import com.dealio.app.data.api.ApiClient
import com.dealio.app.data.api.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Notification channel id — must match the backend's android.notification.channelId. */
const val DEFAULT_CHANNEL_ID = "dealio_default"

/**
 * Registers this device's FCM token with the backend so the user can receive
 * pushes. Best-effort and only when logged in (the endpoint requires auth).
 */
object Push {

    /** Fetches the current FCM token and registers it, if the user is logged in. */
    fun ensureRegistered(context: Context) {
        val app = context.applicationContext
        if (!TokenStore(app).isLoggedIn) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) registerToken(app, token)
        }
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
