package com.dealio.app.ui.builder.conversations

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
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.tools.BuilderToolsViewModel
import com.dealio.app.ui.flow.ConversationInbox
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.InboxDeal

/**
 * The builder's inbox, one row per thread rather than per deal.
 *
 * Which threads a deal offers is not uniform here: on a CP-attached deal before
 * booking the private buyer thread is withheld and the group stands in for it,
 * so the rows follow the same roster the deal screen renders.
 */
@Composable
fun BuilderConversationsScreen(nav: NavController, vm: BuilderToolsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    SubScreenScaffold("Conversations", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }

        val deals = state.deals.map { d ->
            InboxDeal(
                dealId = d.id,
                title = d.customerName,
                subtitle = listOfNotNull(
                    d.projectName.ifBlank { null },
                    d.cpName?.let { "via $it" },
                ).joinToString(" · "),
                rawStatus = d.status,
                // A CP with no name on the payload is still a CP — deciding from
                // the id keeps the roster in step with what the backend allows.
                hasCp = d.cpId != null || d.cpName != null,
                cpName = d.cpName,
                customerName = d.customerName,
            )
        }

        ConversationInbox(
            viewer = DealRole.BUILDER,
            deals = deals,
            onOpen = { dealId, thread -> nav.navigate(BuilderRoutes.dealDetail(dealId, thread)) },
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            empty = {
                DealioCard {
                    EmptyState(
                        Icons.Outlined.ChatBubbleOutline,
                        "No conversations yet",
                        "When a customer books a site visit, your chat with them and the channel partner appears here.",
                    )
                }
            },
        )
    }
}
