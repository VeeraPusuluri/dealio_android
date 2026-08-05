package com.dealio.app.ui.cp.meetups

import android.app.Application
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpMeetup
import com.dealio.app.data.api.CpMeetupInvitee
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.components.shareViaWhatsApp
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.meetups.CategoryChip
import com.dealio.app.ui.meetups.ChoiceChips
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupDetailLine
import com.dealio.app.ui.meetups.MeetupHero
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.MeetupPhotoGrid
import com.dealio.app.ui.meetups.MeetupPhotoViewer
import com.dealio.app.ui.meetups.TopicChips
import com.dealio.app.ui.meetups.Rsvp
import com.dealio.app.ui.meetups.RsvpPill
import com.dealio.app.ui.meetups.RsvpSummary
import com.dealio.app.ui.meetups.VisibilityChip
import com.dealio.app.ui.meetups.addMeetupToCalendar
import com.dealio.app.ui.meetups.meetupDayLabel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.Mist
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeetupDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val meetup: CpMeetup? = null,
    val people: List<Invitee> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val deleted: Boolean = false,
)

class CpMeetupDetailViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(MeetupDetailState())
    val state: StateFlow<MeetupDetailState> = _state.asStateFlow()
    private var id: Long = 0

    fun load(meetupId: Long, silent: Boolean = false) {
        id = meetupId
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMeetup(meetupId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, meetup = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
            // Fetched after the meetup so the page paints first — the invite list
            // is only needed once the organiser reaches for "Invite more".
            if (_state.value.people.isEmpty()) {
                (repo.getInvitable() as? ApiResult.Success)?.let { r ->
                    _state.update { it.copy(people = mergeInvitable(r.data)) }
                }
            }
        }
    }

    /** Applies a result that returns the whole meetup, so the page never goes stale. */
    private fun apply(r: ApiResult<CpMeetup>, success: String?) {
        _state.update {
            when (r) {
                is ApiResult.Success -> it.copy(busy = false, meetup = r.data, message = success)
                is ApiResult.Error -> it.copy(busy = false, message = r.message)
            }
        }
    }

    fun invite(people: List<Invitee>) {
        if (people.isEmpty()) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = repo.addInvitees(id, people.map { it.toPayload() })
            apply(r, if (people.size == 1) "${people.first().name} invited" else "${people.size} invited")
        }
    }

    fun setRsvp(inviteeId: Long, rsvp: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch { apply(repo.setInviteeRsvp(id, inviteeId, rsvp), null) }
    }

    fun removeInvitee(inviteeId: Long) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch { apply(repo.removeInvitee(id, inviteeId), "Removed from the list") }
    }

    fun cancel(reason: String?) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch { apply(repo.cancelMeetup(id, reason), "Meetup cancelled") }
    }

    fun delete() {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = repo.deleteMeetup(id)) {
                is ApiResult.Success -> _state.update { it.copy(busy = false, deleted = true) }
                is ApiResult.Error -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/**
 * The event page.
 *
 * Meetup.com's centre of gravity is the event, not the list, and this is the
 * screen a partner works from: it is where they see who actually answered, add
 * the people they forgot, and send the invite again.
 */
@Composable
fun CpMeetupDetailScreen(
    nav: NavController,
    meetupId: Long,
    vm: CpMeetupDetailViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var inviting by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var photoAt by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(meetupId) { vm.load(meetupId) }
    // Picks up an edit made on the form screen, which pops straight back here.
    RefreshOnResume { if (!state.loading) vm.load(meetupId, silent = true) }
    LaunchedEffect(state.deleted) { if (state.deleted) nav.popBackStack() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    SubScreenScaffold("Meetup", nav) { inner ->
      Box(Modifier.fillMaxSize().padding(inner)) {
        val m = state.meetup
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(meetupId) })
            m == null -> ErrorState("Meetup not found", onRetry = { vm.load(meetupId) })
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                val category = MeetupCategory.from(m.category)
                // The organiser sees the same cover the customer will, so the
                // event page doubles as a check on what was uploaded.
                MeetupHero(category, height = 150, coverImage = m.coverImage)

                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryChip(category)
                        Spacer(Modifier.width(8.dp))
                        VisibilityChip(m.isPublic, m.city)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(m.title, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)

                    if (m.isCancelled) {
                        Spacer(Modifier.height(12.dp))
                        CancelledBanner(m.cancelReason)
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── When and where ──────────────────────────────────────
                    DealioCard {
                        MeetupDetailLine(Icons.Outlined.CalendarMonth, meetupDayLabel(m.date))
                        Spacer(Modifier.height(9.dp))
                        MeetupDetailLine(Icons.Outlined.Schedule, m.time)
                        if (MeetupMode.from(m.mode) != MeetupMode.ONLINE) {
                            Spacer(Modifier.height(9.dp))
                            MeetupDetailLine(Icons.Outlined.LocationOn, m.location, maxLines = 3)
                        }
                        if (!m.onlineLink.isNullOrBlank()) {
                            Spacer(Modifier.height(9.dp))
                            MeetupDetailLine(Icons.Outlined.Videocam, m.onlineLink!!, maxLines = 1)
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!m.mapsLink.isNullOrBlank()) {
                                SoftAction("Open in Maps", Icons.Outlined.LocationOn) {
                                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, m.mapsLink!!.toUri())) }
                                }
                            }
                            SoftAction("Add to calendar", Icons.Outlined.CalendarMonth) {
                                addMeetupToCalendar(ctx, m.title, m.location, m.description, m.date, m.time)
                            }
                        }
                    }

                    if (!m.description.isNullOrBlank() || m.topics.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        DealioCard {
                            SectionLabel("About")
                            if (!m.description.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(m.description!!, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                            if (m.topics.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                TopicChips(m.topics)
                            }
                        }
                    }

                    // The pictures as the invitee will see them — this page is
                    // also where an organiser checks what they actually uploaded.
                    if (m.photos.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        DealioCard {
                            SectionLabel("Photos")
                            Spacer(Modifier.height(10.dp))
                            MeetupPhotoGrid(m.photos, onOpen = { photoAt = it })
                        }
                    }

                    // ── Who is coming ───────────────────────────────────────
                    Spacer(Modifier.height(12.dp))
                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Who's coming", Modifier.weight(1f))
                            if (m.capacity != null) {
                                Text(
                                    "${m.counts.goingHeads}/${m.capacity}",
                                    color = if (m.isFull) Orange else TextSecondary,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        RsvpSummary(m.counts.going, m.counts.maybe, m.counts.noReply, fontSize = 13)

                        if (m.invitees.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            m.invitees.forEach { g ->
                                InviteeManageRow(
                                    invitee = g,
                                    enabled = !state.busy && !m.isCancelled,
                                    onCall = {
                                        runCatching {
                                            ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${g.phone}".toUri()))
                                        }
                                    },
                                    onSetRsvp = { vm.setRsvp(g.id, it) },
                                    onRemove = { vm.removeInvitee(g.id) },
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        SoftAction("Invite more people", Icons.Outlined.PersonAdd, enabled = !m.isCancelled) {
                            inviting = true
                        }
                    }

                    if (!m.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        PrivateNote(m.notes!!)
                    }

                    // ── Actions ─────────────────────────────────────────────
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { shareViaWhatsApp(ctx, meetupShareText(m), "Share meetup") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) {
                        Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share invite", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineAction("Edit", Icons.Outlined.Edit, Modifier.weight(1f)) {
                            nav.navigate(CpRoutes.meetupForm(m.id))
                        }
                        if (!m.isCancelled) {
                            OutlineAction("Cancel", Icons.Outlined.EventBusy, Modifier.weight(1f), tint = Orange) {
                                cancelling = true
                            }
                        }
                        OutlineAction("Delete", Icons.Outlined.Delete, Modifier.weight(1f), tint = ErrorRed) {
                            confirmDelete = true
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
      }
    }

    if (inviting) {
        val onList = state.meetup?.invitees.orEmpty()
            .map { it.phone.filter(Char::isDigit).takeLast(10) }.toSet()
        InviteMoreSheet(
            people = state.people,
            alreadyInvited = onList,
            saving = state.busy,
            onDismiss = { inviting = false },
            onInvite = { picked -> vm.invite(picked); inviting = false },
        )
    }

    photoAt?.let { at ->
        state.meetup?.photos?.takeIf { it.isNotEmpty() }?.let { photos ->
            MeetupPhotoViewer(photos, at) { photoAt = null }
        }
    }

    if (cancelling) {
        CancelDialog(onDismiss = { cancelling = false }) { reason ->
            vm.cancel(reason); cancelling = false
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this meetup?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "It disappears for everyone, including people who said they were going. " +
                        "If it is off rather than gone, cancel it instead — that tells them why.",
                    color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = TextSecondary) }
            },
        )
    }
}

// ─── Pieces ──────────────────────────────────────────────────────────────────

@Composable
private fun CancelledBanner(reason: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(ErrorRed.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.EventBusy, null, tint = ErrorRed, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text("Cancelled", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!reason.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(reason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun PrivateNote(note: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Mist).border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(14.dp),
    ) {
        SectionLabel("Private note · only you")
        Spacer(Modifier.height(6.dp))
        Text(note, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

/**
 * One person on the list.
 *
 * Tapping the row cycles their answer rather than opening a menu: a partner
 * working down a list of replies from WhatsApp does this a dozen times in a row,
 * and a two-tap menu each time is the difference between doing it and not.
 */
@Composable
private fun InviteeManageRow(
    invitee: CpMeetupInvitee,
    enabled: Boolean,
    onCall: () -> Unit,
    onSetRsvp: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val rsvp = Rsvp.from(invitee.rsvp)

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .clickable(enabled = enabled) { menu = true }
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Mist), contentAlignment = Alignment.Center) {
            Text(initialsOf(invitee.name), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    invitee.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (invitee.foundItThemselves) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Found it", color = IconGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(IconGreen.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                if (invitee.guests > 0) "${invitee.phone} · +${invitee.guests}" else invitee.phone,
                color = TextSecondary, fontSize = 11.sp, maxLines = 1,
            )
        }
        RsvpPill(rsvp)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Outlined.Phone, "Call ${invitee.name}", tint = Teal,
            modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onCall() }.padding(7.dp),
        )
    }

    if (menu) {
        AlertDialog(
            onDismissRequest = { menu = false },
            title = { Text(invitee.name, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Are they coming?", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    ChoiceChips(
                        options = listOf(Rsvp.GOING, Rsvp.MAYBE, Rsvp.DECLINED, Rsvp.INVITED),
                        selected = rsvp,
                        label = { it.label },
                        tint = { it.tint },
                        onPick = { menu = false; onSetRsvp(it.wire) },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { menu = false; onRemove() }) {
                    Text("Remove from list", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { menu = false }) { Text("Close", color = TextSecondary) } },
        )
    }
}

@Composable
private fun SoftAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(Teal.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (enabled) Teal else TextSecondary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label, color = if (enabled) Teal else TextSecondary,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

@Composable
private fun OutlineAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    onClick: () -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Adding people after the fact — the same picker the form uses. */
@Composable
private fun InviteMoreSheet(
    people: List<Invitee>,
    alreadyInvited: Set<String>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onInvite: (List<Invitee>) -> Unit,
) {
    var picked by remember { mutableStateOf(setOf<String>()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                SectionLabel("Invite more")
                Spacer(Modifier.height(4.dp))
                Text(
                    "People already on the list are ticked and can't be added twice.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                )
                Spacer(Modifier.height(14.dp))
                InvitePicker(
                    people = people,
                    selected = picked,
                    alreadyInvited = alreadyInvited,
                    onToggle = { p -> picked = if (picked.contains(p.key)) picked - p.key else picked + p.key },
                    maxHeight = 320,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onInvite(people.filter { picked.contains(it.key) }) },
                        enabled = picked.isNotEmpty() && !saving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) {
                        Text(
                            if (picked.isEmpty()) "Invite" else "Invite ${picked.size}",
                            color = Color.White, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelDialog(onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this meetup?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text(
                    "It stays visible to everyone who was asked, marked cancelled, so nobody turns up to an empty site.",
                    color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    placeholder = { Text("Why? (optional)", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim().ifBlank { null }) }) {
                Text("Cancel meetup", color = Orange, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep it", color = TextSecondary) } },
    )
}

/**
 * The invite as a message.
 *
 * Reads as an invitation rather than a record — what, when, where — because it
 * lands in a chat with someone who was not looking at the app. The maps link
 * goes last, where a phone will make it tappable without breaking the address
 * above it.
 */
fun meetupShareText(m: CpMeetup): String = buildString {
    appendLine(m.title)
    appendLine()
    appendLine("When: ${meetupDayLabel(m.date)}, ${m.time}")
    if (MeetupMode.from(m.mode) != MeetupMode.ONLINE) appendLine("Where: ${m.location}")
    if (!m.description.isNullOrBlank()) {
        appendLine()
        appendLine(m.description!!)
    }
    if (m.isCancelled) {
        appendLine()
        appendLine("This meetup has been cancelled.${m.cancelReason?.let { " $it" } ?: ""}")
    }
    if (!m.mapsLink.isNullOrBlank()) {
        appendLine()
        append(m.mapsLink)
    }
}.trim()
