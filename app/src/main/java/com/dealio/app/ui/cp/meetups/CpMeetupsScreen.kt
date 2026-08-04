package com.dealio.app.ui.cp.meetups

import android.app.Application
import androidx.compose.material.icons.outlined.Phone
import androidx.core.net.toUri
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.CpMeetup
import com.dealio.app.data.api.CpMeetupInviteePayload
import com.dealio.app.data.api.CreateCpMeetupRequest
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.components.shareViaWhatsApp
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Mist
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.subtleShadow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CpMeetupsState(
    val loading: Boolean = true,
    val error: String? = null,
    val meetups: List<CpMeetup> = emptyList(),
    val contacts: List<CpContact> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
)

class CpMeetupsViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpMeetupsState())
    val state: StateFlow<CpMeetupsState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val meetups = repo.getMeetups()
            // Contacts are the invite list, so they load alongside rather than on
            // opening the form — a partner arranging something should not wait.
            val contacts = repo.getContacts()
            _state.update {
                when (meetups) {
                    is ApiResult.Success -> it.copy(
                        loading = false, error = null, meetups = meetups.data,
                        contacts = (contacts as? ApiResult.Success)?.data ?: it.contacts,
                    )
                    is ApiResult.Error -> it.copy(loading = false, error = meetups.message)
                }
            }
        }
    }

    fun create(req: CreateCpMeetupRequest, onDone: () -> Unit) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val r = repo.createMeetup(req)
            _state.update {
                it.copy(
                    saving = false,
                    message = (r as? ApiResult.Error)?.message ?: "Meetup created — share it with your invitees.",
                )
            }
            if (r is ApiResult.Success) { load(silent = true); onDone() }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val r = repo.deleteMeetup(id)
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Meetup removed") }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun CpMeetupsScreen(nav: NavController, vm: CpMeetupsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showForm by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CpMeetup?>(null) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    SubScreenScaffold("Meetups", nav) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
                state.meetups.isEmpty() -> PortalEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "No meetups yet",
                    subtitle = "Arrange a site walk-through or an investor evening, pick who to invite, and send it over WhatsApp.",
                    actionLabel = "Create a meetup",
                    onAction = { showForm = true },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.meetups.size) { i ->
                        MeetupCard(
                            state.meetups[i],
                            onOpen = { detail = state.meetups[i] },
                            onDelete = { vm.delete(state.meetups[i].id) },
                        )
                    }
                }
            }

            if (state.meetups.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showForm = true },
                    containerColor = Teal,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New meetup", fontWeight = FontWeight.SemiBold)
                }
            }

            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }

    detail?.let { m ->
        MeetupDetailDialog(m, onDismiss = { detail = null })
    }

    if (showForm) {
        MeetupForm(
            contacts = state.contacts,
            saving = state.saving,
            onDismiss = { showForm = false },
            onCreate = { req -> vm.create(req) { showForm = false } },
        )
    }
}

