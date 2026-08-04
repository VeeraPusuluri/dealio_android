package com.dealio.app.ui.cp.overview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dealio.app.data.api.CpAuthorizedBuilder
import com.dealio.app.ui.cp.CpCredentialCard
import com.dealio.app.ui.cp.EngravedLabel
import com.dealio.app.ui.cp.metalFor
import com.dealio.app.ui.theme.NavyDeep

/**
 * The credential, presented on its own.
 *
 * There is no dialog chrome around it — no white panel, no title bar. Tapping
 * the avatar produces the card and nothing else, so the gesture reads as taking
 * out your card rather than opening a settings sheet. The two actions sit below
 * the card as plain engraved labels, deliberately quiet: the card is the content.
 */
@Composable
fun CpIdentityDialog(
    name: String,
    tier: String,
    photoUrl: String?,
    phone: String?,
    city: String?,
    reraNumber: String?,
    authorizedBuilders: List<CpAuthorizedBuilder>,
    uploadingPhoto: Boolean,
    onPickPhoto: (Uri) -> Unit,
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(onPickPhoto)
    }
    val metal = metalFor(tier)
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CpCredentialCard(
                name = name,
                tier = tier,
                photoUrl = photoUrl,
                phone = phone,
                city = city,
                reraNumber = reraNumber,
                authorizedBuilders = authorizedBuilders,
                uploadingPhoto = uploadingPhoto,
                onPhotoClick = { photoPicker.launch("image/*") },
            )

            if (authorizedBuilders.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "No builder authorisations yet. When a builder authorises you, it appears on your card.",
                    color = metal.face.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
            }

            // The actions need a field of their own. Left bare they sat on whatever
            // the page happened to show through the scrim, and engraved caps on a
            // half-lit list are unreadable.
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyDeep.copy(alpha = 0.88f))
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EngravedLabel(
                    "Close",
                    Color.White.copy(alpha = 0.60f),
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    size = 11,
                )
                EngravedLabel(
                    "Share",
                    metal.face,
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable {
                            shareCredential(context, name, tier, phone, city, reraNumber, authorizedBuilders)
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    size = 11,
                )
                EngravedLabel(
                    "View profile",
                    metal.face,
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable { onViewProfile() }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    size = 11,
                )
            }
        }
    }
}

/**
 * What a partner sends a customer to prove who they are.
 *
 * Written as a message someone would actually paste into a chat — no field
 * labels, no "Dealio profile" preamble — because it is read by a customer, not
 * filled in by the partner. The builder authorisation leads, since that is the
 * part the customer cannot verify for themselves.
 */
private fun credentialShareText(
    name: String,
    tier: String,
    phone: String?,
    city: String?,
    reraNumber: String?,
    authorizedBuilders: List<CpAuthorizedBuilder>,
): String = buildString {
    appendLine(name)
    authorizedBuilders.forEach { appendLine("Authorised channel partner for ${it.companyName}") }
    if (authorizedBuilders.isEmpty()) appendLine("$tier Partner on Dealio")
    city?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
    reraNumber?.takeIf { it.isNotBlank() }?.let { appendLine("RERA $it") }
    phone?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
}.trim()

/**
 * Shares the credential, preferring WhatsApp.
 *
 * Aiming straight at WhatsApp skips a chooser the partner would pick it from
 * anyway, but the app may not be installed — so a failure falls through to the
 * ordinary chooser rather than dead-ending on a tap that appears to do nothing.
 */
private fun shareCredential(
    context: Context,
    name: String,
    tier: String,
    phone: String?,
    city: String?,
    reraNumber: String?,
    authorizedBuilders: List<CpAuthorizedBuilder>,
) {
    val text = credentialShareText(name, tier, phone, city, reraNumber, authorizedBuilders)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val whatsApp = Intent(send).setPackage("com.whatsapp")
    if (runCatching { context.startActivity(whatsApp) }.isSuccess) return

    val business = Intent(send).setPackage("com.whatsapp.w4b")
    if (runCatching { context.startActivity(business) }.isSuccess) return

    runCatching { context.startActivity(Intent.createChooser(send, "Share your credential")) }
}
