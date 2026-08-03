package com.dealio.app.push

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dealio.app.MainActivity
import com.dealio.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages. When the app is backgrounded, a `notification` payload is
 * shown by the system automatically; this handles the foreground / data-only case
 * and keeps the registered token in sync.
 */
class DealioFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Push.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Dealio"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        // The server sends the persisted Notification row id. Posting under it means
        // FCM's at-least-once redelivery updates the existing tray entry instead of
        // stacking a second copy of the same alert. Fall back to a unique id only
        // when the id is absent, so unrelated alerts still can't overwrite each other.
        val notifId = message.data["notificationId"]?.toIntOrNull()
            ?: System.currentTimeMillis().toInt()
        showNotification(notifId, title, body)
    }

    private fun showNotification(notifId: Int, title: String, body: String) {
        // POST_NOTIFICATIONS may be denied on Android 13+ — bail quietly if so.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(notifId, notification)
    }
}
