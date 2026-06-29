package com.dealio.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dealio.app.R

/** Brand typeface — Plus Jakarta Sans, bundled as static weights in res/font. */
val JakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

// Start from the Material 3 defaults so every style (incl. the ones we don't
// customize) carries the brand font, then override sizes where we want them.
private val base = Typography()

val DealioTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = JakartaFamily),
    displayMedium = base.displayMedium.copy(fontFamily = JakartaFamily),
    displaySmall = base.displaySmall.copy(fontFamily = JakartaFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = JakartaFamily),
    headlineMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = base.headlineSmall.copy(fontFamily = JakartaFamily),
    titleLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = base.titleMedium.copy(fontFamily = JakartaFamily),
    titleSmall = base.titleSmall.copy(fontFamily = JakartaFamily),
    bodyLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = base.bodySmall.copy(fontFamily = JakartaFamily),
    labelLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = base.labelMedium.copy(fontFamily = JakartaFamily),
    labelSmall = base.labelSmall.copy(fontFamily = JakartaFamily),
)
