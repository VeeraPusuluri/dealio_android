package com.dealio.app.ui.cp.leads

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.CpDealDetail
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
    val message: String? = null,
)

// Messaging is deliberately absent. A conversation is between the CP and a
// person, not about this deal, so it lives entirely in Conversations — this
// page links there and keeps to the deal. ThreadRepository is still here for
// the nudge, which genuinely is about a stalled transaction.
class CpDealDetailViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpDealDetailState())
    val state: StateFlow<CpDealDetailState> = _state.asStateFlow()
    private val threads = ThreadRepository()
    private var dealId = 0L

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

    fun addFollowUp(dueDate: String, dueTime: String?, reason: String) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.createFollowUp(dealId, dueDate, dueTime, reason)
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Follow-up scheduled.") }
        }
    }

    /**
     * Book a site visit for this lead's customer.
     *
     * The CP could already do this from a project screen, but only by retyping
     * the customer's name and phone — from a lead we know both, so they come
     * straight off the deal. The booking upserts the deal server-side, which is
     * what carries it from Profile Created to Meeting Requested, so reload
     * afterwards to pick up the new stage.
     */
    fun bookVisit(date: String, time: String, type: String, notes: String, onDone: () -> Unit) {
        val deal = _state.value.deal ?: return
        val builderId = deal.builderId
        if (builderId == null) {
            _state.update { it.copy(message = "This lead has no builder attached — open it from the project instead.") }
            return
        }
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.bookVisit(
                builderId = builderId,
                projectId = deal.projectId,
                customerName = deal.customerName,
                customerPhone = deal.customerPhone,
                date = date,
                time = time,
                type = type,
                notes = notes.ifBlank { null },
            )
            _state.update {
                it.copy(
                    working = false,
                    message = (r as? ApiResult.Error)?.message ?: "Visit requested for ${deal.customerName}.",
                )
            }
            if (r is ApiResult.Success) { onDone(); load(dealId, silent = true) }
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
