package com.dealio.app.ui.components

import android.content.Context
import android.content.Intent

/**
 * Sends [text] to WhatsApp, falling back to the system chooser.
 *
 * Partners send everything over WhatsApp, so aiming at it directly skips a
 * chooser they would pick it from anyway. It may not be installed — on a tablet,
 * on a work device — so a failed launch falls through rather than dead-ending on
 * a tap that appears to do nothing. WhatsApp Business is tried in between,
 * because a partner is more likely to run that one than neither.
 *
 * Reaching WhatsApp by package needs the `<queries>` entries in the manifest;
 * without them Android 11+ hides it and every share takes the long way round.
 */
fun shareViaWhatsApp(context: Context, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        if (runCatching { context.startActivity(Intent(send).setPackage(pkg)) }.isSuccess) return
    }
    runCatching { context.startActivity(Intent.createChooser(send, chooserTitle)) }
}