@Composable
private fun MeetupCard(m: CpMeetup, onOpen: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .subtleShadow(radius = 18.dp)
            .clip(shape)
            .background(Color.White, shape)
            .border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Text(m.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        DetailLine(Icons.Outlined.CalendarMonth, formatDate(m.date))
        Spacer(Modifier.height(5.dp))
        DetailLine(Icons.Outlined.Schedule, m.time)
        Spacer(Modifier.height(5.dp))
        DetailLine(Icons.Outlined.LocationOn, m.location)

        if (m.invitees.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionLabel("${m.invitees.size} invited")
            Spacer(Modifier.height(7.dp))
            Text(
                m.invitees.joinToString(", ") { it.name },
                color = TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (!m.notes.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(m.notes!!, color = TextSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Teal.copy(alpha = 0.10f))
                    .clickable { shareViaWhatsApp(ctx, meetupShareText(m), "Share meetup") }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Share, null, tint = Teal, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text("Share invite", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Remove", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The invite as a message.
 *
 * Reads as an invitation rather than a record — what, when, where — because it
 * lands in a chat with someone who was not looking at the app. The maps link
 * goes last, where a phone will make it tappable without breaking the address
 * above it.
 */
private fun meetupShareText(m: CpMeetup): String = buildString {
    appendLine(m.title)
    appendLine()
    appendLine("When: ${formatDate(m.date)}, ${m.time}")
    appendLine("Where: ${m.location}")
    if (!m.notes.isNullOrBlank()) {
        appendLine()
        appendLine(m.notes!!)
    }
    if (!m.mapsLink.isNullOrBlank()) {
        appendLine()
        append(m.mapsLink)
    }
}.trim()

@Composable
private fun MeetupForm(
    contacts: List<CpContact>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateCpMeetupRequest) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mapsLink by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var selectedTime by remember { mutableStateOf(meetupTimes[3]) }
    val picked = remember { mutableStateOf(setOf<Long>()) }

    val canSave = title.isNotBlank() && location.isNotBlank() && !saving

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                SectionLabel("New meetup")
                Spacer(Modifier.height(4.dp))
                Text("Everyone you pick gets the same invite.", color = TextSecondary, fontSize = 12.sp)

                Spacer(Modifier.height(16.dp))
                FormLabel("What is it")
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("e.g. Nature County site walk-through", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.height(14.dp))
                FormLabel("Where")
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Address or landmark", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(), minLines = 2,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mapsLink, onValueChange = { mapsLink = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Google Maps link (optional)", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.height(14.dp))
                FormLabel("When")
                DayStrip(selectedDate) { selectedDate = it }
                Spacer(Modifier.height(8.dp))
                ChipWrap(meetupTimes, selectedTime) { selectedTime = it }

                Spacer(Modifier.height(14.dp))
                FormLabel(if (picked.value.isEmpty()) "Who to invite" else "Who to invite · ${picked.value.size} selected")
                if (contacts.isEmpty()) {
                    Text(
                        "No contacts yet. Add them under More → Contacts and they'll show up here.",
                        color = TextSecondary, fontSize = 12.sp,
                    )
                } else {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState()),
                    ) {
                        contacts.forEach { c ->
                            ContactPickRow(c, picked.value.contains(c.id)) {
                                picked.value = if (picked.value.contains(c.id)) picked.value - c.id else picked.value + c.id
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                FormLabel("Notes")
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    placeholder = { Text("Anything they should know", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onCreate(
                                CreateCpMeetupRequest(
                                    title = title.trim(),
                                    location = location.trim(),
                                    date = selectedDate,
                                    time = selectedTime,
                                    mapsLink = mapsLink.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null },
                                    invitees = contacts.filter { picked.value.contains(it.id) }
                                        .map { CpMeetupInviteePayload(contactId = it.id, name = it.name, phone = it.phone) },
                                ),
                            )
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text(if (saving) "Creating…" else "Create meetup", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun ContactPickRow(c: CpContact, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle() }.padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (selected) Teal else Mist),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(17.dp))
            } else {
                Text(initialsOf(c.name), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(c.phone, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

/** The next fortnight. A meetup is arranged days ahead, not months. */
@Composable
private fun DayStrip(selected: String, onPick: (String) -> Unit) {
    val days = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()) } }
    ChipWrap(days.map { it.toString() }, selected, label = { iso -> dayChipLabel(iso) }, onPick = onPick)
}

private fun dayChipLabel(iso: String): String {
    val d = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> "${d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}"
    }
}

/** Chips wrap rather than scroll: inside a dialog a sideways strip hides options. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipWrap(
    options: List<String>,
    selected: String,
    label: (String) -> String = { it },
    onPick: (String) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val on = opt == selected
            Text(
                label(opt),
                color = if (on) Color.White else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Teal else Color.White)
                    .border(1.dp, if (on) Teal else CardBorder, RoundedCornerShape(10.dp))
                    .clickable { onPick(opt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

private val meetupTimes = listOf("09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM", "06:00 PM")


/**
 * The whole meetup.
 *
 * The card is a summary — it truncates the invite list and the notes because a
 * list of cards has to stay scannable. This is where a partner checks who they
 * actually asked and reaches one of them, so every invitee is listed with their
 * number and each is callable.
 */
@Composable
private fun MeetupDetailDialog(m: CpMeetup, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                SectionLabel("Meetup")
                Spacer(Modifier.height(6.dp))
                Text(m.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(16.dp))
                DetailLine(Icons.Outlined.CalendarMonth, formatDate(m.date))
                Spacer(Modifier.height(7.dp))
                DetailLine(Icons.Outlined.Schedule, m.time)
                Spacer(Modifier.height(7.dp))
                DetailLine(Icons.Outlined.LocationOn, m.location)

                if (!m.mapsLink.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Open in Maps", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp))
                            .background(Teal.copy(alpha = 0.10f))
                            .clickable {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, m.mapsLink!!.toUri())) }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                if (!m.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Notes")
                    Spacer(Modifier.height(6.dp))
                    Text(m.notes!!, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel(if (m.invitees.isEmpty()) "No one invited yet" else "Invited · ${m.invitees.size}")
                Spacer(Modifier.height(8.dp))
                m.invitees.forEach { g ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${g.phone}".toUri())) } }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Mist),
                            contentAlignment = Alignment.Center,
                        ) { Text(initialsOf(g.name), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(g.phone, color = TextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Outlined.Phone, "Call ${g.name}", tint = Teal, modifier = Modifier.size(17.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { shareViaWhatsApp(ctx, meetupShareText(m), "Share meetup") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) {
                        Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Share invite", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
