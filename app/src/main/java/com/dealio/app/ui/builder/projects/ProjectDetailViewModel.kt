package com.dealio.app.ui.builder.projects

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.ProjectDocument
import com.dealio.app.ui.builder.BuilderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProjectDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val project: Project? = null,
    val documents: List<ProjectDocument> = emptyList(),
    val uploading: Boolean = false,
    val message: String? = null,
)

class ProjectDetailViewModel(app: Application) : BuilderViewModel(app) {

    private val _state = MutableStateFlow(ProjectDetailState())
    val state: StateFlow<ProjectDetailState> = _state.asStateFlow()

    fun load(projectId: Long) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getProject(projectId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, project = r.data) }
                    when (val docs = repo.getDocuments(projectId)) {
                        is ApiResult.Success -> _state.update { it.copy(documents = docs.data) }
                        is ApiResult.Error -> {}
                    }
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    /**
     * Reads the picked file and uploads it under [docType], then reloads the list
     * so the new document appears without a manual refresh.
     */
    fun uploadDocument(uri: Uri, docType: String) {
        val projectId = _state.value.project?.id ?: return
        _state.update { it.copy(uploading = true) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val name = fileName(ctx, uri)
                    val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    bytes?.let { Triple(it, name, mime) }
                }.getOrNull()
            }
            if (file == null) {
                _state.update { it.copy(uploading = false, message = "Couldn't read that file.") }
                return@launch
            }
            val (bytes, name, mime) = file
            val r = repo.uploadProjectDocument(projectId, docType, bytes, name, mime)
            _state.update {
                it.copy(
                    uploading = false,
                    message = (r as? ApiResult.Error)?.message ?: "Uploaded $name.",
                )
            }
            if (r is ApiResult.Success) {
                (repo.getDocuments(projectId) as? ApiResult.Success)?.let { d ->
                    _state.update { it.copy(documents = d.data) }
                }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

private fun fileName(ctx: android.content.Context, uri: Uri): String {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0) c.getString(i)?.let { return it }
        }
    }
    return uri.lastPathSegment ?: "upload"
}
