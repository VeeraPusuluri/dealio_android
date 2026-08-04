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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.PortalHeader
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpLeadCard
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun LeadsScreen(nav: NavController, vm: LeadsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    RefreshOnResume { vm.load(silent = true) }

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
                title = "My leads",
                subtitle = "Everyone you introduced to a project",
                stats = buildList {
                    if (state.all.isNotEmpty()) {
                        add("${state.all.size}" to if (state.all.size == 1) "lead" else "leads")
                        val inPlay = state.all.sumOf { it.estimatedCommission ?: 0.0 }
                        if (inPlay > 0) add(formatINRShort(inPlay) to "in play")
                    }
                },
                trailing = { AddLeadButton { showChooser = true } },
            )

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
                    subtitle = "No lead is sitting at this stage right now.",
                    actionLabel = "Show all leads",
                    onAction = { vm.setFilter("All") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLeadSheet(
    projects: List<Project>,
    contacts: List<CpContact>,
    working: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (projectId: Long, name: String, phone: String, email: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var projectMenuOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Add a lead", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // ── Project selector ──
            SectionLabel("Project")
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = projectMenuOpen, onExpandedChange = { projectMenuOpen = it }) {
                OutlinedTextField(
                    value = selectedProject?.let { p -> listOfNotNull(p.name, p.city).joinToString(" · ") } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(if (projects.isEmpty()) "No projects available" else "Select a project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(projectMenuOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = dealioFieldColors(),
                )
                DropdownMenu(expanded = projectMenuOpen, onDismissRequest = { projectMenuOpen = false }) {
                    projects.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(listOfNotNull(p.name, p.city).joinToString(" · ")) },
                            onClick = { selectedProject = p; projectMenuOpen = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Customer ──
            SectionLabel("Customer")
            Spacer(Modifier.height(8.dp))
            if (contacts.isNotEmpty()) {
                Text("Pick from contacts", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    contacts.forEach { c ->
                        val sel = name == c.name && phone == c.phone
                        Text(
                            c.name,
                            color = if (sel) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                                .border(1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(10.dp))
                                .clickable { name = c.name; phone = c.phone; email = c.email ?: "" }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Customer name") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Email (optional)") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(20.dp))

            val project = selectedProject
            val canSubmit = !working && project != null && name.isNotBlank() && phone.length >= 6
            Button(
                onClick = { if (project != null) onSubmit(project.id, name.trim(), phone, email) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
            ) {
                if (working) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                else Text("Add lead", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
