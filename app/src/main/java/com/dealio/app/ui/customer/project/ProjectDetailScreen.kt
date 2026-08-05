package com.dealio.app.ui.customer.project

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Elevator
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import org.json.JSONArray
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.ProjectDocument
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.availableUnitsOrDerived
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.priceHigh
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.builder.titleCase
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.components.meetingTypes
import com.dealio.app.ui.components.shareViaWhatsApp
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Mist
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.NavyPrimary
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.pow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** How long each hero image holds before the carousel moves on. */
private const val HERO_ADVANCE_MS = 4_000L

/** Location advantages shown before the section asks to be expanded. */
private const val LOCATION_ADV_PREVIEW = 4

private val timeSlots = listOf("10:00 AM", "11:00 AM", "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM")
// The CP's own booking form offers the same three — see meetingTypes.
private val visitTypes = meetingTypes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(nav: NavController, projectId: Long, vm: ProjectDetailViewModel = viewModel()) {
    LaunchedEffect(projectId) { vm.load(projectId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showBooking by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    val p = state.project
    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (p != null) {
                Row(
                    // Inset after the border so the white runs to the bottom edge
                    // while the buttons clear the system navigation bar.
                    Modifier.fillMaxWidth().background(Color.White).border(1.dp, CardBorder, RoundedCornerShape(0.dp))
                        .navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { nav.navigate(CustomerRoutes.loanApply(p.id, p.builderId)) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Apply loan", color = Navy, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = { showBooking = true },
                        modifier = Modifier.weight(1.3f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Book a visit", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(projectId) }, modifier = Modifier.padding(inner))
            p != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = inner.calculateBottomPadding() + 16.dp),
            ) {
                item { HeroHeader(p, galleryUrls(p, state.documents)) { nav.navigateUp() } }
                projectDetailSections(
                    p = p,
                    documents = state.documents,
                    showConfigActions = true,
                    working = state.working,
                    onShortlist = { cfg -> vm.shortlist(cfg, mapOf("bhkType" to cfg, "price" to priceText(p))) },
                    onPricing = { cfg -> vm.requestPricing(cfg, mapOf("bhkType" to cfg), "Please share pricing for $cfg") },
                )
            }
        }
    }

    if (showBooking && p != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showBooking = false }, sheetState = sheetState, containerColor = Color.White) {
            BookingSheet(working = state.working) { date, time, type, notes ->
                vm.bookVisit(date, time, type, notes) { showBooking = false }
            }
        }
    }

}

internal fun priceText(p: Project): String {
    val lo = p.priceLow(); val hi = p.priceHigh()
    return when {
        (lo ?: 0.0) > 0 && (hi ?: 0.0) > 0 && hi != lo -> "${formatINRShort(lo)} – ${formatINRShort(hi)}"
        (lo ?: 0.0) > 0 -> "${formatINR(lo)} onwards"
        else -> "Price on request"
    }
}

