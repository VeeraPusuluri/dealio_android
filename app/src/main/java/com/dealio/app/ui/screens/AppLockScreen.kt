package com.dealio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.TealDeep

/** Full-screen cover shown while the biometric app-lock is engaged. */
@Composable
fun AppLockScreen(onUnlock: () -> Unit, onSignOut: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(NavyDeep, NavyMid, TealDeep),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(20.dp)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Deal") }
                    withStyle(SpanStyle(color = TealBright)) { append("io") }
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(6.dp)
            Text(
                "Locked for your security",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )

            Spacer(28.dp)
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Unlock", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(4.dp)
            TextButton(onClick = onSignOut) {
                Text("Sign out", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Spacer(h: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(h))
}
