package com.dealio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.TextSecondary

/**
 * Branded shell for the auth screens: a navy gradient hero carrying the Dealio
 * mark, the active role, an eyebrow/headline/subtitle block and a step track,
 * flowing into a floating white form card that overlaps it. A footer is pinned
 * to the bottom so short steps read as deliberate breathing room rather than
 * empty space. The [content] slot holds the step-specific fields and actions.
 *
 * [accentOnDark] tints the hero glow, eyebrow and step track — the role pickers
 * pass the selected role's color through it, so choosing "Builder" re-lights the
 * whole page in that role's teal. Pass a color already lifted for the navy (see
 * `Color.onNavy()`); the raw card colors go muddy here.
 *
 * [heroTrailing] is an optional slot on the logo row, used for the role chip.
 */
@Composable
fun AuthScaffold(
    eyebrow: String,
    headline: String,
    subtitle: String,
    accentOnDark: Color = TealBright,
    step: Int = 1,
    totalSteps: Int = 2,
    highlights: List<String> = emptyList(),
    heroTrailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent by animateColorAsState(accentOnDark, label = "authAccent")

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        // Force the body to be at least a screen tall so the bottom footer can be
        // pushed down with a weighted spacer; taller content just scrolls.
        val minBodyHeight = maxHeight

        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.fillMaxWidth().heightIn(min = minBodyHeight)) {

                // ── Brand hero ──
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NavyDeep, NavyMid, NavyDeep),
                                start = Offset(0f, 0f),
                                end = Offset.Infinite,
                            ),
                        ),
                ) {
                    // Role-tinted glow + a softer counterweight, for depth on the navy.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-70).dp)
                            .size(270.dp)
                            .background(
                                Brush.radialGradient(listOf(accent.copy(alpha = 0.40f), Color.Transparent)),
                                CircleShape,
                            ),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-55).dp, y = 45.dp)
                            .size(210.dp)
                            .background(
                                Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)),
                                CircleShape,
                            ),
                    )

                    Column(
                        // Only the status bar sits over this hero; the footer below
                        // takes the navigation-bar inset (see navigationBarsPadding).
                        Modifier
                            .statusBarsPadding()
                            .padding(start = 26.dp, end = 26.dp, top = 22.dp, bottom = 52.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DealioLogo(onDark = true)
                            Spacer(Modifier.weight(1f))
                            heroTrailing?.invoke()
                        }
                        Spacer(Modifier.height(30.dp))
                        Text(
                            eyebrow.uppercase(),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            headline,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 36.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            subtitle,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                        StepTrack(step = step, totalSteps = totalSteps, accentOnDark = accent)
                        if (highlights.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                highlights.forEach { TrustChip(it) }
                            }
                        }
                    }
                }

                // ── Floating form card (overlaps the hero) ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-30).dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                        content = content,
                    )
                }

                Spacer(Modifier.weight(1f))

                // ── Footer pinned to the bottom ──
                Text(
                    "By continuing you agree to our Terms & Privacy Policy.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 30.dp, end = 30.dp, top = 8.dp, bottom = 20.dp),
                )
            }
        }
    }
}

/** Translucent pill used in the hero to surface key selling points. */
@Composable
private fun TrustChip(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
