package com.dealio.app.ui.customer.meetups

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CustomerMeetup
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.meetups.CategoryChip
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupDetailLine
import com.dealio.app.ui.meetups.MeetupHero
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.meetupWhen
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.subtleShadow

/**
 * Meetups, as a customer sees them.
 *
 * Ordered by how much each one is asking of this person: invitations awaiting a
 * reply, then what they have already said yes to, then everything else on in
 * their city. Meetup.com opens on a browse; here, someone who was personally
 * asked should never have to scroll past a listing to find that out.
 */
@Composable
fun CustomerMeetupsScreen(nav: NavController, vm: CustomerMeetupsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // A meetup answered elsewhere, or a new invite, shows on the way back in.
    RefreshOnResume { if (!state.loading) vm.load(silent = true) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    SubScreenScaffold("Meetups", nav) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { CategoryFilter(state.category) { vm.setCategory(it) } }

                    if (state.meetups.isEmpty()) {
                        item {
                            PortalEmptyState(
                                icon = Icons.Outlined.Groups,
                                title = "No meetups yet",
                                subtitle = "Site visits and open houses near you will show up here. " +
                                    "Set your city in your profile to see more.",
                            )
                        }
                    }

                    section("You're invited", state.awaitingReply, nav)
                    section("You're going", state.going, nav)
                    section(
                        if (state.city != null) "On in ${state.city}" else "Near you",
                        state.nearby, nav,
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    items: List<CustomerMeetup>,
    nav: NavController,
) {
    if (items.isEmpty()) return
    item { SectionLabel(title, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
    items(items.size) { i ->
        Box(Modifier.padding(horizontal = 16.dp)) {
            CustomerMeetupCard(items[i]) { nav.navigate(CustomerRoutes.meetupDetail(items[i].id)) }
        }
    }
}

@Composable
private fun CategoryFilter(selected: MeetupCategory?, onPick: (MeetupCategory?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill("All", selected == null, CustomerAccent) { onPick(null) }
        MeetupCategory.entries.forEach { c ->
            FilterPill(c.label, selected == c, c.tint) { onPick(if (selected == c) null else c) }
        }
    }
}

@Composable
private fun FilterPill(label: String, on: Boolean, tint: Color, onClick: () -> Unit) {
    Text(
        label,
        color = if (on) Color.White else TextPrimary,
        fontSize = 12.sp,
        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (on) tint else Color.White)
            .border(1.dp, if (on) tint else CardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * One meetup on a customer's list.
 *
 * Leads with the photograph when there is one. A customer is browsing rather
 * than working through a list, and the picture is what makes them stop — the
 * text below it is what makes them tap. Where no cover was uploaded the strip
 * falls back to the category wash, which keeps the list on one rhythm instead of
 * alternating between tall cards and short ones.
 */
@Composable
fun CustomerMeetupCard(m: CustomerMeetup, onOpen: () -> Unit) {
    val category = MeetupCategory.from(m.category)
    val shape = RoundedCornerShape(18.dp)

    Column(
        Modifier.fillMaxWidth().subtleShadow(radius = 18.dp).clip(shape)
            .background(Color.White, shape).border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .clickable { onOpen() },
    ) {
        MeetupHero(
            category = category,
            height = 132,
            coverImage = m.coverImage,
            scrim = !m.coverImage.isNullOrBlank(),
        ) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    m.isCancelled -> Badge("Cancelled", ErrorRed, onImage = true)
                    m.isGoing -> Badge("You're going", com.dealio.app.ui.components.IconGreen, onImage = true)
                    m.awaitingReply -> Badge("Invited", CustomerAccent, onImage = true)
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
        CategoryChip(category)

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.People, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (m.goingCount == 0) "Be the first to go"
                else "${m.goingCount} going",
                color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text("by ${m.hostName}", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
        }
    }
}

/**
 * A status flag. [onImage] fills it solid — a tinted wash reads as mud over a
 * photograph, and this is the one label that has to survive whatever is behind it.
 */
@Composable
private fun Badge(label: String, tint: Color, onImage: Boolean = false) {
    Text(
        label,
        color = if (onImage) Color.White else tint,
        fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (onImage) tint else tint.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

