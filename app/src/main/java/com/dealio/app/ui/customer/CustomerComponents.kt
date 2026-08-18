package com.dealio.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.monthYear
import com.dealio.app.ui.builder.priceHigh
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.builder.titleCase
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.CustomerAccentDeep
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.softShadow

private fun priceRange(p: Project): String {
    val lo = p.priceLow()
    val hi = p.priceHigh()
    return when {
        (lo ?: 0.0) > 0 && (hi ?: 0.0) > 0 && hi != lo -> "${formatINRShort(lo)} – ${formatINRShort(hi)}"
        (lo ?: 0.0) > 0 -> "${formatINRShort(lo)}+"
        else -> "On request"
    }
}

private fun hasPrice(p: Project) = (p.priceLow() ?: 0.0) > 0 || (p.priceHigh() ?: 0.0) > 0

/** Sale status reduced to a dot and a word — the same reduction the web card makes. */
private data class StatusMeta(val dot: Color, val label: String)

private val SellingGreen = Color(0xFF12896F)
private val ConstructionAmber = Color(0xFFA9761F)
private val ClosingRust = Color(0xFFC2410C)

private fun statusMeta(raw: String?): StatusMeta? {
    val key = raw?.trim()?.takeIf { it.isNotBlank() }?.uppercase()?.replace(' ', '_') ?: return null
    return when (key) {
        "ACTIVE", "LAUNCHED" -> StatusMeta(SellingGreen, "Selling")
        "READY_TO_MOVE" -> StatusMeta(SellingGreen, "Ready to move")
        "CLOSING_SOON" -> StatusMeta(ClosingRust, "Closing soon")
        "NEW_LAUNCH" -> StatusMeta(CustomerAccentDeep, "New launch")
        "PRE_LAUNCH" -> StatusMeta(CustomerAccentDeep, "Pre-launch")
        "UNDER_CONSTRUCTION" -> StatusMeta(ConstructionAmber, "Under construction")
        // An unrecognised status is still worth saying, just without a claim
        // about what it means — a grey dot reads as "stated, not classified".
        else -> StatusMeta(TextSecondary, titleCase(raw))
    }
}

@Composable
private fun HeroImage(p: Project, modifier: Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(NavyMid, CustomerAccent)))) {
        val url = resolveUrl(p.imageUrl ?: p.coverUrl)
        if (url != null) {
            AsyncImage(model = url, contentDescription = p.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Apartment, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
            }
        }
    }
}

/** The all-caps micro-label above a figure. One label treatment for the whole card. */
@Composable
private fun Eyebrow(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = TextSecondary,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        textAlign = align,
    )
}

