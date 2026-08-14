package com.dealio.app.ui.builder.meetings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.Meeting
import com.dealio.app.ui.builder.DateField
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.components.CalMeeting
import com.dealio.app.ui.components.FormSheet
import com.dealio.app.ui.components.ListCalendarToggle
import com.dealio.app.ui.components.MeetingsCalendar
import com.dealio.app.ui.components.SheetField
import com.dealio.app.ui.components.SheetGhostButton
import com.dealio.app.ui.components.SheetSubmitButton
import com.dealio.app.ui.components.calDate
import com.dealio.app.ui.components.meetingStatusColor
import com.dealio.app.ui.meetups.ChoiceChips
import com.dealio.app.ui.meetups.FormLabel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealGradient
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(nav: NavController, vm: MeetingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<Meeting?>(null) }
    var action by remember { mutableStateOf<MeetingAction?>(null) }
    var calendar by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    com.dealio.app.ui.builder.RefreshOnResume { vm.load(silent = true) }

    val calMeetings = state.visible.mapNotNull { m ->
        val d = calDate(m.confirmedDate ?: m.preferredDate) ?: return@mapNotNull null
        CalMeeting(
            id = "mtg-${m.id}", date = d,
            time = (m.confirmedTime ?: m.preferredTime).ifBlank { null },
            title = m.customerName.ifBlank { "Visitor" }, subtitle = m.projectName,
            status = m.status, color = meetingStatusColor(m.status),
        )
    }

    com.dealio.app.ui.builder.SubScreenScaffold("Site visits", nav) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                ListCalendarToggle(calendar = calendar, onChange = { calendar = it })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.filters.forEach { f ->
                    val sel = state.filter == f
                    Box(
                        Modifier.weight(1f)
                            .background(if (sel) NavyMid else Color.White, RoundedCornerShape(10.dp))
                            .border(1.dp, if (sel) NavyMid else CardBorder, RoundedCornerShape(10.dp))
                            .clickable { vm.setFilter(f) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(f, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            // Answering a request closes the sheet, so without this the builder
            // taps Approve and the screen just… returns, with the row's new
            // status the only clue anything happened.
            state.toast?.let { msg ->
                LaunchedEffect(msg) { delay(3_000); vm.clearToast() }
                Text(
                    msg, color = Teal, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, vm::load)
                calendar -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    MeetingsCalendar(calMeetings)
                }
                state.visible.isEmpty() -> EmptyState(Icons.Outlined.CalendarMonth, "No ${state.filter.lowercase()} site visits", "Visit requests from customers appear here.")
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.visible.size) { i -> MeetingCard(state.visible[i]) { sheet = state.visible[i] } }
                }
            }
        }
    }

    // The detail sheet and the action form are one sheet at a time, not stacked:
    // two ModalBottomSheets open together fight over the scrim, and cancelling
    // the form should land back on the visit you were reading.
    if (sheet != null && action == null) {
        val m = sheet!!
        ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = sheetState, containerColor = Color.White) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(m.customerName, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                StatusChip(m.status)
                Spacer(Modifier.height(12.dp))
                InfoRow("Project", m.projectName)
                InfoRow("Requested", "${formatDate(m.preferredDate)} · ${m.preferredTime}")
                InfoRow("Confirmed", m.confirmedDate?.let { "${formatDate(it)} · ${m.confirmedTime ?: ""}" })
                InfoRow("Type", m.meetingType)
                InfoRow("Phone", m.customerPhone.ifBlank { "Contact via channel partner" })
                InfoRow("Channel partner", m.cpName ?: "Direct")
                InfoRow("Notes", m.notes)

                Spacer(Modifier.height(18.dp))
                MeetingActions(m.status, enabled = !state.working) { action = it }
            }
        }
    }

    if (sheet != null && action != null) {
        MeetingActionSheet(
            meeting = sheet!!,
            action = action!!,
            working = state.working,
            onDismiss = { action = null },
            onSubmit = { date, time, notes ->
                val m = sheet!!
                val chosen = action!!
                vm.updateStatus(m, chosen.status, date, time, notes, done = chosen.done)
                action = null
                sheet = null
            },
        )
    }
}

