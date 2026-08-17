package com.dealio.app.ui.customer.journey

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.CustomerDeal
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.UnitRow
import com.dealio.app.ui.customer.CustomerViewModel
import com.dealio.app.ui.flow.unitsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DealDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val deal: CustomerDeal? = null,
    val working: Boolean = false,
    val message: String? = null,
    // ── The unit picker ──
    /** Open when the buyer is choosing a flat off the project's matrix. */
    val picking: Boolean = false,
    val loadingUnits: Boolean = false,
    val units: List<UnitRow> = emptyList(),
    /** The project behind the matrix — carries the builderId a shortlist needs. */
    val project: Project? = null,
    val pickedUnit: UnitRow? = null,
    /** The unit already shortlisted, once one is. */
    val shortlistedUnitId: String? = null,
)

// Messaging is deliberately absent. A conversation is between the buyer and a
// person, not about this deal, so it lives entirely in Conversations — this page
// links there and keeps to the deal. ThreadRepository is still here for the
// nudge, which genuinely is about a stalled transaction.
class DealDetailViewModel(app: Application) : CustomerViewModel(app) {

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
                    message = when (r) {
                        is ApiResult.Success -> "Nudged. They'll see what's needed next."
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
            when (val r = repo.getMyDeals()) {
                is ApiResult.Success -> {
                    val deal = r.data.firstOrNull { it.dealId == id }
                    if (deal == null) _state.update { it.copy(loading = false, error = "Deal not found") }
                    else _state.update { it.copy(loading = false, deal = deal) }
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun confirm() = act { repo.confirmDeal(dealId) }
    fun acceptNegotiation() = act { repo.acceptNegotiation(dealId) }

    // ─── The unit picker ─────────────────────────────────────────────────────
    //
    // Naming an actual flat is the move the app was missing. The buyer could
    // shortlist a *configuration* from the project page — "2 BHK" — which told
    // the builder what shape of home they wanted and gave them nothing to
    // reserve. The website has always picked off the project's unit matrix, and
    // it is the unit id that the shortlist, the pricing request and eventually
    // the booking all travel on.

    /**
     * Open the picker, loading the project's matrix behind it.
     *
     * The project is fetched rather than taken off the deal because the deal
     * payload carries a project *name* and no inventory — and because the
     * project row is also where the builderId a shortlist needs lives.
     */
    fun startPickingUnit() {
        val projectId = _state.value.deal?.projectId ?: return
        _state.update { it.copy(picking = true, loadingUnits = true, pickedUnit = null) }
        viewModelScope.launch {
            when (val r = repo.getProject(projectId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loadingUnits = false,
                        project = r.data,
                        // Sold and booked units stay in the list: a buyer needs
                        // to see the whole board, including what has gone. The
                        // grid refuses to select them.
                        units = unitsOf(r.data),
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(loadingUnits = false, picking = false, message = r.message)
                }
            }
        }
    }

    fun stopPickingUnit() = _state.update { it.copy(picking = false) }

    fun pickUnit(unit: UnitRow) = _state.update { it.copy(pickedUnit = unit) }

    /**
     * Shortlist the picked unit against this deal's project.
     *
     * `unitDetails` mirrors the shape the website sends so a shortlist made in
     * the app renders identically in the builder's queue — same keys, same
     * strings, no second format for the same record.
     */
    fun shortlistPickedUnit() {
        val s = _state.value
        val unit = s.pickedUnit ?: return
        val deal = s.deal ?: return
        val builderId = s.project?.builderId
        if (builderId == null) {
            _state.update { it.copy(message = "This project has no builder attached yet.") }
            return
        }
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = repo.shortlistUnit(
                builderId = builderId,
                projectId = deal.projectId,
                cpId = null,
                unitId = unit.id,
                details = mapOf(
                    "unitNumber" to unit.id,
                    "tower" to unit.tower,
                    "floor" to unit.floor?.toString(),
                    "bhkType" to unit.bhk,
                    "carpetArea" to unit.areaSqft?.let { "$it sqft" },
                    "facing" to unit.facing,
                    "status" to unit.status,
                ),
            )
            _state.update {
                it.copy(
                    working = false,
                    picking = r is ApiResult.Error,
                    shortlistedUnitId = if (r is ApiResult.Success) unit.id else it.shortlistedUnitId,
                    message = when (r) {
                        is ApiResult.Success ->
                            "Unit ${unit.id} shortlisted. The builder will review it and share a price."
                        is ApiResult.Error -> r.message
                    },
                )
            }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    /**
     * Submits the signed agreement the buyer picked.
     *
     * Read off the main thread — a scanned agreement is a multi-megabyte PDF and
     * `openInputStream` on a document provider is a real IPC round trip. An
     * unreadable pick reports rather than uploading an empty file, which the
     * server would accept as a valid agreement.
     */
    fun uploadSignedAgreement(uri: Uri) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    Triple(bytes, displayName(ctx, uri), ctx.contentResolver.getType(uri) ?: "application/pdf")
                }.getOrNull()
            }
            if (picked == null || picked.first.isEmpty()) {
                _state.update { it.copy(working = false, message = "Could not read that file. Try another.") }
                return@launch
            }
            val (bytes, name, mime) = picked
            val r = repo.uploadSignedAgreement(dealId, bytes, name, mime)
            _state.update {
                it.copy(
                    working = false,
                    message = when (r) {
                        is ApiResult.Success -> "Signed agreement sent to the builder."
                        is ApiResult.Error -> r.message
                    },
                )
            }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    private fun act(block: suspend () -> ApiResult<Any>) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val r = block()
            _state.update { it.copy(working = false, message = (r as? ApiResult.Error)?.message ?: "Done!") }
            if (r is ApiResult.Success) load(dealId, silent = true)
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/** The file's own name, so the builder sees what the buyer sent rather than a content id. */
private fun displayName(ctx: android.content.Context, uri: Uri): String {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0) c.getString(i)?.takeIf { it.isNotBlank() }?.let { return it }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "signed-agreement.pdf"
}
