package com.dealio.app.ui.builder.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.availableUnitsOrDerived
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.builder.titleCase
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dealio.app.data.api.ProjectDocument

@Composable
fun ProjectDetailScreen(nav: NavController, projectId: Long, vm: ProjectDetailViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(projectId) { vm.load(projectId) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    var pendingType by remember { mutableStateOf<String?>(null) }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val type = pendingType
        if (uri != null && type != null) vm.uploadDocument(uri, type)
        pendingType = null
    }

    Box(Modifier.fillMaxSize()) {
        com.dealio.app.ui.builder.SubScreenScaffold(
            title = state.project?.name ?: "Project",
            nav = nav,
            actions = {
                if (state.project != null) {
                    Icon(
                        Icons.Outlined.Edit, "Edit", tint = Teal,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp)
                            .clickable { nav.navigate(BuilderRoutes.projectForm(projectId)) },
                    )
                }
            },
        ) { pad ->
            when {
                state.loading -> LoadingState(Modifier.padding(pad))
                state.error != null -> ErrorState(state.error!!, { vm.load(projectId) }, Modifier.padding(pad))
                state.project != null -> ProjectDetailBody(
                    p = state.project!!,
                    documents = state.documents,
                    uploading = state.uploading,
                    modifier = Modifier.padding(pad),
                    onUpload = { docType ->
                        pendingType = docType
                        pickFile.launch(arrayOf("image/*", "application/pdf", "video/*", "*/*"))
                    },
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

/**
 * The document types the viewer screens classify on. "Floor Plan" and
 * "Tower Plan - n" are matched by substring in the customer/CP project pages,
 * so uploading under the wrong label puts the file in the wrong section — hence
 * a fixed list rather than a free-text field.
 */
private val DOC_TYPES = listOf(
    "Floor Plan",
    "Tower Plan - 1",
    "Tower Plan - 2",
    "Tower Plan - 3",
    "Project Image",
    "Brochure",
    "RERA Certificate",
    "Price List",
    "Other",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DocumentsCard(
    documents: List<ProjectDocument>,
    uploading: Boolean,
    onUpload: (String) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DealioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Documents")
            Spacer(Modifier.weight(1f))
            if (uploading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Teal, strokeWidth = 2.dp)
            } else {
                Row(
                    Modifier
                        .background(Teal.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                        .clickable { picking = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Upload, null, tint = Teal, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Upload", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (documents.isEmpty()) {
            Text(
                "No floor plans, brochures or certificates yet. Buyers see these on the project page.",
                color = TextSecondary, fontSize = 12.sp,
            )
        } else {
            documents.forEach { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            resolveUrl(d.url)?.let { u ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, u.toUri())) }
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Description, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.docType.ifBlank { "Document" }, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(d.name, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    if (picking) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { picking = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                Text("What is this file?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "The type decides where buyers see it — floor plans in the plans row, tower plans against their tower.",
                    color = TextSecondary, fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                DOC_TYPES.forEach { t ->
                    Text(
                        t,
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { picking = false; onUpload(t) }
                            .padding(vertical = 12.dp),
                    )
                    HorizontalDivider(color = CardBorder.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDetailBody(
    p: Project,
    documents: List<ProjectDocument>,
    uploading: Boolean,
    modifier: Modifier,
    onUpload: (String) -> Unit,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero
        Box(Modifier.fillMaxWidth().height(190.dp).background(Brush.linearGradient(listOf(NavyMid, Teal)))) {
            val url = resolveUrl(p.imageUrl ?: p.coverUrl)
            if (url != null) {
                AsyncImage(url, p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Apartment, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                }
            }
            Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) { StatusChip(titleCase(p.status ?: "Active")) }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(p.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(p.address, p.locality, p.city, p.pincode).joinToString(", ").ifBlank { "—" },
                color = TextSecondary, fontSize = 13.sp,
            )
            if (!p.description.isNullOrBlank()) {
                Text(p.description!!, color = TextSecondary, fontSize = 13.sp)
            }

            // Key metrics
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Price from", if ((p.priceMin ?: 0.0) > 0) formatINRShort(p.priceMin) else "—", Modifier.weight(1f))
                Metric("Price to", if ((p.priceMax ?: 0.0) > 0) formatINRShort(p.priceMax) else "—", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Total units", "${p.totalUnits ?: 0}", Modifier.weight(1f))
                Metric("Available", "${p.availableUnitsOrDerived() ?: 0}", Modifier.weight(1f))
                Metric("Sold", "${p.soldUnits ?: 0}", Modifier.weight(1f))
            }

            // Overview
            DealioCard {
                SectionLabel("Overview")
                Spacer(Modifier.height(8.dp))
                InfoRow("Type", titleCase(p.projectType))
                InfoRow("Possession", com.dealio.app.ui.builder.formatDate(p.possessionDate))
                InfoRow("Towers", p.towers?.toString())
                InfoRow("Floors / tower", p.floorsPerTower?.toString())
                InfoRow("Land area", p.landArea)
                InfoRow("Clubhouse", p.clubhouseAreaSqft?.let { "$it sq.ft" })
                InfoRow("Price / sq.ft", if ((p.pricePerSqftFrom ?: 0.0) > 0) "${formatINRShort(p.pricePerSqftFrom)} – ${formatINRShort(p.pricePerSqftTo)}" else null)
                InfoRow("Maintenance", p.maintenanceCharges?.let { formatINRShort(it) })
            }

            // Configurations
            if (!p.configurations.isNullOrEmpty()) {
                DealioCard {
                    SectionLabel("Configurations")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.configurations!!.forEach { ChipTag(it) }
                    }
                }
            }

            // Amenities
            if (!p.amenities.isNullOrEmpty()) {
                DealioCard {
                    SectionLabel("Amenities")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.amenities!!.forEach { ChipTag(it) }
                    }
                }
            }

            // Nearby
            if (!p.nearbyHighlights.isNullOrEmpty()) {
                DealioCard {
                    SectionLabel("Nearby highlights")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.nearbyHighlights!!.forEach { ChipTag(it) }
                    }
                }
            }

            // Specifications
            p.specifications?.let { s ->
                val rows = listOf(
                    "Structure" to s.structure, "Flooring" to s.flooring, "Doors" to s.doors,
                    "Windows" to s.windows, "Electrical" to s.electrical, "Plumbing" to s.plumbing,
                    "Kitchen" to s.kitchen, "Bathrooms" to s.bathrooms, "Painting" to s.painting,
                ).filter { !it.second.isNullOrBlank() }
                if (rows.isNotEmpty()) DealioCard {
                    SectionLabel("Specifications")
                    Spacer(Modifier.height(8.dp))
                    rows.forEach { InfoRow(it.first, it.second) }
                }
            }

            // Payment plans
            if (!p.paymentPlans.isNullOrEmpty()) {
                DealioCard {
                    SectionLabel("Payment plans")
                    Spacer(Modifier.height(8.dp))
                    p.paymentPlans!!.forEach {
                        Text(it.name ?: "", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (!it.description.isNullOrBlank()) Text(it.description!!, color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            // Location advantages
            if (!p.locationAdvantages.isNullOrEmpty()) {
                DealioCard {
                    SectionLabel("Location advantages")
                    Spacer(Modifier.height(8.dp))
                    p.locationAdvantages!!.filter { !it.name.isNullOrBlank() }.forEach {
                        InfoRow(it.name ?: "", listOfNotNull(it.distanceKm?.let { d -> "$d km" }, it.driveMinutes?.let { m -> "$m min" }).joinToString(" · "))
                    }
                }
            }

            // RERA & commission
            DealioCard {
                SectionLabel("Compliance & commission")
                Spacer(Modifier.height(8.dp))
                InfoRow("RERA number", p.reraNumber)
                InfoRow("RERA expiry", com.dealio.app.ui.builder.formatDate(p.reraExpiry))
                InfoRow("RERA state", p.reraState)
                InfoRow("Building permit", p.buildingPermitNumber)
                InfoRow("Commission", p.commissionValue?.let { "$it%" })
                InfoRow("CP incentive", p.cpIncentive)
            }

            DocumentsCard(documents, uploading, onUpload)

            // Builder profile
            if (!p.builderName.isNullOrBlank() || !p.builderAbout.isNullOrBlank()) {
                DealioCard {
                    SectionLabel("Developer")
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Company", p.builderName)
                    InfoRow("Established", p.builderYearEstablished?.toString())
                    InfoRow("Delivered", p.builderDeliveredProjects?.let { "$it projects" })
                    InfoRow("Website", p.builderWebsite)
                    if (!p.builderAbout.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(p.builderAbout!!, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(value, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ChipTag(text: String) {
    Box(
        Modifier
            .background(Teal.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) { Text(text, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}
