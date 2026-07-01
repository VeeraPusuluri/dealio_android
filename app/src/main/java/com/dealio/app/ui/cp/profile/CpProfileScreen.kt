package com.dealio.app.ui.cp.profile

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpInfo
import com.dealio.app.data.api.CpProfile
import com.dealio.app.data.api.CpProfileUpdateRequest
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpProfileState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: CpProfile? = null,
    val message: String? = null,
    val uploadingDoc: String? = null,
    val sendingOtp: Boolean = false,
    val otpSent: Boolean = false,
    val verifyingOtp: Boolean = false,
)

class CpProfileViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpProfileState())
    val state: StateFlow<CpProfileState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getProfile()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, profile = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun save(city: String, bio: String, rera: String) {
        viewModelScope.launch {
            val r = repo.updateProfile(CpProfileUpdateRequest(city = city.ifBlank { null }, bio = bio.ifBlank { null }, reraNumber = rera.ifBlank { null }))
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Profile updated") }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun uploadDocument(docType: String, uri: Uri) {
        val context = getApplication<Application>()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = if (mime.contains("png")) "png" else if (mime.contains("pdf")) "pdf" else "jpg"
        _state.update { it.copy(uploadingDoc = docType) }
        viewModelScope.launch {
            val r = repo.uploadDocument(docType, bytes, "$docType.$ext", mime)
            _state.update {
                it.copy(
                    uploadingDoc = null,
                    message = (r as? ApiResult.Error)?.message ?: "Document uploaded — pending review",
                )
            }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun sendPhoneOtp(phone: String) {
        _state.update { it.copy(sendingOtp = true) }
        viewModelScope.launch {
            val r = repo.sendPhoneOtp(phone)
            _state.update {
                it.copy(
                    sendingOtp = false,
                    otpSent = r is ApiResult.Success,
                    message = (r as? ApiResult.Error)?.message ?: "OTP sent to $phone",
                )
            }
        }
    }

    fun verifyPhoneOtp(phone: String, otp: String, onVerified: () -> Unit) {
        _state.update { it.copy(verifyingOtp = true) }
        viewModelScope.launch {
            val r = repo.verifyPhone(phone, otp)
            _state.update {
                it.copy(
                    verifyingOtp = false,
                    message = (r as? ApiResult.Error)?.message ?: "Phone verified",
                )
            }
            if (r is ApiResult.Success) {
                _state.update { it.copy(otpSent = false) }
                load(silent = true)
                onVerified()
            }
        }
    }

    fun resetOtpState() = _state.update { it.copy(otpSent = false) }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun CpProfileScreen(nav: NavController, vm: CpProfileViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    var phoneOtpDialogFor by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val aadhaarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadDocument("aadhaar", it) }
    }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadDocument("pan", it) }
    }

    SubScreenScaffold("Profile", nav) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() }, modifier = Modifier.padding(inner))
            else -> {
                val p = state.profile
                val cp = p?.cp
                Box(Modifier.fillMaxSize().padding(inner)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).background(Teal, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Text(initialsOf(p?.fullName), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p?.fullName ?: "Partner", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(p?.phone ?: "", color = TextSecondary, fontSize = 13.sp)
                            }
                            Row(
                                Modifier.background(Orange.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.WorkspacePremium, null, tint = Orange, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(cp?.tier ?: "Silver", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox("Total earned", formatINRShort(cp?.totalEarnings ?: 0.0), Modifier.weight(1f))
                        StatBox("Total deals", (cp?.totalDeals ?: 0).toString(), Modifier.weight(1f))
                    }

                    DealioCard {
                        SectionLabel("Verification")
                        Spacer(Modifier.height(10.dp))
                        VerifyRow(
                            label = "Phone",
                            verified = cp?.phoneVerified == true,
                            actionLabel = "Verify",
                            onAction = { phoneOtpDialogFor = p?.phone ?: "" },
                        )
                        DocVerifyRow(
                            label = "Aadhaar",
                            verified = cp?.aadhaarVerified == true,
                            hasDoc = !cp?.aadhaarUrl.isNullOrBlank(),
                            uploading = state.uploadingDoc == "aadhaar",
                            onUpload = { aadhaarPicker.launch("*/*") },
                        )
                        DocVerifyRow(
                            label = "PAN",
                            verified = cp?.panVerified == true,
                            hasDoc = !cp?.panUrl.isNullOrBlank(),
                            uploading = state.uploadingDoc == "pan",
                            onUpload = { panPicker.launch("*/*") },
                        )
                    }

                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Details", Modifier.weight(1f))
                            Text("Edit", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(4.dp).background(Teal.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)
                                    .let { it }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        InfoRow("City", cp?.city)
                        InfoRow("RERA number", cp?.reraNumber)
                        InfoRow("Email", p?.email)
                        if (!cp?.bio.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(cp!!.bio!!, color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showEdit = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Edit profile", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }

                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                }

                if (showEdit) {
                    EditDialog(cp, onDismiss = { showEdit = false }) { city, bio, rera -> vm.save(city, bio, rera); showEdit = false }
                }

                phoneOtpDialogFor?.let { phone ->
                    PhoneOtpDialog(
                        phone = phone,
                        otpSent = state.otpSent,
                        sending = state.sendingOtp,
                        verifying = state.verifyingOtp,
                        onSendOtp = { vm.sendPhoneOtp(phone) },
                        onVerify = { otp -> vm.verifyPhoneOtp(phone, otp) { phoneOtpDialogFor = null } },
                        onDismiss = { vm.resetOtpState(); phoneOtpDialogFor = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(value, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun VerifyRow(label: String, verified: Boolean, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (verified) Icons.Outlined.CheckCircle else Icons.Outlined.PendingActions,
            null, tint = if (verified) StatusColors.Green else Orange, modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (!verified && actionLabel != null && onAction != null) {
            Text(
                actionLabel, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(Teal.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clickableAction(onAction),
            )
        } else {
            Text(if (verified) "Verified" else "Pending", color = if (verified) StatusColors.Green else Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DocVerifyRow(label: String, verified: Boolean, hasDoc: Boolean, uploading: Boolean, onUpload: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (verified) Icons.Outlined.CheckCircle else Icons.Outlined.PendingActions,
            null, tint = if (verified) StatusColors.Green else Orange, modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        when {
            uploading -> CircularProgressIndicator(Modifier.size(16.dp), color = Teal, strokeWidth = 2.dp)
            verified -> Text("Verified", color = StatusColors.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            hasDoc -> Text("Under review", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            else -> Text(
                "Upload", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(Teal.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clickableAction(onUpload),
            )
        }
    }
}

private fun Modifier.clickableAction(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(onClick = onClick),
)

@Composable
private fun PhoneOtpDialog(
    phone: String,
    otpSent: Boolean,
    sending: Boolean,
    verifying: Boolean,
    onSendOtp: () -> Unit,
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify phone", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text("We'll send a one-time code to $phone", color = TextSecondary, fontSize = 13.sp)
                if (otpSent) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = otp, onValueChange = { otp = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Enter OTP") },
                        singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (otpSent) onVerify(otp) else onSendOtp() },
                enabled = !sending && !verifying,
            ) {
                Text(if (otpSent) "Verify" else "Send OTP", color = Teal, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
    )
}

@Composable
private fun EditDialog(cp: CpInfo?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var city by remember { mutableStateOf(cp?.city ?: "") }
    var bio by remember { mutableStateOf(cp?.bio ?: "") }
    var rera by remember { mutableStateOf(cp?.reraNumber ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(city, bio, rera) }) { Text("Save", color = Teal, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        title = { Text("Edit profile", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(value = city, onValueChange = { city = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), label = { Text("City") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors())
                OutlinedTextField(value = rera, onValueChange = { rera = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), label = { Text("RERA number") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), label = { Text("Bio") }, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(), minLines = 2)
            }
        },
    )
}
