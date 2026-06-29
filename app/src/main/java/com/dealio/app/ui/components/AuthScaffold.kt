package com.dealio.app.ui.components

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.NavyPrimary
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.TextSecondary

/**
 * Branded shell for the auth screens: a navy gradient hero (teal glow + trust
 * strip) carrying the Dealio mark and headline, flowing into a floating white
 * form card that overlaps the hero. A footer is pinned to the bottom so short
 * steps read as deliberate breathing room rather than empty space. The [content]
 * slot holds the step-specific fields and actions.
 */
@Composable
fun AuthScaffold(
    headline: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
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
                        .background(Brush.verticalGradient(listOf(NavyDeep, NavyMid, NavyPrimary))),
                ) {
                    // Teal glow + soft orbs add depth and colour to the flat navy.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-70).dp)
                            .size(230.dp)
                            .background(
                                Brush.radialGradient(listOf(TealBright.copy(alpha = 0.24f), Color.Transparent)),
                                CircleShape,
                            ),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-55).dp, y = 45.dp)
                            .size(180.dp)
                            .background(
                                Brush.radialGradient(listOf(TealBright.copy(alpha = 0.10f), Color.Transparent)),
                                CircleShape,
                            ),
                    )

                    Column(
                        Modifier
                            .systemBarsPadding()
                            .padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 54.dp),
                    ) {
                        DealioLogo(onDark = true)
                        Spacer(Modifier.height(36.dp))
                        Text(
                            headline,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            subtitle,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrustChip("Free forever")
                            TrustChip("All roles")
                            TrustChip("RERA-ready")
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
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
