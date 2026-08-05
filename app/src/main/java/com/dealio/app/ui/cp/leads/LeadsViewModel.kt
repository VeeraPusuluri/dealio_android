package com.dealio.app.ui.cp.leads

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.Spreadsheet
import com.dealio.app.ui.cp.contacts.ImportContact
import com.dealio.app.ui.cp.contacts.phoneIdentity
import com.dealio.app.ui.cp.contacts.rowsToContacts
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.CpLead
import com.dealio.app.data.api.Project
import com.dealio.app.ui.cp.CpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    // Bulk import staging — a lead needs a project, so the whole batch is filed
    // against one, chosen in the review sheet.
    val staged: List<ImportContact>? = null,
    val importProjectId: Long? = null,
    val importing: Boolean = false,
    val importProgress: Int = 0,
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
            (repo.getContacts() as? ApiResult.Success)?.let { r ->
                _state.update { it.copy(contacts = onePerPerson(r.data)) }
            }
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

    // ── Bulk import ──────────────────────────────────────────────────────────

    fun stageFromSheet(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val staged = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    rowsToContacts(Spreadsheet.read(bytes, sheetName(ctx, uri)))
                }.getOrDefault(emptyList())
            }
            _state.update { it.copy(staged = staged, importProjectId = it.projects.firstOrNull()?.id) }
        }
    }

    fun setImportProject(id: Long) = _state.update { it.copy(importProjectId = id) }

    fun toggleStaged(index: Int) = _state.update { s ->
        s.copy(staged = s.staged?.mapIndexed { i, c -> if (i == index) c.copy(selected = !c.selected) else c })
    }

    fun selectAllStaged(select: Boolean) = _state.update { s ->
        s.copy(staged = s.staged?.map { it.copy(selected = select) })
    }

    fun clearStaged() = _state.update { it.copy(staged = null, importProgress = 0) }

    /** One POST per row — there is no bulk endpoint. Failures are counted, not fatal. */
    fun importStaged() {
        val projectId = _state.value.importProjectId ?: return
        val chosen = _state.value.staged?.filter { it.selected }.orEmpty()
        if (chosen.isEmpty()) return
        _state.update { it.copy(importing = true, importProgress = 0) }
        viewModelScope.launch {
            var ok = 0
            chosen.forEachIndexed { i, c ->
                if (repo.createLead(projectId, c.name, c.phone, c.email) is ApiResult.Success) ok++
                _state.update { it.copy(importProgress = i + 1) }
            }
            val failed = chosen.size - ok
            _state.update {
                it.copy(
                    importing = false, staged = null, importProgress = 0,
                    message = if (failed == 0) "Imported $ok lead${if (ok == 1) "" else "s"}."
                    else "Imported $ok of ${chosen.size} — $failed could not be saved.",
                )
            }
            load(silent = true)
        }
    }
}

/**
 * One chip per person for the add-lead form's contact shortcuts.
 *
 * A book collects twins: importing the phone a second time re-adds everyone
 * already in it, and a buyer entered by hand turns up again in the next
 * spreadsheet — the server stores each row as it arrives without matching on
 * the number. Harmless on the contacts page, where the CP can see and delete
 * the extra, but the picker is a single scrolling row where the same name
 * twice is just noise.
 *
 * The newest row wins, being the one the CP last touched, and an older twin
 * lends whatever the newer one left blank rather than being dropped whole —
 * picking a contact should still fill in the email that was saved for them.
 */
internal fun onePerPerson(contacts: List<CpContact>): List<CpContact> {
    val byPerson = LinkedHashMap<String, CpContact>()
    contacts.forEach { c ->
        // Contacts without a usable number are keyed by name, so two different
        // people missing one stay apart instead of collapsing into each other.
        val key = phoneIdentity(c.countryCode, c.phone)
            .takeIf { it.length >= 6 }
            ?: "name:${c.name.trim().lowercase()}"
        val kept = byPerson[key]
        byPerson[key] = if (kept == null) c else kept.copy(
            name = kept.name.ifBlank { c.name },
            email = kept.email?.takeIf { it.isNotBlank() } ?: c.email,
        )
    }
    return byPerson.values.toList()
}

private fun sheetName(ctx: android.content.Context, uri: Uri): String {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0) c.getString(i)?.let { return it }
        }
    }
    return uri.lastPathSegment.orEmpty()
}
