package com.dealio.app.ui.cp.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.components.FormSheet
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.PortalHeader
import com.dealio.app.ui.components.SheetField
import com.dealio.app.ui.components.SheetSection
import com.dealio.app.ui.components.SheetSubmitButton
import com.dealio.app.ui.cp.CpLeadCard
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.SurfaceTintTeal
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Leads | Deals, with counts.
 *
 * The counts are the point: before this, both words described the same list, so
 * "12 leads" and "12 deals" were the same twelve people. Showing the two numbers
 * side by side is what makes the split legible at a glance.
 */
@Composable
private fun SideSwitch(side: LeadSide, leadCount: Int, dealCount: Int, onPick: (LeadSide) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(SurfaceTintTeal, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(LeadSide.LEADS to leadCount, LeadSide.DEALS to dealCount).forEach { (s, count) ->
            val sel = side == s
            Box(
                Modifier
                    .weight(1f)
                    .background(if (sel) Navy else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onPick(s) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${s.label}  $count",
                    color = if (sel) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun LeadsScreen(nav: NavController, vm: LeadsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    RefreshOnResume { vm.load(silent = true) }

    // Arriving from a stat tile on the home screen, which says which half it
    // meant. Applied and cleared here rather than read as a nav argument — see
    // CpLeadsTabRequest for why an argument cannot work for this tab.
    val requestedSide by CpLeadsTabRequest.side.collectAsStateWithLifecycle()
    LaunchedEffect(requestedSide) {
        requestedSide?.let { vm.setSide(it); CpLeadsTabRequest.clear() }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    var showAddLead by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }

    val pickSheet = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.stageFromSheet(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PortalHeader(
                title = "My pipeline",
                subtitle = "Everyone you introduced, from first enquiry to close",
                stats = buildList {
                    if (state.all.isNotEmpty()) {
                        add("${state.leads.size}" to "leads")
                        add("${state.deals.size}" to "deals")
                        val inPlay = state.all.sumOf { it.estimatedCommission ?: 0.0 }
                        if (inPlay > 0) add(formatINRShort(inPlay) to "in play")
                    }
                },
                trailing = { AddLeadButton { showChooser = true } },
            )

            if (state.all.isNotEmpty()) {
                SideSwitch(
                    side = state.side,
                    leadCount = state.leads.size,
                    dealCount = state.deals.size,
                    onPick = vm::setSide,
                )
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.statuses.forEach { st ->
                    val sel = state.statusFilter == st
                    Text(
                        st,
                        color = if (sel) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .background(if (sel) NavyMid else Color.White, RoundedCornerShape(10.dp))
                            .border(1.dp, if (sel) NavyMid else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { vm.setFilter(st) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
                state.filtered.isEmpty() && state.statusFilter != "All" -> PortalEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "Nothing at ${state.statusFilter}",
                    subtitle = "No one is sitting at this stage right now.",
                    actionLabel = "Show all",
                    onAction = { vm.setFilter("All") },
                )
                // An empty Deals side is not an empty account — it means nothing
                // has reached Negotiation yet, so it must not offer "add your
                // first lead" as though the CP had none.
                state.filtered.isEmpty() && state.side == LeadSide.DEALS -> PortalEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "No deals yet",
                    subtitle = "A lead becomes a deal once it reaches Negotiation. You have ${state.leads.size} still being worked.",
                    actionLabel = "Back to leads",
                    onAction = { vm.setSide(LeadSide.LEADS) },
                )
                state.filtered.isEmpty() && state.deals.isNotEmpty() -> PortalEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "No open leads",
                    subtitle = "Everyone you introduced has moved through to a deal.",
                    actionLabel = "See your ${state.deals.size} deals",
                    onAction = { vm.setSide(LeadSide.DEALS) },
                )
                state.filtered.isEmpty() -> PortalEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "No leads yet",
                    subtitle = "Log the buyers you introduce and Dealio tracks the deal — and your commission — through to close.",
                    actionLabel = "Add your first lead",
                    onAction = { showAddLead = true },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.filtered.size) { i ->
                        CpLeadCard(state.filtered[i]) { nav.navigate(CpRoutes.dealDetail(state.filtered[i].id)) }
                    }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (showChooser) {
        AddLeadChooser(
            onManual = { showChooser = false; showAddLead = true },
            onFromFile = {
                showChooser = false
                pickSheet.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/comma-separated-values",
                        "text/plain",
                        "*/*",
                    ),
                )
            },
            onDismiss = { showChooser = false },
        )
    }

    state.staged?.let { staged ->
        LeadImportSheet(
            items = staged,
            projects = state.projects,
            selectedProjectId = state.importProjectId,
            working = state.importing,
            progress = state.importProgress,
            onPickProject = vm::setImportProject,
            onToggle = vm::toggleStaged,
            onSelectAll = vm::selectAllStaged,
            onConfirm = vm::importStaged,
            onDismiss = vm::clearStaged,
        )
    }

    if (showAddLead) {
        AddLeadSheet(
            projects = state.projects,
            contacts = state.contacts,
            working = state.working,
            onDismiss = { showAddLead = false },
            onSubmit = { projectId, name, phone, email -> vm.createLead(projectId, name, phone, email) { showAddLead = false } },
        )
    }
}

@Composable
private fun AddLeadButton(onClick: () -> Unit) {
    Row(
        Modifier
            // Navy on the navy hero was invisible; teal is the portal's action colour.
            .background(Teal, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AddLeadSheet(
    projects: List<Project>,
    contacts: List<CpContact>,
    working: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (projectId: Long, name: String, phone: String, email: String) -> Unit,
) {
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val project = selectedProject
    val canSubmit = project != null && name.isNotBlank() && phone.length >= 6

    FormSheet(
        title = "Add a lead",
        subtitle = "Tag a buyer to the project you're taking them to.",
        icon = Icons.Outlined.PersonAdd,
        onDismiss = onDismiss,
        footer = {
            // What is still missing, said plainly — a greyed-out button with no
            // reason attached is the most common dead end in a form like this.
            if (!canSubmit) {
                val missing = listOfNotNull(
                    "a project".takeIf { project == null },
                    "a name".takeIf { name.isBlank() },
                    "a phone number".takeIf { phone.length < 6 },
                )
                Text(
                    "Still needed: ${missing.joinToString(", ")}",
                    color = TextSecondary, fontSize = 11.5.sp,
                )
                Spacer(Modifier.height(10.dp))
            }
            SheetSubmitButton(
                text = "Add lead",
                enabled = canSubmit,
                working = working,
                onClick = { if (project != null) onSubmit(project.id, name.trim(), phone, email) },
            )
        },
    ) {
        SheetSection("Project") {
            if (projects.isEmpty()) {
                Text(
                    "You aren't empanelled on any project yet.",
                    color = TextSecondary, fontSize = 12.5.sp,
                )
            } else {
                // A phone-height list of cards beats a dropdown here: the CP
                // picks by commission as much as by name, and a menu row cannot
                // show that without truncating.
                projects.forEach { p ->
                    ProjectPickRow(
                        project = p,
                        selected = selectedProject?.id == p.id,
                        onClick = { selectedProject = if (selectedProject?.id == p.id) null else p },
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("Customer") {
            if (contacts.isNotEmpty()) {
                Text("From your contacts", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    contacts.forEach { c ->
                        ContactPickChip(
                            contact = c,
                            selected = name == c.name && phone == c.phone,
                            onClick = { name = c.name; phone = c.phone; email = c.email ?: "" },
                        )
                    }
                }
            }
            SheetField(
                value = name, onValueChange = { name = it },
                label = "Full name", icon = Icons.Outlined.Person, placeholder = "e.g. Ramesh Kumar",
            )
            SheetField(
                value = phone, onValueChange = { phone = it.filter(Char::isDigit) },
                label = "Phone", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone,
                placeholder = "10-digit mobile",
            )
            SheetField(
                value = email, onValueChange = { email = it },
                label = "Email (optional)", icon = Icons.Outlined.MailOutline, keyboardType = KeyboardType.Email,
            )
        }
    }
}

/** One selectable project, with the number the CP actually decides on. */
@Composable
private fun ProjectPickRow(project: Project, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) SurfaceTintTeal else Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) Teal else CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                project.name, color = TextPrimary, fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
            val sub = listOfNotNull(
                project.city,
                project.commissionValue?.takeIf { it > 0 }?.let { pct ->
                    val shown = if (pct % 1.0 == 0.0) pct.toLong().toString() else pct.toString()
                    "$shown% commission"
                },
            )
            if (sub.isNotEmpty()) {
                Text(sub.joinToString(" · "), color = TextSecondary, fontSize = 11.5.sp, maxLines = 1)
            }
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, "Selected", tint = Teal, modifier = Modifier.size(20.dp))
        }
    }
}

/** Contact shortcut: initials avatar plus name, so two Rameshes stay apart. */
@Composable
private fun ContactPickChip(contact: CpContact, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(if (selected) Teal else Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) Teal else CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .background(if (selected) Color.White.copy(alpha = 0.22f) else SurfaceTintTeal, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                contact.name.trim().take(1).uppercase().ifBlank { "?" },
                color = if (selected) Color.White else Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                contact.name, color = if (selected) Color.White else TextPrimary,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
            Text(
                contact.phone,
                color = if (selected) Color.White.copy(alpha = 0.8f) else TextSecondary,
                fontSize = 10.sp, maxLines = 1,
            )
        }
    }
}
