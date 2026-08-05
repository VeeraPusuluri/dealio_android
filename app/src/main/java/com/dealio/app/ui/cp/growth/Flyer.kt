package com.dealio.app.ui.cp.growth

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.dealio.app.data.api.CpProfile
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.builder.titleCase
import com.dealio.app.ui.theme.GoldStar
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.io.File
import java.io.FileOutputStream

/**
 * Shareable project flyers.
 *
 * A flyer is a real image the CP can drop into a WhatsApp status or an Instagram post,
 * not a block of text. The poster is composed at a fixed 360×450 dp design grid and
 * rendered through a density override so the captured file is exactly 1080×1350 px on
 * every device — the same output whether the CP is on a cheap phone or a tablet.
 *
 * Commission figures deliberately never appear on any template: these files get
 * forwarded to buyers.
 */
enum class FlyerTemplate(val id: String, val label: String, val blurb: String) {
    /** Everything: photo, the full fact sheet, and the walkthrough video link. */
    Showcase("showcase", "Showcase", "Photo, full details and the walkthrough video link"),

    /** The flyer plus the CP's own contact card — the one they send to their own leads. */
    CoBranded("branded", "Co-branded", "Project flyer with your name, phone and RERA on it"),

    /** Project only. For forwarding onward, or posting where contact details don't belong. */
    Clean("clean", "Clean", "Just the project — none of your details on it"),
}

private const val FLYER_W_DP = 360f
private const val FLYER_H_DP = 450f

/** 1080×1350 — the 4:5 portrait size Instagram, WhatsApp status and Facebook all accept unscaled. */
private const val FLYER_W_PX = 1080f
private const val FLYER_H_PX = 1350f

// ─── Preview + capture ───────────────────────────────────────────────────────

/**
 * Renders [template] at full poster resolution and records it into [layer] so
 * [captureFlyer] can hand back a bitmap, while showing it scaled to fit the card.
 *
 * The density override is what decouples the output file from the device: the poster
 * always measures 1080 px wide regardless of the screen, and pinning `fontScale` to 1
 * keeps a CP's system font-size setting out of the shared image.
 *
 * [onHeroSettled] fires once the project photo has loaded or failed, so the caller can
 * hold the share buttons until there is actually something to capture.
 */
@Composable
fun FlyerPreview(
    template: FlyerTemplate,
    project: Project,
    profile: CpProfile?,
    layer: GraphicsLayer,
    onHeroSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scale = with(LocalDensity.current) { maxWidth.toPx() } / FLYER_W_PX
        val previewHeight = with(LocalDensity.current) { (FLYER_H_PX * scale).toDp() }

        Box(
            Modifier.fillMaxWidth().height(previewHeight).clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.TopStart,
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density = FLYER_W_PX / FLYER_W_DP, fontScale = 1f),
            ) {
                Box(
                    Modifier
                        .requiredSize(FLYER_W_DP.dp, FLYER_H_DP.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                        },
                ) {
                    FlyerPoster(template, project, profile, onHeroSettled)
                }
            }
        }
    }
}

/**
 * Pulls the recorded poster out of [layer] and writes it to the share cache.
 *
 * JPEG rather than PNG: the poster is photo-led, and a 1080×1350 PNG runs several MB
 * for no visible gain once WhatsApp re-encodes it anyway.
 */
suspend fun captureFlyer(ctx: Context, layer: GraphicsLayer, fileName: String): File {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    // toImageBitmap() can hand back a HARDWARE-config bitmap, which has no pixel data
    // to read back on the CPU — compress() throws on it. Copy down before encoding.
    val encodable =
        if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false)
        else bitmap
    val dir = File(ctx.cacheDir, "shared_flyers").apply { mkdirs() }
    val file = File(dir, fileName)
    FileOutputStream(file).use { encodable.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    return file
}

// ─── Sharing ─────────────────────────────────────────────────────────────────

private fun flyerUri(ctx: Context, file: File): Uri =
    FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)

/**
 * Sends the flyer out through the share sheet, or straight to [targetPackage] when one
 * is given (WhatsApp). [caption] rides along as the message body so the CP does not have
 * to paste it separately.
 */
fun shareFlyer(ctx: Context, file: File, caption: String?, targetPackage: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, flyerUri(ctx, file))
        if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val direct = targetPackage?.let { pkg ->
        Intent(send).setPackage(pkg).takeIf { it.resolveActivity(ctx.packageManager) != null }
    }
    val intent = direct ?: Intent.createChooser(send, "Share flyer")
    runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/**
 * Saves the flyer into the device gallery under Pictures/Dealio.
 *
 * Scoped storage (API 29+) lets us do this with no runtime permission at all. Below that
 * it would mean holding WRITE_EXTERNAL_STORAGE just for this one button, which is not a
 * trade worth making — those devices get told to use Share instead.
 */
