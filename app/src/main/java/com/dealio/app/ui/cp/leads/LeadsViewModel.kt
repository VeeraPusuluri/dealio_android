package com.dealio.app.ui.cp.leads

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.CpLead
import com.dealio.app.data.api.Project
import com.dealio.app.ui.cp.CpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeadsState(
    val loading: Boolean = true,
    val error: String? = null,
    val all: List<CpLead> = emptyList(),
    val statusFilter: String = "All",
    // Pickers for the "Add lead" form
    val projects: List<Project> = emptyList(),
    val contacts: List<CpContact> = emptyList(),
    val working: Boolean = false,
    val message: String? = null,
) {
    val statuses: List<String> get() = listOf("All") + all.map { it.status }.distinct()
    val filtered: List<CpLead> get() = if (statusFilter == "All") all else all.filter { it.status == statusFilter }
}

class LeadsViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(LeadsState())
    val state: StateFlow<LeadsState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getLeads()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, all = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
            // Pickers for the add-lead form (best-effort; failures leave them empty).
            (repo.getProjects() as? ApiResult.Success)?.let { r -> _state.update { it.copy(projects = r.data) } }
            (repo.getContacts() as? ApiResult.Success)?.let { r -> _state.update { it.copy(contacts = r.data) } }
        }
    }

    fun setFilter(f: String) = _state.update { it.copy(statusFilter = f) }

    fun createLead(projectId: Long, name: String, phone: String, email: String, onDone: () -> Unit) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val r = repo.createLead(projectId, name, phone, email.ifBlank { null })) {
                is ApiResult.Success -> {
                    onDone()
                    _state.update { it.copy(working = false, message = "Lead added for $name.") }
                    load(silent = true)
                }
                is ApiResult.Error -> _state.update { it.copy(working = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
