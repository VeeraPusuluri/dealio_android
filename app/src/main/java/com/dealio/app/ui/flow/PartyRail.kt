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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * The other parties on a deal, and the thread you have with each.
 *
 * A deal carries four threads — one private pair per couple, plus a three-way
 * group — and the backend has always supported them. The apps never did: each
 * screen had a single composer wired to whichever counterparty the backend
 * defaulted to, so a CP could talk to the builder and to nobody else.
 *
 * Selecting a party here switches both the transcript and where the composer
 * sends. Which parties appear is decided by [rosterFor]; the backend is still
 * the authority and only returns threads the caller may read.
 */

/** A selectable thread: the counterparty, or the three-way group. */
data class ThreadTarget(
    /** What the send endpoints call this: "builder" | "cp" | "customer" | "group". */
    val recipientRole: String,
    val label: String,
    val initials: String,
    val color: Color,
    val isGroup: Boolean = false,
)

/**
 * The threads [viewer] can choose between on this deal.
 *
 * Mirrors the backend's visibleThreadKeys: the group only exists once a CP is
 * attached, and pre-booking the private builder<->customer pair is withheld so
 * the CP cannot be cut out — that conversation goes to the group instead.
 */
fun rosterFor(
    viewer: DealRole,
    hasCp: Boolean,
    rawStatus: String?,
    builderName: String? = null,
    cpName: String? = null,
    customerName: String? = null,
): List<ThreadTarget> {
    val escrowed = hasCp && stageIndex(rawStatus) < stageIndex("Booked")
    val builder = ThreadTarget("builder", builderName ?: "Builder", initialsOf(builderName ?: "Builder"), roleColor(DealRole.BUILDER))
    val cp = ThreadTarget("cp", cpName ?: "Advisor", initialsOf(cpName ?: "Advisor"), roleColor(DealRole.CP))
    val customer = ThreadTarget("customer", customerName ?: "Customer", initialsOf(customerName ?: "Customer"), roleColor(DealRole.CUSTOMER))
    val group = ThreadTarget("group", "All three", "3", Navy, isGroup = true)

    val out = mutableListOf<ThreadTarget>()
    when (viewer) {
        DealRole.CP -> { out += builder; out += customer }
        DealRole.BUILDER -> { if (hasCp) out += cp; if (!escrowed) out += customer }
        DealRole.CUSTOMER -> { if (hasCp) out += cp; if (!escrowed) out += builder }
    }
    if (hasCp) out += group
    return out
}

/** The threadKey a target maps to, for filtering the transcript. */
fun threadKeyFor(viewer: DealRole, target: ThreadTarget): String {
    if (target.isGroup) return "group"
    val a = when (viewer) {
        DealRole.BUILDER -> "builder"; DealRole.CP -> "cp"; DealRole.CUSTOMER -> "customer"
    }
    return listOf(a, target.recipientRole).sorted().joinToString("-")
}

private fun initialsOf(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

@Composable
fun PartyRail(
    targets: List<ThreadTarget>,
    selected: ThreadTarget?,
    onSelect: (ThreadTarget) -> Unit,
    modifier: Modifier = Modifier,
    unreadOf: (ThreadTarget) -> Int = { 0 },
) {
    if (targets.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Text(
            "ON THIS DEAL",
            color = TextSecondary, fontSize = 9.5.sp,
            fontWeight = FontWeight.Black, letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            targets.forEach { t ->
                val isSelected = t.recipientRole == selected?.recipientRole
                Column(
                    Modifier
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(11.dp))
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) t.color else CardBorder,
                            RoundedCornerShape(11.dp),
                        )
                        .clickable { onSelect(t) }
                        .padding(vertical = 9.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(26.dp).background(
                            if (t.isGroup) t.color.copy(alpha = 0.12f) else t.color,
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t.initials,
                            color = if (t.isGroup) t.color else Color.White,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        t.label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    val unread = unreadOf(t)
                    if (unread > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text("$unread new", color = t.color, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
