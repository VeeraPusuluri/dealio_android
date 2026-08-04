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

// ─── Portal hero ─────────────────────────────────────────────────────────────
//
// The hero used to run navy → bright teal across a wide span of hue, with a cyan
// glow on top of that. Two strong colours and a spotlight is the gradient every
// app ships with, and next to the credential card — deep navy, one restrained
// sheen — it looked like a different product.
//
// So the hero is cut from the same material: it stays inside the navy family and
// lifts only slightly toward slate at the far corner. Depth comes from value
// rather than from a second hue.

private val HeroBase = Color(0xFF0A1A2B)
private val HeroMid = Color(0xFF14304A)
private val HeroLift = Color(0xFF17414F)

/** The surface every portal tab opens on. */
val PortalHeroGradient: Brush
    get() = Brush.linearGradient(listOf(HeroBase, HeroMid, HeroLift))

/**
 * Eyebrow text on the hero — "Welcome back", tab subtitles.
 *
 * A muted aqua rather than [TealBright]: neon cyan on deep navy is legible but
 * reads as a demo, and it competed with the tier foil on the credential.
 */
val HeroAccent = Color(0xFF8FBFD0)

/** Corner highlight on the hero — a wash, not a spotlight. */
val HeroHighlight = Color(0xFF5E9AB0)

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
