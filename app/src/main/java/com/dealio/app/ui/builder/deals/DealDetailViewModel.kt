package com.dealio.app.ui.builder.deals

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.DealDetail
import com.dealio.app.ui.builder.BuilderViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Forward status flow for a deal, mirroring the web BuilderDealsPage STAGES. */
// The stage ladder now lives in ui/flow/DealFlow.kt, mirroring the backend's
// canonical list. The six-stage version that used to sit here started at
// "Meeting Done", so anything earlier matched nothing on the deal screen.

data class DealDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val deal: DealDetail? = null,
    val working: Boolean = false,
    val toast: String? = null,
)

// Messaging is deliberately absent. A conversation is between the builder and a
// person, not about this deal, so it lives entirely in Conversations — this page
// links there and keeps to the deal. ThreadRepository is still here for the
// nudge, which genuinely is about a stalled transaction.
class DealDetailViewModel(app: Application) : BuilderViewModel(app) {

    private val _state = MutableStateFlow(DealDetailState())
    val state: StateFlow<DealDetailState> = _state.asStateFlow()

    private var dealId: Long = 0
    private val threads = ThreadRepository()

    /** Nudge whoever the deal is waiting on; the cooldown reply is worth showing. */
    fun nudge() {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = threads.nudge(dealId)
            _state.update {
                it.copy(
                    working = false,
                    toast = when (r) {
                        is ApiResult.Success -> "Nudged. They'll see what the deal is waiting for."
                        is ApiResult.Error -> r.message
                    },
                )
            }
        }
    }

    fun load(id: Long) {
        dealId = id
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getDeal(id)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, deal = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun refresh() = load(dealId)

    fun updateStatus(status: String) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val r = repo.updateDealStatus(dealId, status)) {
                is ApiResult.Success -> { _state.update { it.copy(working = false, toast = "Moved to $status") }; load(dealId) }
                is ApiResult.Error -> _state.update { it.copy(working = false, toast = r.message) }
            }
        }
    }

    /**
     * Countersign the signed agreement.
     *
     * Deliberately not `updateStatus("Pending Booking")`: that route moves the
     * deal whether or not the buyer ever sent a signed copy, and does not tell
     * the CP or the buyer that it was accepted. The 400 this can return — "no
     * signed agreement has been submitted yet" — is the answer, so it is
     * surfaced rather than swallowed.
     */
    fun acceptAgreement() {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val r = repo.acceptAgreement(dealId)) {
                is ApiResult.Success -> { _state.update { it.copy(working = false, toast = "Agreement accepted") }; load(dealId) }
                is ApiResult.Error -> _state.update { it.copy(working = false, toast = r.message) }
            }
        }
    }

    fun markSold() {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val r = repo.markDealSold(dealId)) {
                is ApiResult.Success -> { _state.update { it.copy(working = false, toast = "Unit marked sold") }; load(dealId) }
                is ApiResult.Error -> _state.update { it.copy(working = false, toast = r.message) }
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
}
