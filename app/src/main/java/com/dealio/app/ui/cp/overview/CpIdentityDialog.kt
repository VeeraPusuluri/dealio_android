package com.dealio.app.ui.cp.overview

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.dealio.app.data.api.CpAuthorizedBuilder
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * The CP's credential card, opened by tapping the avatar on the overview header.
 *
 * The authorised-builder rows are the point of the dialog: they are what a CP
 * shows a customer to prove they represent a builder, so they render only from
 * [authorizedBuilders] returned by the server — never inferred from deals.
 */
@Composable
fun CpIdentityDialog(
    name: String,
    tier: String,
    photoUrl: String?,
    phone: String?,
    city: String?,
    authorizedBuilders: List<CpAuthorizedBuilder>,
    uploadingPhoto: Boolean,
    onPickPhoto: (Uri) -> Unit,
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(onPickPhoto)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Teal)
                            .clickable(enabled = !uploadingPhoto) { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            uploadingPhoto -> CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 3.dp)
                            !photoUrl.isNullOrBlank() -> AsyncImage(
                                model = photoUrl,
                                contentDescription = name,
                                modifier = Modifier.size(96.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            else -> Text(initialsOf(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                        }
                    }
                    // Sits on the photo rather than beside it so the badge reads as
                    // part of the identity, the way a verified tick does elsewhere.
                    val badge = if (authorizedBuilders.isNotEmpty()) StatusColors.Green else Orange
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Color.White)
                            .clickable(enabled = !uploadingPhoto) { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (authorizedBuilders.isNotEmpty()) {
                            Icon(Icons.Filled.Verified, "Authorised", tint = badge, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Outlined.PhotoCamera, "Change photo", tint = Teal, modifier = Modifier.size(17.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(name, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                val subtitle = listOfNotNull(phone?.takeIf { it.isNotBlank() }, city?.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = TextSecondary, fontSize = 13.sp)
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.background(Orange.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = Orange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("$tier Partner", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(18.dp))
                if (authorizedBuilders.isEmpty()) {
                    Text(
                        "No builder authorisations yet. Once a builder authorises you, it appears here.",
                        color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
                    )
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        authorizedBuilders.forEach { builder ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(StatusColors.Green.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.dp, StatusColors.Green.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Verified, null, tint = StatusColors.Green, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text("Authorised CP for", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text(builder.companyName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary, fontWeight = FontWeight.SemiBold) }
                    TextButton(onClick = onViewProfile) { Text("View profile", color = Teal, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
