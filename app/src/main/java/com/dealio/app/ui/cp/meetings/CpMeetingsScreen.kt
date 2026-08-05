package com.dealio.app.ui.cp.meetings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.Meeting
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.components.CalMeeting
import com.dealio.app.ui.components.FormSheet
import com.dealio.app.ui.components.ListCalendarToggle
import com.dealio.app.ui.components.MeetingsCalendar
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.SheetField
import com.dealio.app.ui.components.SheetSection
import com.dealio.app.ui.components.SheetSubmitButton
import com.dealio.app.ui.components.calDate
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.components.meetingStatusColor
import com.dealio.app.ui.components.meetingTypes
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.cp.leads.onePerPerson
import com.dealio.app.ui.meetups.ChoiceChips
import com.dealio.app.ui.meetups.meetupDayLabel
import com.dealio.app.ui.meetups.meetupTimes
import java.time.LocalDate
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpMeetingsState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<Meeting> = emptyList(),
    val message: String? = null,
    // Pickers for the "New meeting" form
    val projects: List<Project> = emptyList(),
    val contacts: List<CpContact> = emptyList(),
    val booking: Boolean = false,
)

class CpMeetingsViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpMeetingsState())
    val state: StateFlow<CpMeetingsState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMeetings()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
            // Pickers for the booking form (best-effort; failures leave them empty).
            (repo.getProjects() as? ApiResult.Success)?.let { r -> _state.update { it.copy(projects = r.data) } }
            (repo.getContacts() as? ApiResult.Success)?.let { r ->
                _state.update { it.copy(contacts = onePerPerson(r.data)) }
            }
        }
    }

    /**
     * Asks the builder for a slot on a buyer's behalf.
     *
     * It goes out as a request, not a booking: the builder confirms the time,
     * which is why the new row lands as "Pending". The server can refuse — the
     * slot may already be confirmed for someone else, or the customer may be
     * inside another partner's 90-day lock — so its reason is what the CP is
     * shown rather than a generic failure.
     */
    fun book(
        builderId: Long,
        projectId: Long,
        customerName: String,
        customerPhone: String,
        date: String,
        time: String,
        type: String,
        notes: String,
        onDone: () -> Unit,
    ) {
        _state.update { it.copy(booking = true) }
        viewModelScope.launch {
            val r = repo.bookVisit(
                builderId = builderId,
                projectId = projectId,
                customerName = customerName.trim(),
                customerPhone = customerPhone,
                date = date,
                time = time,
                type = type,
                notes = notes.trim().ifBlank { null },
            )
            when (r) {
                is ApiResult.Success -> {
                    onDone()
                    _state.update {
                        it.copy(booking = false, message = "Requested for ${customerName.trim()} — waiting on the builder to confirm.")
                    }
                    load(silent = true)
                }
                is ApiResult.Error -> _state.update { it.copy(booking = false, message = r.message) }
            }
        }
    }

    fun saveNote(meetingId: Long, notes: String, rating: Int?) {
        viewModelScope.launch {
            val r = repo.addMeetingNote(meetingId, notes, rating)
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Note saved") }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun CpMeetingsScreen(nav: NavController, vm: CpMeetingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var noteTarget by remember { mutableStateOf<Meeting?>(null) }
    var calendar by remember { mutableStateOf(false) }
    var booking by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    // A refused booking says why — a slot already taken, or another partner's
    // lock on this buyer — so it has to be shown, not swallowed.
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val calMeetings = state.items.mapNotNull { m ->
        val d = calDate(m.confirmedDate ?: m.preferredDate) ?: return@mapNotNull null
        CalMeeting(
            id = "mtg-${m.id}", date = d,
            time = (m.confirmedTime ?: m.preferredTime).ifBlank { null },
            title = m.customerName.ifBlank { "Visitor" }, subtitle = m.projectName,
            status = m.status, color = meetingStatusColor(m.status),
        )
    }

    SubScreenScaffold(
        "Meetings", nav,
        actions = { NewMeetingButton { booking = true } },
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() }, modifier = Modifier.padding(inner))
            else -> Column(Modifier.padding(inner).fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    ListCalendarToggle(calendar = calendar, onChange = { calendar = it })
                }
                if (calendar) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        MeetingsCalendar(calMeetings)
                    }
                } else if (state.items.isEmpty()) {
                    PortalEmptyState(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "No meetings",
                        subtitle = "Ask a builder to hold a slot for one of your buyers, and it appears here once they confirm.",
                        actionLabel = "Arrange a meeting",
                        onAction = { booking = true },
                    )
                } else LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items.size) { i ->
                        val m = state.items[i]
                        DealioCard(Modifier.clickable { noteTarget = m }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.customerName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(m.projectName, color = TextSecondary, fontSize = 12.sp)
                            }
                            StatusChip(m.status)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(formatDate(m.confirmedDate ?: m.preferredDate), color = TextPrimary, fontSize = 13.sp)
                            Spacer(Modifier.width(14.dp))
                            Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(m.confirmedTime ?: m.preferredTime, color = TextPrimary, fontSize = 13.sp)
                        }
                        if (!m.cpNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Your note: ${m.cpNotes}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }

    noteTarget?.let { m ->
        MeetingNoteDialog(m, onDismiss = { noteTarget = null }) { notes, rating ->
            vm.saveNote(m.id, notes, rating); noteTarget = null
        }
    }

    if (booking) {
        NewMeetingSheet(
            projects = state.projects,
            contacts = state.contacts,
            working = state.booking,
            onDismiss = { booking = false },
            onSubmit = { project, name, phone, date, time, type, notes ->
                // A project with no builder behind it cannot be booked against —
                // the meeting row is the builder's to confirm.
                project.builderId?.let { builderId ->
                    vm.book(builderId, project.id, name, phone, date, time, type, notes) { booking = false }
                }
            },
        )
    }
}

