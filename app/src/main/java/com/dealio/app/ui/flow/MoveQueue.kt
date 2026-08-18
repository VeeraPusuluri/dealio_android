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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.components.LocalSurfaceAccent
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * The deals that cannot progress without you.
 *
 * Every home screen opened with metric tiles — true but inert, because a count
 * of leads never says which one needs you today. This is the same baton the deal
 * screens render, gathered into a to-do list and sorted so the ones going stale
 * surface first.
 */

/** One row, already reduced to what the queue needs. */
data class MoveItem(
    val dealId: Long,
    val title: String,
    val subtitle: String,
    val rawStatus: String,
    val cpAgreed: Boolean = false,
    val customerConfirmed: Boolean = false,
    /** Days since the deal last moved; null when unknown. */
    val idleDays: Int? = null,
)

/**
 * Keep only the deals whose baton is on [viewer], stalled first, then oldest.
 *
 * Sorting stalled-first is what makes the queue double as the pipeline-hygiene
 * tool the platform lacked: the deal nobody has touched for a fortnight is the
 * one most likely to be lost, and it was previously indistinguishable from a
 * lead created this morning.
 */
fun movesFor(viewer: DealRole, items: List<MoveItem>): List<MoveItem> =
    items.filter { batonOf(it.rawStatus, it.cpAgreed, it.customerConfirmed).heldBy(viewer) }
        .sortedByDescending { it.idleDays ?: 0 }

@Composable
fun MoveQueue(
    viewer: DealRole,
    items: List<MoveItem>,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    max: Int = 4,
    /** Buyers get reassurance when the queue is empty; the trades get nothing. */
    emptyMessage: String? = null,
) {
    val moves = movesFor(viewer, items)
    if (moves.isEmpty() && emptyMessage == null) return

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (moves.isEmpty()) "Nothing needs you" else "Your move",
                color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            )
            if (moves.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.background(LocalSurfaceAccent.current, RoundedCornerShape(11.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("${moves.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            if (moves.isEmpty()) emptyMessage ?: ""
            else "Deals that cannot progress without you",
            color = TextSecondary, fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        moves.take(max).forEach { item ->
            val baton = batonOf(item.rawStatus, item.cpAgreed, item.customerConfirmed)
            val stalled = (item.idleDays ?: 0) >= STALLED_AFTER_DAYS
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(
                        if (stalled) 1.6.dp else 1.dp,
                        if (stalled) StalledAmber else CardBorder,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onOpen(item.dealId) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(30.dp).background(roleColor(viewer).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.title.trim().take(1).uppercase().ifBlank { "?" },
                        color = roleColor(viewer), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(1.dp))
                    Text(
                        // The action, not the stage — the queue is a to-do list.
                        baton.action,
                        color = if (stalled) StalledAmber else LocalSurfaceAccent.current,
                        fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                    Text(item.subtitle, color = TextSecondary, fontSize = 10.5.sp, maxLines = 1)
                }
                if (item.idleDays != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (item.idleDays <= 0) "today" else "${item.idleDays}d",
                        color = if (stalled) StalledAmber else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (stalled) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private val StalledAmber = Color(0xFFD97706)

/**
 * Whole days between an ISO-8601 timestamp and now, or null if unparseable.
 *
 * Only the date part is used — the queue sorts by staleness in days, and a deal
 * touched this morning versus last night is the same answer.
 */
fun idleDaysSince(iso: String?): Int? {
    val date = iso?.take(10) ?: return null
    return runCatching {
        val then = java.time.LocalDate.parse(date)
        java.time.temporal.ChronoUnit.DAYS.between(then, java.time.LocalDate.now()).toInt()
    }.getOrNull()
}
