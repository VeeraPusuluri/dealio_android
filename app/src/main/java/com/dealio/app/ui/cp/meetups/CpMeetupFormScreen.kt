package com.dealio.app.ui.cp.meetups

import android.app.Application
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpMeetup
import com.dealio.app.data.api.CreateCpMeetupRequest
import com.dealio.app.data.api.UpdateCpMeetupRequest
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.meetups.ChoiceChips
import com.dealio.app.ui.meetups.FormLabel
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.meetupDayLabel
import com.dealio.app.ui.meetups.meetupTimes
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MeetupFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val people: List<Invitee> = emptyList(),
    val editing: CpMeetup? = null,
    val message: String? = null,
    val savedId: Long? = null,
)

class CpMeetupFormViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(MeetupFormState())
    val state: StateFlow<MeetupFormState> = _state.asStateFlow()

    /**
     * Loads the invite list, and the meetup itself when editing.
     *
     * The invite list is fetched up front rather than when the picker opens: a
     * partner filling this in should never wait on a spinner to answer the one
     * question the form is actually about.
     */
    fun start(meetupId: Long?) {
        if (_state.value.people.isNotEmpty() || _state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val invitable = repo.getInvitable()
            val editing = meetupId?.let { id -> (repo.getMeetup(id) as? ApiResult.Success)?.data }
            _state.update {
                it.copy(
                    loading = false,
                    people = (invitable as? ApiResult.Success)?.data?.let(::mergeInvitable) ?: emptyList(),
                    editing = editing,
                )
            }
        }
    }

    fun create(req: CreateCpMeetupRequest) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.createMeetup(req)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        saving = false,
                        savedId = r.data.id,
                        message = if (req.invitees.isEmpty()) "Meetup created"
                        else "Meetup created — ${req.invitees.size} invited",
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }

    fun update(id: Long, req: UpdateCpMeetupRequest) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.updateMeetup(id, req)) {
                is ApiResult.Success -> _state.update { it.copy(saving = false, savedId = r.data.id, message = "Changes saved") }
                is ApiResult.Error -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/**
 * Create or edit a meetup.
 *
 * A full screen rather than the dialog this used to be. Once a meetup carries a
 * category, a visibility, a city and an invite list, a capped-height dialog
 * hides most of the form behind a scroll — and the invite list, the part that
 * decides whether anyone turns up, was the part getting hidden.
 */
@Composable
fun CpMeetupFormScreen(
    nav: NavController,
    meetupId: Long?,
    vm: CpMeetupFormViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(meetupId) { vm.start(meetupId) }

    // Leave once saved. The detail screen reloads on resume, so an edit shows
    // there without this screen having to hand anything back.
    LaunchedEffect(state.savedId) { if (state.savedId != null) nav.popBackStack() }

    val editing = state.editing
    val isEdit = meetupId != null

    SubScreenScaffold(if (isEdit) "Edit meetup" else "New meetup", nav) { inner ->
        if (state.loading) {
            LoadingState(Modifier.padding(inner))
            return@SubScreenScaffold
        }
        // Keyed on the loaded row so the fields seed once it arrives, rather
        // than staying empty because they were remembered before the fetch.
        key(editing?.id ?: 0L) {
            MeetupFormBody(
                inner = inner,
                editing = editing,
                people = state.people,
                saving = state.saving,
                message = state.message,
                onDismissMessage = vm::clearMessage,
                onSubmit = { create, update, invitees ->
                    if (isEdit && editing != null) vm.update(editing.id, update)
                    else vm.create(create.copy(invitees = invitees.map { it.toPayload() }))
                },
            )
        }
    }
}

@Composable
private fun MeetupFormBody(
    inner: androidx.compose.foundation.layout.PaddingValues,
    editing: CpMeetup?,
    people: List<Invitee>,
    saving: Boolean,
    message: String?,
    onDismissMessage: () -> Unit,
    onSubmit: (CreateCpMeetupRequest, UpdateCpMeetupRequest, List<Invitee>) -> Unit,
) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    // A new meetup starts on the commonest kind, not on OTHER — `from(null)`
    // falls through to OTHER, which is the right answer for an unknown value off
    // the wire and the wrong one for a blank form.
    var category by remember {
        mutableStateOf(editing?.let { MeetupCategory.from(it.category) } ?: MeetupCategory.SITE_VISIT)
    }
    var mode by remember { mutableStateOf(MeetupMode.from(editing?.mode)) }
    var location by remember { mutableStateOf(editing?.location ?: "") }
    var city by remember { mutableStateOf(editing?.city ?: "") }
    var mapsLink by remember { mutableStateOf(editing?.mapsLink ?: "") }
    var onlineLink by remember { mutableStateOf(editing?.onlineLink ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var capacity by remember { mutableStateOf(editing?.capacity?.toString() ?: "") }
    var isPublic by remember { mutableStateOf(editing?.isPublic ?: true) }
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().plusDays(1).toString()) }
    var time by remember { mutableStateOf(editing?.time ?: "04:00 PM") }
    var picked by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(message) { if (message != null) onDismissMessage() }

    val needsPlace = mode != MeetupMode.ONLINE
    val canSave = title.isNotBlank() && (location.isNotBlank() || !needsPlace) && !saving

    Column(
        Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // ── What ────────────────────────────────────────────────────────────
        DealioCard {
            FormLabel("What is it")
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. Nature County site walk-through", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
            Spacer(Modifier.height(14.dp))
            FormLabel("Kind of meetup")
            ChoiceChips(
                options = MeetupCategory.entries,
                selected = category,
                label = { it.label },
                tint = { it.tint },
                onPick = { category = it },
            )
            Spacer(Modifier.height(14.dp))
            FormLabel("Description")
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
                placeholder = { Text("What will happen, and why someone should come", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── When ────────────────────────────────────────────────────────────
        DealioCard {
            FormLabel("When")
            DayStrip(date) { date = it }
            Spacer(Modifier.height(10.dp))
            ChoiceChips(meetupTimes, time, label = { it }, onPick = { time = it })
        }

        Spacer(Modifier.height(12.dp))

        // ── Where ───────────────────────────────────────────────────────────
        DealioCard {
            FormLabel("Where")
            ChoiceChips(MeetupMode.entries, mode, label = { it.label }, onPick = { mode = it })
            if (needsPlace) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    placeholder = { Text("Address or landmark", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mapsLink, onValueChange = { mapsLink = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Google Maps link (optional)", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
            }
            if (mode != MeetupMode.IN_PERSON) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = onlineLink, onValueChange = { onlineLink = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Zoom or Meet link", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Only people who say they're going will see this link.",
                    color = TextSecondary, fontSize = 11.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Who can see it ──────────────────────────────────────────────────
        DealioCard {
            FormLabel("Who can see this")
            VisibilityToggle(isPublic = isPublic, onChange = { isPublic = it })
            if (isPublic) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = city, onValueChange = { city = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("City") },
                    placeholder = { Text("e.g. Hyderabad", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (city.isBlank())
                        "Set a city, or customers browsing won't find this."
                    else
                        "Customers who follow $city will see this in their app.",
                    color = if (city.isBlank()) com.dealio.app.ui.theme.Orange else TextSecondary,
                    fontSize = 11.sp, lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            FormLabel("Limit numbers (optional)")
            OutlinedTextField(
                value = capacity, onValueChange = { v -> capacity = v.filter { it.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Leave blank for no limit", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Who to invite. Creation only: on an edit the invite list is
        // managed from the event page, where the RSVPs are.
        if (editing == null) {
            DealioCard {
                FormLabel(invitePickerLabel(picked.size))
                Text(
                    "Everyone you pick gets the same invite. Dealio customers see it in their app too.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                )
                Spacer(Modifier.height(12.dp))
                InvitePicker(
                    people = people,
                    selected = picked,
                    onToggle = { p -> picked = if (picked.contains(p.key)) picked - p.key else picked + p.key },
                )
                if (picked.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SelectedCountBar(picked.size)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Private note ────────────────────────────────────────────────────
        DealioCard {
            FormLabel("Private note")
            Text("Only you see this — it is not part of the invite.", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
        }

        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = when {
                saving -> "Saving…"
                editing != null -> "Save changes"
                picked.isEmpty() -> "Create meetup"
                else -> "Create and invite ${picked.size}"
            },
            enabled = canSave,
            onClick = {
                val cap = capacity.toIntOrNull()?.takeIf { it > 0 }
                val cityValue = city.trim().ifBlank { null }
                onSubmit(
                    CreateCpMeetupRequest(
                        title = title.trim(),
                        location = location.trim().ifBlank { if (needsPlace) "" else "Online" },
                        date = date,
                        time = time,
                        description = description.trim().ifBlank { null },
                        category = category.wire,
                        city = cityValue,
                        mapsLink = mapsLink.trim().ifBlank { null },
                        mode = mode.wire,
                        onlineLink = onlineLink.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null },
                        visibility = if (isPublic) "PUBLIC" else "PRIVATE",
                        capacity = cap,
                    ),
                    UpdateCpMeetupRequest(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        category = category.wire,
                        location = location.trim().ifBlank { if (needsPlace) "" else "Online" },
                        city = cityValue,
                        mapsLink = mapsLink.trim().ifBlank { null },
                        mode = mode.wire,
                        onlineLink = onlineLink.trim().ifBlank { null },
                        date = date,
                        time = time,
                        notes = notes.trim().ifBlank { null },
                        visibility = if (isPublic) "PUBLIC" else "PRIVATE",
                        capacity = cap,
                    ),
                    people.filter { picked.contains(it.key) },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Public or invite-only, spelled out.
 *
 * A switch would make this a setting people skim past. Two labelled cards, each
 * saying what actually happens, make it a decision — which it is, because one of
 * them puts a partner's name and a physical address in front of strangers.
 */
@Composable
private fun VisibilityToggle(isPublic: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VisibilityOption(
            title = "Anyone in the city",
            body = "Customers browsing meetups will find it",
            selected = isPublic,
            modifier = Modifier.weight(1f),
        ) { onChange(true) }
        VisibilityOption(
            title = "Invite only",
            body = "Only the people you pick can see it",
            selected = !isPublic,
            modifier = Modifier.weight(1f),
        ) { onChange(false) }
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    body: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Teal.copy(alpha = 0.08f) else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Teal else CardBorder, shape)
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Text(
            title,
            color = if (selected) Teal else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Text(body, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

/** The next fortnight. A meetup is arranged days ahead, not months. */
@Composable
private fun DayStrip(selected: String, onPick: (String) -> Unit) {
    val days = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()).toString() } }
    ChoiceChips(days, selected, label = { meetupDayLabel(it) }, onPick = onPick)
}
