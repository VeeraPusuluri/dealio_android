package com.dealio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealio.app.ui.auth.AuthStep
import com.dealio.app.ui.auth.AuthViewModel
import com.dealio.app.ui.auth.DealioRole
import com.dealio.app.ui.auth.RoleCustomer
import com.dealio.app.ui.auth.SignupRoles
import com.dealio.app.ui.auth.onNavy
import com.dealio.app.ui.components.AuthScaffold
import com.dealio.app.ui.components.DealioButton
import com.dealio.app.ui.components.DemoCodeHint
import com.dealio.app.ui.components.ErrorText
import com.dealio.app.ui.components.FieldGroupLabel
import com.dealio.app.ui.components.OtpInput
import com.dealio.app.ui.components.PhoneField
import com.dealio.app.ui.components.RoleCardList
import com.dealio.app.ui.components.RoleHeroChip
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.findActivity
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun SignupScreen(
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    // Firebase attaches its app verification (Play Integrity, reCAPTCHA fallback)
    // to the hosting Activity, so the OTP send needs it.
    val activity = LocalContext.current.findActivity()

    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(RoleCustomer) }
    var countryCode by remember { mutableStateOf("+91") }
    var phone by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var referralOpen by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedInUser) {
        if (state.loggedInUser != null) onSignedUp()
    }

    fun send(isResend: Boolean = false) {
        viewModel.sendOtp(
            activity = activity,
            isSignup = true,
            phone = phone,
            countryCode = countryCode,
            fullName = fullName,
            role = role.value,
            referralCode = referralCode,
            isResend = isResend,
        )
    }

    val onDetails = state.step == AuthStep.DETAILS
    val accent = role.color
    val accentDark = accent.onNavy()

    AuthScaffold(
        eyebrow = if (onDetails) "Create account" else "Step 2 · Verify",
        headline = if (onDetails) "Join Dealio" else "Verify your phone",
        subtitle = if (onDetails) "Choose how you'll use Dealio — free forever, for every role."
        else "We sent a 6-digit code to ${state.maskedPhone ?: "your phone"}.",
        accentOnDark = accentDark,
        step = if (onDetails) 1 else 2,
        highlights = if (onDetails) listOf("Free forever", "RERA-ready") else emptyList(),
        heroTrailing = { RoleHeroChip(role = role, accentOnDark = accentDark) },
    ) {
        if (onDetails) {
            FieldGroupLabel("I am a…")
            Spacer(Modifier.height(10.dp))
            RoleCardList(
                roles = SignupRoles,
                selected = role,
                onSelect = { role = it; viewModel.clearError() },
                enabled = !state.loading,
            )
            Spacer(Modifier.height(24.dp))

            FieldGroupLabel("Your details")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; viewModel.clearError() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
                singleLine = true,
                label = { Text("Full name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(14.dp),
                colors = dealioFieldColors(accent),
            )
            Spacer(Modifier.height(14.dp))
            PhoneField(
                countryCode = countryCode,
                onCountryCodeChange = { countryCode = it; viewModel.clearError() },
                phone = phone,
                onPhoneChange = { phone = it; viewModel.clearError() },
                enabled = !state.loading,
                accent = accent,
            )

            // Most sign-ups have no referral code — keep it out of the way until asked for.
            if (referralOpen) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it.uppercase(); viewModel.clearError() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                    singleLine = true,
                    label = { Text("Referral code") },
                    placeholder = { Text("CP-JOHN-42") },
                    shape = RoundedCornerShape(14.dp),
                    colors = dealioFieldColors(accent),
                )
            } else {
                TextButton(
                    onClick = { referralOpen = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Have a referral code?",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            DealioButton(
                text = "Create my account",
                loading = state.loading,
                enabled = phone.length >= 6 && fullName.isNotBlank(),
                accent = accent,
                onClick = { send() },
            )
            ErrorText(state.error)
        } else {
            // The picker is off-screen now, so restate what's being created.
            SignupSummary(role = role, name = fullName)
            Spacer(Modifier.height(18.dp))
            OtpInput(
                value = otp,
                onValueChange = { otp = it; viewModel.clearError() },
                enabled = !state.loading,
                accent = accent,
            )
            DemoCodeHint(state.demoCode) { otp = it }
            Spacer(Modifier.height(24.dp))
            DealioButton(
                text = "Verify & create account",
                loading = state.loading,
                enabled = otp.length == 6,
                accent = accent,
                onClick = { viewModel.verifySignup(otp = otp) },
            )
            ErrorText(state.error)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { otp = ""; viewModel.backToDetails() }) {
                    Text("Edit details", color = TextSecondary)
                }
                if (state.resendSecondsLeft > 0) {
                    Text(
                        "Resend in ${state.resendSecondsLeft}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                } else {
                    TextButton(onClick = { otp = ""; send(isResend = true) }) {
                        Text("Resend code", color = accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Already have an account?",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onGoToLogin) {
                Text("Sign in", color = Teal, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Recap of the account about to be created, shown on the verify step. */
@Composable
private fun SignupSummary(role: DealioRole, name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = role.color.copy(alpha = 0.07f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = role.color,
            ) {
                Icon(
                    role.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "CREATING A ${role.label.uppercase()} ACCOUNT",
                    color = role.color,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    name.ifBlank { "Your account" },
                    color = Navy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
