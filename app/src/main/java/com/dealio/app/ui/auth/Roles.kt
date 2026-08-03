package com.dealio.app.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.max
import kotlin.math.min
import android.graphics.Color as AndroidColor

/**
 * An account type, with everything the auth screens need to render it.
 *
 * Labels and colors mirror the web app's `roleLabels` / `roleColors`
 * (`stores/useAuthStore.ts`) so a Builder is the same teal on both platforms.
 */
data class DealioRole(
    /** Wire value — the backend expects the role uppercased. */
    val value: String,
    val label: String,
    /** One-word form, for the tight sign-in pills. */
    val shortLabel: String,
    val tagline: String,
    val color: Color,
    val icon: ImageVector,
)

val RoleCustomer = DealioRole(
    value = "CUSTOMER",
    label = "Customer",
    shortLabel = "Customer",
    tagline = "Monitor your property journey",
    color = Color(0xFF16A34A),
    icon = Icons.Outlined.Person,
)

val RoleCp = DealioRole(
    value = "CP",
    label = "Channel Partner",
    shortLabel = "Partner",
    tagline = "Track pipeline & commissions",
    color = Color(0xFFE87722),
    icon = Icons.Outlined.Handshake,
)

val RoleBuilder = DealioRole(
    value = "BUILDER",
    label = "Builder",
    shortLabel = "Builder",
    tagline = "Manage inventory, RERA & leads",
    color = Color(0xFF0A7E8C),
    icon = Icons.Outlined.Apartment,
)

val RoleBank = DealioRole(
    value = "BANK",
    label = "Bank Officer",
    shortLabel = "Bank",
    tagline = "Process loan cases faster",
    color = Color(0xFF2E5D8E),
    icon = Icons.Outlined.AccountBalance,
)

val RoleNri = DealioRole(
    value = "NRI",
    label = "NRI Buyer",
    shortLabel = "NRI",
    tagline = "Invest & manage remotely",
    color = Color(0xFFF5A623),
    icon = Icons.Outlined.Public,
)

val RoleAdmin = DealioRole(
    value = "ADMIN",
    label = "Admin",
    shortLabel = "Admin",
    tagline = "Platform administration",
    color = Color(0xFF6B3FA0),
    icon = Icons.Outlined.AdminPanelSettings,
)

/** Roles that can open an account from the app — admins are provisioned, not signed up. */
val SignupRoles = listOf(RoleCustomer, RoleCp, RoleBuilder, RoleBank, RoleNri)

/** Roles the sign-in picker can express — a superset of [SignupRoles]. */
val SigninRoles = SignupRoles + RoleAdmin

/**
 * Look up a role by its wire value. Null for the backend roles that have no
 * picker entry (VENDOR, LANDOWNER, REFERRAL) — callers must not block on those.
 */
fun roleFor(value: String?): DealioRole? {
    val wire = value?.trim()?.uppercase() ?: return null
    return SigninRoles.firstOrNull { it.value == wire }
}

/**
 * Role colors are picked to read on white cards, so the darker ones (bank blue,
 * builder teal) go muddy on the navy hero.
 *
 * Raise the value and ease off the saturation rather than blending toward white:
 * a straight blend lightens but also greys, which turned the bank's navy-blue
 * into something that read as disabled. This keeps the hue intact.
 */
fun Color.onNavy(): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    hsv[1] = min(hsv[1], 0.62f)
    hsv[2] = max(hsv[2], 0.88f)
    return Color(AndroidColor.HSVToColor(hsv))
}
