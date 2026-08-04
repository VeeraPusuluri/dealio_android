package com.dealio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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

/** Radius of the corner glow on the portal hero. */
private val GLOW_RADIUS = 110.dp

/**
 * The navy→teal surface every portal tab opens on.
 *
 * Exposed on its own so a tab that needs bespoke hero content (the CP overview's
 * avatar-and-tier welcome, say) sits on exactly the same gradient, radius and
 * highlight as the title-and-stats tabs, instead of hand-rolling a near-match.
 */
@Composable
fun PortalHeaderSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(NavyTealGradient),
    ) {
        // Soft highlight so the flat gradient reads with a bit of depth. Drawn against
        // the parent's own size rather than as a 220.dp child — as a sized child it set
        // a 220.dp floor on the hero, so a short tab (a title with no stat pills) got
        // the same tall bar as a full one, padded out with dead navy.
        Box(
            Modifier.matchParentSize().drawBehind {
                val r = GLOW_RADIUS.toPx()
                val centre = Offset(size.width - r, r)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(TealBright.copy(alpha = 0.20f), Color.Transparent),
                        center = centre,
                        radius = r,
                    ),
                    radius = r,
                    center = centre,
                )
            },
        )
        Column(
            // Top-anchored, so only the status bar matters. systemBarsPadding() also
            // reserved the navigation-bar inset as padding under the title.
            Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            content = content,
        )
    }
}

/**
 * Branded hero for the top-level tabs of the customer and channel-partner portals.
 *
 * Both portals had tabs that opened on the gradient and tabs that opened on a
 * plain white bar, so moving between them felt like moving between two different
 * apps. This is that gradient as one shared piece.
 *
 * [stats] renders as translucent pills under the title — the at-a-glance numbers
 * for the tab (e.g. "3 upcoming", "₹1.2 Cr in play").
 */
@Composable
fun PortalHeader(
    title: String,
    subtitle: String? = null,
    stats: List<Pair<String, String>> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
) {
    PortalHeaderSurface {
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
                stats.forEach { (value, label) -> PortalStatPill(value, label) }
            }
        }
    }
}

@Composable
private fun PortalStatPill(value: String, label: String) {
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
 * Empty state with somewhere to go. The plain icon-and-text version left blank
 * tabs as dead ends — most tabs in both portals are empty until the user does
 * something elsewhere, so each one now points at that place.
 */
@Composable
fun PortalEmptyState(
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
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(actionLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
