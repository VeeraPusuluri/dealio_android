package com.dealio.app.ui.cp.overview

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpAuthorizedBuilder
import com.dealio.app.data.api.CpDueToday
import com.dealio.app.data.api.CpLead
import com.dealio.app.ui.cp.CpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.dealio.app.ui.flow.MoveItem
import com.dealio.app.ui.flow.idleDaysSince
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpOverviewState(
    val loading: Boolean = true,
    val error: String? = null,
    val name: String = "Partner",
    val tier: String = "Silver",
    val photoUrl: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val reraNumber: String? = null,
    val authorizedBuilders: List<CpAuthorizedBuilder> = emptyList(),
    /** The CP's own user id, shown on the credential. Null until the profile loads. */
    val partnerId: Long? = null,
    val uploadingPhoto: Boolean = false,
    val message: String? = null,
    val totalEarnings: Double = 0.0,
    val pendingCommission: Double = 0.0,
    val totalDeals: Int = 0,
    val leadsCount: Int = 0,
    /**
     * Every lead reduced for the move queue — built from the whole list, not the
     * recent five, since the deal that needs you most is usually the oldest.
     */
    val moves: List<MoveItem> = emptyList(),
    val recentLeads: List<CpLead> = emptyList(),
    val due: CpDueToday = CpDueToday(),
)

class CpOverviewViewModel(app: Application) : CpViewModel(app) {

    private val _state = MutableStateFlow(CpOverviewState(name = repo.name))
    val state: StateFlow<CpOverviewState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val profile = repo.getProfile()
            val leads = repo.getLeads()
            val due = repo.getDueToday()

            if (leads is ApiResult.Error && profile is ApiResult.Error) {
                _state.update { it.copy(loading = false, error = leads.message) }
                return@launch
            }
            val profileData = (profile as? ApiResult.Success)?.data
            val cp = profileData?.cp
            val leadList = (leads as? ApiResult.Success)?.data ?: emptyList()
            _state.update {
                it.copy(
                    loading = false, error = null,
                    name = profileData?.fullName ?: repo.name,
                    tier = cp?.tier ?: "Silver",
                    photoUrl = cp?.photoUrl,
                    phone = profileData?.phone,
                    city = cp?.city,
                    reraNumber = cp?.reraNumber,
                    authorizedBuilders = profileData?.authorizedBuilders ?: emptyList(),
                    partnerId = profileData?.id?.takeIf { it > 0 },
                    totalEarnings = cp?.totalEarnings ?: 0.0,
                    pendingCommission = cp?.pendingCommission ?: 0.0,
                    totalDeals = cp?.totalDeals ?: leadList.size,
                    leadsCount = leadList.size,
                    moves = leadList.map { l ->
                        MoveItem(
                            dealId = l.id,
                            title = l.customerName,
                            subtitle = l.projectName,
                            rawStatus = l.status,
                            cpAgreed = l.cpAgreed,
                            customerConfirmed = l.customerConfirmed,
                            idleDays = idleDaysSince(l.updatedAt.ifBlank { l.createdAt }),
                        )
                    },
                    recentLeads = leadList.take(5),
                    due = (due as? ApiResult.Success)?.data ?: CpDueToday(),
                )
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        val context = getApplication<Application>()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = if (mime.contains("png")) "png" else if (mime.contains("webp")) "webp" else "jpg"
        _state.update { it.copy(uploadingPhoto = true) }
        viewModelScope.launch {
            val r = repo.uploadDocument("photo", bytes, "photo.$ext", mime)
            _state.update {
                it.copy(
                    uploadingPhoto = false,
                    message = (r as? ApiResult.Error)?.message ?: "Profile photo updated",
                )
            }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
