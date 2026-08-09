package com.dealio.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.dealio.app.data.api.ApiClient
import com.dealio.app.ui.navigation.DealioNavHost
import com.dealio.app.ui.theme.DealioTheme

class MainActivity : FragmentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Every hero in the app already reserves the status-bar inset itself and
        // the floating nav pill pads for the navigation bar, but nothing ever
        // opted the window in. Android 15+ forces edge-to-edge on a targetSdk 36
        // app, so the layouts got what they expected there and nowhere else: on
        // Android 14 and below the hero stopped short of a separately-coloured
        // status bar. Asking for it explicitly makes every API level agree.
        //
        // The status bar is transparent with `dark` icons — dark meaning "for a
        // dark background", i.e. light glyphs, which is what the navy hero every
        // portal tab opens on needs. The default `auto` would tint them for the
        // system light/dark setting instead and put dark glyphs on the navy.
        // Screens with a white top bar override it with [DarkStatusBarIcons].
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        ApiClient.init(applicationContext)
        requestNotificationPermission()
        setContent {
            DealioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DealioNavHost()
                }
            }
        }
    }

    /** Android 13+ requires runtime consent to post notifications. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
