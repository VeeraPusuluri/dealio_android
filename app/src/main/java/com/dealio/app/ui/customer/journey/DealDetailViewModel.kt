package com.dealio.app.ui.customer.journey

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.CustomerDeal
import com.dealio.app.data.api.ThreadRef
import com.dealio.app.ui.customer.CustomerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DealDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val deal: CustomerDeal? = null,
    val working: Boolean = false,
    val sending: Boolean = false,
    val message: String? = null,
    /** Unread count per threadKey, for the party rail's badges. */
    val unread: Map<String, Int> = emptyMap(),
)

class DealDetailViewModel(app: Application) : CustomerViewModel(app) {

    private val _state = MutableStateFlow(DealDetailState())
    val state: StateFlow<DealDetailState> = _state.asStateFlow()

    private var dealId: Long = 0
    private val threads = ThreadRepository()

    /** Refresh the rail's unread badges. Keys come from the screen, which owns the roster. */
    fun refreshUnread(threadKeys: List<String>) {
        if (threadKeys.isEmpty()) return
        viewModelScope.launch {
            val r = threads.summaries(threadKeys.map { ThreadRef(dealId, it) })
            if (r is ApiResult.Success) {
                _state.update { s -> s.copy(unread = r.data.associate { it.threadKey to it.unreadCount }) }
            }
        }
    }

    /** Nudge whoever the deal is waiting on; the cooldown reply is worth showing. */
    fun nudge() {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = threads.nudge(dealId)
            _state.update {
                it.copy(
                    working = false,
                    message = when (r) {
                        is ApiResult.Success -> "Nudged. They'll see what's needed next."
                        is ApiResult.Error -> r.message
                    },
                )
            }
        }
    }

    /** Optimistic: clear the badge now, since a failed mark costs only a stale badge. */
    fun markThreadRead(threadKey: String) {
        if (_state.value.unread[threadKey].let { it == null || it == 0 }) return
        _state.update { it.copy(unread = it.unread - threadKey) }
        viewModelScope.launch { threads.markRead(dealId, threadKey) }
    }

    fun load(id: Long, silent: Boolean = false) {
        dealId = id
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMyDeals()) {
                is ApiResult.Success -> {
                    val deal = r.data.firstOrNull { it.dealId == id }
                    if (deal == null) _state.update { it.copy(loading = false, error = "Deal not found") }
                    else _state.update { it.copy(loading = false, deal = deal) }
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun confirm() = act { repo.confirmDeal(dealId) }
    fun acceptNegotiation() = act { repo.acceptNegotiation(dealId) }

    private fun act(block: suspend () -> ApiResult<Any>) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = block()
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Done!") }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    fun sendMessage(text: String, recipientRole: String = "builder") {
        if (text.isBlank()) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            val r = repo.sendDealMessage(dealId, recipientRole, text.trim())
            _state.update { it.copy(sending = false, message = (r as? ApiResult.Error)?.message) }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
