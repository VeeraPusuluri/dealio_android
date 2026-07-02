package com.dealio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** A meeting normalised for the calendar. */
data class CalMeeting(
    val id: String,
    val date: LocalDate,
    val time: String?,
    val title: String,
    val subtitle: String?,
    val status: String?,
    val color: Color,
)

/** "2026-07-02" / "2026-07-02T…" → LocalDate (null if unparseable). */
fun calDate(iso: String?): LocalDate? {
    if (iso == null || iso.length < 10) return null
    return runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
}

/** Status → dot/badge colour, shared across roles. */
fun meetingStatusColor(status: String?): Color = when (status?.lowercase()) {
    "confirmed" -> Color(0xFF3B82F6)
    "completed" -> Color(0xFF10B981)
    "rescheduled" -> Color(0xFFF97316)
    "cancelled", "rejected" -> Color(0xFFEF4444)
    "follow-up required", "follow-up" -> Color(0xFF8B5CF6)
    "pending", "requested" -> Color(0xFFF59E0B)
    else -> Teal
}

private val WEEKDAYS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

/**
 * Month calendar of meetings with a per-day dot and a selected-day agenda list.
 * Role-agnostic — pass a list of [CalMeeting].
 */
@Composable
fun MeetingsCalendar(meetings: List<CalMeeting>, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }

    val byDay = remember(meetings) { meetings.groupBy { it.date } }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Month card ──
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Today",
                        color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { month = YearMonth.from(today); selected = today }
                            .background(Teal.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = TextSecondary,
                        modifier = Modifier.padding(start = 6.dp).size(26.dp).clip(CircleShape).clickable { month = month.minusMonths(1) })
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = TextSecondary,
                        modifier = Modifier.size(26.dp).clip(CircleShape).clickable { month = month.plusMonths(1) })
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    WEEKDAYS.forEach { w ->
                        Text(w.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))

                val firstDow = month.atDay(1).dayOfWeek.value % 7  // Sunday = 0
                val len = month.lengthOfMonth()
                val cells = List(firstDow) { null } + (1..len).map { month.atDay(it) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(((cells.size + 6) / 7 * 44).dp),
                    userScrollEnabled = false,
                ) {
                    items(cells.size) { idx ->
                        val d = cells[idx]
                        if (d == null) {
                            Box(Modifier.aspectRatio(1f))
                        } else {
                            DayCell(d, d == selected, d == today, byDay[d].orEmpty()) { selected = d }
                        }
                    }
                }
            }
        }

        // ── Agenda for the selected day ──
        val dayMeetings = byDay[selected].orEmpty().sortedBy { it.time ?: "" }
        Text(
            selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                ", ${selected.dayOfMonth} " + selected.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        )
        if (dayMeetings.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(6.dp))
                Text("Nothing scheduled", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            dayMeetings.forEach { m -> AgendaRow(m) }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, today: Boolean, dots: List<CalMeeting>, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Teal else if (today) Teal.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                color = if (selected) Color.White else if (today) Teal else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected || today) FontWeight.SemiBold else FontWeight.Normal,
            )
            Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                dots.take(3).forEach { m ->
                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.White.copy(alpha = 0.9f) else m.color))
                }
            }
        }
    }
}

@Composable
private fun AgendaRow(m: CalMeeting) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp)) {
            Box(Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(m.color))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    m.time?.let { Text(prettyTime(it), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                }
                m.subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
                m.status?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it, color = m.color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.background(m.color.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun prettyTime(t: String): String {
    val parts = t.split(":")
    if (parts.size < 2) return t
    val h = parts[0].toIntOrNull() ?: return t
    val min = parts[1].take(2)
    val ampm = if (h >= 12) "PM" else "AM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:$min $ampm"
}

/** Small List / Calendar segmented toggle. */
@Composable
fun ListCalendarToggle(calendar: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Navy.copy(alpha = 0.06f))
            .padding(3.dp),
    ) {
        listOf(false to "List", true to "Calendar").forEach { (isCal, label) ->
            val active = isCal == calendar
            Text(
                label,
                color = if (active) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onChange(isCal) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
