package com.dealio.app.ui.cp.leads

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.CpDealDetail
import com.dealio.app.data.api.ThreadRef
import com.dealio.app.ui.cp.CpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpDealDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val deal: CpDealDetail? = null,
    val working: Boolean = false,
    val sending: Boolean = false,
    val message: String? = null,
    /** Unread count per threadKey, for the party rail's badges. */
    val unread: Map<String, Int> = emptyMap(),
)

class CpDealDetailViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpDealDetailState())
    val state: StateFlow<CpDealDetailState> = _state.asStateFlow()
    private val threads = ThreadRepository()
    private var dealId = 0L

    /**
     * Refresh the unread badges for this deal's threads.
     *
     * The screen supplies the keys because it owns the roster; the backend still
     * authorizes each pair and silently drops any the caller may not see.
     */
    fun refreshUnread(threadKeys: List<String>) {
        if (threadKeys.isEmpty()) return
        viewModelScope.launch {
            val r = threads.summaries(threadKeys.map { ThreadRef(dealId, it) })
            if (r is ApiResult.Success) {
                _state.update { s -> s.copy(unread = r.data.associate { it.threadKey to it.unreadCount }) }
            }
        }
    }

    /**
     * Nudge whoever the deal is waiting on.
     *
     * Surfaced through the existing snackbar either way: the cooldown reply is
     * the whole point of the rate limit and the user needs to read it.
     */
    fun nudge() {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = threads.nudge(dealId)
            _state.update {
                it.copy(
                    working = false,
                    message = when (r) {
                        is ApiResult.Success -> "Nudged. They'll see what the deal is waiting for."
                        is ApiResult.Error -> r.message
                    },
                )
            }
        }
    }

    /**
     * Mark a thread read and clear its badge locally.
     *
     * Optimistic: the badge clears immediately rather than waiting for the round
     * trip, since a failed mark costs a stale badge and nothing more.
     */
    fun markThreadRead(threadKey: String) {
        if (_state.value.unread[threadKey].let { it == null || it == 0 }) return
        _state.update { it.copy(unread = it.unread - threadKey) }
        viewModelScope.launch { threads.markRead(dealId, threadKey) }
    }

    fun load(id: Long, silent: Boolean = false) {
        dealId = id
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getDeal(id)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, deal = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun agree() {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.agreeDeal(dealId)
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "You've agreed to this deal.") }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    fun sendMessage(text: String, recipientRole: String = "builder") {
        if (text.isBlank()) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            val r = repo.sendDealMessage(dealId, text.trim(), recipientRole)
            _state.update { it.copy(sending = false, message = (r as? ApiResult.Error)?.message) }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    fun addFollowUp(dueDate: String, dueTime: String?, reason: String) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.createFollowUp(dealId, dueDate, dueTime, reason)
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Follow-up scheduled.") }
        }
    }

    fun logCall(outcome: String, duration: String, notes: String?, nextDate: String?, nextTime: String?) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.createCallLog(dealId, outcome, duration, notes, nextDate, nextTime)
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Call logged.") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
