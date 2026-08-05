package com.dealio.app.ui.customer.saved

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.Shortlist
import com.dealio.app.ui.customer.CustomerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Units this customer told a builder they were interested in. */
    val items: List<Shortlist> = emptyList(),
    /** Whole projects bookmarked privately — nobody was told about these. */
    val projects: List<Project> = emptyList(),
    val message: String? = null,
) {
    val isEmpty: Boolean get() = items.isEmpty() && projects.isEmpty()
}

class SavedViewModel(app: Application) : CustomerViewModel(app) {

    private val _state = MutableStateFlow(SavedState())
    val state: StateFlow<SavedState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMyShortlists()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
            (repo.getSavedProjects() as? ApiResult.Success)?.let { r ->
                _state.update { it.copy(projects = r.data) }
            }
        }
    }

    /** Removes a bookmark from here — the same toggle as the one on the card. */
    fun unsave(projectId: Long) {
        val previous = _state.value.projects
        _state.update { it.copy(projects = it.projects.filterNot { p -> p.id == projectId }) }
        viewModelScope.launch {
            val r = repo.unsaveProject(projectId)
            _state.update {
                if (r is ApiResult.Error) it.copy(projects = previous, message = r.message)
                else it.copy(message = "Removed from saved")
            }
        }
    }

    fun requestPricing(s: Shortlist) {
        val builderId = s.builderId ?: return
        viewModelScope.launch {
            val r = repo.requestPricing(
                builderId, s.projectId, s.unitId,
                mapOf("bhkType" to (s.unitDetails?.bhkType), "unit" to s.unitId),
                "Please share pricing for Unit ${s.unitId}",
            )
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Pricing request sent.") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