/** Header action, matching the "Add" pill the leads and contacts pages use. */
@Composable
private fun NewMeetingButton(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(end = 8.dp)
            .background(Teal, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text("New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Arranging a builder appointment for a buyer.
 *
 * Everything the form needs is already in the partner's own data, so it asks
 * for as little typing as possible: the project carries the builder, a saved
 * contact carries the name and number, and the day and time are picked from
 * the same chips the meetup form uses rather than typed as text.
 */
@Composable
private fun NewMeetingSheet(
    projects: List<Project>,
    contacts: List<CpContact>,
    working: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        project: Project, name: String, phone: String,
        date: String, time: String, type: String, notes: String,
    ) -> Unit,
) {
    var project by remember { mutableStateOf<Project?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var time by remember { mutableStateOf("11:00 AM") }
    var type by remember { mutableStateOf(meetingTypes.first()) }
    var notes by remember { mutableStateOf("") }

    val days = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()).toString() } }
    val chosen = project
    val canSubmit = chosen != null && name.isNotBlank() && phone.length >= 6

    FormSheet(
        title = "New meeting",
        subtitle = "Ask a builder to hold a slot for one of your buyers.",
        icon = Icons.Outlined.CalendarMonth,
        onDismiss = onDismiss,
        footer = {
            if (!canSubmit) {
                val missing = listOfNotNull(
                    "a project".takeIf { chosen == null },
                    "a name".takeIf { name.isBlank() },
                    "a phone number".takeIf { phone.length < 6 },
                )
                Text("Still needed: ${missing.joinToString(", ")}", color = TextSecondary, fontSize = 11.5.sp)
                Spacer(Modifier.height(10.dp))
            }
            SheetSubmitButton(
                text = "Request meeting",
                enabled = canSubmit,
                working = working,
                onClick = { chosen?.let { onSubmit(it, name.trim(), phone, date, time, type, notes) } },
            )
        },
    ) {
        SheetSection("Project") {
            if (projects.isEmpty()) {
                Text("No projects to book against yet.", color = TextSecondary, fontSize = 12.5.sp)
            } else {
                ChoiceChips(
                    options = projects,
                    selected = chosen,
                    label = { it.name },
                    onPick = { p -> project = if (chosen?.id == p.id) null else p },
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("Customer") {
            if (contacts.isNotEmpty()) {
                Text("From your contacts", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                ChoiceChips(
                    options = contacts,
                    selected = contacts.firstOrNull { it.name == name && it.phone == phone },
                    label = { it.name },
                    onPick = { c -> name = c.name; phone = c.phone },
                )
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
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("When") {
            ChoiceChips(days, date, label = { meetupDayLabel(it) }, onPick = { date = it })
            Spacer(Modifier.height(2.dp))
            ChoiceChips(meetupTimes, time, label = { it }, onPick = { time = it })
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("Kind of meeting") {
            ChoiceChips(meetingTypes, type, label = { it }, onPick = { type = it })
            SheetField(
                value = notes, onValueChange = { notes = it },
                label = "Notes for the builder (optional)",
                placeholder = "What the buyer wants to see",
                singleLine = false, minLines = 2,
            )
        }
    }
}

@Composable
private fun MeetingNoteDialog(m: Meeting, onDismiss: () -> Unit, onSave: (String, Int?) -> Unit) {
    var notes by remember { mutableStateOf(m.cpNotes ?: "") }
    var rating by remember { mutableIntStateOf(m.cpRating ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(notes.trim(), rating.takeIf { it > 0 }) }) { Text("Save", color = Teal, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        title = { Text("Meeting note", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text("Rate this visit", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row {
                    (1..5).forEach { s ->
                        Icon(
                            if (rating >= s) Icons.Filled.Star else Icons.Outlined.StarBorder, "Rate $s",
                            tint = if (rating >= s) Orange else TextSecondary,
                            modifier = Modifier.size(28.dp).padding(end = 4.dp).clickable { rating = s },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Notes") }, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(), minLines = 2)
            }
        },
    )
}
