package com.dealio.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.dealio.app.data.api.ApiClient
import com.dealio.app.push.DEFAULT_CHANNEL_ID
import com.dealio.app.push.Push

/** Application entry point — sets up the push notification channel + API client. */
class DealioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        createNotificationChannel()
        // If already logged in from a previous session, (re)register the FCM token.
        Push.ensureRegistered(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                "Dealio Notifications",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Deal updates, meetings and alerts" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
