package com.dealio.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealio.app.SIGNUP_ENABLED
import com.dealio.app.ui.auth.AuthStep
import com.dealio.app.ui.auth.AuthViewModel
import com.dealio.app.ui.auth.DealioRole
import com.dealio.app.ui.auth.RoleCustomer
import com.dealio.app.ui.auth.SigninRoles
import com.dealio.app.ui.auth.isSignInOption
import com.dealio.app.ui.auth.roleFor
import com.dealio.app.ui.components.AuthScaffold
import com.dealio.app.ui.components.DealioButton
import com.dealio.app.ui.components.DemoCodeHint
import com.dealio.app.ui.components.ErrorText
import com.dealio.app.ui.components.FieldGroupLabel
import com.dealio.app.ui.components.heroAccentFor
import com.dealio.app.ui.components.OtpInput
import com.dealio.app.ui.components.PhoneField
import com.dealio.app.ui.components.RoleHeroChip
import com.dealio.app.ui.components.RolePillRow
import com.dealio.app.ui.components.SignInNotice
import com.dealio.app.ui.findActivity
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoToSignup: () -> Unit,
    /** Why the user was sent here, when they did not choose to sign out. */
    notice: String? = null,
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    // Firebase attaches its app verification (Play Integrity, reCAPTCHA fallback)
    // to the hosting Activity, so the OTP send needs it.
    val activity = LocalContext.current.findActivity()

    var role by remember { mutableStateOf(RoleCustomer) }
    var countryCode by remember { mutableStateOf("+91") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedInUser) {
        if (state.loggedInUser != null) onLoggedIn()
    }

    fun send(signingInAs: DealioRole = role, isResend: Boolean = false) {
        viewModel.sendOtp(
            activity = activity,
            isSignup = false,
            phone = phone,
            countryCode = countryCode,
            role = signingInAs.value,
            isResend = isResend,
        )
    }

    val onDetails = state.step == AuthStep.DETAILS
    val accent = role.color
    // The hero glows in the portal's colour, not simply the role's, so the
    // surface someone signs in on is the one they land in. Only the hero: the
    // card's own controls stay on the role's brand colour, which reads on white.
    val accentDark = heroAccentFor(role)

    AuthScaffold(
        eyebrow = if (onDetails) "Sign in" else "Step 2 · Verify",
        headline = if (onDetails) "Welcome back" else "Enter the code",
        subtitle = if (onDetails) "Pick your account type, then sign in with your phone number."
        else "We sent a 6-digit code to ${state.maskedPhone ?: "your phone"}.",
        accentOnDark = accentDark,
        step = if (onDetails) 1 else 2,
        highlights = if (onDetails) listOf("12,400+ members", "OTP secured") else emptyList(),
        heroTrailing = { RoleHeroChip(role = role, accentOnDark = accentDark) },
    ) {
        if (onDetails) {
            SignInNotice(notice)
            FieldGroupLabel("Sign in as")
            Spacer(Modifier.height(10.dp))
            RolePillRow(
                roles = SigninRoles,
                selected = role,
                onSelect = { role = it; viewModel.clearError() },
                enabled = !state.loading,
            )
            Spacer(Modifier.height(10.dp))
            RoleTagline(role)
            Spacer(Modifier.height(22.dp))

            PhoneField(
                countryCode = countryCode,
                onCountryCodeChange = { countryCode = it; viewModel.clearError() },
                phone = phone,
                onPhoneChange = { phone = it; viewModel.clearError() },
                enabled = !state.loading,
                accent = accent,
            )
            Spacer(Modifier.height(24.dp))
            DealioButton(
                text = "Send code",
                loading = state.loading,
                enabled = phone.length >= 6,
                accent = accent,
                onClick = { send() },
            )
            ErrorText(state.error)

            // The pre-flight knows which role this number really is, so offer the
            // fix rather than leaving the user to guess which pill to try next.
            // Only for roles the picker has: an admin number still gets the
            // "registered as an Admin account" error, but no shortcut in — that
            // would put back the sign-in path the Admin pill was removed to close.
            val suggested = roleFor(state.mismatchedRole)?.takeIf { it.isSignInOption() }
            AnimatedVisibility(visible = suggested != null) {
                if (suggested != null) {
                    Spacer(Modifier.height(12.dp))
                    SwitchRoleAction(
                        role = suggested,
                        onClick = { role = suggested; viewModel.clearError(); send(suggested) },
                    )
                }
            }
        } else {
            OtpInput(
                value = otp,
                onValueChange = { otp = it; viewModel.clearError() },
                enabled = !state.loading,
                accent = accent,
            )
            DemoCodeHint(state.demoCode) { otp = it }
            Spacer(Modifier.height(24.dp))
            DealioButton(
                text = "Verify & sign in",
                loading = state.loading,
                enabled = otp.length == 6,
                accent = accent,
                onClick = { viewModel.verifyLogin(otp = otp) },
            )
            ErrorText(state.error)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { otp = ""; viewModel.backToDetails() }) {
                    Text("Change number", color = TextSecondary)
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

        // Self-serve signup is off for now (see SIGNUP_ENABLED). SignupScreen and
        // its nav route are left intact behind the flag, so restoring the offer
        // is a one-line change rather than rebuilding the flow.
        if (SIGNUP_ENABLED) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New to Dealio?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onGoToSignup) {
                    Text("Create an account", color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** One-line description of the picked role, in that role's color. */
@Composable
private fun RoleTagline(role: DealioRole) {
    val tint by animateColorAsState(role.color, label = "roleTagline")
    Text(
        role.tagline,
        color = tint,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** Inline "you're on the wrong pill" correction, offered after a role mismatch. */
@Composable
private fun SwitchRoleAction(role: DealioRole, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = role.color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, role.color.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                role.icon,
                contentDescription = null,
                tint = role.color,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Continue as ${role.label}",
                color = role.color,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = role.color,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
