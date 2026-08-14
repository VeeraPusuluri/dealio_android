package com.dealio.app.ui.cp.conversations

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.cp.growth.CpGrowthViewModel
import com.dealio.app.ui.flow.ConversationInbox
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.InboxDeal

/**
 * The CP's inbox: every referred deal, and within it the builder thread, the
 * buyer thread and the three-way group as separate rows.
 *
 * A channel partner is the only party who always has two counterparties, so the
 * flat one-row-per-deal list hurt them most — a builder's reply and a buyer's
 * reply arrived at the same undifferentiated row.
 */
@Composable
fun ConversationsScreen(
    nav: NavController,
    /** Narrows the inbox to one deal — how the deal page's Message button arrives. */
    dealId: Long? = null,
    vm: CpGrowthViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    SubScreenScaffold(if (dealId == null) "Conversations" else "Messages", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }

        // Each referred deal is a conversation; the CP is on every one of them by
        // definition, so the group thread always exists.
        val deals = state.leads.distinctBy { it.id }.filter { dealId == null || it.id == dealId }.map {
            InboxDeal(
                dealId = it.id,
                title = it.customerName,
                subtitle = it.projectName,
                rawStatus = it.status,
                hasCp = true,
                customerName = it.customerName,
            )
        }

        ConversationInbox(
            viewer = DealRole.CP,
            deals = deals,
            onOpen = { id, thread -> nav.navigate(CpRoutes.thread(id, thread)) },
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            empty = {
                DealioCard {
                    EmptyState(
                        Icons.Outlined.ChatBubbleOutline,
                        "No conversations yet",
                        "Refer a lead from the Projects page to start chatting with the customer and builder.",
                    )
                }
            },
        )
    }
}
