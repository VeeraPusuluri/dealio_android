package com.dealio.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Brand gradients ─────────────────────────────────────────────────────────
// Mirrors the web app's hero treatments (deep navy field, teal accents, warm CTA).

/** Deep navy hero gradient — used behind page headers and feature cards. */
val NavyHeroGradient: Brush
    get() = Brush.verticalGradient(listOf(NavyDeep, NavyMid))

/** Diagonal navy gradient with a hint of teal at the corner — richer hero look. */
val NavyTealGradient: Brush
    get() = Brush.linearGradient(listOf(NavyDeep, NavyPrimary, TealDeep))

/** Bright teal call-to-action gradient. */
val TealGradient: Brush
    get() = Brush.linearGradient(listOf(Teal, TealBright))

/** Warm accent gradient for highlight CTAs / earnings. */
val OrangeGradient: Brush
    get() = Brush.linearGradient(listOf(Color(0xFFFF9A3D), Orange))

/** Soft surface wash used behind tinted icon chips. */
fun tintBrush(color: Color): Brush =
    Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.06f)))

// ─── Elevation ───────────────────────────────────────────────────────────────
// A single, brand-tinted soft shadow so cards read with depth instead of a flat
// outline. Navy-tinted spot keeps shadows from looking muddy on the mist field.

/** Soft, brand-tinted card shadow. Apply before background/clip. */
fun Modifier.softShadow(
    elevation: Dp = 10.dp,
    radius: Dp = 22.dp,
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(radius),
    ambientColor = NavyDeep.copy(alpha = 0.10f),
    spotColor = NavyDeep.copy(alpha = 0.16f),
)

/** Lighter shadow for smaller tiles / chips. */
fun Modifier.subtleShadow(
    elevation: Dp = 6.dp,
    radius: Dp = 16.dp,
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(radius),
    ambientColor = NavyDeep.copy(alpha = 0.06f),
    spotColor = NavyDeep.copy(alpha = 0.12f),
)

// ─── Extra accent tokens ─────────────────────────────────────────────────────
val SurfaceTintTeal = Color(0xFFEAFAFC)
val SurfaceTintOrange = Color(0xFFFFF3E9)
val SurfaceTintNavy = Color(0xFFEEF2F8)
val GoldStar = Color(0xFFF5B301)

@Composable
fun rememberBrandShapes() = Unit
