package com.dealio.app.ui.customer.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.dealio.app.ui.builder.pipeline.stageLabel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.TextSecondary

/**
 * The buyer-facing view of a deal's progress.
 *
 * The pipeline behind a deal has ten stages ("Profile Created", "Pending
 * Booking", …) which are the sales team's language, not the buyer's. These are
 * the five phases a customer actually experiences; several internal stages fold
 * into each.
 */
enum class JourneyPhase(val label: String, val nextStep: String) {
    ENQUIRY("Enquiry", "Book a site visit to move forward"),
    VISIT("Visit", "Share your feedback after the visit"),
    DEAL("Deal", "Agree pricing and paperwork"),
    BOOKING("Booking", "Complete your booking payment"),
    HANDOVER("Handover", "Registration and possession"),
}

/** Which phase a raw deal status belongs to. Unknown values fall back to Enquiry. */
fun phaseFor(dealStatus: String): JourneyPhase = when (stageLabel(dealStatus.trim())) {
    "New Lead", "Profile Created" -> JourneyPhase.ENQUIRY
    "Meeting Requested", "Meeting Confirmed", "Meeting Done" -> JourneyPhase.VISIT
    "Negotiation", "Agreement" -> JourneyPhase.DEAL
    "Pending Booking", "Booked" -> JourneyPhase.BOOKING
    "Closed" -> JourneyPhase.HANDOVER
    else -> JourneyPhase.ENQUIRY
}

/**
 * Five-step progress track: filled behind the current phase, hollow ahead of it.
 * Labels sit under each node so the buyer can see the whole path, not just where
 * they are.
 */
@Composable
fun JourneyTrack(current: JourneyPhase, modifier: Modifier = Modifier) {
    val phases = JourneyPhase.entries
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        phases.forEachIndexed { index, phase ->
            val done = index < current.ordinal
            val active = index == current.ordinal
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
                    // Connectors run behind the node, clipped to this cell's half-widths
                    // so the first and last nodes don't sprout stubs.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Connector(filled = done || active, visible = index > 0)
                        Spacer(Modifier.size(18.dp))
                        Connector(filled = done, visible = index < phases.lastIndex)
                    }
                    Node(done = done, active = active)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    phase.label,
                    color = if (done || active) CustomerAccent else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Connector(filled: Boolean, visible: Boolean) {
    Box(
        Modifier
            .weight(1f)
            .height(2.dp)
            .background(
                when {
                    !visible -> Color.Transparent
                    filled -> CustomerAccent.copy(alpha = 0.45f)
                    else -> CardBorder
                },
                RoundedCornerShape(50),
            ),
    )
}

@Composable
private fun Node(done: Boolean, active: Boolean) {
    when {
        done -> Box(
            Modifier.size(18.dp).background(CustomerAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }

        active -> Box(
            Modifier.size(18.dp).background(CustomerAccent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(9.dp).background(CustomerAccent, CircleShape)) }

        else -> Box(Modifier.size(11.dp).background(CardBorder, CircleShape))
    }
}

/** "Up next" hint under the track — tells the buyer what actually moves things along. */
@Composable
fun NextStepRow(current: JourneyPhase) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CustomerAccent.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("UP NEXT", color = CustomerAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        Text(current.nextStep, color = TextSecondary, fontSize = 11.5.sp)
    }
}
