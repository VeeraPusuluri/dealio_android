package com.dealio.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * Branded hero for the customer tabs.
 *
 * Explore and Profile already opened on the navy→teal gradient while Visits,
 * Journey and Saved opened on a plain white bar, so moving between tabs felt like
 * moving between two different apps. This is that gradient as a shared piece.
 *
 * [stats] renders as translucent pills under the title — the at-a-glance numbers
 * for the tab (e.g. "3 upcoming", "₹1.2 Cr in play").
 */
@Composable
fun CustomerHeader(
    title: String,
    subtitle: String? = null,
    stats: List<Pair<String, String>> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(NavyTealGradient),
    ) {
        // Soft highlight so the flat gradient reads with a bit of depth.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(220.dp)
                .background(
                    Brush.radialGradient(listOf(TealBright.copy(alpha = 0.20f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Column(Modifier.systemBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, color = TealBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                trailing?.invoke()
            }
            if (stats.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEach { (value, label) -> StatPill(value, label) }
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(
        Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

/**
 * Empty state with somewhere to go. The plain icon-and-text version left the
 * blank tabs as dead ends — every customer tab is empty until the buyer does
 * something on Explore, so each one now points there.
 */
@Composable
fun CustomerEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .background(
                    Brush.linearGradient(listOf(Teal.copy(alpha = 0.16f), TealBright.copy(alpha = 0.06f))),
                    RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Teal, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(18.dp))
        Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp,
                    vertical = 12.dp,
                ),
            ) {
                Text(actionLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
