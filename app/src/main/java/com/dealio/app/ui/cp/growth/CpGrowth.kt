package com.dealio.app.ui.cp.growth

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.CpLead
import com.dealio.app.data.api.CpProfile
import com.dealio.app.data.api.Project
import com.dealio.app.ui.cp.CpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared state for the channel-partner growth tools (leads, profile, contacts, projects). */
data class CpGrowthState(
    val loading: Boolean = true,
    val error: String? = null,
    val leads: List<CpLead> = emptyList(),
    val profile: CpProfile? = null,
    val contacts: List<CpContact> = emptyList(),
    val projects: List<Project> = emptyList(),
)

/**
 * One view-model backing every growth screen. Each screen reads the slice it needs;
 * loads are best-effort so a single failing endpoint doesn't blank the whole screen.
 */
class CpGrowthViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpGrowthState())
    val state: StateFlow<CpGrowthState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val leads = (repo.getLeads() as? ApiResult.Success)?.data ?: emptyList()
            val profile = (repo.getProfile() as? ApiResult.Success)?.data
            val contacts = (repo.getContacts() as? ApiResult.Success)?.data ?: emptyList()
            val projects = (repo.getProjects() as? ApiResult.Success)?.data ?: emptyList()
            _state.update {
                it.copy(loading = false, leads = leads, profile = profile, contacts = contacts, projects = projects)
            }
        }
    }
}

// ─── Share / intent helpers ──────────────────────────────────────────────────

/** Opens WhatsApp (or the chooser) pre-filled with [text], optionally to a 10-digit Indian [phone]. */
fun openWhatsApp(ctx: Context, phone: String?, text: String) {
    val digits = phone?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }
    val to = if (digits != null) "91$digits" else ""
    val url = "https://wa.me/$to?text=${Uri.encode(text)}"
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(ctx, "WhatsApp not available") }
}

fun dial(ctx: Context, phone: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(ctx, "Cannot place call") }
}

fun copyToClipboard(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    toast(ctx, "$label copied")
}

fun shareText(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { ctx.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

// ─── Small shared bits ───────────────────────────────────────────────────────

@Composable
fun ComingSoonPill(text: String = "Coming Soon", modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color(0xFFD97706),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color(0xFFFDF3E7), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Short INR like ₹1.2L / ₹3Cr from a plain number (for referral/social tiles). */
fun fmtShortRupee(n: Double): String = when {
    n >= 1_00_00_000 -> "₹${"%.1f".format(n / 1_00_00_000)}Cr"
    n >= 1_00_000 -> "₹${(n / 1_00_000).toInt()}L"
    else -> "₹${n.toLong()}"
}
