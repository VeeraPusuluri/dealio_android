package com.dealio.app.ui.cp.projects

import android.app.Application
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.ProjectDocument
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.customer.project.ProjectImagePager
import com.dealio.app.ui.customer.project.galleryUrls
import com.dealio.app.ui.customer.project.projectDetailSections
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpProjectDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val project: Project? = null,
    val documents: List<ProjectDocument> = emptyList(),
    val working: Boolean = false,
    val message: String? = null,
    val shareUrl: String? = null,
)

class CpProjectDetailViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpProjectDetailState())
    val state: StateFlow<CpProjectDetailState> = _state.asStateFlow()

    fun load(id: Long) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getProject(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, project = r.data) }
                    // Project photos / floor plans for the gallery + plan sections (best-effort).
                    r.data.builderId?.let { bid ->
                        val docs = (repo.getProjectDocuments(bid, id) as? ApiResult.Success)?.data ?: emptyList()
                        _state.update { it.copy(documents = docs) }
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun share() {
        val id = _state.value.project?.id ?: return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val r = repo.getShareLink(id)) {
                is ApiResult.Success -> _state.update { it.copy(working = false, shareUrl = r.data.url) }
                is ApiResult.Error -> _state.update { it.copy(working = false, message = r.message) }
            }
        }
    }

    fun addLead(name: String, phone: String, email: String) {
        val id = _state.value.project?.id ?: return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.createLead(id, name, phone, email)
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Lead added — find it under Leads.") }
        }
    }

    fun bookVisit(customerName: String, customerPhone: String, date: String, time: String, type: String, notes: String, onDone: () -> Unit) {
        val p = _state.value.project ?: return
        val builderId = p.builderId ?: run {
            _state.update { it.copy(message = "This project can't take bookings right now — builder details are unavailable.") }
            return
        }
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.bookVisit(builderId, p.id, customerName, customerPhone, date, time, type, notes.ifBlank { null })
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Visit booked for $customerName — the builder will confirm shortly.") }
            if (r is ApiResult.Success) onDone()
        }
    }

    fun clearShareUrl() = _state.update { it.copy(shareUrl = null) }
    fun clearMessage() = _state.update { it.copy(message = null) }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CpProjectDetailScreen(nav: NavController, projectId: Long, vm: CpProjectDetailViewModel = viewModel()) {
    LaunchedEffect(projectId) { vm.load(projectId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAddLead by remember { mutableStateOf(false) }
    var showBooking by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    LaunchedEffect(state.shareUrl) {
        state.shareUrl?.let { url ->
            val p = state.project
            val text = "Check out ${p?.name ?: "this project"} on Dealio: $url"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            runCatching { context.startActivity(Intent.createChooser(send, "Share project")) }
            vm.clearShareUrl()
        }
    }

    val p = state.project
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(p?.name ?: "Project", fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1) },
                navigationIcon = { IconButton(onClick = { nav.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy) } },
                actions = {
                    if (p != null) {
                        IconButton(onClick = { showAddLead = true }) {
                            Icon(Icons.Outlined.PersonAdd, "Add lead", tint = Navy)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Navy),
            )
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(projectId) }, modifier = Modifier.padding(inner))
            p != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    // Rich image carousel (below the app bar, which already shows the name)
                    item { ProjectImagePager(galleryUrls(p, state.documents), p.name, height = 220.dp) }

                    // Locality + the CP's commission callout
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(listOfNotNull(p.locality, p.city).joinToString(", ").ifBlank { "—" }, color = TextSecondary, fontSize = 13.sp)
                            }
                            if ((p.commissionValue ?: 0.0) > 0) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Earn ${p.commissionValue}% commission",
                                    color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.background(Teal.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }

                            // Both actions sit under the project's identity, where a
                            // partner can reach them without reading to the end and
                            // without a bar covering the page the whole way down.
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { vm.share() }, enabled = !state.working,
                                    modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp),
                                ) {
                                    Icon(Icons.Outlined.Share, null, tint = Navy, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp)); Text("Share link", color = Navy, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { showBooking = true },
                                    modifier = Modifier.weight(1.2f).height(46.dp), shape = RoundedCornerShape(13.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                ) {
                                    Icon(Icons.Outlined.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp)); Text("Book a visit", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // The full project detail — shared verbatim with the customer view
                    // (read-only configurations; no shortlist/get-price actions for the CP).
                    projectDetailSections(p = p, documents = state.documents, showConfigActions = false)
                }
        }
    }

    if (showAddLead && p != null) {
        AddLeadDialog(working = state.working, onDismiss = { showAddLead = false }) { name, phone, email ->
            vm.addLead(name, phone, email); showAddLead = false
        }
    }

    if (showBooking && p != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showBooking = false }, sheetState = sheetState, containerColor = Color.White) {
            CpBookingSheet(projectName = p.name, working = state.working) { name, phone, date, time, type, notes ->
                vm.bookVisit(name, phone, date, time, type, notes) { showBooking = false }
            }
        }
    }
}

private val bookingTimeSlots = listOf("10:00 AM", "11:00 AM", "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM")
private val bookingVisitTypes = listOf("Site Visit", "Virtual Tour", "Office Meeting")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CpBookingSheet(
    projectName: String,
    working: Boolean,
    onConfirm: (name: String, phone: String, date: String, time: String, type: String, notes: String) -> Unit,
) {
    val dates = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()) } }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(dates.first()) }
    var selectedTime by remember { mutableStateOf(bookingTimeSlots.first()) }
    var selectedType by remember { mutableStateOf(bookingVisitTypes.first()) }
    var notes by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Book a visit", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("with the builder of $projectName", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        SectionLabel("Customer")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Customer name") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = phone, onValueChange = { phone = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Customer phone") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        Spacer(Modifier.height(16.dp))

        SectionLabel("Date")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dates.forEach { d ->
                val label = "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
                BookingChip(label, d == selectedDate) { selectedDate = d }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Time")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookingTimeSlots.forEach { t -> BookingChip(t, t == selectedTime) { selectedTime = t } }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Type")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookingVisitTypes.forEach { t -> BookingChip(t, t == selectedType) { selectedType = t } }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") }, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(), minLines = 2,
        )
        Spacer(Modifier.height(20.dp))

        val canBook = !working && name.isNotBlank() && phone.length >= 6
        Button(
            onClick = { onConfirm(name.trim(), phone, selectedDate.toString(), selectedTime, selectedType, notes) },
            enabled = canBook,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) {
            if (working) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
            else {
                Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Book visit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BookingChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun AddLeadDialog(working: Boolean, onDismiss: () -> Unit, onConfirm: (name: String, phone: String, email: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name, phone, email) }, enabled = !working && phone.length >= 6) {
                Text("Add lead", color = Teal, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        title = { Text("Add a lead", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Customer name") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email (optional)") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors())
            }
        },
    )
}
