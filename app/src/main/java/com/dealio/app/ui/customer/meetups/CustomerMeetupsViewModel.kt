package com.dealio.app.ui.customer.meetups

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.CustomerRepository
import com.dealio.app.data.api.CustomerMeetup
import com.dealio.app.ui.meetups.MeetupCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerMeetupsState(
    val loading: Boolean = true,
    val error: String? = null,
    val meetups: List<CustomerMeetup> = emptyList(),
    /** The city the list is showing, as the server resolved it. */
    val city: String? = null,
    val category: MeetupCategory? = null,
    val busyId: Long? = null,
    val message: String? = null,
) {
    /**
     * Asked personally, and still owe an answer. These come first everywhere —
     * someone waiting on this customer specifically outranks a listing.
     */
    val awaitingReply: List<CustomerMeetup> get() = meetups.filter { it.awaitingReply && !it.isCancelled }

    /** Answered yes. Their plans. */
    val going: List<CustomerMeetup> get() = meetups.filter { it.isGoing }

    /** Everything else on offer — found by browsing, not by being asked. */
    val nearby: List<CustomerMeetup>
        get() = meetups.filter { !it.awaitingReply && !it.isGoing }
}

/**
 * The customer's view of meetups.
 *
 * One view model behind the full list and the Explore strip, so the two never
 * disagree about what is on and what this customer already answered.
 */
class CustomerMeetupsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CustomerRepository(app)
    private val _state = MutableStateFlow(CustomerMeetupsState())
    val state: StateFlow<CustomerMeetupsState> = _state.asStateFlow()

    init { load() }

    /**
     * Passing no city lets the server fall back to this customer's saved
     * preference, which is the case that matters — the list should be right
     * without the app having to know what they picked.
     */
    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMeetups(category = _state.value.category?.wire)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, error = null, meetups = r.data.meetups, city = r.data.city)
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun setCategory(category: MeetupCategory?) {
        _state.update { it.copy(category = category) }
        load(silent = true)
    }

    fun rsvp(meetupId: Long, rsvp: String, guests: Int = 0) {
        _state.update { it.copy(busyId = meetupId) }
        viewModelScope.launch {
            when (val r = repo.rsvpMeetup(meetupId, rsvp, guests)) {
                is ApiResult.Success -> _state.update { s ->
                    s.copy(
                        busyId = null,
                        // Swap the one row rather than refetching: the list keeps
                        // its scroll position and the change lands immediately.
                        meetups = s.meetups.map { if (it.id == meetupId) r.data else it },
                        message = when (rsvp) {
                            "GOING" -> "You're going"
                            "MAYBE" -> "Marked as maybe"
                            else -> "Thanks for letting them know"
                        },
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(busyId = null, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
