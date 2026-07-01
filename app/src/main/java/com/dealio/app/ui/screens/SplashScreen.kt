package com.dealio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.dealio.app.R
import com.dealio.app.ui.theme.NavyDeep
import kotlinx.coroutines.delay

/**
 * Launch splash — plays the designed "Dealio Splash" Lottie animation
 * (`res/raw/dealio_splash.json`): the app mark assembles, the wordmark builds
 * in, and the "Find your next deal" tagline wipes in over a navy backdrop.
 *
 * [onFinished] fires when the animation completes (or after a safety timeout if
 * the composition somehow fails to load), matching the iOS splash behaviour.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.dealio_splash),
    )
    // Play through exactly once at native speed.
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
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
        delay(3_500)
        finishOnce()
    }

    LottieAnimation(
        composition = composition,
        progress = { progress },
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            // The JSON carries its own navy background; this matches it so there's
            // no flash before the composition paints its first frame.
            .background(NavyDeep),
    )
}
