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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.IconOrange
import com.dealio.app.ui.components.IconPurple
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

/**
 * "Saturday, 8 August 2026" — the date written out.
 *
 * The long form is for the event page, where someone is deciding whether they
 * can make it and wants the day of the week and the year spelled out. Lists keep
 * [meetupDayLabel], which is shorter and says "Tomorrow" when that is the useful
 * thing to know.
 */
fun meetupFullDate(date: String): String {
    val d = runCatching { LocalDate.parse(date.take(10)) }.getOrNull() ?: return date
    return d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US))
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
 * A photograph when the organiser supplied one, and the category wash when they
 * did not. Plenty of meetups will never have a cover — a partner arranging a
 * site visit from a car park has no picture to hand — so the fallback is a
 * designed state rather than an apology: the tint says what kind of thing this
 * is before you have read a word.
 *
 * The scrim only comes out for a photograph. Over the flat wash it would darken
 * a colour that was chosen, and anything laid on top of the gradient already has
 * the contrast it needs.
 */
@Composable
fun MeetupHero(
    category: MeetupCategory,
    modifier: Modifier = Modifier,
    height: Int = 116,
    coverImage: String? = null,
    scrim: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val cover = coverImage?.takeIf { it.isNotBlank() }?.let { resolveUrl(it) }
    Box(
        modifier.fillMaxWidth().height(height.dp).background(category.gradient),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (scrim) {
                // Bottom-weighted, so a title sitting on the photograph stays
                // readable without flattening the top half of the picture.
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.10f),
                            1f to Color.Black.copy(alpha = 0.62f),
                        ),
                    ),
                )
            }
        } else {
            Icon(
                category.icon, null,
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size((height * 0.55f).dp),
            )
        }
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

// ─── Event page pieces ───────────────────────────────────────────────────────
// Shared by the organiser's event page and the customer's, for the same reason
// the vocabulary is: the two describe one gathering, and the moment either owns
// a piece the other needs, it gets imported across the app's main seam.

/** What the gathering is about, as read-only pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicChips(topics: List<String>, modifier: Modifier = Modifier) {
    if (topics.isEmpty()) return
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        topics.forEach { topic ->
            Text(
                topic,
                color = TextPrimary, fontSize = 12.sp, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 13.dp, vertical = 7.dp),
            )
        }
    }
}

/**
 * The row of faces for who is coming.
 *
 * Initials rather than photographs. The server sends first names only — this
 * reaches any stranger browsing a public meetup — and a circle with a letter in
 * it carries the same "these are people, not a number" weight that makes a
 * count worth reading. [extra] is everyone the preview did not name, including
 * the guests they are bringing.
 */
@Composable
fun AvatarStack(names: List<String>, extra: Int, modifier: Modifier = Modifier, size: Int = 34) {
    if (names.isEmpty() && extra <= 0) return
    val tints = listOf(Teal, IconGreen, IconOrange, IconPurple, IconBlue)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        names.forEachIndexed { i, name ->
            val tint = tints[i % tints.size]
            Box(
                Modifier
                    // Overlap after the first, so the row reads as a group.
                    .offset(x = (-9 * i).dp)
                    .size(size.dp).clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1).uppercase(),
                    color = tint, fontSize = (size * 0.38f).sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        if (extra > 0) {
            Box(
                Modifier.offset(x = (-9 * names.size).dp)
                    .size(size.dp).clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+$extra",
                    color = TextSecondary, fontSize = (size * 0.32f).sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Venue photographs.
 *
 * One big frame with the rest stacked beside it, rather than an even grid: the
 * first picture is the one the organiser chose to lead with, and giving every
 * photo equal weight makes a set of four look like a contact sheet. Anything
 * past the third is a count on the last tile, which is also what taps into the
 * viewer at that index.
 */
@Composable
fun MeetupPhotoGrid(photos: List<String>, modifier: Modifier = Modifier, onOpen: (Int) -> Unit = {}) {
    if (photos.isEmpty()) return
    val shape = RoundedCornerShape(14.dp)

    if (photos.size == 1) {
        PhotoTile(photos[0], shape, Modifier.fillMaxWidth().height(180.dp)) { onOpen(0) }
        return
    }
    Row(modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PhotoTile(photos[0], shape, Modifier.weight(1f).fillMaxHeight()) { onOpen(0) }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PhotoTile(photos[1], shape, Modifier.fillMaxWidth().weight(1f)) { onOpen(1) }
            if (photos.size > 2) {
                PhotoTile(
                    photos[2], shape, Modifier.fillMaxWidth().weight(1f),
                    overlayCount = photos.size - 3,
                ) { onOpen(2) }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    url: String,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    overlayCount: Int = 0,
    onClick: () -> Unit,
) {
    Box(modifier.clip(shape).background(CardBorder.copy(alpha = 0.4f)).clickable { onClick() }) {
        AsyncImage(
            model = resolveUrl(url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (overlayCount > 0) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$overlayCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * One photograph, full bleed, dismissed by tapping the backdrop.
 *
 * A viewer rather than nothing behind the grid: the tiles are small and the
 * point of a venue photograph is to be looked at.
 *
 * The paging controls sit at the left and right edges, vertically centred, and
 * the counter at the top. The obvious place for them is a row along the bottom —
 * which is exactly where the system draws the gesture indicator, and it lands on
 * top of them. Edges are also where a thumb already is.
 */
@Composable
fun MeetupPhotoViewer(photos: List<String>, startIndex: Int, onDismiss: () -> Unit) {
    var index by remember(startIndex) { mutableIntStateOf(startIndex.coerceIn(0, photos.lastIndex)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)).clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = resolveUrl(photos[index]),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (photos.size > 1) {
                    Text(
                        "${index + 1} / ${photos.size}",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            if (photos.size > 1) {
                ViewerArrow(
                    Icons.AutoMirrored.Outlined.ArrowBack, "Previous photo", index > 0,
                    Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                ) { index-- }
                ViewerArrow(
                    Icons.AutoMirrored.Outlined.ArrowForward, "Next photo", index < photos.lastIndex,
                    Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                ) { index++ }
            }
        }
    }
}

@Composable
private fun ViewerArrow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Icon(
        icon, label,
        tint = Color.White.copy(alpha = if (enabled) 1f else 0.25f),
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.18f else 0.07f))
            .clickable(enabled = enabled) { onClick() }
            .padding(11.dp),
    )
}
