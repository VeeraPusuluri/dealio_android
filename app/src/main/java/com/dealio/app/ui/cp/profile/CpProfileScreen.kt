package com.dealio.app.ui.cp.profile

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpInfo
import com.dealio.app.data.api.CpProfile
import com.dealio.app.data.api.CpProfileUpdateRequest
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.components.AppLockToggleRow
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.ButtonDisabled
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.subtleShadow
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpCredentialCard
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

    fun save(fullName: String, city: String, bio: String) {
        viewModelScope.launch {
            val r = repo.updateProfile(
                CpProfileUpdateRequest(
                    fullName = fullName.ifBlank { null },
                    city = city.ifBlank { null },
                    bio = bio.ifBlank { null },
                ),
            )
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
            // A profile photo isn't reviewed by anyone, so "pending review" would be
            // a lie about what happens next.
            val done = if (docType == "photo") "Profile photo updated" else "Document uploaded — pending review"
            _state.update {
                it.copy(
                    uploadingDoc = null,
                    message = (r as? ApiResult.Error)?.message ?: done,
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
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadDocument("photo", it) }
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
                    // The credential is the hero. Everything below it is plain white
                    // surfaces so the card is the only thing on the page with shine.
                    CpCredentialCard(
                        name = p?.fullName ?: "Partner",
                        tier = cp?.tier ?: "Silver",
                        photoUrl = cp?.photoUrl,
                        phone = p?.phone,
                        city = cp?.city,
                        reraNumber = cp?.reraNumber,
                        authorizedBuilders = p?.authorizedBuilders ?: emptyList(),
                        uploadingPhoto = state.uploadingDoc == "photo",
                        onPhotoClick = { photoPicker.launch("image/*") },
                    )

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
                            // Was styled as a button but had no click handler, so the
                            // only way to edit was the slab at the bottom of the page.
                            // Wiring it here lets that slab go.
                            Text(
                                "Edit", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Teal.copy(alpha = 0.10f))
                                    .clickable { showEdit = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // InfoRow draws nothing for a null value, so a CP who has
                        // filled in none of these got a card with a heading and an
                        // empty body. Say what's missing and where to fix it.
                        val hasDetails = !cp?.city.isNullOrBlank() || !cp?.reraNumber.isNullOrBlank() ||
                            !p?.email.isNullOrBlank() || !cp?.bio.isNullOrBlank()
                        if (!hasDetails) {
                            Text(
                                "Nothing added yet. Tap Edit to add your city and a line about what you sell.",
                                color = TextSecondary, fontSize = 12.sp,
                            )
                        } else {
                            InfoRow("City", cp?.city)
                            InfoRow("RERA number", cp?.reraNumber)
                            InfoRow("Email", p?.email)
                            if (!cp?.bio.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(cp!!.bio!!, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    DealioCard {
                        SectionLabel("Security")
                        Spacer(Modifier.height(10.dp))
                        AppLockToggleRow()
                    }

                    Spacer(Modifier.height(4.dp))
                }

                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                }

                if (showEdit) {
                    EditDialog(
                        fullName = p?.fullName,
                        cp = cp,
                        onDismiss = { showEdit = false },
                    ) { name, city, bio -> vm.save(name, city, bio); showEdit = false }
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
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .subtleShadow(radius = 18.dp)
            .clip(shape)
            .background(Color.White, shape)
            .border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .padding(16.dp),
    ) {
        // Label above value: the figure is what you scan for, so it gets the
        // last word rather than being read past on the way down.
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
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

/**
 * Edit sheet.
 *
 * Only the three things a partner actually maintains: the name customers see,
 * the city they work, and how they introduce themselves. RERA is issued by a
 * regulator and verified by an admin — it was editable here, which invited a CP
 * to type a number nobody had checked. It still shows on the profile and on the
 * credential; it is just no longer something you can set about yourself.
 *
 * Styled as a plain white sheet on purpose: the credential is the only surface
 * in this flow that gets any shine, and a form competing with it would read as
 * decoration rather than hierarchy.
 */
@Composable
private fun EditDialog(
    fullName: String?,
    cp: CpInfo?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(fullName ?: "") }
    var city by remember { mutableStateOf(cp?.city ?: "") }
    var bio by remember { mutableStateOf(cp?.bio ?: "") }
    val canSave = name.isNotBlank()

    // Three fields plus labels and a validation line is taller than a dialog
    // window on a short screen, and a Dialog does not scroll on its own — the
    // sheet has to bound itself and scroll inside, or it renders off-screen.
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                SectionLabel("Edit profile")
                Spacer(Modifier.height(4.dp))
                Text(
                    "This is what customers and builders see.",
                    color = TextSecondary, fontSize = 12.sp,
                )

                Spacer(Modifier.height(18.dp))
                FieldLabel("Name")
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Your full name", color = TextSecondary.copy(alpha = 0.6f)) },
                    isError = name.isBlank(),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                if (name.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Add a name so customers know who they're dealing with.", color = ErrorRed, fontSize = 11.sp)
                }

                Spacer(Modifier.height(14.dp))
                FieldLabel("City")
                OutlinedTextField(
                    value = city, onValueChange = { city = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Where you work", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.height(14.dp))
                FieldLabel("Bio")
                OutlinedTextField(
                    value = bio, onValueChange = { bio = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    placeholder = { Text("A line about what you sell", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, city, bio) },
                        enabled = canSave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, disabledContainerColor = ButtonDisabled),
                    ) { Text("Save changes", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