// ─── Answering a visit request ───────────────────────────────────────────────
// The same five answers the web portal offers, in the same words and the same
// order. A builder who arranges visits on both should not have to work out that
// "Mark rescheduled" here is "Propose New Time" there — and that one used to be
// a lie on Android: it sent the status with no new slot on it, so the customer
// was told their visit had moved to exactly the time they had already asked for.

private enum class MeetingAction(
    val status: String,
    /** Label in the action list — the web portal's wording, verbatim. */
    val label: String,
    val title: String,
    val cta: String,
    val done: String,
    val accent: Color,
    val bg: Color,
    val icon: ImageVector,
    /** Filled and loud, or tinted and quiet. Only one action per state is loud. */
    val filled: Boolean = false,
    /** Approving and proposing both commit to a slot; the rest carry none. */
    val needsSlot: Boolean = false,
    val noteLabel: String,
    val notePlaceholder: String,
) {
    APPROVE(
        "Confirmed", "Approve Meeting", "Approve meeting", "Approve", "Visit confirmed",
        Teal, StatusColors.GreenBg, Icons.Outlined.CheckCircle, filled = true, needsSlot = true,
        noteLabel = "Note for the customer (optional)",
        notePlaceholder = "e.g. Ask for Ravi at the site office.",
    ),
    RESCHEDULE(
        "Rescheduled", "Propose New Time", "Propose new time", "Send new time", "New time proposed",
        StatusColors.Amber, StatusColors.AmberBg, Icons.Outlined.CalendarMonth, needsSlot = true,
        noteLabel = "Why the change (optional)",
        notePlaceholder = "e.g. The site is closed that morning.",
    ),
    REJECT(
        "Cancelled", "Reject Request", "Reject request", "Reject request", "Request rejected",
        StatusColors.Red, StatusColors.RedBg, Icons.Outlined.Cancel,
        noteLabel = "Reason (optional)",
        notePlaceholder = "Shown to the customer and the channel partner.",
    ),
    COMPLETE(
        "Completed", "Mark as Completed", "Mark completed", "Mark completed", "Visit marked completed",
        StatusColors.Blue, StatusColors.BlueBg, Icons.Outlined.TaskAlt, filled = true,
        noteLabel = "Visit notes",
        notePlaceholder = "e.g. Interested in Tower A, 15th floor. Wants a quote by Friday.",
    ),
    FOLLOWUP(
        "Follow-up Required", "Flag Follow-up", "Flag follow-up", "Flag follow-up", "Follow-up flagged",
        StatusColors.Purple, StatusColors.PurpleBg, Icons.Outlined.ChatBubbleOutline,
        noteLabel = "What needs following up",
        notePlaceholder = "e.g. Send the payment plan for the 3BHK.",
    ),
    CANCEL(
        "Cancelled", "Cancel Visit", "Cancel visit", "Cancel visit", "Visit cancelled",
        StatusColors.Red, StatusColors.RedBg, Icons.Outlined.Cancel,
        noteLabel = "Reason (optional)",
        notePlaceholder = "Shown to the customer and the channel partner.",
    ),
}

/**
 * Which answers a visit is open to.
 *
 * A rescheduled visit keeps the same answers as a confirmed one: the builder
 * has proposed a time and still has to be able to close it out afterwards. The
 * web portal leaves that state with nothing to do, which is a gap there rather
 * than a rule to copy.
 */
private fun actionsFor(status: String): List<MeetingAction> = when (status.lowercase()) {
    "pending" -> listOf(MeetingAction.APPROVE, MeetingAction.RESCHEDULE, MeetingAction.REJECT)
    "confirmed", "rescheduled" -> listOf(MeetingAction.COMPLETE, MeetingAction.FOLLOWUP, MeetingAction.CANCEL)
    "completed" -> listOf(MeetingAction.FOLLOWUP)
    else -> emptyList()
}