@Composable
private fun ImageBadge(text: String, background: Color) {
    Text(
        text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * Configurations split into the values and the unit they share, when they do.
 *
 * A plot project lists "200 sq yd, 300 sq yd, 400 sq yd, 500 sq yd" — four chips
 * that spend most of their width restating the unit, and only three of which fit
 * a phone. Factored, all four fit and the unit is said once. Short units are
 * left alone: "2 BHK" is the term people use, and "2 | 3" under a "BHK" label
 * is a worse reading of it than the chip it replaces.
 */
private const val FACTOR_UNIT_MIN_LENGTH = 4

private fun splitConfigs(configs: List<String>): Pair<List<String>, String?> {
    if (configs.size < 2) return configs to null
    val split = configs.map { it.trim().split(Regex("\\s+"), limit = 2) }
    if (split.any { it.size != 2 }) return configs to null
    val unit = split.first()[1]
    if (unit.length < FACTOR_UNIT_MIN_LENGTH) return configs to null
    if (split.any { !it[1].equals(unit, ignoreCase = true) }) return configs to null
    return split.map { it[0] } to unit
}

/** One configuration ("3 BHK", "200 sq yd") as a bordered pill. */
@Composable
private fun ConfigChip(text: String) {
    Text(
        text,
        color = TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .background(Color(0xFFF7F9FC), RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/**
 * Full-width browse card.
 *
 * Laid out in three tiers rather than as one stack of same-sized lines: the
 * photograph with its badges, the identity (status, name, place), and the two
 * commercial figures a shortlisting buyer actually compares between cards —
 * price and possession — set apart below a rule. This mirrors the web card
 * (`CustomerHome.tsx`), where the same three tiers already carry the browse
 * grid; the flat five-line version this replaces gave the possession date and
 * the price the same weight as the plot sizes.
 */
@Composable
fun CustomerProjectCard(
    p: Project,
    /** Null hides the bookmark entirely — for lists where saving makes no sense. */
    saved: Boolean? = null,
    onToggleSave: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val status = statusMeta(p.status)
    val configs = p.configurations?.filter { it.isNotBlank() } ?: emptyList()
    val possession = monthYear(p.possessionDate)

    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(radius = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(158.dp)) {
            HeroImage(p, Modifier.matchParentSize())
            Column(
                Modifier.align(Alignment.TopStart).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (p.featured) ImageBadge("Featured", Orange)
                if (p.closingSoon) ImageBadge("Closing soon", ClosingRust)
            }
            if (saved != null && onToggleSave != null) {
                BookmarkButton(saved, Modifier.align(Alignment.TopEnd).padding(8.dp), onToggleSave)
            }
        }

        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 13.dp)) {
            // Status and RERA — the two facts that qualify everything below them.
            if (status != null || !p.reraNumber.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status != null) {
                        Box(Modifier.size(6.dp).background(status.dot, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Eyebrow(status.label)
                    }
                    if (!p.reraNumber.isNullOrBlank()) {
                        if (status != null) {
                            Spacer(Modifier.width(8.dp))
                            Text("·", color = CardBorder, fontSize = 11.sp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Filled.CheckCircle, null, tint = CustomerAccentDeep, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("RERA", color = CustomerAccentDeep, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                p.name,
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    // Trimmed: several localities are stored with a trailing
                    // space, which joined as "Kondurg , Hyderabad".
                    listOfNotNull(p.locality, p.city).map { it.trim() }.filter { it.isNotEmpty() }
                        .joinToString(", ").ifBlank { "—" },
                    color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }

            if (configs.isNotEmpty()) {
                val (values, unit) = splitConfigs(configs)
                // Whatever fits a phone width; the rest become a count, so a
                // project with eight plot sizes does not wrap to a second row.
                val shown = if (unit == null) 3 else 4
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    values.take(shown).forEach { ConfigChip(it) }
                    if (values.size > shown) ConfigChip("+${values.size - shown}")
                    if (unit != null) {
                        Text(unit, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder.copy(alpha = 0.8f)))
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(if (hasPrice(p)) "Starting" else "Price")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        priceRange(p),
                        color = if (hasPrice(p)) CustomerAccent else TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (possession != null) {
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Eyebrow("Possession", align = TextAlign.End)
                        Spacer(Modifier.height(3.dp))
                        Text(possession, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Save-for-later toggle, filled once it is on.
 *
 * Deliberately not the same gesture as shortlisting a unit: that tells the
 * builder a buyer is interested and cannot be undone, while this is private and
 * exists to be changed its mind about.
 */
@Composable
fun BookmarkButton(
    saved: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(34.dp)
            // Over a photograph, the icon alone disappears against a bright sky.
            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            if (saved) "Saved — tap to remove" else "Save this project",
            tint = Color.White,
            modifier = Modifier.size(19.dp),
        )
    }
}

/**
 * Compact card for the featured carousel.
 *
 * No "Featured" badge here: every card in that rail is featured, so the badge
 * only repeated the heading above it.
 */
@Composable
fun FeaturedCard(p: Project, onClick: () -> Unit) {
    Column(
        Modifier
            .width(220.dp)
            .softShadow(radius = 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable { onClick() },
    ) {
        HeroImage(p, Modifier.fillMaxWidth().height(120.dp))
        Column(Modifier.padding(12.dp)) {
            Text(p.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.city ?: "—", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(priceRange(p), color = CustomerAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
