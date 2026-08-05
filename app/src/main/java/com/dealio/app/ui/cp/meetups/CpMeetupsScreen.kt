package com.dealio.app.ui.cp.meetups

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpMeetup
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.shareViaWhatsApp
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.meetups.CategoryChip
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupDetailLine
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.RsvpSummary
import com.dealio.app.ui.meetups.VisibilityChip
import com.dealio.app.ui.meetups.isPastMeetup
import com.dealio.app.ui.meetups.meetupWhen
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.subtleShadow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpMeetupsState(
    val loading: Boolean = true,
    val error: String? = null,
    val meetups: List<CpMeetup> = emptyList(),
    val message: String? = null,
)

class CpMeetupsViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpMeetupsState())
    val state: StateFlow<CpMeetupsState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMeetups()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, error = null, meetups = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

private enum class MeetupTab(val label: String) { UPCOMING("Upcoming"), PAST("Past") }

/**
 * The partner's meetups.
 *
 * Split upcoming from past rather than showing one long list: everything a
 * partner does here is about what has not happened yet, and a finished meetup
 * competing for the same space is noise. Past ones stay reachable because they
 * are the record of who came.
 */
@Composable
fun CpMeetupsScreen(nav: NavController, vm: CpMeetupsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(MeetupTab.UPCOMING) }

    // Reload whenever this screen comes back to the front, so a meetup created
    // or edited on another screen is here when the user returns.
    RefreshOnResume { vm.load(silent = true) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val upcoming = state.meetups.filter { !isPastMeetup(it.date) }
    val past = state.meetups.filter { isPastMeetup(it.date) }.reversed()
    val shown = if (tab == MeetupTab.UPCOMING) upcoming else past

    SubScreenScaffold("Meetups", nav) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
                state.meetups.isEmpty() -> PortalEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "No meetups yet",
                    subtitle = "Arrange a site walk-through or an investor evening, pick who to invite, " +
                        "and let customers in your city find it.",
                    actionLabel = "Create a meetup",
                    onAction = { nav.navigate(CpRoutes.meetupForm()) },
                )
                else -> Column(Modifier.fillMaxSize()) {
                    TabStrip(
                        tab = tab,
                        upcomingCount = upcoming.size,
                        pastCount = past.size,
                        onSelect = { tab = it },
                    )
                    if (shown.isEmpty()) {
                        PortalEmptyState(
                            icon = if (tab == MeetupTab.UPCOMING) Icons.Outlined.CalendarMonth else Icons.Outlined.EventBusy,
                            title = if (tab == MeetupTab.UPCOMING) "Nothing coming up" else "Nothing in the past",
                            subtitle = if (tab == MeetupTab.UPCOMING)
                                "Your finished meetups are under Past."
                            else
                                "Meetups move here the day after they happen.",
                        )
                    } else LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(shown.size) { i ->
                            MeetupCard(shown[i]) { nav.navigate(CpRoutes.meetupDetail(shown[i].id)) }
                        }
                    }
                }
            }

            if (state.meetups.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(CpRoutes.meetupForm()) },
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
}

@Composable
private fun TabStrip(tab: MeetupTab, upcomingCount: Int, pastCount: Int, onSelect: (MeetupTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp)).background(Navy.copy(alpha = 0.06f)).padding(3.dp),
    ) {
        MeetupTab.entries.forEach { t ->
            val on = t == tab
            val count = if (t == MeetupTab.UPCOMING) upcomingCount else pastCount
            Text(
                if (count > 0) "${t.label} · $count" else t.label,
                color = if (on) Teal else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 9.dp),
            )
        }
    }
}

/**
 * One meetup, scannable.
 *
 * No cover strip: the category chip already carries the colour, and the RSVP
 * counts are the thing a partner opens this list to read. Anything above them
 * costs the vertical space that keeps them above the fold.
 */
@Composable
private fun MeetupCard(m: CpMeetup, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val category = MeetupCategory.from(m.category)
    val shape = RoundedCornerShape(18.dp)

    Column(
        Modifier.fillMaxWidth().subtleShadow(radius = 18.dp).clip(shape)
            .background(Color.White, shape).border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .clickable { onOpen() },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(category)
                Spacer(Modifier.weight(1f))
                if (m.isCancelled) {
                    Text(
                        "Cancelled", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(ErrorRed.copy(alpha = 0.10f)).padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                } else if (m.isPublic) {
                    VisibilityChip(true, m.city)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                m.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp,
            )

            Spacer(Modifier.height(10.dp))
            MeetupDetailLine(Icons.Outlined.CalendarMonth, meetupWhen(m.date, m.time), maxLines = 1)
            if (MeetupMode.from(m.mode) != MeetupMode.ONLINE) {
                Spacer(Modifier.height(5.dp))
                MeetupDetailLine(Icons.Outlined.LocationOn, m.location, maxLines = 1)
            }

            Spacer(Modifier.height(12.dp))
            RsvpSummary(m.counts.going, m.counts.maybe, m.counts.noReply)

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Teal.copy(alpha = 0.10f))
                    .clickable { shareViaWhatsApp(ctx, meetupShareText(m), "Share meetup") }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Share, null, tint = Teal, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(7.dp))
                Text("Share invite", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
