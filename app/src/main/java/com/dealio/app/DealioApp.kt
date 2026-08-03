package com.dealio.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.dealio.app.data.api.ApiClient
import com.dealio.app.push.DEFAULT_CHANNEL_ID
import com.dealio.app.push.Push

/** Application entry point — sets up the push notification channel + API client. */
class DealioApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        createNotificationChannel()
        // If already logged in from a previous session, (re)register the FCM token.
        Push.ensureRegistered(this)
    }

    /**
     * Project photos come from CloudFront with `Cache-Control: public, max-age=0`,
     * which Coil honours by default — so every cover image counted as stale and was
     * re-fetched each time a list scrolled or a screen was reopened, all the way to
     * origin (CloudFront reports a miss on them too).
     *
     * Ignoring the header and caching on our own terms is safe here because upload
     * URLs are timestamped (`.../1785497030749-asretieyn49.png`): replacing a photo
     * produces a new URL, so a cached entry can never go stale under us.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(MAX_IMAGE_DISK_CACHE_BYTES)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(180)
        .build()

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

    private companion object {
        const val MAX_IMAGE_DISK_CACHE_BYTES = 150L * 1024 * 1024
    }
}
