package com.dealio.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Paints the status-bar icons dark for as long as this screen is composed.
 *
 * The app draws edge-to-edge, so the status bar has no colour of its own — it
 * shows whatever the screen puts underneath it. Every portal tab opens on the
 * navy hero, so light icons are the app-wide default (set once in MainActivity)
 * and only the screens that put a *white* bar up there have to say otherwise.
 * Without this the clock and the signal bars go white-on-white and disappear.
 *
 * Restoring the light setting on dispose is what keeps the default honest: a
 * screen states its own need and leaves the bar as it found it, so the tab you
 * come back to doesn't inherit dark icons onto its navy hero.
 */
@Composable
fun DarkStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = true
        onDispose { controller.isAppearanceLightStatusBars = false }
    }
}

/** Compose hands out a themed ContextWrapper, not the Activity itself. */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
