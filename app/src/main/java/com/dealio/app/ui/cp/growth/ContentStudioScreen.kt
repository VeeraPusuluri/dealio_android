package com.dealio.app.ui.cp.growth

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Platform(val id: String, val label: String, val color: Color)

private val platforms = listOf(
    Platform("whatsapp", "WhatsApp", Color(0xFF25D366)),
    Platform("instagram", "Instagram", Color(0xFFE4405F)),
    Platform("facebook", "Facebook", Color(0xFF1877F2)),
    Platform("linkedin", "LinkedIn", Color(0xFF0A66C2)),
)

private const val WHATSAPP_PACKAGE = "com.whatsapp"

/**
 * Content Studio — the two things a CP needs before posting a project: words and an image.
 *
 * Captions are generated three at a time, one per tone, because the right caption depends
 * on who is being posted to and that is a judgement only the CP can make. Flyers render as
 * real 1080×1350 images through [FlyerPreview], and whichever caption the CP picked travels
 * with the flyer when it is shared.
 */
@Composable
fun ContentStudioScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedId by remember { mutableLongStateOf(-1L) }
    var mode by remember { mutableStateOf("caption") }

    // Caption state
    var offerId by remember { mutableStateOf<String?>(null) }
    var platform by remember { mutableStateOf("whatsapp") }
    var variants by remember { mutableStateOf<List<CaptionVariant>>(emptyList()) }
    var chosenTone by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var seed by remember { mutableIntStateOf(0) }
    var generating by remember { mutableStateOf(false) }

    // Flyer state
    var template by remember { mutableStateOf(FlyerTemplate.Showcase) }
    var heroSettled by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val flyerLayer = rememberGraphicsLayer()

    fun resetCaptions() {
        variants = emptyList(); chosenTone = null; draft = ""; seed = 0
    }

    SubScreenScaffold("Content Studio", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val selected = state.projects.firstOrNull { it.id == selectedId }

        // Only once the flyer tab is actually open: the QR is drawn on the poster and
        // nowhere else, so a CP writing captions never pays for the call.
        LaunchedEffect(selectedId, mode) {
            if (mode == "flyer" && selectedId > 0L) vm.loadShareQr(selectedId)
        }

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Project
            DealioCard {
                SectionLabel("1. Pick a project")
                Spacer(Modifier.height(10.dp))
                if (state.projects.isEmpty()) {
                    Text("No projects yet. They appear here once builders publish them.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(
                        Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.projects.forEach { p ->
                            val sel = p.id == selectedId
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(if (sel) Color(0xFFEAFAFC) else Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedId = p.id
                                        resetCaptions()
                                        heroSettled = false
                                    }
                                    .padding(12.dp),
                            ) {
                                Text(p.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${p.builderName ?: "—"} · ${p.city ?: ""}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (selected == null) {
                Text(
                    "Pick a project to write captions and build a flyer for it.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                return@Column
            }

            // 2. What to make
            Row(
                Modifier.fillMaxWidth().background(Color(0xFFEDF1F7), RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ModeTab("Captions", mode == "caption") { mode = "caption" }
                ModeTab("Flyer", mode == "flyer") { mode = "flyer" }
            }

            if (mode == "caption") {
                // Offer — what is actually on the table decides what the caption says
                val offer = offerTypeOf(offerId)
                DealioCard {
                    SectionLabel("2. What is on offer?")
                    Text(
                        "The caption is built around this, so pick the one you are actually selling on.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        offerTypes.forEach { o ->
                            val sel = offerId == o.id
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (sel) Color(0xFFF3FCFD) else Color.White, RoundedCornerShape(13.dp))
                                    .border(if (sel) 1.5.dp else 1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(13.dp))
                                    .clickable { offerId = o.id; resetCaptions() }
                                    .padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(o.emoji, fontSize = 15.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(o.label, color = if (sel) Teal else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(o.keyFeature, color = TextSecondary, fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                                if (sel) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Outlined.CheckCircle, null, tint = Teal, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }

                if (offer == null) {
                    Text(
                        "Pick an offer type and the captions will be written around its terms.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    return@Column
                }

                // Platform
                DealioCard {
                    SectionLabel("3. Choose platform")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        platforms.forEach { pl ->
                            val sel = platform == pl.id
                            Box(
                                Modifier.background(if (sel) pl.color else Color.White, RoundedCornerShape(10.dp))
                                    .border(1.dp, if (sel) pl.color else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable { platform = pl.id; resetCaptions() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(pl.label, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                GradientButton(
                    text = when {
                        generating -> "Writing…"
                        variants.isEmpty() -> "Write 3 captions"
                        else -> "Write 3 more"
                    },
                    icon = if (variants.isEmpty()) Icons.Outlined.AutoAwesome else Icons.Outlined.Refresh,
                    enabled = !generating,
                    onClick = {
                        // captionVariants is a local pure function — there is
                        // nothing to wait for. The 450ms here was theatre, to make
                        // writing captions feel like it cost something.
                        generating = true
                        val next = if (variants.isEmpty()) 0 else seed + 1
                        scope.launch {
                            seed = next
                            variants = captionVariants(selected, offer, platform, next)
                            chosenTone = null
                            draft = ""
                            generating = false
                        }
                    },
                )

                if (variants.isNotEmpty()) {
                    DealioCard {
                        SectionLabel("4. Pick the one that fits")
                        Text(
                            "Same offer, three angles. Tap one to edit and send it.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            variants.forEach { v ->
                                val sel = chosenTone == v.tone.id
                                Column(
                                    Modifier.fillMaxWidth()
                                        .background(if (sel) Color(0xFFF3FCFD) else Color.White, RoundedCornerShape(14.dp))
                                        .border(if (sel) 1.5.dp else 1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(14.dp))
                                        .clickable { chosenTone = v.tone.id; draft = v.text }
                                        .padding(13.dp),
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(v.tone.label, color = if (sel) Teal else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        if (sel) {
                                            Icon(Icons.Outlined.CheckCircle, null, tint = Teal, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Use this", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Text(v.tone.blurb, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        v.text,
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        maxLines = if (sel) Int.MAX_VALUE else 5,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                if (chosenTone != null) {
                    DealioCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("5. Edit and send")
                            Row(
                                Modifier.clickable { copyToClipboard(ctx, "Caption", draft) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.ContentCopy, null, tint = Teal, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                            textStyle = TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = Teal,
                            ),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton(
                                label = if (platform == "whatsapp") "Send on WhatsApp" else "Share",
                                fill = if (platform == "whatsapp") Color(0xFF25D366) else Teal,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (platform == "whatsapp") openWhatsApp(ctx, null, draft) else shareText(ctx, draft)
                            }
                            Box(
                                Modifier.background(Color(0xFFEDF1F7), RoundedCornerShape(12.dp))
                                    .clickable { mode = "flyer" }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Add a flyer", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // ─── Flyer ───────────────────────────────────────────────────
                DealioCard {
                    SectionLabel("2. Choose a template")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlyerTemplate.entries.forEach { t ->
                            val sel = template == t
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (sel) Color(0xFFF3FCFD) else Color.White, RoundedCornerShape(13.dp))
                                    .border(if (sel) 1.5.dp else 1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(13.dp))
                                    .clickable { template = t }
                                    .padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.label, color = if (sel) Teal else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(t.blurb, color = TextSecondary, fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                                if (sel) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Outlined.CheckCircle, null, tint = Teal, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                    if (template == FlyerTemplate.Showcase && selected.videoUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "This project has no walkthrough video yet, so the footer invites the buyer to ask for one.",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }

                DealioCard(contentPadding = 12.dp) {
                    SectionLabel("3. Preview")
                    Spacer(Modifier.height(10.dp))
                    FlyerPreview(
                        template = template,
                        project = selected,
                        profile = state.profile,
                        layer = flyerLayer,
                        onHeroSettled = { heroSettled = true },
                        qr = state.shareQr[selected.id],
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Shares as a 1080 × 1350 image" + if (draft.isNotBlank()) ", with your caption attached." else ".",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                val heroUrl = resolveUrl(selected.imageUrl ?: selected.coverUrl)
                // The QR is part of the poster, so capturing before the share-link call
                // settles would bake a flyer with a hole where the tracked link belongs.
                val ready = (heroSettled || heroUrl == null) && state.shareQr.containsKey(selected.id)

                /** Renders once, then hands the file to [then]. Shared by all three actions. */
                fun withFlyer(then: (java.io.File) -> Unit) {
                    if (exporting) return
                    exporting = true
                    scope.launch {
                        runCatching {
                            captureFlyer(ctx, flyerLayer, "flyer-${selected.id}-${template.id}.jpg")
                        }.onSuccess(then).onFailure {
                            Toast.makeText(ctx, "Could not build the flyer", Toast.LENGTH_SHORT).show()
                        }
                        exporting = false
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionButton(
                        label = if (exporting) "Preparing…" else "WhatsApp",
                        fill = Color(0xFF25D366),
                        enabled = ready && !exporting,
                        modifier = Modifier.weight(1f),
                    ) { withFlyer { file -> shareFlyer(ctx, file, draft.takeIf { it.isNotBlank() }, WHATSAPP_PACKAGE) } }

                    ActionButton(
                        label = "Share",
                        fill = Teal,
                        icon = Icons.Outlined.Share,
                        enabled = ready && !exporting,
                        modifier = Modifier.weight(1f),
                    ) { withFlyer { file -> shareFlyer(ctx, file, draft.takeIf { it.isNotBlank() }) } }

                    ActionButton(
                        label = "Save",
                        fill = Color(0xFFEDF1F7),
                        textColor = TextSecondary,
                        icon = Icons.Outlined.Download,
                        enabled = ready && !exporting,
                    ) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            Toast.makeText(ctx, "Use Share to save on this Android version", Toast.LENGTH_SHORT).show()
                        } else {
                            withFlyer { file ->
                                val name = "Dealio-${selected.name.filter { it.isLetterOrDigit() }}-${template.id}.jpg"
                                val ok = saveFlyerToGallery(ctx, file, name)
                                Toast.makeText(ctx, if (ok) "Saved to Pictures/Dealio" else "Could not save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                if (!ready) {
                    Text("Building your flyer…", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }

                if (draft.isBlank()) {
                    Box(
                        Modifier.fillMaxWidth().background(Color(0xFFF7FAFB), RoundedCornerShape(14.dp))
                            .clickable { mode = "caption" }.padding(14.dp),
                    ) {
                        Text(
                            "Write a caption first and it will be attached to the flyer when you share it.",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            // Tips
            Column(Modifier.fillMaxWidth().background(Color(0xFFF7FAFB), RoundedCornerShape(14.dp)).padding(14.dp)) {
                Text("Tips for better engagement", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Post during peak hours: 7–9 AM or 7–9 PM IST",
                    "The co-branded flyer converts best in one-to-one WhatsApp chats",
                    "Use the clean flyer when you want the buyer to forward it on",
                    "Always include a clear call-to-action (DM, call, WhatsApp)",
                ).forEach { tip ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(tip, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f)
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(11.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    fill: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .background(if (enabled) fill else fill.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
