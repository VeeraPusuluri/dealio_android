package com.dealio.app.ui.customer.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CustomerDeal
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.MoveItem
import com.dealio.app.ui.flow.MoveQueue
import com.dealio.app.ui.flow.idleDaysSince
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.pipeline.stageLabel
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.PortalHeader
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun JourneyScreen(nav: NavController, vm: JourneyViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    RefreshOnResume { vm.load(silent = true) }

    val totalValue = state.deals.sumOf { it.dealValue ?: 0.0 }
    val booked = state.deals.count { phaseFor(it.dealStatus).ordinal >= JourneyPhase.BOOKING.ordinal }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PortalHeader(
                title = "My journey",
                subtitle = "Every home you're in the running for",
                stats = buildList {
                    add("${state.deals.size}" to "active")
                    if (totalValue > 0) add(formatINRShort(totalValue) to "in play")
                    if (booked > 0) add("$booked" to "booked")
                },
            )
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() }, modifier = Modifier.padding(inner))
            state.deals.isEmpty() -> Box(Modifier.padding(inner)) {
                PortalEmptyState(
                    icon = Icons.Outlined.Timeline,
                    title = "Your journey starts here",
                    subtitle = "Book a visit or shortlist a home and you'll be able to track every step of it from this tab.",
                    actionLabel = "Browse homes",
                    onAction = { nav.navigate(CustomerRoutes.EXPLORE) },
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = inner.calculateTopPadding() + 12.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // A buyer with nothing to do gets told so, rather than an absence
                // they have to interpret.
                item {
                    MoveQueue(
                        viewer = DealRole.CUSTOMER,
                        items = state.deals.map { d ->
                            MoveItem(
                                dealId = d.dealId,
                                title = d.projectName,
                                subtitle = d.builderName ?: "Your purchase",
                                rawStatus = d.dealStatus,
                                cpAgreed = d.cpAgreed,
                                customerConfirmed = d.customerConfirmed,
                                idleDays = idleDaysSince(d.createdAt),
                            )
                        },
                        onOpen = { nav.navigate(CustomerRoutes.dealDetail(it)) },
                        emptyMessage = "Everything is with your builder or advisor right now.",
                    )
                }

                items(state.deals.size) { i ->
                    DealCard(state.deals[i]) { nav.navigate(CustomerRoutes.dealDetail(state.deals[i].dealId)) }
                }
            }
        }
    }
}

@Composable
private fun DealCard(d: CustomerDeal, onClick: () -> Unit) {
    val phase = phaseFor(d.dealStatus)
    DealioCard(Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    d.projectName,
                    color = TextPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if ((d.dealValue ?: 0.0) > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatINRShort(d.dealValue),
                        color = Teal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // The API sends the stage either display-cased ("New Lead") or as the
            // raw enum ("NEW_LEAD") depending on how it was last written; normalise
            // so the chip never shows SHOUTING_SNAKE_CASE.
            StatusChip(stageLabel(d.dealStatus))
        }

        Spacer(Modifier.height(16.dp))
        JourneyTrack(phase)
        Spacer(Modifier.height(14.dp))
        NextStepRow(phase)

        if (d.loanCaseId != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(StatusColors.BlueBg, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AccountBalance, null, tint = StatusColors.Blue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Home loan · ${formatINRShort(d.loanAmount)} · ${d.loanStatus ?: "Applied"}",
                    color = StatusColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (d.messages.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.Chat,
                    null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${d.messages.size} message${if (d.messages.size != 1) "s" else ""} · tap to open",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
