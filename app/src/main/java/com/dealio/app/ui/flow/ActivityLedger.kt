package com.dealio.app.ui.flow

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
import com.dealio.app.data.api.DealEvent
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * What has happened on this deal, newest first.
 *
 * Every row is dotted in the colour of whoever acted, so a glance answers "is
 * this deal moving, and who is moving it?" — the question the ledger exists for.
 * Summaries are phrased server-side at insert time; nothing is re-derived here.
 */
@Composable
fun ActivityLedger(
    events: List<DealEvent>,
    modifier: Modifier = Modifier,
    max: Int = 6,
) {
    if (events.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Text(
            "ACTIVITY",
            color = TextSecondary, fontSize = 9.5.sp,
            fontWeight = FontWeight.Black, letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(10.dp))
        events.take(max).forEachIndexed { index, e ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = if (index == events.take(max).lastIndex) 0.dp else 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(7.dp).background(dotColor(e.actorRole), CircleShape))
                    // A connector to the next entry, so the column reads as one thread.
                    if (index != events.take(max).lastIndex) {
                        Box(Modifier.width(1.dp).height(20.dp).background(CardBorder))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.summary, color = TextPrimary, fontSize = 12.5.sp, lineHeight = 16.sp)
                    if (e.createdAt.isNotBlank()) {
                        Text(relativeDay(e.createdAt), color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun dotColor(actorRole: String): Color = when (actorRole.lowercase()) {
    "builder" -> roleColor(DealRole.BUILDER)
    "cp" -> roleColor(DealRole.CP)
    "customer" -> roleColor(DealRole.CUSTOMER)
    else -> CardBorder
}

/**
 * "today" / "3d" from an ISO timestamp.
 *
 * Deliberately string-sliced rather than parsed: the ledger only needs day
 * granularity, and pulling in a date library for a subtitle is not worth it.
 */
private fun relativeDay(iso: String): String {
    val date = iso.take(10)
    if (date.length != 10) return ""
    return date
}
