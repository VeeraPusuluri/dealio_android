package com.dealio.app.ui.customer.meetups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.meetups.CategoryChip
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.meetupWhen
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * The meetups row on Explore.
 *
 * Sits under Featured, above the homes list: a customer browsing property is
 * exactly who should know there is an open house on this weekend, but they came
 * looking for homes, so this earns a strip rather than the top of the page.
 *
 * Renders nothing at all when there is nothing on. An empty "Meetups near you"
 * heading is worse than no heading — it teaches people the section is dead.
 */
@Composable
fun MeetupsStrip(nav: NavController, vm: CustomerMeetupsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading || state.meetups.isEmpty()) return

    // Invitations first, then whatever is soonest. The list arrives sorted by
    // date, so this only has to lift the personal ones to the front.
    val shown = (state.awaitingReply + state.going + state.nearby).take(8)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { nav.navigate(CustomerRoutes.MEETUPS) }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(
                if (state.city != null) "Meetups in ${state.city}" else "Meetups near you",
                Modifier.weight(1f),
            )
            if (state.awaitingReply.isNotEmpty()) {
                Text(
                    "${state.awaitingReply.size} invite${if (state.awaitingReply.size == 1) "" else "s"}",
                    color = CustomerAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(CustomerAccent.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text("See all", color = CustomerAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Outlined.ChevronRight, null, tint = CustomerAccent, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            shown.forEach { m ->
                MeetupStripCard(m) { nav.navigate(CustomerRoutes.meetupDetail(m.id)) }
            }
        }
    }
}

@Composable
private fun MeetupStripCard(m: CustomerMeetup, onOpen: () -> Unit) {
    val category = MeetupCategory.from(m.category)
    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier.width(232.dp).clip(shape).background(Color.White, shape)
            .border(1.dp, CardBorder.copy(alpha = 0.7f), shape)
            .clickable { onOpen() },
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp).background(category.gradient)) {
            Icon(
                category.icon, null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(34.dp).align(Alignment.Center),
            )
            if (m.awaitingReply) {
                Text(
                    "Invited", color = CustomerAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .clip(RoundedCornerShape(5.dp)).background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            } else if (m.isGoing) {
                Text(
                    "Going", color = IconGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .clip(RoundedCornerShape(5.dp)).background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Column(Modifier.padding(12.dp)) {
            CategoryChip(category)
            Spacer(Modifier.height(8.dp))
            Text(
                m.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            StripLine(Icons.Outlined.CalendarMonth, meetupWhen(m.date, m.time))
            Spacer(Modifier.height(4.dp))
            StripLine(Icons.Outlined.LocationOn, m.city?.takeIf { it.isNotBlank() } ?: m.location)
            Spacer(Modifier.height(4.dp))
            StripLine(
                Icons.Outlined.People,
                if (m.goingCount == 0) "Be the first" else "${m.goingCount} going",
            )
        }
    }
}

@Composable
private fun StripLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
