package com.dealio.app.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * The header block every deal screen opens with, for all three roles.
 *
 * Only three things vary by role: whether the exact stage is shown under the
 * track, which register the labels are written in, and what the baton's action
 * button does. The shape is identical everywhere, which is what makes the three
 * portals read as one product.
 */

/** Role colour identifies people, never actions — CTAs keep the app accent. */
fun roleColor(role: DealRole): Color = when (role) {
    DealRole.BUILDER -> Navy
    DealRole.CP -> Teal
    DealRole.CUSTOMER -> Orange
}

// ─── PhaseTrack ──────────────────────────────────────────────────────────────

/**
 * Five-phase progress track: filled behind the current phase, hollow ahead.
 *
 * [exactStage] is shown beneath the track for builder and CP, who work the
 * pipeline in its own vocabulary. Pass null for a buyer — they must never see it.
 */
@Composable
fun PhaseTrack(
    current: JourneyPhase,
    modifier: Modifier = Modifier,
    accent: Color = Teal,
    exactStage: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            JourneyPhase.entries.forEachIndexed { index, phase ->
                val done = index < current.ordinal
                val active = index == current.ordinal
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
                        // Connectors run behind the node, clipped to this cell's
                        // half-widths so the end nodes don't sprout stubs.
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Connector(filled = done || active, visible = index > 0, accent = accent)
                            Spacer(Modifier.size(18.dp))
                            Connector(filled = done, visible = index < JourneyPhase.entries.lastIndex, accent = accent)
                        }
                        Node(done = done, active = active, accent = accent)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        phase.label,
                        color = if (done || active) accent else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        if (exactStage != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Stage · $exactStage",
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Connector(
    filled: Boolean,
    visible: Boolean,
    accent: Color,
) {
    Box(
        Modifier.weight(1f).height(2.dp).background(
            when {
                !visible -> Color.Transparent
                filled -> accent.copy(alpha = 0.45f)
                else -> CardBorder
            },
            RoundedCornerShape(50),
        ),
    )
}

@Composable
private fun Node(done: Boolean, active: Boolean, accent: Color) {
    when {
        done -> Box(
            Modifier.size(18.dp).background(accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }

        active -> Box(
            Modifier.size(18.dp).background(accent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(9.dp).background(accent, CircleShape)) }

        else -> Box(Modifier.size(11.dp).background(CardBorder, CircleShape))
    }
}

// ─── BatonCard ───────────────────────────────────────────────────────────────

/**
 * The answer to "is this waiting on me?".
 *
 * Owns the screen's single primary action, so no deal screen ever presents two
 * competing main CTAs. When the viewer does not hold the baton it shows who does
 * and offers a nudge instead.
 *
 * @param viewer the role looking at the screen
 * @param onAction invoked when the holder taps the primary CTA; null hides it
 * @param onNudge  invoked when a waiting party nudges the holder; null hides it
 * @param waitingDays how long the current holder has had it, for stalled styling
 */
@Composable
fun BatonCard(
    baton: Baton,
    viewer: DealRole,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onNudge: (() -> Unit)? = null,
    waitingDays: Int? = null,
    buyerRegister: Boolean = false,
) {
    val yours = baton.heldBy(viewer)
    val stalled = !yours && (waitingDays ?: 0) >= STALLED_AFTER_DAYS
    val accent = when {
        baton.isComplete -> StatusGreen
        yours -> Teal
        stalled -> StatusAmber
        else -> CardBorder
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(if (yours || stalled) 2.dp else 1.dp, accent, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BatonChip(baton, viewer, yours, stalled, buyerRegister)
                Spacer(Modifier.height(8.dp))
                Text(
                    // The buyer never reads the imperative written for the CP or
                    // builder — they read the same wait, phrased for them.
                    if (buyerRegister) baton.buyerCopy else baton.action,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!yours && waitingDays != null && !baton.isComplete) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (waitingDays <= 0) "Since today" else "Since $waitingDays days ago",
                        color = if (stalled) StatusAmber else TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = if (stalled) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            if (yours && onAction != null && !baton.isComplete) {
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                ) {
                    Text(actionLabel ?: "Do it", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            } else if (!yours && onNudge != null && !baton.isComplete) {
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = onNudge, shape = RoundedCornerShape(10.dp)) {
                    Text("Nudge", color = Teal, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BatonChip(
    baton: Baton,
    viewer: DealRole,
    yours: Boolean,
    stalled: Boolean,
    buyerRegister: Boolean,
) {
    val (text, fg, bg) = when {
        baton.isComplete -> Triple("COMPLETE", StatusGreen, StatusGreenBg)
        yours -> Triple("YOUR MOVE", Color.White, Teal)
        buyerRegister -> Triple("NOTHING NEEDED YET", Orange, Orange.copy(alpha = 0.12f))
        else -> {
            val who = baton.holders.joinToString(" and ") { it.label }
            Triple(
                "WAITING ON ${who.uppercase()}",
                if (stalled) StatusAmber else Navy,
                if (stalled) StatusAmberBg else Navy.copy(alpha = 0.07f),
            )
        }
    }
    Box(Modifier.background(bg, RoundedCornerShape(11.dp)).padding(horizontal = 9.dp, vertical = 4.dp)) {
        Text(text, color = fg, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

/** Past this, a waiting deal is stalled. Mirrors the admin surface's STALE_LEAD_DAYS. */
const val STALLED_AFTER_DAYS = 14

private val StatusGreen = Color(0xFF059669)
private val StatusGreenBg = Color(0xFFE8F6F1)
private val StatusAmber = Color(0xFFD97706)
private val StatusAmberBg = Color(0xFFFDF3E7)

// ─── DealSpine ───────────────────────────────────────────────────────────────

/**
 * PhaseTrack + BatonCard, the block that opens every deal screen.
 *
 * @param buyerRegister true for the customer portal — hides the exact stage and
 *        switches every string to the buyer's language.
 */
@Composable
fun DealSpine(
    rawStatus: String?,
    viewer: DealRole,
    modifier: Modifier = Modifier,
    cpAgreed: Boolean = false,
    customerConfirmed: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onNudge: (() -> Unit)? = null,
    waitingDays: Int? = null,
    buyerRegister: Boolean = false,
) {
    val baton = batonOf(rawStatus, cpAgreed, customerConfirmed)
    val accent = if (buyerRegister) Orange else Teal
    Column(modifier.fillMaxWidth()) {
        PhaseTrack(
            current = phaseOf(rawStatus),
            accent = accent,
            // The buyer must never see pipeline vocabulary; the others work in it.
            exactStage = if (buyerRegister) null else canonicalStage(rawStatus),
        )
        Spacer(Modifier.height(14.dp))
        BatonCard(
            baton = baton,
            viewer = viewer,
            actionLabel = actionLabel,
            onAction = onAction,
            onNudge = onNudge,
            waitingDays = waitingDays,
            buyerRegister = buyerRegister,
        )
    }
}
