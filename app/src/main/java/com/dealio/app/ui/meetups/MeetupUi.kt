package com.dealio.app.ui.meetups

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Time ────────────────────────────────────────────────────────────────────

/**
 * Parses the stored `date` + `time` pair.
 *
 * The pair is what every client displays, and the time half is free text as the
 * organiser picked it ("04:00 PM"), so this tolerates both 12- and 24-hour
 * shapes and falls back to midnight rather than failing the whole row.
 */
fun meetupDateTime(date: String, time: String): LocalDateTime? {
    val d = runCatching { LocalDate.parse(date.take(10)) }.getOrNull() ?: return null
    return LocalDateTime.of(d, parseTime(time) ?: LocalTime.MIDNIGHT)
}

private fun parseTime(raw: String): LocalTime? {
    val s = raw.trim().uppercase(Locale.US)
    if (s.isBlank()) return null
    for (pattern in listOf("hh:mm a", "h:mm a", "HH:mm")) {
        val parsed = runCatching {
            LocalTime.parse(s, DateTimeFormatter.ofPattern(pattern, Locale.US))
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

/** "Today", "Tomorrow", else "Fri, 8 Aug" — how a person says a near date. */
fun meetupDayLabel(date: String): String {
    val d = runCatching { LocalDate.parse(date.take(10)) }.getOrNull() ?: return date
    return when (d) {
        LocalDate.now() -> "Today"
        LocalDate.now().plusDays(1) -> "Tomorrow"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US))
    }
}

/** The one-line "when": "Tomorrow · 04:00 PM". */
fun meetupWhen(date: String, time: String): String =
    listOf(meetupDayLabel(date), time.trim()).filter { it.isNotBlank() }.joinToString(" · ")

fun isPastMeetup(date: String): Boolean =
    runCatching { LocalDate.parse(date.take(10)).isBefore(LocalDate.now()) }.getOrDefault(false)

/**
 * Hands the meetup to whatever calendar the phone uses.
 *
 * An `ACTION_INSERT` rather than a direct write: it needs no calendar
 * permission, and it lets the user pick the account and confirm — which is what
 * someone expects when an app puts something in their diary. Two hours is
 * assumed, since a meetup carries no end time yet.
 */
fun addMeetupToCalendar(
    ctx: Context,
    title: String,
    location: String,
    description: String?,
    date: String,
    time: String,
) {
    val start = meetupDateTime(date, time) ?: return
    val beginMs = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        if (!description.isNullOrBlank()) putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMs)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, beginMs + 2 * 60 * 60 * 1000)
    }
    runCatching { ctx.startActivity(intent) }
}

// ─── Pieces ──────────────────────────────────────────────────────────────────

/**
 * The banner at the top of an event.
 *
 * Stands in for the photograph a Meetup event would carry. The category tint
 * does the work a cover image would: it says what kind of thing this is before
 * you have read a word.
 */
@Composable
fun MeetupHero(
    category: MeetupCategory,
    modifier: Modifier = Modifier,
    height: Int = 116,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier.fillMaxWidth().height(height.dp).background(category.gradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            category.icon, null,
            tint = Color.White.copy(alpha = 0.30f),
            modifier = Modifier.size((height * 0.55f).dp),
        )
        content()
    }
}

@Composable
fun MeetupDetailLine(icon: ImageVector, text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(15.dp).padding(top = 1.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            text, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp,
            maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A category badge — icon plus name, in the category's own colour. */
@Composable
fun CategoryChip(category: MeetupCategory, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(category.tint.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(category.icon, null, tint = category.tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(category.label, color = category.tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun RsvpPill(rsvp: Rsvp, modifier: Modifier = Modifier) {
    Text(
        rsvp.label,
        color = rsvp.tint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(7.dp))
            .background(rsvp.tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Who can see this — the one control a partner most needs to be sure about. */
@Composable
fun VisibilityChip(isPublic: Boolean, city: String?, modifier: Modifier = Modifier) {
    val tint = if (isPublic) IconGreen else TextSecondary
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isPublic) Icons.Outlined.Public else Icons.Outlined.Lock, null,
            tint = tint, modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            if (isPublic) "Open to ${city?.takeIf { it.isNotBlank() } ?: "your city"}" else "Invite only",
            color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

/**
 * "12 going · 3 maybe · 5 no reply".
 *
 * Zero-count parts are dropped rather than shown as "0 maybe" — the line exists
 * to be read at a glance, and padding it with nothing to report makes it slower
 * to read, not more informative.
 */
@Composable
fun RsvpSummary(
    going: Int,
    maybe: Int,
    noReply: Int,
    modifier: Modifier = Modifier,
    fontSize: Int = 12,
) {
    val parts = buildList {
        if (going > 0) add(Rsvp.GOING to "$going going")
        if (maybe > 0) add(Rsvp.MAYBE to "$maybe maybe")
        if (noReply > 0) add(Rsvp.INVITED to "$noReply no reply")
    }
    if (parts.isEmpty()) {
        Text("Nobody invited yet", color = TextSecondary, fontSize = fontSize.sp, modifier = modifier)
        return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { i, (rsvp, label) ->
            if (i > 0) Text(" · ", color = TextSecondary, fontSize = fontSize.sp)
            Box(Modifier.size(6.dp).clip(CircleShape).background(rsvp.tint))
            Spacer(Modifier.width(5.dp))
            Text(label, color = TextPrimary, fontSize = fontSize.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** A wrapping set of single-choice chips. Wraps rather than scrolls so nothing hides. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
    tint: (T) -> Color = { Teal },
) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val on = opt == selected
            val c = tint(opt)
            Text(
                label(opt),
                color = if (on) Color.White else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c else Color.White)
                    .border(1.dp, if (on) c else CardBorder, RoundedCornerShape(10.dp))
                    .clickable { onPick(opt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/** The times a partner actually picks. Half-hours would be a longer list, not a better one. */
val meetupTimes = listOf(
    "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
    "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM",
    "06:00 PM", "07:00 PM", "08:00 PM",
)
