package com.dealio.app.ui.builder.overview

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.DealSummary
import com.dealio.app.ui.builder.BuilderViewModel
import com.dealio.app.ui.flow.MoveItem
import com.dealio.app.ui.flow.idleDaysSince
import com.dealio.app.ui.flow.isDealStage
import com.dealio.app.ui.flow.isLeadStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewState(
    val loading: Boolean = true,
    val error: String? = null,
    val projects: Int = 0,
    val leads: Int = 0,
    val deals: Int = 0,
    val booked: Int = 0,
    val revenue: Double = 0.0,
    val recentDeals: List<DealSummary> = emptyList(),
    val builderName: String? = null,
    /**
     * The builder's own photo, from the cached session.
     *
     * The hero showed initials unconditionally, so a builder could upload a
     * picture in Settings and never see it anywhere — the CP home has shown
     * theirs all along.
     */
    val avatarUrl: String? = null,
    /** Every deal reduced for the move queue — the whole list, not the recent few. */
    val moves: List<MoveItem> = emptyList(),
)

class OverviewViewModel(app: Application) : BuilderViewModel(app) {

    private val _state = MutableStateFlow(OverviewState())
    val state: StateFlow<OverviewState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val user = repo.currentUser
            val name = user?.fullName
            val projects = repo.getProjects()
            val leads = repo.getLeads()
            val deals = repo.getDeals()

            val firstError = listOf(projects, leads, deals)
                .filterIsInstance<ApiResult.Error>().firstOrNull()
            if (firstError != null && projects is ApiResult.Error) {
                _state.update { it.copy(loading = false, error = firstError.message, builderName = name, avatarUrl = user?.avatarUrl) }
                return@launch
            }

            val projectList = (projects as? ApiResult.Success)?.data ?: emptyList()
            // The two tiles counted the same rows twice — "21 Active Leads" and
            // "21 Deals" were the same twenty-one people, because /leads and
            // /deals returned an identical set. Split them on the conversion
            // stage so the numbers add up to the pipeline instead of doubling it.
            val leadList = ((leads as? ApiResult.Success)?.data ?: emptyList())
                .filter { isLeadStage(it.stage) }
            val dealList = ((deals as? ApiResult.Success)?.data ?: emptyList())
                .filter { isDealStage(it.status) }
            val booked = dealList.filter { it.status.lowercase() in listOf("booked", "closed") }
            _state.update {
                it.copy(
                    loading = false,
                    error = null,
                    projects = projectList.size,
                    leads = leadList.size,
                    deals = dealList.size,
                    booked = booked.size,
                    revenue = booked.sumOf { d -> d.dealValue ?: 0.0 },
                    recentDeals = dealList.take(6),
                    // The move queue spans the whole pipeline, not just the deals
                    // half: "Confirm a site visit slot" is owed at Meeting
                    // Requested, a lead stage. While /leads and /deals returned
                    // the same rows this could be built from either one; now that
                    // they partition, building it from deals alone would silently
                    // drop every lead waiting on the builder.
                    moves = leadList.map { l ->
                        MoveItem(
                            dealId = l.id.toLongOrNull() ?: 0L,
                            title = l.customerName,
                            subtitle = l.projectName,
                            rawStatus = l.stage,
                            idleDays = l.daysInStage,
                        )
                    } + dealList.map { d ->
                        MoveItem(
                            dealId = d.id,
                            title = d.customerName,
                            subtitle = d.projectName,
                            rawStatus = d.status,
                            // The builder never holds the baton at Agreement, the
                            // only stage the flags affect, so their absence here
                            // cannot change what lands in this queue.
                            idleDays = idleDaysSince(d.updatedAt.ifBlank { d.createdAt }),
                        )
                    },
                    builderName = name,
                    avatarUrl = user?.avatarUrl,
                )
            }
        }
    }
}
