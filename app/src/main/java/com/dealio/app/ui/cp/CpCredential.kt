package com.dealio.app.ui.cp

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dealio.app.data.api.CpAuthorizedBuilder
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyPrimary
import com.dealio.app.ui.theme.softShadow

// ─── The credential ──────────────────────────────────────────────────────────
//
// A channel partner's standing is already a metal hierarchy in the data —
// Silver, Gold, Platinum — and a builder authorisation is an endorsement they
// show a customer to prove they can sell. So the CP's identity is rendered as
// one artifact: an engraved, foil-edged card whose metal IS their tier.
//
// The same card is the hero of the profile page and the whole content of the
// dialog behind the avatar, so opening the dialog reads as "showing your card"
// rather than "a popup appeared". Everything around it stays plain white
// surfaces — the card is the only place this app spends any shine.

/** The foil a tier is struck in. */
data class TierMetal(val face: Color, val edge: Color, val label: String)

private val Silver   = TierMetal(Color(0xFFB6C0CE), Color(0xFF8794A6), "Silver")
private val Gold     = TierMetal(Color(0xFFE3A94B), Color(0xFFA9762A), "Gold")
private val Platinum = TierMetal(Color(0xFFCFE4EC), Color(0xFF6FA8B8), "Platinum")

fun metalFor(tier: String?): TierMetal = when (tier?.trim()?.lowercase()) {
    "gold" -> Gold
    "platinum" -> Platinum
    else -> Silver
}

/** Engraved register: small, letterspaced caps. Used for every label on the card. */
@Composable
fun EngravedLabel(text: String, color: Color, modifier: Modifier = Modifier, size: Int = 10) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp,
    )
}

/**
 * The CP's credential card.
 *
 * [onPhotoClick] makes the portrait the upload target — on a card, the portrait
 * is the obvious thing to tap, so a separate "change photo" control would only
 * be a second place to look.
 */
@Composable
fun CpCredentialCard(
    name: String,
    tier: String,
    photoUrl: String?,
    phone: String?,
    city: String?,
    reraNumber: String? = null,
    authorizedBuilders: List<CpAuthorizedBuilder> = emptyList(),
    uploadingPhoto: Boolean = false,
    onPhotoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val metal = metalFor(tier)
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier
            .fillMaxWidth()
            .softShadow(elevation = 14.dp, radius = 22.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(NavyDeep, NavyPrimary, NavyDeep)))
            // The foil edge is the tier. It is the only hairline on the card, so
            // the metal is legible at a glance without a badge announcing it.
            .border(1.dp, metal.edge.copy(alpha = 0.55f), shape),
    ) {
        // A single diagonal sheen across the face. One sweep reads as struck
        // metal; several read as a gradient demo.
        Box(
            Modifier.matchParentSize().background(
                Brush.linearGradient(
                    0.0f to Color.Transparent,
                    0.42f to metal.face.copy(alpha = 0.10f),
                    0.52f to metal.face.copy(alpha = 0.03f),
                    1.0f to Color.Transparent,
                ),
            ),
        )

        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CredentialPortrait(name, photoUrl, metal, uploadingPhoto, onPhotoClick)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    EngravedLabel("${metal.label} Partner", metal.face)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = listOfNotNull(phone?.takeIf { it.isNotBlank() }, city?.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(sub, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (!reraNumber.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EngravedLabel("RERA", Color.White.copy(alpha = 0.40f))
                    Spacer(Modifier.width(8.dp))
                    Text(reraNumber, color = Color.White.copy(alpha = 0.80f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (authorizedBuilders.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                // Hairline rule in the tier metal separates who they are from who
                // vouches for them — the two halves of a credential.
                Box(Modifier.fillMaxWidth().height(1.dp).background(metal.edge.copy(alpha = 0.35f)))
                Spacer(Modifier.height(14.dp))
                EngravedLabel(
                    if (authorizedBuilders.size == 1) "Authorised CP for" else "Authorised CP for",
                    metal.face.copy(alpha = 0.75f),
                )
                authorizedBuilders.forEach { builder ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, tint = metal.face, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            builder.companyName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialPortrait(
    name: String,
    photoUrl: String?,
    metal: TierMetal,
    uploading: Boolean,
    onPhotoClick: (() -> Unit)?,
) {
    val ring = RoundedCornerShape(18.dp)
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(64.dp)
                .clip(ring)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, metal.face.copy(alpha = 0.45f), ring)
                .then(if (onPhotoClick != null && !uploading) Modifier.clickable { onPhotoClick() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // Initials are drawn first and the photo sits on top, so a URL that
            // fails to load leaves the initials showing instead of an empty tile.
            Text(initialsOf(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            if (!photoUrl.isNullOrBlank() && !uploading) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    modifier = Modifier.size(64.dp).clip(ring),
                    contentScale = ContentScale.Crop,
                )
            }
            if (uploading) {
                Box(Modifier.size(64.dp).clip(ring).background(NavyDeep.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = metal.face, strokeWidth = 2.dp)
                }
            }
        }
        if (onPhotoClick != null && !uploading) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(metal.face)
                    .clickable { onPhotoClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PhotoCamera, "Change photo", tint = NavyDeep, modifier = Modifier.size(13.dp))
            }
        }
    }
}

/** Quiet row used under the credential when a CP has no authorisations yet. */
@Composable
fun NoAuthorisationsNote(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "No builder authorisations yet. When a builder authorises you, it appears on your card.",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
        )
    }
}