@Composable
private fun MeetingActions(status: String, enabled: Boolean, onPick: (MeetingAction) -> Unit) {
    val actions = actionsFor(status)
    Text(
        "ACTIONS", color = TextSecondary, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
    )
    Spacer(Modifier.height(10.dp))
    if (actions.isEmpty()) {
        Text(
            "No further actions available.", color = TextSecondary, fontSize = 12.5.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = TextAlign.Center,
        )
        return
    }
    actions.forEach { a ->
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(if (a.filled) 50.dp else 46.dp)
                .background(if (a.filled) TealGradient else SolidColor(a.bg), RoundedCornerShape(14.dp))
                .then(if (a.filled) Modifier else Modifier.border(1.dp, a.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)))
                .clickable(enabled = enabled) { onPick(a) },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    a.icon, null,
                    tint = if (a.filled) Color.White else a.accent,
                    modifier = Modifier.size(if (a.filled) 18.dp else 16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    a.label,
                    color = if (a.filled) Color.White else a.accent,
                    fontSize = 14.sp,
                    fontWeight = if (a.filled) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

/** The slots a builder actually offers. Matches what the booking screens show. */
private val visitSlots = listOf(
    "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
    "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM", "06:00 PM",
)

@Composable
private fun MeetingActionSheet(
    meeting: Meeting,
    action: MeetingAction,
    working: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (date: String, time: String, notes: String) -> Unit,
) {
    // Approving starts from the slot the customer asked for, so the common
    // answer — yes, that time — is one tap. Proposing starts there too, since a
    // new time is usually a nudge off the requested one rather than a fresh pick.
    var date by remember(meeting.id, action) { mutableStateOf(meeting.confirmedDate ?: meeting.preferredDate) }
    var time by remember(meeting.id, action) { mutableStateOf(meeting.confirmedTime ?: meeting.preferredTime) }
    var notes by remember(meeting.id, action) { mutableStateOf("") }

    // A slot booked from the web portal ("2:00 PM") is not spelled the way this
    // list spells it. Carry it as its own chip rather than silently dropping the
    // time the customer is expecting.
    val slots = remember(time) {
        if (time.isBlank() || visitSlots.contains(time)) visitSlots else listOf(time) + visitSlots
    }
    val ready = !action.needsSlot || (date.isNotBlank() && time.isNotBlank())

    FormSheet(
        title = action.title,
        icon = action.icon,
        onDismiss = onDismiss,
        subtitle = "${meeting.customerName} · ${meeting.projectName}",
        accent = action.accent,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetGhostButton("Back") { onDismiss() }
                SheetSubmitButton(
                    text = action.cta,
                    enabled = ready,
                    working = working,
                    onClick = { onSubmit(date, time, notes) },
                    modifier = Modifier.weight(1f),
                    gradient = if (action.filled) TealGradient
                               else Brush.linearGradient(listOf(action.accent, action.accent)),
                )
            }
        },
    ) {
        if (action.needsSlot) {
            DateField(
                label = if (action == MeetingAction.RESCHEDULE) "New date" else "Confirmed date",
                value = date,
                required = true,
                onChange = { date = it },
            )
            FormLabel(if (action == MeetingAction.RESCHEDULE) "New time" else "Confirmed time")
            ChoiceChips(
                options = slots,
                selected = time.takeIf { it.isNotBlank() },
                label = { it },
                onPick = { time = it },
                tint = { action.accent },
            )
            Spacer(Modifier.height(16.dp))
        }
        SheetField(
            value = notes,
            onValueChange = { notes = it },
            label = action.noteLabel,
            placeholder = action.notePlaceholder,
            singleLine = false,
            minLines = 3,
        )
    }
}

@Composable
private fun MeetingCard(m: Meeting, onClick: () -> Unit) {
    DealioCard(Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.customerName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(m.projectName, color = TextSecondary, fontSize = 12.sp)
            }
            StatusChip(m.status)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text("${formatDate(m.confirmedDate ?: m.preferredDate)} · ${m.confirmedTime ?: m.preferredTime}",
                color = TextSecondary, fontSize = 12.sp)
            if (!m.meetingType.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                Text("· ${m.meetingType}", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
