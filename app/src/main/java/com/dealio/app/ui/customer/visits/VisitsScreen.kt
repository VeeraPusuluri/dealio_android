package com.dealio.app.ui.customer.visits

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.Meeting
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.PortalHeader
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.components.CalMeeting
import com.dealio.app.ui.components.ListCalendarToggle
import com.dealio.app.ui.components.MeetingsCalendar
import com.dealio.app.ui.components.calDate
import com.dealio.app.ui.components.meetingStatusColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun VisitsScreen(nav: NavController, vm: VisitsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var calendar by remember { mutableStateOf(false) }
    // The visit the buyer opened. Both views feed the same sheet, so a visit
    // tapped on the calendar tells them exactly what one tapped in the list does.
    var detail by remember { mutableStateOf<Meeting?>(null) }
    RefreshOnResume { vm.load(silent = true) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val calMeetings = state.meetings.mapNotNull { m ->
        val d = calDate(m.confirmedDate ?: m.preferredDate) ?: return@mapNotNull null
        CalMeeting(
            id = "mtg-${m.id}", date = d,
            time = (m.confirmedTime ?: m.preferredTime).ifBlank { null },
            title = m.projectName.ifBlank { "Site visit" }, subtitle = m.meetingType,
            status = m.status, color = meetingStatusColor(m.status),
        )
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            val completed = state.meetings.count { it.status.equals("Completed", true) }
            PortalHeader(
                title = "Site visits",
                subtitle = "See homes before you commit",
                stats = buildList {
                    add("${state.meetings.size - completed}" to "upcoming")
                    if (completed > 0) add("$completed" to "visited")
                },
            )
        },
    ) { inner ->
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
                        MeetingsCalendar(
                            calMeetings,
                            // The calendar carries a normalised row, not the visit
                            // itself, so map back by the id it was built with.
                            onMeetingClick = { cal ->
                                detail = state.meetings.firstOrNull { "mtg-${it.id}" == cal.id }
                            },
                        )
                    }
                } else if (state.meetings.isEmpty()) {
                    PortalEmptyState(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "No visits booked",
                        subtitle = "Walking the site is the fastest way to tell a shortlist apart. Book one from any project page.",
                        actionLabel = "Find a project",
                        onAction = { nav.navigate(CustomerRoutes.EXPLORE) },
                    )
                } else LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.meetings.size) { i ->
                        VisitCard(
                            state.meetings[i],
                            onClick = { detail = state.meetings[i] },
                            onRate = { r -> vm.rate(state.meetings[i].id, r) },
                        )
                    }
                }
            }
        }
    }

    detail?.let { m ->
        VisitDetailSheet(
            m,
            onDismiss = { detail = null },
            onRate = { r -> vm.rate(m.id, r) },
        )
    }
}

@Composable
private fun VisitCard(m: Meeting, onClick: () -> Unit, onRate: (Int) -> Unit) {
    DealioCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.projectName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (!m.meetingType.isNullOrBlank()) Text(m.meetingType!!, color = TextSecondary, fontSize = 12.sp)
            }
            StatusChip(m.status)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(formatDate(m.confirmedDate ?: m.preferredDate), color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(m.confirmedTime ?: m.preferredTime, color = TextPrimary, fontSize = 13.sp)
        }
        m.whereLine()?.let {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(it, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (!m.cpName.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Arranged by ${m.cpName}", color = TextSecondary, fontSize = 12.sp)
        }
        if (!m.notes.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(m.notes!!, color = TextSecondary, fontSize = 12.sp)
        }

        if (m.status.equals("Completed", true)) {
            Spacer(Modifier.height(12.dp))
            Text("Rate your visit", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            RatingRow(m.customerRating, onRate)
        }
    }
}

/** Street and city, whichever the builder has filled in. Null when neither. */
private fun Meeting.whereLine(): String? =
    listOfNotNull(projectAddress?.takeIf { it.isNotBlank() }, projectCity?.takeIf { it.isNotBlank() })
        .joinToString(", ")
        .takeIf { it.isNotBlank() }

@Composable
private fun RatingRow(current: Int?, onRate: (Int) -> Unit) {
    Row {
        (1..5).forEach { star ->
            val filled = (current ?: 0) >= star
            Icon(
                if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                "Rate $star",
                tint = if (filled) Orange else TextSecondary,
                modifier = Modifier.size(28.dp).padding(end = 4.dp).clickable { onRate(star) },
            )
        }
    }
}

/**
 * Everything about one visit, from either view.
 *
 * The two questions a buyer actually has on the day — where is it, and who do I
 * call if I am lost at the gate — had no answer anywhere in the app: the list
 * showed a project name and a time, and the calendar was not even tappable. Both
 * now open this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitDetailSheet(m: Meeting, onDismiss: () -> Unit, onRate: (Int) -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val confirmed = m.confirmedDate != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.projectName.ifBlank { "Site visit" }, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (!m.meetingType.isNullOrBlank()) {
                        Text(m.meetingType!!, color = TextSecondary, fontSize = 13.sp)
                    }
                }
                StatusChip(m.status)
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("When")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = Teal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatDate(m.confirmedDate ?: m.preferredDate), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Outlined.Schedule, null, tint = Teal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text((m.confirmedTime ?: m.preferredTime).ifBlank { "—" }, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                // A requested slot is not a booked one, and reading one as the
                // other is how a buyer turns up to a locked gate.
                if (confirmed) "Confirmed by the builder." else "You asked for this slot — the builder is still confirming it.",
                color = TextSecondary, fontSize = 12.sp,
            )

            Spacer(Modifier.height(18.dp))
            SectionLabel("Where")
            Spacer(Modifier.height(8.dp))
            Text(
                m.whereLine() ?: "The builder has not shared a street address. Search the project by name.",
                color = if (m.whereLine() != null) TextPrimary else TextSecondary,
                fontSize = 14.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(10.dp))
            SheetAction(Icons.Outlined.Map, "Open in Maps") {
                // Falls back to the project name, which is all the web page has
                // ever had to search on.
                val query = m.whereLine()?.let { "${m.projectName}, $it" } ?: m.projectName
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"),
                    ),
                )
            }

            val contactName = m.cpName?.takeIf { it.isNotBlank() } ?: m.builderName?.takeIf { it.isNotBlank() }
            val contactPhone = m.cpPhone?.takeIf { it.isNotBlank() } ?: m.builderPhone?.takeIf { it.isNotBlank() }
            if (contactName != null || contactPhone != null) {
                Spacer(Modifier.height(18.dp))
                SectionLabel(if (m.cpName.isNullOrBlank()) "Who to contact" else "Your advisor")
                Spacer(Modifier.height(8.dp))
                Text(contactName ?: "Contact", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (contactPhone != null) {
                    Text(contactPhone, color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    SheetAction(Icons.Outlined.Phone, "Call ${contactName ?: contactPhone}") {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone")))
                    }
                }
            }

            if (!m.notes.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Your note")
                Spacer(Modifier.height(8.dp))
                Text(m.notes!!, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
            }

            if (m.status.equals("Completed", true)) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Rate your visit")
                Spacer(Modifier.height(8.dp))
                RatingRow(m.customerRating, onRate)
            }
        }
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(icon, null, tint = Teal, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
