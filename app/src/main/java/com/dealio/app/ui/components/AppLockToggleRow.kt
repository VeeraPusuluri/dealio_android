package com.dealio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.dealio.app.data.AppLockStore
import com.dealio.app.data.canUseAppLock
import com.dealio.app.data.promptAppLock
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealDeep
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * Settings row that toggles the biometric app-lock. Turning it on prompts for
 * authentication first; drop it into any role's profile/settings screen.
 */
@Composable
fun AppLockToggleRow() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val store = remember { AppLockStore(context) }
    val available = remember { canUseAppLock(context) }
    var enabled by remember { mutableStateOf(store.enabled) }

    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Lock,
            null,
            tint = Color.White,
            modifier = Modifier
                .size(30.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Teal, TealDeep)),
                    RoundedCornerShape(8.dp),
                )
                .padding(6.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("App Lock", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                if (available) "Require fingerprint, face, or PIN to open Dealio"
                else "Set up a fingerprint, face, or screen lock first",
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = enabled,
            enabled = available,
            onCheckedChange = { want ->
                if (want) {
                    activity?.let {
                        promptAppLock(
                            it,
                            title = "Enable app lock",
                            subtitle = "Confirm it's you to turn on app lock",
                            onSuccess = { store.enabled = true; enabled = true },
                        )
                    }
                } else {
                    store.enabled = false
                    enabled = false
                }
            },
            colors = SwitchDefaults.colors(checkedTrackColor = Teal),
        )
    }
}