fun saveFlyerToGallery(ctx: Context, file: File, displayName: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Dealio")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = ctx.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}

// ─── Poster ──────────────────────────────────────────────────────────────────

@Composable
private fun FlyerPoster(
    template: FlyerTemplate,
    p: Project,
    profile: CpProfile?,
    onHeroSettled: () -> Unit,
) {
    val heroHeight = when (template) {
        FlyerTemplate.Showcase -> 186.dp
        FlyerTemplate.CoBranded -> 176.dp
        FlyerTemplate.Clean -> 216.dp
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        FlyerHero(p, heroHeight, onHeroSettled)

        Column(Modifier.weight(1f).padding(horizontal = 18.dp, vertical = 14.dp)) {
            FlyerPriceRow(p)
            Spacer(Modifier.height(12.dp))
            FlyerFacts(p, compact = template == FlyerTemplate.CoBranded)
            if (template != FlyerTemplate.CoBranded) {
                p.amenities?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let { list ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        list.take(4).joinToString("  ·  "),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        when (template) {
            FlyerTemplate.Showcase -> VideoBand(p.videoUrl)
            FlyerTemplate.CoBranded -> ContactBand(profile)
            FlyerTemplate.Clean -> CleanFooter(p)
        }
    }
}

@Composable
private fun FlyerHero(p: Project, height: androidx.compose.ui.unit.Dp, onSettled: () -> Unit) {
    val url = resolveUrl(p.imageUrl ?: p.coverUrl)
    Box(Modifier.fillMaxWidth().height(height)) {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(NavyDeep, NavyMid, Teal))),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    url,
                    p.name,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success || state is AsyncImagePainter.State.Error) onSettled()
                    },
                )
            } else {
                Icon(Icons.Outlined.Apartment, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(46.dp))
            }
        }

        // Scrim so the title stays readable over a bright or busy photo.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to NavyDeep.copy(alpha = 0.15f),
                    1f to NavyDeep.copy(alpha = 0.88f),
                ),
            ),
        )

        p.status?.takeIf { it.isNotBlank() }?.let { status ->
            Box(
                Modifier.padding(14.dp).background(Teal, RoundedCornerShape(7.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(titleCase(status), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                p.name,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 27.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val where = listOfNotNull(p.locality?.takeIf { it.isNotBlank() }, p.city?.takeIf { it.isNotBlank() })
                .joinToString(", ")
            if (where.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(where, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun FlyerPriceRow(p: Project) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text("STARTING FROM", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                p.priceLow()?.let { compactPrice(it) } ?: "Price on request",
                color = TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        p.builderName?.takeIf { it.isNotBlank() }?.let { builder ->
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(140.dp)) {
                Text("BY", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    builder,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

/** The fact sheet — two columns, and any row whose field is missing simply isn't drawn. */
@Composable
private fun FlyerFacts(p: Project, compact: Boolean) {
    val facts = buildList {
        p.configurations?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?.let { add("Configurations" to it.joinToString(" / ")) }
        p.possessionDate?.takeIf { it.isNotBlank() }?.let { add("Possession" to formatDate(it)) }
        if (!compact) {
            p.projectType?.takeIf { it.isNotBlank() }?.let { add("Type" to titleCase(it)) }
            p.totalUnits?.takeIf { it > 0 }?.let { total ->
                val towers = p.towers?.takeIf { it > 0 }
                add("Scale" to if (towers != null) "$total units · $towers towers" else "$total units")
            }
        }
        p.reraNumber?.takeIf { it.isNotBlank() }?.let { add("RERA" to it) }
    }
    if (facts.isEmpty()) return

    Column(
        Modifier.fillMaxWidth().background(Color(0xFFF6F9FB), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        facts.take(if (compact) 3 else 5).forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(96.dp))
                Text(
                    value,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

/** Showcase footer — the walkthrough link, or an invitation to ask for one when there is no video. */
@Composable
private fun VideoBand(videoUrl: String?) {
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(NavyDeep, NavyMid)))
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (videoUrl != null) Icons.Outlined.PlayCircle else Icons.Outlined.CalendarMonth,
            null,
            tint = Color(0xFF6FD8E8),
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (videoUrl != null) "Watch the walkthrough" else "Site visits open",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                videoUrl ?: "Ask me for a walkthrough video and floor plans",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        DealioMark(onDark = true)
    }
}

/** Co-branded footer — who to call. The whole point of the template. */
@Composable
private fun ContactBand(profile: CpProfile?) {
    val name = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Your channel partner"
    val phone = profile?.phone?.takeIf { it.isNotBlank() }
    val rera = profile?.cp?.reraNumber?.takeIf { it.isNotBlank() }
    val tier = profile?.cp?.tier?.takeIf { it.isNotBlank() }
    val photo = resolveUrl(profile?.cp?.photoUrl)

    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(NavyDeep, NavyMid)))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)).background(Teal), contentAlignment = Alignment.Center) {
            Text(initialsOf(name), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (photo != null) AsyncImage(photo, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tier != null) {
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier.background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Verified, null, tint = GoldStar, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(tier, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (phone != null) {
                Spacer(Modifier.height(1.dp))
                Text(phone, color = Color(0xFF9FD3E0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (rera != null) {
                Text("RERA $rera", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        DealioMark(onDark = true)
    }
}

/** Clean footer — a hairline and the RERA line, nothing that ties the flyer to anyone. */
@Composable
private fun CleanFooter(p: Project) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF6F9FB)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            p.reraNumber?.takeIf { it.isNotBlank() }?.let { "RERA $it" } ?: "Details on request",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        DealioMark(onDark = false)
    }
}

@Composable
private fun DealioMark(onDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Teal))
        Spacer(Modifier.width(5.dp))
        Text(
            "Dealio",
            color = if (onDark) Color.White.copy(alpha = 0.75f) else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}
