package com.dealio.app.ui.builder.tools

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.DealSummary
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.ProjectDocument
import com.dealio.app.ui.builder.BuilderViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared state for the builder utility screens (AI, tours, documents, demand letters, conversations). */
data class BuilderToolsState(
    val loading: Boolean = true,
    val error: String? = null,
    val projects: List<Project> = emptyList(),
    val deals: List<DealSummary> = emptyList(),
)

class BuilderToolsViewModel(app: Application) : BuilderViewModel(app) {
    private val _state = MutableStateFlow(BuilderToolsState())
    val state: StateFlow<BuilderToolsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val projects = (repo.getProjects() as? ApiResult.Success)?.data ?: emptyList()
            val deals = (repo.getDeals() as? ApiResult.Success)?.data ?: emptyList()
            _state.update { it.copy(loading = false, projects = projects, deals = deals) }
        }
    }

    /** On-demand document fetch for the selected project (used by the document vault). */
    suspend fun documents(projectId: Long): List<ProjectDocument> =
        (repo.getDocuments(projectId) as? ApiResult.Success)?.data ?: emptyList()
}
