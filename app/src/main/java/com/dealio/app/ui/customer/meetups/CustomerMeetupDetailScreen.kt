package com.dealio.app.ui.customer.meetups

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.ApiResult
import com.dealio.app.data.CustomerRepository
import com.dealio.app.data.api.CustomerMeetup
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.shareViaWhatsApp
import com.dealio.app.ui.meetups.AvatarStack
import com.dealio.app.ui.meetups.CategoryChip
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupHero
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.MeetupPhotoGrid
import com.dealio.app.ui.meetups.MeetupPhotoViewer
import com.dealio.app.ui.meetups.Rsvp
import com.dealio.app.ui.meetups.TopicChips
import com.dealio.app.ui.meetups.addMeetupToCalendar
import com.dealio.app.ui.meetups.meetupFullDate
import com.dealio.app.ui.meetups.meetupWhen
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.Mist
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerMeetupDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val meetup: CustomerMeetup? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class CustomerMeetupDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CustomerRepository(app)
    private val _state = MutableStateFlow(CustomerMeetupDetailState())
    val state: StateFlow<CustomerMeetupDetailState> = _state.asStateFlow()
    private var id: Long = 0

    fun load(meetupId: Long) {
        id = meetupId
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getMeetup(meetupId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, meetup = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun rsvp(rsvp: String, guests: Int) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = repo.rsvpMeetup(id, rsvp, guests)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        busy = false, meetup = r.data,
                        message = when (rsvp) {
                            "GOING" -> "You're going — the host can see you're coming"
                            "MAYBE" -> "Marked as maybe"
                            else -> "Thanks for letting them know"
                        },
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/**
 * The event page a customer answers from.
 *
 * Built like the listing on a ticketing app rather than like the rest of the
 * portal: a photograph first, then what it is, when, where and who — because
 * this is the one screen whose job is to talk a stranger into turning up, and
 * a stack of labelled form rows does not do that.
 *
 * Two things are pinned rather than scrolled. Answering sits in a bar at the
 * bottom, because it is the only thing this screen asks and it should not depend
 * on having reached the end; and the header stays put with share on it, because
 * sending a meetup to a friend is what makes one fill up.
 */
@Composable
fun CustomerMeetupDetailScreen(
    nav: NavController,
    meetupId: Long,
    vm: CustomerMeetupDetailViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scroll = rememberScrollState()
    var guests by remember { mutableIntStateOf(0) }
    var viewerAt by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(meetupId) { vm.load(meetupId) }
    LaunchedEffect(state.meetup?.myGuests) { state.meetup?.let { guests = it.myGuests } }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    // The header swaps "Event" for the title once the title itself has scrolled
    // away, so there is always something on screen saying which meetup this is.
    val showTitle by remember { derivedStateOf { scroll.value > 260 } }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        val m = state.meetup
        when {
            state.loading -> LoadingState(Modifier.statusBarsPadding().padding(top = 56.dp))
            state.error != null -> ErrorState(
                state.error!!,
                onRetry = { vm.load(meetupId) },
                modifier = Modifier.statusBarsPadding().padding(top = 56.dp),
            )
            m == null -> ErrorState(
                "Meetup not found",
                onRetry = { vm.load(meetupId) },
                modifier = Modifier.statusBarsPadding().padding(top = 56.dp),
            )
            else -> {
                MeetupBody(
                    m = m,
                    scroll = scroll,
                    ctx = ctx,
                    onOpenPhoto = { viewerAt = it },
                )
                if (!m.isCancelled) {
                    RsvpBar(
                        meetup = m,
                        guests = guests,
                        busy = state.busy,
                        onGuests = { guests = it },
                        onPick = { vm.rsvp(it, guests) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        EventTopBar(
            title = if (showTitle) m?.title else null,
            onBack = { nav.popBackStack() },
            onShare = m?.let { { shareViaWhatsApp(ctx, customerShareText(it), "Share meetup") } },
            onCalendar = m?.takeIf { !it.isCancelled }?.let {
                { addMeetupToCalendar(ctx, it.title, it.location, it.description, it.date, it.time) }
            },
        )

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
        )
    }

    viewerAt?.let { at ->
        state.meetup?.photos?.takeIf { it.isNotEmpty() }?.let { photos ->
            MeetupPhotoViewer(photos, at) { viewerAt = null }
        }
    }
}

// ─── Page ────────────────────────────────────────────────────────────────────

@Composable
private fun MeetupBody(
    m: CustomerMeetup,
    scroll: androidx.compose.foundation.ScrollState,
    ctx: Context,
    onOpenPhoto: (Int) -> Unit,
) {
    val category = MeetupCategory.from(m.category)
    val mode = MeetupMode.from(m.mode)

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        // Clears the floating header. The hero starts under it rather than
        // behind it — a photograph with controls sitting on top of it reads well
        // only when there is a photograph, and most meetups will not have one.
        Spacer(Modifier.statusBarsPadding().height(56.dp))

        Box(Modifier.padding(horizontal = 16.dp)) {
            MeetupHero(
                category = category,
                height = 200,
                coverImage = m.coverImage,
                scrim = !m.coverImage.isNullOrBlank(),
                modifier = Modifier.clip(RoundedCornerShape(18.dp)),
            ) {
                Row(
                    Modifier.align(Alignment.TopEnd).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        m.isCancelled -> HeroBadge("Cancelled", ErrorRed)
                        m.isGoing -> HeroBadge("You're going", IconGreen)
                        m.awaitingReply -> HeroBadge("You're invited", CustomerAccent)
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            CategoryChip(category)
            Spacer(Modifier.height(10.dp))
            Text(
                m.title,
                color = TextPrimary, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, lineHeight = 31.sp,
            )

            if (m.isCancelled) {
                Spacer(Modifier.height(14.dp))
                CancelledBanner(m.cancelReason)
            }

            // ── When ────────────────────────────────────────────────────────
            Spacer(Modifier.height(18.dp))
            FactRow(
                icon = Icons.Outlined.CalendarMonth,
                tint = Orange,
                title = meetupFullDate(m.date),
                body = m.time,
                action = if (m.isCancelled) null else Icons.Outlined.CalendarMonth,
                actionLabel = "Add to calendar",
                onAction = {
                    addMeetupToCalendar(ctx, m.title, m.location, m.description, m.date, m.time)
                },
            )

            // ── Where ───────────────────────────────────────────────────────
            if (mode != MeetupMode.ONLINE) {
                Divider()
                FactRow(
                    icon = Icons.Outlined.LocationOn,
                    tint = CustomerAccent,
                    title = m.city?.takeIf { it.isNotBlank() } ?: "Venue",
                    body = m.location,
                    bodyLines = 4,
                    action = m.mapsLink?.takeIf { it.isNotBlank() }?.let { Icons.Outlined.Map },
                    actionLabel = "Open in Maps",
                    onAction = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, m.mapsLink!!.toUri())) }
                    },
                )
            }

            // ── Joining link ────────────────────────────────────────────────
            if (mode != MeetupMode.IN_PERSON) {
                Divider()
                if (!m.onlineLink.isNullOrBlank()) {
                    FactRow(
                        icon = Icons.Outlined.Videocam,
                        tint = CustomerAccent,
                        title = "Join online",
                        body = m.onlineLink!!,
                        bodyLines = 2,
                        onBodyClick = {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, m.onlineLink!!.toUri())) }
                        },
                    )
                } else {
                    FactRow(
                        icon = Icons.Outlined.Videocam,
                        tint = TextSecondary,
                        title = "Online",
                        body = "The joining link appears here once you say you're going.",
                        bodyLines = 3,
                    )
                }
            }

            Divider()

            // ── Host ────────────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            HostCard(m, ctx)

            // ── Topics ──────────────────────────────────────────────────────
            if (m.topics.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                TopicChips(m.topics)
            }

            // ── About ───────────────────────────────────────────────────────
            if (!m.description.isNullOrBlank()) {
                Spacer(Modifier.height(22.dp))
                SectionHeading("About this meetup")
                Spacer(Modifier.height(10.dp))
                ExpandableText(m.description!!)
            }

            // ── Who is coming ───────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            GoingCard(m)

            // ── Photos ──────────────────────────────────────────────────────
            if (m.photos.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeading("Photos")
                Spacer(Modifier.height(12.dp))
                MeetupPhotoGrid(m.photos, onOpen = onOpenPhoto)
            }

            // Clears the pinned bar so the last thing on the page is reachable.
            // The bar grew by the navigation-bar inset, so this has to as well.
            Spacer(Modifier.height(160.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/**
 * Back, share, and add-to-calendar, floating over the page.
 *
 * The two actions sit together in one pill the way the reference does it, so
 * they read as "things you can do with this event" rather than as two unrelated
 * icons. Share is the one that matters: a meetup fills up because someone sent
 * it to a friend, and burying that at the bottom of the page means it never
 * happens.
 */
@Composable
private fun EventTopBar(
    title: String?,
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    onCalendar: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)

        Crossfade(title, Modifier.weight(1f).padding(horizontal = 8.dp), label = "eventTitle") { shown ->
            Text(
                shown ?: "Event",
                color = TextPrimary,
                fontSize = if (shown == null) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (onShare != null || onCalendar != null) {
            Row(
                Modifier.clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(1.dp, CardBorder, RoundedCornerShape(22.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onShare?.let { PillIcon(Icons.Outlined.Share, "Share meetup", it) }
                onCalendar?.let { PillIcon(Icons.Outlined.CalendarMonth, "Add to calendar", it) }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        icon, label, tint = TextPrimary,
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(Color.White)
            .border(1.dp, CardBorder, CircleShape)
            .clickable { onClick() }
            .padding(10.dp),
    )
}

@Composable
private fun PillIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        icon, label, tint = TextPrimary,
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onClick() }.padding(10.dp),
    )
}

/**
 * One fact about the event: a tinted icon, a strong line, a quiet one.
 *
 * The repeated shape is the point. Date, place and joining link are the three
 * questions someone asks in that order, and giving them one layout lets the eye
 * skip between them instead of reading each as a new thing.
 */
@Composable
private fun FactRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    bodyLines: Int = 2,
    action: ImageVector? = null,
    actionLabel: String = "",
    onAction: () -> Unit = {},
    onBodyClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .let { if (onBodyClick != null) it.clickable { onBodyClick() } else it }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }

        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                body,
                color = if (onBodyClick != null) CustomerAccent else TextSecondary,
                fontSize = 13.sp, lineHeight = 19.sp,
                maxLines = bodyLines, overflow = TextOverflow.Ellipsis,
            )
        }

        if (action != null) {
            Spacer(Modifier.width(10.dp))
            Icon(
                action, actionLabel, tint = CustomerAccent,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                    .background(CustomerAccent.copy(alpha = 0.10f))
                    .clickable { onAction() }
                    .padding(9.dp),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder.copy(alpha = 0.7f)))
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

/**
 * The blurb, cut to five lines with a way to open it.
 *
 * An organiser's description runs to whatever they felt like typing, and letting
 * it push the faces and the photographs off the bottom of the page costs more
 * than it gives — the people who want the detail will tap.
 */
@Composable
private fun ExpandableText(text: String) {
    var expanded by remember { mutableStateOf(false) }
    var clipped by remember { mutableStateOf(false) }

    Text(
        text,
        color = TextSecondary, fontSize = 13.sp, lineHeight = 21.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 5,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) clipped = it.hasVisualOverflow },
    )
    if (clipped) {
        Text(
            if (expanded) "Show less" else "Read more",
            color = CustomerAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp).clickable { expanded = !expanded },
        )
    }
}