/**
 * The full informational body of a project — price, facts, plans, tour, loan
 * calculator, availability, amenities, specs, developer, etc. Shared verbatim by
 * the customer explore detail and the CP portal detail so both stay in parity.
 *
 * Callers render the hero themselves (the chrome differs) and then invoke this
 * inside their `LazyColumn`. Set [showConfigActions] to surface the customer's
 * shortlist / get-price buttons; the CP portal shows configurations read-only.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.projectDetailSections(
    p: Project,
    documents: List<ProjectDocument>,
    showConfigActions: Boolean,
    working: Boolean = false,
    onShortlist: (cfg: String) -> Unit = {},
    onPricing: (cfg: String) -> Unit = {},
) {
    // Every section below reads from this rather than the raw list, so a file
    // uploaded twice is one entry wherever it appears.
    val docs = dedupeDocuments(documents)

    // A plotted layout is sold by the yard, not by the bedroom. The builder form
    // already drops BHK configurations, towers and floors for this type and asks
    // for plot sizes and a plot count instead — so the detail view has to read
    // the same way, or a partner is shown "Towers —" and "Configurations" for
    // land that has neither.
    val isPlot = p.projectType.equals("Plot", ignoreCase = true)

    item {
        Column(Modifier.padding(16.dp)) {
            Text("Starting price", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(priceText(p), color = Teal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if ((p.pricePerSqftFrom ?: 0.0) > 0) {
                // Land is quoted by the yard, not the foot. The builder form asks
                // for the rate in the same unit it is shown in here, so a plot's
                // rate is entered and read as ₹/sq.yd throughout.
                val unit = if (isPlot) "sq.yd" else "sq.ft"
                Text("₹${p.pricePerSqftFrom!!.toLong()}/$unit onwards", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }

    // Quick facts
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isPlot) {
                Fact("Plot size", p.configurations?.firstOrNull() ?: "—", Modifier.weight(1f))
                Fact("Total plots", p.totalUnits?.toString() ?: "—", Modifier.weight(1f))
            } else {
                Fact("Config", p.configurations?.firstOrNull() ?: "—", Modifier.weight(1f))
                Fact("Towers", p.towers?.toString() ?: "—", Modifier.weight(1f))
            }
            Fact("Possession", p.possessionDate?.take(7) ?: "—", Modifier.weight(1f))
        }
    }

    // Plans. Land has a layout, not floor plans — same uploaded documents, named
    // for what they actually are.
    val floorPlans = floorPlanDocs(docs)
    item {
        Section(if (isPlot) "Layout plan" else "Floor plans") {
            if (floorPlans.isEmpty()) {
                PlanNotProvided(
                    Icons.Outlined.Map,
                    if (isPlot) "Layout plan not provided by the builder yet. You can request it during a site visit."
                    else "Floor plans not provided by the builder yet. You can request them during a site visit.",
                )
            } else {
                FloorPlansRow(floorPlans)
            }
        }
    }

    // Tower plans — per-tower selector, strict per-tower matching. A plotted
    // layout has no towers, so the section would only ever be empty.
    if (!isPlot) {
        item { Section("Tower plans") { TowerPlansSection(p, docs) } }
    }

    // Everything else the builder uploaded.
    //
    // The gallery takes the photographs and the section above takes the plans;
    // nothing took the rest, so a brochure or a RERA certificate was stored,
    // listed in the builder's own portal, and then simply absent here. Sorted
    // brochure-first because that is the file a partner sends a customer.
    val extraDocs = otherDocs(docs)
    if (extraDocs.isNotEmpty()) {
        item {
            Section("Documents") {
                extraDocs.forEachIndexed { i, d ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    DocumentRow(d)
                }
            }
        }
    }

    // Virtual tour — walkthrough video links, or "not provided"
    item { Section("Virtual tour") { VirtualTourSection(p.videoUrl) } }

    // Home-loan EMI calculator
    item { Section("Loan") { LoanCalculator(p.priceLow() ?: p.priceHigh() ?: 50_00_000.0, p.name) } }

    // Availability
    if ((p.totalUnits ?: 0) > 0) {
        item { Box(Modifier.padding(top = 12.dp)) { AvailabilityBar(p, isPlot) } }
    }

    // Video / Maps quick actions
    if (!p.videoUrl.isNullOrBlank() || !p.googleMapsLink.isNullOrBlank()) {
        item { LinkButtons(p) }
    }

    // Nearby highlights
    if (!p.nearbyHighlights.isNullOrEmpty()) {
        item {
            Section("Nearby") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.nearbyHighlights!!.forEach { n ->
                        Row(
                            Modifier.background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Place, null, tint = Teal, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(n, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (!p.description.isNullOrBlank()) {
        item { Section("About this project") { Text(p.description!!, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp) } }
    }

    // Configurations — with customer shortlist / get-price actions, or read-only
    // for the CP portal. For land these rows are the yardages on sale.
    //
    // Read-only, the sizes are near-identical strings; stacked as full-width rows
    // they read as filler and push the page down. A partner asking "what can I
    // offer?" wants them at a glance, so they sit as spec tiles with the figure
    // large and its unit engraved beneath. Customers keep the rows, which carry
    // shortlist and pricing actions the tiles have no room for.
    if (!p.configurations.isNullOrEmpty()) {
        item {
            Section(if (isPlot) "Plot sizes" else "Configurations") {
                if (showConfigActions) {
                    p.configurations!!.forEach { cfg ->
                        ConfigRow(cfg = cfg, working = working, onShortlist = { onShortlist(cfg) }, onPricing = { onPricing(cfg) })
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Smallest first. Builders enter sizes in whatever order they
                        // think of them, and "300 100 200 400" set as a row of figures
                        // reads as a mistake rather than a range.
                        orderedConfigurations(p.configurations!!).forEach { cfg -> SpecTile(cfg) }
                    }
                }
            }
        }
    }

    if (!p.amenities.isNullOrEmpty()) {
        item {
            Section("Amenities") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.amenities!!.forEach { a ->
                        // A flat teal wash behind every chip made the whole block one
                        // colour and buried the icons that tell them apart. White with
                        // a hairline lets each amenity's own icon do the work.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White)
                                .border(1.dp, CardBorder, RoundedCornerShape(11.dp))
                                .padding(start = 7.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
                        ) {
                            val tint = amenityTint(a)
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(tint.copy(alpha = 0.13f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(amenityIcon(a), null, tint = tint, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(a, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    // Structure, flooring, kitchen, bathrooms — all things a built unit has and
    // a plot does not. Shown for a plot they would read as an empty promise.
    val specs = p.specifications
    if (specs != null && !isPlot) {
        item {
            Section("Specifications") {
                InfoRow("Structure", specs.structure)
                InfoRow("Flooring", specs.flooring)
                InfoRow("Kitchen", specs.kitchen)
                InfoRow("Bathrooms", specs.bathrooms)
                InfoRow("Doors & windows", listOfNotNull(specs.doors, specs.windows).joinToString(" / ").ifBlank { null })
                InfoRow("Painting", specs.painting)
            }
        }
    }

    if (!p.paymentPlans.isNullOrEmpty()) {
        item {
            Section("Payment plans") {
                p.paymentPlans!!.forEach { plan ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(plan.name ?: "Plan", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (!plan.description.isNullOrBlank()) Text(plan.description!!, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (!p.locationAdvantages.isNullOrEmpty()) {
        item {
            Section("Location advantages") {
                // Proximity is the whole point of this section, so the distance is
                // pulled out of the sentence and set right-aligned where it can be
                // scanned down the column instead of hunted for inside prose.
                val points = buildList {
                    p.locationAdvantages!!.forEach { la ->
                        // Blank (not null) distance/drive values are the norm here, so filter
                        // them out — joining them raw produced a bare "km · min" with no numbers.
                        //
                        // Category is deliberately not among these. It reads as a measure in
                        // a right-aligned metric column — "CORPORATE" set where a distance
                        // belongs looks like one — and it describes the place, not how far
                        // away it is, which is what this section is for.
                        val detail = listOfNotNull(
                            la.distanceKm?.takeIf { it.isNotBlank() }?.let { "$it km" },
                            la.driveMinutes?.takeIf { it.isNotBlank() }?.let { "$it min" },
                        ).joinToString(" · ")
                        splitAdvantagePoints(la.name).forEachIndexed { i, point ->
                            add(point to if (i == 0) detail else "")
                        }
                    }
                }
                // Builders paste long lists here — a dozen entries is normal and
                // buries everything below the section. Show enough to judge the
                // location, and let the rest be asked for.
                var expanded by remember { mutableStateOf(false) }
                val shown = if (expanded) points else points.take(LOCATION_ADV_PREVIEW)
                shown.forEachIndexed { i, (point, detail) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder.copy(alpha = 0.5f)))
                    LocationAdvRow(point, detail)
                }
                if (points.size > LOCATION_ADV_PREVIEW) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder.copy(alpha = 0.5f)))
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { expanded = !expanded }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // Naming the count is the point: "show more" hides how
                            // much more there is to read.
                            if (expanded) "Show less" else "Show all ${points.size} advantages",
                            color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            null, tint = Teal, modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    // The developer's own credential: who is building this, how long they have
    // been at it, and the registration number that makes it checkable. Rendered
    // as a plate rather than label/value rows because a partner reads it out to a
    // customer — and because RERA is a certificate, not a table cell.
    item {
        Section("Developer") {
            DeveloperPlate(p)
        }
    }
}

@Composable
private fun DeveloperPlate(p: Project) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(NavyDeep, NavyPrimary)))
            .padding(18.dp),
    ) {
        // No "Developer" label here — the section heading above the plate already
        // says it, and printing it twice is the kind of thing that reads as
        // decoration rather than structure.
        Text(
            p.builderName?.takeIf { it.isNotBlank() } ?: "—",
            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )

        val established = p.builderYearEstablished?.toString()
        val delivered = p.builderDeliveredProjects?.toString()
        if (established != null || delivered != null) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                established?.let { PlateStat("Established", it) }
                delivered?.let { PlateStat("Delivered", it) }
            }
        }

        if (!p.reraNumber.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
            Spacer(Modifier.height(14.dp))
            PlateLabel("RERA registration", Color.White.copy(alpha = 0.45f))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.reraNumber!!,
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp, modifier = Modifier.weight(1f),
                )
                p.reraExpiry?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        "Valid to ${formatDate(it)}",
                        color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        if (!p.status.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.13f))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(TealBright))
                Spacer(Modifier.width(7.dp))
                Text(titleCase(p.status), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PlateLabel(text: String, color: Color) {
    Text(
        text.uppercase(), color = color,
        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
    )
}

@Composable
private fun PlateStat(label: String, value: String) {
    Column {
        PlateLabel(label, Color.White.copy(alpha = 0.45f))
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Sizes smallest-first when every one of them leads with a number; otherwise the
 * builder's own order, since there is no sound way to rank "Penthouse".
 */
