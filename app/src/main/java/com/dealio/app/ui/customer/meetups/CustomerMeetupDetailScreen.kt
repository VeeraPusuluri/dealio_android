package com.dealio.app.ui.customer.meetups

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
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.CustomerRepository
import com.dealio.app.data.api.CustomerMeetup
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.meetups.CategoryChip
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupDetailLine
import com.dealio.app.ui.meetups.MeetupHero
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.Rsvp
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

data class CustomerMeetupDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val meetup: CustomerMeetup? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class CustomerMeetupDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CustomerRepository(app)
    private val _state = MutableStateFlow(CustomerMeetupDetailState())
    val state: StateFlow<CustomerMeetupDetailState> = _state.asStateFlow()
    private var id: Long = 0

    fun load(meetupId: Long) {
        id = meetupId
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMeetup(meetupId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, meetup = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun rsvp(rsvp: String, guests: Int) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = repo.rsvpMeetup(id, rsvp, guests)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        busy = false, meetup = r.data,
                        message = when (rsvp) {
                            "GOING" -> "You're going — the host can see you're coming"
                            "MAYBE" -> "Marked as maybe"
                            else -> "Thanks for letting them know"
                        },
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/**
 * The event page a customer answers from.
 *
 * The RSVP control sits in a bar pinned to the bottom rather than at the end of
 * the page. Answering is the only thing this screen asks of them, and it should
 * not depend on their having scrolled to the end to find it.
 */
@Composable
fun CustomerMeetupDetailScreen(
    nav: NavController,
    meetupId: Long,
    vm: CustomerMeetupDetailViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var guests by remember { mutableIntStateOf(0) }

    LaunchedEffect(meetupId) { vm.load(meetupId) }
    LaunchedEffect(state.meetup?.myGuests) { state.meetup?.let { guests = it.myGuests } }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    SubScreenScaffold("Meetup", nav) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            val m = state.meetup
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(meetupId) })
                m == null -> ErrorState("Meetup not found", onRetry = { vm.load(meetupId) })
                else -> {
                    val category = MeetupCategory.from(m.category)
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        MeetupHero(category, height = 140)

                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryChip(category)
                                if (m.invited) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "You were invited", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            .background(Teal.copy(alpha = 0.12f))
                                            .padding(horizontal = 9.dp, vertical = 5.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                m.title, color = TextPrimary, fontSize = 21.sp,
                                fontWeight = FontWeight.Bold, lineHeight = 27.sp,
                            )

                            if (m.isCancelled) {
                                Spacer(Modifier.height(12.dp))
                                CancelledBanner(m.cancelReason)
                            }

                            Spacer(Modifier.height(16.dp))
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
                                } else if (MeetupMode.from(m.mode) != MeetupMode.IN_PERSON && !m.isGoing) {
                                    Spacer(Modifier.height(9.dp))
                                    Text(
                                        "The joining link appears here once you say you're going.",
                                        color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                                    )
                                }

                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!m.mapsLink.isNullOrBlank()) {
                                        SoftAction("Open in Maps", Icons.Outlined.LocationOn) {
                                            runCatching {
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, m.mapsLink!!.toUri()))
                                            }
                                        }
                                    }
                                    SoftAction("Add to calendar", Icons.Outlined.CalendarMonth) {
                                        addMeetupToCalendar(ctx, m.title, m.location, m.description, m.date, m.time)
                                    }
                                }
                            }

                            if (!m.description.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                DealioCard {
                                    SectionLabel("About")
                                    Spacer(Modifier.height(8.dp))
                                    Text(m.description!!, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            HostCard(m, ctx)

                            Spacer(Modifier.height(12.dp))
                            DealioCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.People, null, tint = Teal, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(9.dp))
                                    Text(
                                        if (m.goingCount == 0) "Nobody has said yes yet — be the first"
                                        else "${m.goingCount} going",
                                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    )
                                    if (m.capacity != null) {
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            "${m.goingCount}/${m.capacity}",
                                            color = if (m.isFull) Orange else TextSecondary,
                                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            // Clears the pinned bar so the last card is reachable.
                            Spacer(Modifier.height(140.dp))
                        }
                    }

                    if (!m.isCancelled) {
                        RsvpBar(
                            meetup = m,
                            guests = guests,
                            busy = state.busy,
                            onGuests = { guests = it },
                            onPick = { vm.rsvp(it, guests) },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        }
    }
}

/**
 * Going / Maybe / Can't go, pinned.
 *
 * Three explicit buttons rather than one toggle: "not going" is a real answer
 * the host needs, and a single Going button leaves them unable to tell a no from
 * someone who has not looked yet.
 */
@Composable
private fun RsvpBar(
    meetup: CustomerMeetup,
    guests: Int,
    busy: Boolean,
    onGuests: (Int) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = Rsvp.from(meetup.myRsvp)
    Column(
        modifier.fillMaxWidth()
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (meetup.isFull) {
            Text(
                "This meetup is full.",
                color = Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        // The guest stepper only matters to someone who is coming.
        if (current == Rsvp.GOING) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Bringing anyone?", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Stepper(guests, enabled = !busy) { onGuests(it); onPick("GOING") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Rsvp.GOING, Rsvp.MAYBE, Rsvp.DECLINED).forEach { option ->
                val on = current == option
                val blocked = option == Rsvp.GOING && meetup.isFull && !meetup.isGoing
                Text(
                    option.label,
                    color = when {
                        on -> Color.White
                        blocked -> TextSecondary.copy(alpha = 0.5f)
                        else -> option.tint
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (on) option.tint else option.tint.copy(alpha = 0.10f))
                        .clickable(enabled = !busy && !blocked) { onPick(option.wire) }
                        .padding(vertical = 13.dp),
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", enabled && value > 0) { onChange(value - 1) }
        Text(
            if (value == 0) "Just me" else "+$value",
            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        StepButton("+", enabled && value < 9) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        glyph,
        color = if (enabled) Teal else TextSecondary.copy(alpha = 0.4f),
        fontSize = 17.sp, fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .background(Teal.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled) { onClick() }
            .padding(top = 5.dp),
    )
}

@Composable
private fun HostCard(m: CustomerMeetup, ctx: android.content.Context) {
    DealioCard {
        SectionLabel("Hosted by")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Mist),
                contentAlignment = Alignment.Center,
            ) { Text(initialsOf(m.hostName), color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.hostName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull("Channel Partner", m.hostTier).joinToString(" · "),
                    color = TextSecondary, fontSize = 11.sp,
                )
            }
            // Only someone personally invited gets the host's number — a public
            // listing should not hand out a phone number to every browser.
            if (!m.hostPhone.isNullOrBlank()) {
                Icon(
                    Icons.Outlined.Phone, "Call ${m.hostName}", tint = Teal,
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(Teal.copy(alpha = 0.10f))
                        .clickable {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${m.hostPhone}".toUri()))
                            }
                        }
                        .padding(8.dp),
                )
            }
        }
    }
}

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
            Text("This meetup was cancelled", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!reason.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(reason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun SoftAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Teal.copy(alpha = 0.10f))
            .clickable { onClick() }.padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Teal, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
