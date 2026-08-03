package com.dealio.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * The Activity hosting this composable.
 *
 * Compose hands out a Context that is often a ContextWrapper rather than the
 * Activity itself, so unwrap until we find one. Firebase phone auth needs the
 * real Activity to attach its app-verification flow to.
 */
fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("No Activity found in the context chain")
}