private fun orderedConfigurations(configs: List<String>): List<String> {
    val keyed = configs.map { it to Regex("^\\s*(\\d+(?:\\.\\d+)?)").find(it.trim())?.groupValues?.get(1)?.toDoubleOrNull() }
    return if (keyed.any { it.second == null }) configs else keyed.sortedBy { it.second }.map { it.first }
}

/**
 * One size on sale — "200 sq yd", "3BHK".
 *
 * The figure is what distinguishes one from the next, so it is set large with
 * its unit engraved beneath. A label with no leading number ("Penthouse") has
 * nothing to split and is simply set whole.
 */
@Composable
private fun SpecTile(label: String) {
    val split = Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*(.+)$").find(label.trim())
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .widthIn(min = 92.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (split != null) {
            Text(
                split.groupValues[1], color = TextPrimary, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                split.groupValues[2].trim().uppercase(), color = TextSecondary,
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp,
            )
        } else {
            Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LocationAdvRow(text: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(Teal.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Place, null, tint = Teal, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(11.dp))
        Text(text, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        if (detail.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            Text(
                detail.uppercase(), color = TextSecondary,
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Read-only configuration tile for portals that don't shortlist (CP). */
@Composable
internal fun ConfigInfoRow(cfg: String, isPlot: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().background(Teal.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A building icon next to "200 sq yd" describes the wrong thing.
        Icon(
            if (isPlot) Icons.Outlined.Landscape else Icons.Outlined.Apartment,
            null, tint = Teal, modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(cfg, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Peeking image carousel with pagination dots — the shared gallery used by the
 * customer hero (with a back button + title overlay) and the CP portal (bare).
 */
@Composable
internal fun ProjectImagePager(
    images: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 270.dp,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val pagerState = rememberPagerState { images.size }

    // Advance on its own, so the rest of the builder's photographs are seen
    // rather than waiting behind a swipe nobody makes.
    //
    // Keyed on settledPage, not on the current page or scroll flag: those change
    // *during* the animation this effect starts, which would cancel it midway and
    // strand the pager between two images. Settling — whether from the timer or
    // from the partner's own swipe — is also exactly when the wait should restart,
    // so a photo someone just swiped to gets a full turn on screen.
    if (images.size > 1) {
        LaunchedEffect(images.size) {
            snapshotFlow { pagerState.settledPage }.collectLatest { page ->
                delay(HERO_ADVANCE_MS)
                // Checked here rather than as a key: a finger on the carousel wins.
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((page + 1) % images.size)
                }
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(height).background(Color.White)) {
            when {
                images.size > 1 -> {
                    // Peeking horizontal scroll — the next/previous image peeks in at the
                    // edges, mirroring the website's horizontal gallery carousel.
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        AsyncImage(
                            model = images[page],
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        )
                    }
                }
                images.size == 1 -> AsyncImage(model = images[0], contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Box(
                    Modifier.fillMaxSize().background(Brush.linearGradient(listOf(NavyMid, Teal))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Apartment, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                }
            }
            overlay()
        }
        // Dots below the images showing the current image — matches the website carousel
        if (images.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                repeat(images.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                            .background(if (active) Teal else Color.LightGray, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(p: Project, images: List<String>, onBack: () -> Unit) {
    ProjectImagePager(images, p.name) {
        // Bottom scrim for legible overlay text
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.65f))),
            ),
        )
        // The photograph runs edge to edge, under the status bar, so the button
        // has to be pushed clear of it by hand. Without statusBarsPadding it
        // starts 8dp from the top of the *window*, and its 48dp touch target
        // ends at 56dp — inside a status bar that is 53dp tall on a Pixel 9 Pro
        // and 62dp on the Fold's cover screen. The system status bar window
        // swallows every tap in that strip, so the arrow simply did nothing.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }

        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            if (!p.status.isNullOrBlank()) {
                Text(
                    titleCase(p.status),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Teal, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(p.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    listOfNotNull(p.locality, p.city).joinToString(", ").ifBlank { "—" },
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun AvailabilityBar(p: Project, isPlot: Boolean = false) {
    val total = (p.totalUnits ?: 0).coerceAtLeast(1)
    val available = (p.availableUnitsOrDerived() ?: 0).coerceIn(0, total)
    val sold = (p.soldUnits ?: 0).coerceIn(0, total)
    val booked = (p.bookedUnits ?: 0).coerceIn(0, total)
    val noun = if (isPlot) "plots" else "units"
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Availability", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("$available of ${p.totalUnits} $noun available", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (available > 0) Box(Modifier.weight(available.toFloat()).fillMaxHeight().background(Teal, RoundedCornerShape(3.dp)))
            if (booked > 0) Box(Modifier.weight(booked.toFloat()).fillMaxHeight().background(StatusColors.Amber, RoundedCornerShape(3.dp)))
            if (sold > 0) Box(Modifier.weight(sold.toFloat()).fillMaxHeight().background(TextSecondary, RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Teal, "Available $available")
            Legend(StatusColors.Amber, "Booked $booked")
            Legend(TextSecondary, "Sold $sold")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun LinkButtons(p: Project) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!p.videoUrl.isNullOrBlank()) {
            LinkButton("Watch video", Icons.Outlined.PlayCircle, Modifier.weight(1f)) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, p.videoUrl!!.toUri())) }
            }
        }
        if (!p.googleMapsLink.isNullOrBlank()) {
            LinkButton("View on map", Icons.Outlined.Map, Modifier.weight(1f)) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, p.googleMapsLink!!.toUri())) }
            }
        }
    }
}

@Composable
private fun LinkButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .background(Teal.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Teal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * The colour an amenity's icon carries.
 *
 * Grouped, not per-amenity: a distinct hue for every entry would be a dozen
 * colours on one screen and would read as decoration. Five families — water,
 * greenery, energy, active, safety — mean the colour says something ("that one
 * is about water") and repeats often enough to be learnable. Everything else
 * stays brand teal.
 *
 * Deliberately no orange: on this page orange means money, and an amenity is
 * not money.
 */
private val AmenityWater  = Color(0xFF2E86C1)
private val AmenityGreen  = Color(0xFF2E9E5B)
private val AmenityEnergy = Color(0xFFC98A16)
private val AmenityActive = Color(0xFF5B62C4)
private val AmenitySafety = Color(0xFF4A6B8A)

private fun amenityTint(name: String): Color {
    val n = name.lowercase()
    return when {
        "pool" in n || "swim" in n || "water" in n || "rain" in n -> AmenityWater
        "park" in n || "garden" in n || "landscap" in n || "green" in n || "jogging" in n -> AmenityGreen
        "power" in n || "backup" in n || "electric" in n || "solar" in n || "ev" in n || "charg" in n -> AmenityEnergy
        "gym" in n || "fitness" in n || "tennis" in n || "basket" in n || "court" in n ||
            "sport" in n || "child" in n || "kid" in n || "play" in n -> AmenityActive
        "security" in n || "guard" in n || "gated" in n || "cctv" in n || "camera" in n ||
            "intercom" in n || "fire" in n -> AmenitySafety
        else -> Teal
    }
}

private fun amenityIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        "pool" in n || "swim" in n -> Icons.Outlined.Pool
        "gym" in n || "fitness" in n -> Icons.Outlined.FitnessCenter
        "child" in n || "kid" in n || "play" in n -> Icons.Outlined.ChildFriendly
        "cctv" in n || "camera" in n -> Icons.Outlined.Videocam
        "security" in n || "guard" in n || "gated" in n || "intercom" in n -> Icons.Outlined.Security
        "power" in n || "backup" in n || "electric" in n -> Icons.Outlined.Bolt
        "parking" in n -> Icons.Outlined.LocalParking
        "park" in n || "garden" in n || "landscap" in n || "jogging" in n || "green" in n -> Icons.Outlined.Park
        "lift" in n || "elevator" in n -> Icons.Outlined.Elevator
        "wifi" in n || "internet" in n || "co-work" in n || "cowork" in n -> Icons.Outlined.Wifi
        "water" in n || "rain" in n -> Icons.Outlined.WaterDrop
        "ev" in n || "charg" in n -> Icons.Outlined.EvStation
        "tennis" in n -> Icons.Outlined.SportsTennis
        "basket" in n || "court" in n || "sport" in n -> Icons.Outlined.SportsBasketball
        "fire" in n -> Icons.Outlined.LocalFireDepartment
        "spa" in n || "yoga" in n || "senior" in n -> Icons.Outlined.Spa
        "solar" in n -> Icons.Outlined.WbSunny
        "club" in n || "lounge" in n || "party" in n || "hall" in n || "indoor" in n -> Icons.Outlined.Deck
        else -> Icons.Outlined.CheckCircle
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(label.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Breaks a location-advantage blob into readable points.
 *
 * Builders paste an entire paragraph into the single `name` field, so every project
 * arrives as one unpunctuated run-on string with no newline or bullet to split on
 * ("...Keesara Road 5 Minutes to Yadadri Temple Just 1 KM from RRR Exit 10 Minutes
 * to Raigiri Railway Station..."). Break before distance/duration phrases and the
 * usual connectors, which is where a new fact reliably starts. Display-only — the
 * stored data is untouched, and structured entry in the builder form remains the
 * real fix.
 */
private val ADV_BREAK = Regex(
    "(?=•)" +
        "|(?=\\b\\d+(?:\\.\\d+)?\\s*(?:km|kms|kilometers?|meters?|metres?|mins?|minutes?|hrs?|hours?)\\b)" +
        "|(?=\\b(?:immediate|adjacent|very near|very close|near by|nearby|near to|close to|proposed|opposite|facing|walking distance|just)\\b)",
    RegexOption.IGNORE_CASE,
)

private fun splitAdvantagePoints(raw: String?): List<String> {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return emptyList()
    val parts = text.split(ADV_BREAK)
        .map { it.trim().trimStart('•').trim().trimEnd('&', '·', ',').trim() }
        .filter { it.isNotEmpty() }
    // A stray connector ("Just") is the head of the next point, not a point of its own.
    val merged = mutableListOf<String>()
    for (part in parts) {
        if (merged.isNotEmpty() && merged.last().split(" ").size < 3) {
            merged[merged.lastIndex] = "${merged.last()} $part"
        } else {
            merged.add(part)
        }
    }
    return merged.ifEmpty { listOf(text) }
}

@Composable
private fun LocationAdvBullet(text: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "•",
            color = Teal,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(text, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            if (detail.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(detail, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        SectionLabel(title)
        Spacer(Modifier.height(10.dp))
        content()
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(CardBorder))
}

@Composable
private fun ConfigRow(cfg: String, working: Boolean, onShortlist: () -> Unit, onPricing: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Teal.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(12.dp),
    ) {
        Text(cfg, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onShortlist, enabled = !working, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(10.dp)) {
                Text("Shortlist", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onPricing, enabled = !working, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)) {
                Text("Get price", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookingSheet(working: Boolean, onConfirm: (date: String, time: String, type: String, notes: String) -> Unit) {
    val dates = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf(dates.first()) }
    var selectedTime by remember { mutableStateOf(timeSlots.first()) }
    var selectedType by remember { mutableStateOf(visitTypes.first()) }
    var notes by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Book a site visit", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SectionLabel("Date")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dates.forEach { d ->
                val sel = d == selectedDate
                val label = "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
                Chip(label, sel) { selectedDate = d }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Time")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            timeSlots.forEach { t -> Chip(t, t == selectedTime) { selectedTime = t } }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Type")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visitTypes.forEach { t -> Chip(t, t == selectedType) { selectedType = t } }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            shape = RoundedCornerShape(12.dp),
            colors = dealioFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            minLines = 2,
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                onConfirm(selectedDate.toString(), selectedTime, selectedType, notes)
            },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) {
            if (working) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
            else {
                Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request visit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else TextSecondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) Teal else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) Teal else CardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ─── Gallery / floor-plan helpers ────────────────────────────────────────────

private fun isImageDoc(d: ProjectDocument): Boolean {
    val u = d.url.lowercase()
    if (listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp").any { u.endsWith(it) }) return true
    // Uploads without a recognizable extension: trust the document type,
    // mirroring the website's gallery filter.
    val t = d.docType.lowercase()
    return t.contains("image") || t.contains("photo")
}
private fun isTowerPlanDoc(d: ProjectDocument): Boolean {
    val t = d.docType.lowercase()
    return t.contains("tower") && t.contains("plan")
}
private fun isFloorPlanDoc(d: ProjectDocument): Boolean {
    val t = d.docType.lowercase()
    return (t.contains("floor") || t.contains("layout")) && !isTowerPlanDoc(d)
}
/**
 * Collapses the same file uploaded more than once.
 *
 * Re-uploading is how builders replace a document — there is no edit — so a
 * project accumulates two rows with the same type and file name pointing at two
 * copies of the same thing. Rendered straight, that is the brochure listed twice.
 *
 * Keyed on type *and* name, not type alone: a builder with a Phase I and a Phase
 * II brochure has two real documents and must keep both. The newest survives,
 * since a re-upload is a replacement.
 */
internal fun dedupeDocuments(docs: List<ProjectDocument>): List<ProjectDocument> =
    docs.groupBy { "${it.docType.trim().lowercase()}|${it.name.trim().lowercase()}" }
        .values
        .map { group -> group.maxByOrNull { it.createdAt } ?: group.first() }
        // groupBy preserves first-seen order of keys; restore the original order
        // so sections that care about sequence are unaffected.
        .sortedBy { kept -> docs.indexOfFirst { it.id == kept.id } }

/** Cover image followed by project photos (deduped) for the hero gallery. */
internal fun galleryUrls(p: Project, docs: List<ProjectDocument>): List<String> {
    val urls = LinkedHashSet<String>()
    resolveUrl(p.imageUrl ?: p.coverUrl)?.let { urls.add(it) }
    dedupeDocuments(docs).filter { isImageDoc(it) && !isFloorPlanDoc(it) && !isTowerPlanDoc(it) }
        .forEach { d -> resolveUrl(d.url)?.let { urls.add(it) } }
    return urls.toList()
}
/** "Floor Plan - 3 BHK - East" → "3 BHK · East"; a plan with no qualifier → its file name. */
private fun floorPlanLabel(d: ProjectDocument): String {
    val rest = d.docType
        .replace(Regex("(?i)(floor|layout|master|site)\\s*plan"), "")
        .trim(' ', '-', '–', '·')
    if (rest.isNotEmpty()) return rest.replace(" - ", " · ")
    // Nothing distinguishing in the type, so name it by the file — minus the
    // extension, which the tile already states.
    return d.name.substringBeforeLast('.').ifBlank { "General" }
}

/** A plan the builder uploaded, and whether it can be shown as a picture. */
private data class PlanDoc(val url: String, val label: String, val isImage: Boolean)

/**
 * Every floor/layout plan on the project.
 *
 * Plans used to be filtered to images only, because the row draws each one as a
 * thumbnail. Builders upload plans as PDFs at least as often, and those were
 * dropped silently — the page then told the partner the builder had not provided
 * a plan while the plan sat in the project. A PDF is now kept and drawn as a
 * document tile; both open the same way on tap.
 */
private fun floorPlanDocs(docs: List<ProjectDocument>): List<PlanDoc> =
    docs.filter { isFloorPlanDoc(it) }
        .mapNotNull { d -> resolveUrl(d.url)?.let { PlanDoc(it, floorPlanLabel(d), isImageDoc(d)) } }

@Composable
private fun FloorPlansRow(plans: List<PlanDoc>) {
    val ctx = LocalContext.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        plans.forEach { plan ->
            Column(
                Modifier.width(220.dp).clickable {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, plan.url.toUri())) }
                },
            ) {
                if (plan.isImage) {
                    AsyncImage(
                        model = plan.url, contentDescription = plan.label, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)).background(Mist),
                    )
                } else {
                    PdfPlanTile(Modifier.fillMaxWidth().height(160.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(plan.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * The EMI estimate as a message.
 *
 * Laid out as inputs then results, because the customer needs to see what was
 * assumed before the monthly figure means anything. It closes by saying the
 * numbers are indicative: this comes off a slider a partner just dragged, and
 * arriving in a chat with no caveat it would read like an offer from a lender.
 */
private fun emiShareText(
    projectName: String,
    propertyValue: Double,
    downAmt: Double,
    downPct: Int,
    loanAmt: Double,
    rate: Float,
    years: Int,
    emi: Double,
    totalInterest: Double,
    totalPayment: Double,
): String = buildString {
    appendLine("$projectName — home loan estimate")
    appendLine()
    appendLine("Property value: ${formatINRShort(propertyValue)}")
    appendLine("Down payment: ${formatINRShort(downAmt)} ($downPct%)")
    appendLine("Loan amount: ${formatINRShort(loanAmt)}")
    appendLine("Interest: ${"%.2f".format(rate)}% p.a. over $years years")
    appendLine()
    appendLine("Monthly EMI: ${formatINR(emi)}")
    appendLine("Total interest: ${formatINRShort(totalInterest)}")
    appendLine("Total cost: ${formatINRShort(totalPayment)}")
    appendLine()
    append("Indicative only — the actual rate and eligibility are set by the lender.")
}

/**
 * Documents with nowhere else to go: brochures, RERA certificates, anything a
 * builder uploads that is neither a photograph nor a plan.
 *
 * Brochure first — it is the one a partner forwards to a customer.
 */
private fun otherDocs(docs: List<ProjectDocument>): List<ProjectDocument> =
    docs.filter { !isImageDoc(it) && !isFloorPlanDoc(it) && !isTowerPlanDoc(it) }
        .sortedBy { if (it.docType.contains("brochure", ignoreCase = true)) 0 else 1 }

@Composable
private fun DocumentRow(d: ProjectDocument) {
    val ctx = LocalContext.current
    val url = resolveUrl(d.url)
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .clickable(enabled = url != null) {
                url?.let { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) } }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Teal.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Description, null, tint = Teal, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                d.docType.ifBlank { "Document" },
                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                d.name.substringBeforeLast('.').ifBlank { "Open" },
                color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // A brochure exists to be forwarded, so the link is one tap away rather
        // than something to open first and share from wherever it lands.
        if (url != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .clickable { shareViaWhatsApp(ctx, "${d.docType.ifBlank { "Document" }}\n$url", "Share document") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Share, "Share ${d.docType}", tint = Teal, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** Stand-in for a plan that is a document rather than a picture. */
@Composable
private fun PdfPlanTile(modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Mist).border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Description, null, tint = Teal, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(8.dp))
        Text("Open PDF", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlanNotProvided(icon: ImageVector, message: String) {
    Column(
        Modifier.fillMaxWidth()
            .background(Mist, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TowerPlansSection(p: Project, docs: List<ProjectDocument>) {
    val ctx = LocalContext.current
    // A tower plan uploaded as a PDF counts too — filtering to images told the
    // partner the builder had provided nothing while the plan sat in the project.
    val towerDocs = remember(docs) { docs.filter { isTowerPlanDoc(it) } }
    val towerCount = maxOf(p.towers ?: towerDocs.size, 1)
    var selected by remember { mutableStateOf(0) }
    // Strict "Tower Plan - {n}" match first, then a digit-boundary fallback —
    // never another tower's plan as a stand-in.
    fun planFor(i: Int): ProjectDocument? =
        towerDocs.firstOrNull { it.docType == "Tower Plan - ${i + 1}" }
            ?: towerDocs.firstOrNull { Regex("(^|[^0-9])${i + 1}([^0-9]|$)").containsMatchIn(it.docType) }

    Column {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(towerCount) { i ->
                val sel = selected == i
                val hasPlan = planFor(i) != null
                Row(
                    Modifier
                        .background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(10.dp))
                        .clickable { selected = i }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Tower ${i + 1}",
                        color = if (sel) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Box(
                        Modifier.size(6.dp).background(
                            when {
                                hasPlan -> if (sel) Color.White else Teal
                                else -> CardBorder
                            },
                            CircleShape,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val plan = planFor(selected)
        val url = plan?.let { resolveUrl(it.url) }
        if (url != null) {
            val open = Modifier.clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }
            if (isImageDoc(plan)) {
                AsyncImage(
                    model = url,
                    contentDescription = "Tower ${selected + 1} plan",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Mist)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .then(open),
                )
            } else {
                PdfPlanTile(Modifier.fillMaxWidth().height(160.dp).then(open))
            }
        } else {
            PlanNotProvided(Icons.Outlined.Apartment, "Tower ${selected + 1} plan not provided by the builder yet.")
        }
    }
}

/**
 * `videoUrl` is either a plain URL or a JSON array of `{label, url}` —
 * the same format the website's tour section parses.
 */
private fun parseTours(videoUrl: String?): List<Pair<String, String>> {
    if (videoUrl.isNullOrBlank()) return emptyList()
    val fromJson = runCatching {
        val arr = JSONArray(videoUrl)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url")
            if (url.isBlank()) null else o.optString("label").ifBlank { "Project Tour" } to url
        }
    }.getOrNull()
    if (fromJson != null) return fromJson
    return listOf("Project Tour" to videoUrl)
}

@Composable
private fun VirtualTourSection(videoUrl: String?) {
    val ctx = LocalContext.current
    val tours = remember(videoUrl) { parseTours(videoUrl) }
    if (tours.isEmpty()) {
        PlanNotProvided(Icons.Outlined.PlayCircle, "Virtual tour not provided by the builder yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tours.forEach { (label, url) ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(listOf(NavyMid, Teal)), RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.PlayCircle, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Watch the project walkthrough", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── Inline home-loan EMI calculator ─────────────────────────────────────────

@Composable
private fun LoanCalculator(price: Double, projectName: String) {
    var propertyValue by remember { mutableFloatStateOf(price.toFloat().coerceAtLeast(5_00_000f)) }
    var downPct by remember { mutableFloatStateOf(20f) }
    var rate by remember { mutableFloatStateOf(8.65f) }
    var years by remember { mutableFloatStateOf(20f) }

    val downAmt = propertyValue * downPct / 100f
    val loanAmt = propertyValue - downAmt
    val r = rate / 12f / 100f
    val n = years * 12f
    val emi = if (r > 0) loanAmt * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1) else loanAmt / n
    val totalPayment = emi * n + downAmt
    val totalInterest = totalPayment - propertyValue

    Column {
        // EMI banner
        Row(
            Modifier.fillMaxWidth().background(Teal.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Estimated EMI", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${formatINR(emi.toDouble())}/mo", color = Teal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Interest", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(formatINRShort(totalInterest.toDouble()), color = androidx.compose.ui.graphics.Color(0xFFEA580C), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Breakdown row
        Row(
            Modifier.fillMaxWidth().background(Mist, RoundedCornerShape(12.dp)).padding(vertical = 12.dp),
        ) {
            BreakdownCell("Down Payment", formatINRShort(downAmt.toDouble()), Teal, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(40.dp).background(CardBorder).align(Alignment.CenterVertically))
            BreakdownCell("Loan Amount", formatINRShort(loanAmt.toDouble()), TextPrimary, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(40.dp).background(CardBorder).align(Alignment.CenterVertically))
            BreakdownCell("Total Cost", formatINRShort(totalPayment.toDouble()), androidx.compose.ui.graphics.Color(0xFFEA580C), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        CalcSlider("Property value", formatINRShort(propertyValue.toDouble()), propertyValue, 5_00_000f..price.toFloat().coerceAtLeast(5_00_001f)) { propertyValue = it }
        CalcSlider("Down payment", "${downPct.toInt()}%", downPct, 10f..50f, steps = 7) { downPct = it }
        CalcSlider("Interest rate", "${"%.2f".format(rate)}%", rate, 7f..15f) { rate = it }
        CalcSlider("Tenure", "${years.toInt()} yr", years, 5f..30f) { years = it }

        // The numbers are worked out with a customer on the phone, so they need a
        // way off the screen. Sends whatever the sliders currently say.
        val ctx = LocalContext.current
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(11.dp))
                .background(Teal.copy(alpha = 0.10f))
                .clickable {
                    shareViaWhatsApp(
                        ctx,
                        emiShareText(
                            projectName = projectName,
                            propertyValue = propertyValue.toDouble(),
                            downAmt = downAmt.toDouble(),
                            downPct = downPct.toInt(),
                            loanAmt = loanAmt.toDouble(),
                            rate = rate,
                            years = years.toInt(),
                            emi = emi.toDouble(),
                            totalInterest = totalInterest.toDouble(),
                            totalPayment = totalPayment.toDouble(),
                        ),
                        "Share EMI estimate",
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Share, null, tint = Teal, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share this estimate", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BreakdownCell(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalcSlider(
    label: String, value: String, current: Float,
    range: ClosedFloatingPointRange<Float>, steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = current, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal),
        )
    }
}
