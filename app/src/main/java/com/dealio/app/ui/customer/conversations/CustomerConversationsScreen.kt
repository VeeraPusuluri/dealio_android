package com.dealio.app.ui.customer.conversations

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CustomerDeal
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.customer.CustomerViewModel
import com.dealio.app.ui.flow.ConversationInbox
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.InboxDeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConvState(val loading: Boolean = true, val error: String? = null, val deals: List<CustomerDeal> = emptyList())

class CustomerConversationsViewModel(app: Application) : CustomerViewModel(app) {
    private val _state = MutableStateFlow(ConvState())
    val state: StateFlow<ConvState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMyDeals()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, deals = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }
}

/**
 * The buyer's inbox — their advisor and their builder as separate rows.
 *
 * The buyer is the party the old single row misled most: "Builder & partner
 * chat" was one line covering two different people, and an unanswered question
 * to the advisor looked identical to one to the builder.
 */
@Composable
fun CustomerConversationsScreen(nav: NavController, vm: CustomerConversationsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    SubScreenScaffold("Conversations", nav) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load, modifier = Modifier.padding(inner))
            else -> {
                val deals = state.deals.map { d ->
                    InboxDeal(
                        dealId = d.dealId,
                        title = d.projectName,
                        subtitle = listOfNotNull(
                            d.builderName,
                            d.cpName?.let { "advised by $it" },
                        ).joinToString(" · "),
                        rawStatus = d.dealStatus,
                        hasCp = d.cpName != null,
                        builderName = d.builderName,
                        cpName = d.cpName,
                    )
                }

                ConversationInbox(
                    viewer = DealRole.CUSTOMER,
                    deals = deals,
                    onOpen = { dealId, thread -> nav.navigate(CustomerRoutes.dealDetail(dealId, thread)) },
                    modifier = Modifier.padding(inner),
                    contentPadding = PaddingValues(16.dp),
                    empty = {
                        DealioCard {
                            EmptyState(
                                Icons.Outlined.ChatBubbleOutline,
                                "No conversations yet",
                                "Book a site visit to start chatting with your builder and channel partner.",
                            )
                        }
                    },
                )
            }
        }
    }
}
