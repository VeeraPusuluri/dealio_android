package com.dealio.app.ui.screens

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.dealio.app.R
import com.dealio.app.ui.theme.JakartaFamily
import com.dealio.app.ui.theme.NavyDeep
import kotlinx.coroutines.delay

// The Lottie was authored at this size; the tagline is placed by mapping
// composition coordinates onto the screen, so it lands where the artwork expects
// it on any device rather than at a guessed fraction of the height.
private const val COMP_W = 1080f
private const val COMP_H = 1920f

/** Where the tagline settles in composition space (the old image layer's y). */
private const val TAGLINE_Y = 1121f

/** The clip is 150 frames; the tagline used to wipe in across these. */
private const val TAGLINE_IN_START = 85f / 150f
private const val TAGLINE_IN_END = 110f / 150f

private val TAGLINE_INK = Color(0xFFC1DAF4)
private val TAGLINE_DOT = Color(0xFF1FF3FF)

/**
 * Launch splash — plays the designed "Dealio Splash" Lottie animation
 * (`res/raw/dealio_splash.json`): the app mark assembles and the wordmark builds
 * in over a navy backdrop.
 *
 * The tagline is **not** part of the animation. It used to be a 525×21 bitmap
 * layer, which the composition then scaled up by ~1.6 on a modern phone and left
 * visibly soft. It is drawn here as real text instead — crisp at any density,
 * and the letter-spacing and colours are editable without re-exporting the clip.
 *
 * [onFinished] fires when the animation completes (or after a safety timeout if
 * the composition somehow fails to load), matching the iOS splash behaviour.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.dealio_splash),
    )
    // Play through exactly once, at double speed. The clip is the first thing
    // between tapping the icon and using the app, and at native speed it held
    // that gap open for its full length every single launch. Doubling keeps the
    // whole animation — mark assembling, wordmark building — and halves the wait.
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
        speed = SPLASH_SPEED,
    )

    // Fire onFinished at most once — the callback pops the splash off the back
    // stack, so a second call (e.g. safety timeout after the clip already ended)
    // must be a no-op.
    val latestOnFinished by rememberUpdatedState(onFinished)
    val finished = remember { mutableStateOf(false) }
    fun finishOnce() {
        if (!finished.value) {
            finished.value = true
            latestOnFinished()
        }
    }

    // Advance when the clip has played to the end. A safety timeout guards against
    // the composition failing to load so the app never gets stuck on the splash.
    LaunchedEffect(progress) {
        if (composition != null && progress >= 1f) finishOnce()
    }
    LaunchedEffect(Unit) {
        delay(SPLASH_TIMEOUT_MS)
        finishOnce()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // The JSON carries its own navy background; this matches it so there's
            // no flash before the composition paints its first frame.
            .background(NavyDeep),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // ContentScale.Crop scales by whichever axis needs more and centres the
        // result, so the composition's centre is the screen's centre. Offsetting
        // from there keeps the tagline locked to the wordmark on every aspect
        // ratio, instead of drifting the way a fixed fraction would.
        val density = LocalDensity.current
        val scale = maxOf(constraints.maxWidth / COMP_W, constraints.maxHeight / COMP_H)
        val offsetY = with(density) { ((TAGLINE_Y - COMP_H / 2f) * scale).toDp() }

        // Fade and lift over the same frames the old layer used, so the timing of
        // the sequence is unchanged.
        val t = ((progress - TAGLINE_IN_START) / (TAGLINE_IN_END - TAGLINE_IN_START))
            .coerceIn(0f, 1f)
        val eased = LinearOutSlowInEasing.transform(t)

        Text(
            text = taglineText(),
            fontFamily = JakartaFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            // Wide tracking is what made the original read as a mark rather than
            // a sentence; em units keep it proportional if the size ever changes.
            letterSpacing = 0.34.em,
            color = TAGLINE_INK,
            modifier = Modifier
                .align(Alignment.Center)
                // x: the tracking leaves a trailing gap after the last letter, so
                // nudge back by half of it to centre the line optically.
                .offset(x = 2.dp, y = offsetY + 8.dp * (1f - eased))
                .alpha(eased),
        )
    }
}

/** Playback rate for the splash clip. 1f is the exported speed. */
private const val SPLASH_SPEED = 2f

/**
 * Safety net for a composition that never loads, so the app cannot be stuck on
 * the splash. Not the normal exit — that is the clip finishing, which at
 * [SPLASH_SPEED] happens well inside this.
 */
private const val SPLASH_TIMEOUT_MS = 1_800L

/** "REAL ESTATE · MADE SIMPLE", with the separator in the brand cyan. */
private fun taglineText(): AnnotatedString = buildAnnotatedString {
    append("REAL ESTATE")
    withStyle(SpanStyle(color = TAGLINE_DOT, fontWeight = FontWeight.Bold)) { append("  ·  ") }
    append("MADE SIMPLE")
}
