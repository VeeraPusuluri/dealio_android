package com.dealio.app.ui.builder.pipeline

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Lead
import com.dealio.app.ui.builder.BuilderViewModel
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.batonOf
import com.dealio.app.ui.flow.canonicalStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A lead row with its raw id and resolved display stage. */
data class LeadRow(val id: Long, val lead: Lead, val stage: String)

/** How the pipeline is cut: by where a lead is, or by who owes its next move. */
enum class PipelineGrouping { STAGE, BATON }

/**
 * The buckets the baton grouping offers, in the order a builder should read them.
 *
 * `COMPLETE` is a real answer (nobody owes anything), and `UNKNOWN` collects rows
 * whose stage isn't on the canonical ladder — surfacing them rather than filing
 * them under a guess, since a status the app doesn't recognise is a data problem
 * worth seeing.
 */
const val BATON_COMPLETE = "complete"
const val BATON_UNKNOWN = "unknown"
val BATON_GROUPS = listOf(DealRole.BUILDER.name, DealRole.CP.name, DealRole.CUSTOMER.name, BATON_COMPLETE, BATON_UNKNOWN)

data class PipelineState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<LeadRow> = emptyList(),
    val selectedStage: String = LEAD_STAGES.first(),
    val grouping: PipelineGrouping = PipelineGrouping.STAGE,
    val updating: Boolean = false,
    val toast: String? = null,
) {
    val counts: Map<String, Int> get() = rows.groupingBy { it.stage }.eachCount()
    val visible: List<LeadRow> get() = rows.filter { it.stage == selectedStage }
    val total: Int get() = rows.size

    /**
     * Leads keyed by who owes the next move.
     *
     * A lead lands in one bucket — its first holder — even at Agreement, where
     * both the partner and the buyer are owed independently; the card says so.
     * The agreement flags aren't on the leads payload, so neither side can be
     * struck off here the way the deal screen strikes them off.
     */
    val batonGroups: Map<String, List<LeadRow>> get() = rows.groupBy { row ->
        when {
            canonicalStage(row.stage) == null -> BATON_UNKNOWN
            else -> batonOf(row.stage).holders.firstOrNull()?.name ?: BATON_COMPLETE
        }
    }
}

class PipelineViewModel(app: Application) : BuilderViewModel(app) {

    private val _state = MutableStateFlow(PipelineState())
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getLeads()) {
                is ApiResult.Success -> {
                    val rows = r.data.map { l ->
                        LeadRow(l.id.toLongOrNull() ?: 0, l, stageLabel(l.stage))
                    }
                    _state.update { it.copy(loading = false, rows = rows) }
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun selectStage(stage: String) = _state.update { it.copy(selectedStage = stage) }

    fun selectGrouping(grouping: PipelineGrouping) = _state.update { it.copy(grouping = grouping) }

    fun moveStage(row: LeadRow, toStage: String) {
        _state.update { it.copy(updating = true) }
        viewModelScope.launch {
            when (val r = repo.updateLeadStage(row.id, stageEnum(toStage))) {
                is ApiResult.Success -> {
                    _state.update { s ->
                        s.copy(
                            updating = false,
                            toast = "Moved to $toStage",
                            rows = s.rows.map { if (it.id == row.id) it.copy(stage = toStage) else it },
                        )
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(updating = false, toast = r.message) }
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
}
