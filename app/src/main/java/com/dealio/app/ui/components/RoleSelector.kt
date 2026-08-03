package com.dealio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.auth.DealioRole
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.FieldFill
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.TextSecondary

/**
 * Compact horizontally-scrolling role pills, for where the picker is a filter on
 * an otherwise short form (sign in). The selected pill fills with the role's
 * color so the choice is unmissable next to five muted neighbours.
 */
@Composable
fun RolePillRow(
    roles: List<DealioRole>,
    selected: DealioRole,
    onSelect: (DealioRole) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        roles.forEach { role ->
            RolePill(
                role = role,
                selected = role.value == selected.value,
                onClick = { onSelect(role) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun RolePill(
    role: DealioRole,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val container by animateColorAsState(
        if (selected) role.color else FieldFill,
        label = "pillContainer",
    )
    val content by animateColorAsState(
        if (selected) Color.White else TextSecondary,
        label = "pillContent",
    )
    // A hair of lift on the active pill — enough to separate it from the row
    // without the chips looking like they're floating away.
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, label = "pillScale")
    val elevation by animateDpAsState(if (selected) 6.dp else 0.dp, label = "pillElevation")

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(50),
        color = container,
        border = if (selected) null else BorderStroke(1.dp, CardBorder),
        shadowElevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.55f)
                .padding(start = 12.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                role.icon,
                contentDescription = null,
                tint = if (selected) Color.White else role.color,
                modifier = Modifier.size(16.dp),
            )
            Text(
                role.shortLabel,
                color = content,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

/**
 * Full-width role rows with a one-line description of what each account gets.
 * Used at sign-up, where picking the wrong type is expensive to undo and the
 * extra height buys real clarity.
 */
@Composable
fun RoleCardList(
    roles: List<DealioRole>,
    selected: DealioRole,
    onSelect: (DealioRole) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        roles.forEach { role ->
            RoleCard(
                role = role,
                selected = role.value == selected.value,
                onClick = { onSelect(role) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun RoleCard(
    role: DealioRole,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val container by animateColorAsState(
        if (selected) role.color.copy(alpha = 0.07f) else FieldFill,
        label = "cardContainer",
    )
    val outline by animateColorAsState(
        if (selected) role.color.copy(alpha = 0.55f) else CardBorder,
        label = "cardOutline",
    )
    val iconTile by animateColorAsState(
        if (selected) role.color else role.color.copy(alpha = 0.12f),
        label = "cardIconTile",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, outline),
    ) {
        Row(
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.55f)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(38.dp).background(iconTile, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    role.icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else role.color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    role.label,
                    color = Navy,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    role.tagline,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                )
            }
            SelectionDot(selected = selected, color = role.color)
        }
    }
}

/** Radio-style affordance: an empty ring until picked, then a filled tick. */
@Composable
private fun SelectionDot(selected: Boolean, color: Color) {
    val fill by animateColorAsState(
        if (selected) color else Color.Transparent,
        label = "dotFill",
    )
    val ring by animateColorAsState(
        if (selected) color else CardBorder,
        label = "dotRing",
    )
    Box(
        Modifier
            .size(21.dp)
            .background(fill, CircleShape)
            .border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/** Small tinted pill naming the active role — sits in the auth hero. */
@Composable
fun RoleHeroChip(role: DealioRole, accentOnDark: Color) {
    val tint by animateColorAsState(accentOnDark, label = "heroChipTint")
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(7.dp).background(tint, CircleShape))
            Text(
                role.shortLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Two-segment progress track showing details → verify. */
@Composable
fun StepTrack(step: Int, totalSteps: Int, accentOnDark: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { index ->
            val done = index < step
            val fill by animateColorAsState(
                if (done) accentOnDark else Color.White.copy(alpha = 0.18f),
                label = "stepFill",
            )
            val width by animateDpAsState(if (done) 28.dp else 16.dp, label = "stepWidth")
            Box(
                Modifier
                    .width(width)
                    .height(4.dp)
                    .background(fill, RoundedCornerShape(50)),
            )
        }
    }
}
