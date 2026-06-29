package com.dealio.app.ui.builder.overview

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.GradientStatTile
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.QuickActionTile
import com.dealio.app.ui.builder.SectionHeader
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatTile
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun OverviewScreen(nav: NavController, vm: OverviewViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    com.dealio.app.ui.builder.RefreshOnResume { vm.load(silent = true) }

    Column(Modifier.fillMaxSize()) {
        // ── Navy hero ──
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .background(NavyTealGradient),
        ) {
            Column(Modifier.systemBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).background(Teal, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initialsOf(state.builderName), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Welcome back", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            state.builderName?.substringBefore(' ') ?: "Builder",
                            color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        Modifier.size(40.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable { nav.navigate(BuilderRoutes.NOTIFICATIONS) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
        }

        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load)
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Projects", state.projects.toString(), Icons.Outlined.Apartment, Teal, Modifier.weight(1f), onClick = { nav.navigate(BuilderRoutes.PROJECTS) })
                        StatTile("Active Leads", state.leads.toString(), Icons.Outlined.Groups, Orange, Modifier.weight(1f), onClick = { nav.navigate(BuilderRoutes.PIPELINE) })
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Deals", state.deals.toString(), Icons.Outlined.Handshake, Teal, Modifier.weight(1f), onClick = { nav.navigate(BuilderRoutes.DEALS) })
                        StatTile("Booked", state.booked.toString(), Icons.Outlined.TrendingUp, StatusColors.Green, Modifier.weight(1f), onClick = { nav.navigate(BuilderRoutes.DEALS) })
                    }
                }
                item {
                    GradientStatTile(
                        label = "Revenue Booked",
                        value = formatINRShort(state.revenue),
                        icon = Icons.Outlined.CurrencyRupee,
                        caption = "${state.booked} units booked",
                        onClick = { nav.navigate(BuilderRoutes.ANALYTICS) },
                    )
                }

                item { SectionLabel("Quick actions", Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionTile("New Project", Icons.Filled.Add, Modifier.weight(1f), accent = Teal) { nav.navigate(BuilderRoutes.projectForm()) }
                        QuickActionTile("Pipeline", Icons.Outlined.Groups, Modifier.weight(1f), accent = Orange) { nav.navigate(BuilderRoutes.PIPELINE) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionTile("Meetings", Icons.Outlined.CalendarMonth, Modifier.weight(1f), accent = StatusColors.Blue) { nav.navigate(BuilderRoutes.MEETINGS) }
                        QuickActionTile("Broadcast", Icons.Outlined.Campaign, Modifier.weight(1f), accent = StatusColors.Purple) { nav.navigate(BuilderRoutes.BROADCAST) }
                    }
                }

                item {
                    SectionHeader(
                        "Recent deals",
                        Modifier.padding(top = 8.dp),
                        actionText = "View all",
                        onAction = { nav.navigate(BuilderRoutes.DEALS) },
                    )
                }
                if (state.recentDeals.isEmpty()) {
                    item { Text("No deals yet.", color = TextSecondary, fontSize = 13.sp) }
                } else {
                    items(state.recentDeals.size) { i ->
                        val d = state.recentDeals[i]
                        DealioCard(Modifier.clickable { nav.navigate(BuilderRoutes.dealDetail(d.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(d.customerName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(d.projectName, color = TextSecondary, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusChip(d.status)
                                    if ((d.dealValue ?: 0.0) > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(formatINRShort(d.dealValue), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
