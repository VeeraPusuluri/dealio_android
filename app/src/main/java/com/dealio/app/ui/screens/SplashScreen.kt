package com.dealio.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.components.DealioMark
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealBright
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    // Continuous ambient motion so the screen never feels frozen while loading.
    val infinite = rememberInfiniteTransition(label = "ambient")
    val glow by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // Staggered entrance: mark first, then wordmark lifts in, then the tagline.
    val markAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "markAlpha",
    )
    val markScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "markScale",
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "wordAlpha",
    )
    val wordLift by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(600, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "wordLift",
    )
    val tagAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 450, easing = FastOutSlowInEasing),
        label = "tagAlpha",
    )
    val footerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 650, easing = FastOutSlowInEasing),
        label = "footerAlpha",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2_100)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF0E2542), NavyMid))),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient corner glows give the flat navy some depth.
        Box(
            Modifier
                .size(360.dp)
                .align(Alignment.TopStart)
                .offset(x = (-130).dp, y = (-150).dp)
                .background(Brush.radialGradient(listOf(Teal.copy(alpha = 0.22f), Color.Transparent))),
        )
        Box(
            Modifier
                .size(420.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 170.dp)
                .background(Brush.radialGradient(listOf(TealBright.copy(alpha = 0.16f), Color.Transparent))),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pulsing halo sitting behind the mark.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(190.dp)
                        .alpha(glow * markAlpha)
                        .background(
                            Brush.radialGradient(listOf(TealBright.copy(alpha = 0.45f), Color.Transparent)),
                            shape = CircleShape,
                        ),
                )
                Box(
                    Modifier
                        .alpha(markAlpha)
                        .scale(markScale),
                ) {
                    DealioMark(size = 96.dp, cornerRadius = 24.dp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Deal") }
                    withStyle(SpanStyle(color = TealBright)) { append("io") }
                },
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .alpha(wordAlpha)
                    .offset(y = wordLift.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Real estate made simple",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(tagAlpha),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(footerAlpha),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "© 2026 Dealio · Free forever for all roles",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp,
            )
        }
    }
}