/**
 * Who is hosting.
 *
 * A photograph when the partner has set one, initials when they have not. The
 * phone number is only here for someone personally invited — a public listing
 * should not hand a partner's number to everyone who scrolls past.
 */
@Composable
private fun HostCard(m: CustomerMeetup, ctx: Context) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Mist.copy(alpha = 0.5f))
            .border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val photo = resolveUrl(m.hostPhoto)
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(initialsOf(m.hostName), color = CustomerAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hosted by", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                m.hostName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull("Channel Partner", m.hostTier).joinToString(" · "),
                color = TextSecondary, fontSize = 11.sp, maxLines = 1,
            )
        }
        if (!m.hostPhone.isNullOrBlank()) {
            Icon(
                Icons.Outlined.Phone, "Call ${m.hostName}", tint = CustomerAccent,
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(CustomerAccent.copy(alpha = 0.12f))
                    .clickable {
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${m.hostPhone}".toUri()))
                        }
                    }
                    .padding(9.dp),
            )
        }
    }
}

/**
 * How many are coming, with faces on it.
 *
 * A bare "8 going" is a statistic; a row of initials is eight people. When
 * nobody has answered yet the card says so plainly and asks — an empty count
 * dressed up as social proof puts people off rather than drawing them in.
 */
@Composable
private fun GoingCard(m: CustomerMeetup) {
    val shape = RoundedCornerShape(16.dp)
    val spotsLeft = m.capacity?.let { (it - m.goingCount).coerceAtLeast(0) }

    Column(
        Modifier.fillMaxWidth().clip(shape).background(Color.White)
            .border(1.dp, CardBorder.copy(alpha = 0.7f), shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.People, null, tint = CustomerAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                if (m.goingCount == 0) "Nobody has said yes yet" else "${m.goingCount} going",
                color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
            if (spotsLeft != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    if (spotsLeft == 0) "Full" else "$spotsLeft of ${m.capacity} left",
                    color = if (spotsLeft == 0) Orange else TextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (m.goingCount == 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Be the first — the host will see you're coming.",
                color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
            )
            return@Column
        }

        if (m.goingNames.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarStack(m.goingNames, extra = (m.goingCount - m.goingNames.size).coerceAtLeast(0))
                Spacer(Modifier.width(6.dp))
                Text(
                    goingLine(m.goingNames, m.goingCount),
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "Priya, Arun and 6 others are going" — the count said as people. */
private fun goingLine(names: List<String>, total: Int): String {
    val shown = names.take(2)
    val rest = total - shown.size
    val who = shown.joinToString(", ")
    return when {
        rest <= 0 && shown.size == 1 -> "$who is going"
        rest <= 0 -> "$who are going"
        else -> "$who and $rest other${if (rest == 1) "" else "s"} are going"
    }
}

@Composable
private fun HeroBadge(label: String, tint: Color) {
    Text(
        label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(tint).padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@Composable
private fun CancelledBanner(reason: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(ErrorRed.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.EventBusy, null, tint = ErrorRed, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text("This meetup was cancelled", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!reason.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(reason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

// ─── Answering ───────────────────────────────────────────────────────────────

/**
 * The pinned answer bar.
 *
 * One dominant button, the way a ticketing app does it, because saying yes is
 * what this page is for. Maybe and Can't go stay reachable underneath rather
 * than as equals: a no is a real answer the host needs, but offering three
 * identical buttons makes someone stop and choose when most of them arrived
 * already meaning yes.
 */
@Composable
private fun RsvpBar(
    meetup: CustomerMeetup,
    guests: Int,
    busy: Boolean,
    onGuests: (Int) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = Rsvp.from(meetup.myRsvp)
    val going = current == Rsvp.GOING
    val blocked = meetup.isFull && !going
    val spotsLeft = meetup.capacity?.let { (it - meetup.goingCount).coerceAtLeast(0) }

    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            // After the background, not before: the white sheet runs to the very
            // bottom of the screen while the buttons sit above the gesture bar.
            // Without this the Attend button is drawn under the system nav.
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // The guest stepper only matters to someone who is coming.
        if (going) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Bringing anyone?", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Stepper(guests, enabled = !busy) { onGuests(it); onPick("GOING") }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (going) "You're going" else "Free to attend",
                    color = if (going) IconGreen else TextPrimary,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        blocked -> "This meetup is full"
                        spotsLeft != null -> "$spotsLeft spot${if (spotsLeft == 1) "" else "s"} left"
                        meetup.goingCount > 0 -> "${meetup.goingCount} going"
                        else -> "Open to everyone"
                    },
                    color = if (blocked) Orange else TextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(14.dp))
            AttendButton(going = going, enabled = !busy && !blocked) {
                onPick(if (going) Rsvp.DECLINED.wire else Rsvp.GOING.wire)
            }
        }

        // The other two answers, quieter. Hidden once someone is going — the
        // button above already toggles back out, and three ways to say no on one
        // bar is a decision nobody asked for.
        if (!going) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Rsvp.MAYBE, Rsvp.DECLINED).forEach { option ->
                    val on = current == option
                    Text(
                        option.label,
                        color = if (on) Color.White else option.tint,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (on) option.tint else option.tint.copy(alpha = 0.10f))
                            .clickable(enabled = !busy) { onPick(option.wire) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** The one button the page is built around. */
@Composable
private fun AttendButton(going: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Text(
        if (going) "Going ✓" else "Attend",
        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(shape)
            .background(
                when {
                    !enabled -> TextSecondary.copy(alpha = 0.35f)
                    going -> IconGreen
                    else -> TextPrimary
                },
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 30.dp, vertical = 14.dp),
    )
}

@Composable
private fun Stepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", enabled && value > 0) { onChange(value - 1) }
        Text(
            if (value == 0) "Just me" else "+$value",
            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        StepButton("+", enabled && value < 9) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        glyph,
        color = if (enabled) CustomerAccent else TextSecondary.copy(alpha = 0.4f),
        fontSize = 17.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .background(CustomerAccent.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled) { onClick() }
            .padding(top = 5.dp),
    )
}

/**
 * What gets sent when a customer shares a meetup.
 *
 * Written as one person passing something on to another — the organiser's own
 * share text is a host's announcement, and forwarding that verbatim reads as a
 * broadcast rather than a recommendation.
 */
private fun customerShareText(m: CustomerMeetup): String = buildString {
    appendLine("Thought you might like this:")
    appendLine()
    appendLine(m.title)
    appendLine(meetupWhen(m.date, m.time))
    if (MeetupMode.from(m.mode) != MeetupMode.ONLINE && m.location.isNotBlank()) appendLine(m.location)
    if (m.hostName.isNotBlank()) appendLine("Hosted by ${m.hostName}")
    m.mapsLink?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
    appendLine()
    append("Shared from Dealio")
}
