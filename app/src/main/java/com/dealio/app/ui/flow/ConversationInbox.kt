package com.dealio.app.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.ThreadRef
import com.dealio.app.data.api.ThreadSummary
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Every conversation this user has, grouped by deal and then by party.
 *
 * The deal screens gained a [PartyRail] — four threads per deal, one per pair
 * plus the group — but the three Conversations screens kept the model that
 * predates it: one row per deal, opening whichever thread the detail screen
 * happened to default to. So the inbox could not answer the only question an
 * inbox exists to answer — who is waiting on a reply — and a message from the
 * builder was indistinguishable from one from the buyer.
 *
 * This renders the same roster the deal screen does, one row per thread, each
 * carrying its own last message and unread count. Tapping a row opens the deal
 * *on that thread*, which is why the deal-detail routes take a `thread` argument.
 *
 * Portable by construction: the roster, the thread keys and the ordering are
 * plain functions over data the three portals already fetch, so iOS and web can
 * mirror this screen without a new endpoint.
 */

/** One deal in the inbox, reduced to what its party rows need. */
data class InboxDeal(
    val dealId: Long,
    /** Who or what this deal is about, in the viewer's terms. */
    val title: String,
    val subtitle: String,
    val rawStatus: String = "",
    val hasCp: Boolean = false,
    val builderName: String? = null,
    val cpName: String? = null,
    val customerName: String? = null,
)

/** A single thread row: the party, its key, and whatever the backend knows of it. */
private data class InboxThread(
    val target: ThreadTarget,
    val threadKey: String,
    val summary: ThreadSummary?,
) {
    val unread get() = summary?.unreadCount ?: 0
    val lastAt get() = summary?.lastMessage?.createdAt ?: ""
}

/**
 * Last message + unread count for every thread on screen, in one request.
 *
 * `POST /threads/summary` takes the (dealId, threadKey) pairs the caller already
 * knows and authorizes each one itself, so this is role-agnostic — the same view
 * model serves the CP, builder and customer inboxes.
 */
class ThreadInboxViewModel : ViewModel() {
    private val threads = ThreadRepository()
    private val _summaries = MutableStateFlow<Map<String, ThreadSummary>>(emptyMap())
    val summaries: StateFlow<Map<String, ThreadSummary>> = _summaries.asStateFlow()

    fun load(refs: List<ThreadRef>) {
        if (refs.isEmpty()) return
        viewModelScope.launch {
            val r = threads.summaries(refs.take(MAX_THREADS))
            if (r is ApiResult.Success) {
                _summaries.value = r.data.associateBy { summaryKey(it.dealId, it.threadKey) }
            }
        }
    }
}

/** The backend caps a summary request at 200 pairs; stop short rather than be truncated. */
private const val MAX_THREADS = 200

private fun summaryKey(dealId: Long, threadKey: String) = "$dealId:$threadKey"

@Composable
fun ConversationInbox(
    viewer: DealRole,
    deals: List<InboxDeal>,
    /** Open a deal on one of its threads. The role is what the rail calls the party. */
    onOpen: (dealId: Long, recipientRole: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    empty: @Composable () -> Unit = {},
    vm: ThreadInboxViewModel = viewModel(),
) {
    val summaries by vm.summaries.collectAsStateWithLifecycle()

    // The roster is derived, not fetched: which threads exist follows from the
    // viewer, whether a CP is attached and the stage — the same rule the backend
    // authorizes with, so asking for a thread we shouldn't see is simply dropped.
    val rosters = remember(deals, viewer) {
        deals.map { d ->
            d to rosterFor(viewer, d.hasCp, d.rawStatus, d.builderName, d.cpName, d.customerName)
        }
    }
    val refs = remember(rosters) {
        rosters.flatMap { (d, roster) -> roster.map { ThreadRef(d.dealId, threadKeyFor(viewer, it)) } }
    }
    LaunchedEffect(refs) { vm.load(refs) }

    // Unread deals first, then whatever moved most recently. ISO-8601 timestamps
    // sort lexicographically, so no parsing is needed to order them.
    val rows = remember(rosters, summaries) {
        rosters.map { (deal, roster) ->
            deal to roster.map { t ->
                val key = threadKeyFor(viewer, t)
                InboxThread(t, key, summaries[summaryKey(deal.dealId, key)])
            }
        }.sortedWith(
            compareByDescending<Pair<InboxDeal, List<InboxThread>>> { (_, threads) ->
                threads.sumOf { it.unread } > 0
            }.thenByDescending { (_, threads) -> threads.maxOfOrNull { it.lastAt } ?: "" },
        )
    }
    val totalUnread = rows.sumOf { (_, threads) -> threads.sumOf { it.unread } }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                buildString {
                    append("${deals.size} ${if (deals.size == 1) "conversation" else "conversations"}")
                    if (totalUnread > 0) append(" · $totalUnread unread")
                },
                color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            )
        }
        if (deals.isEmpty()) {
            item { empty() }
        } else {
            items(rows.size) { i ->
                val (deal, threads) = rows[i]
                DealThreads(deal, threads, onOpen)
            }
        }
    }
}

@Composable
private fun DealThreads(
    deal: InboxDeal,
    threads: List<InboxThread>,
    onOpen: (Long, String) -> Unit,
) {
    val unread = threads.sumOf { it.unread }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(if (unread > 0) 1.5.dp else 1.dp, if (unread > 0) UnreadAccent else CardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(deal.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (deal.subtitle.isNotBlank()) {
                    Text(deal.subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
            canonicalStage(deal.rawStatus)?.let {
                Text(it.uppercase(), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        threads.forEach { row -> ThreadRow(row) { onOpen(deal.dealId, row.target.recipientRole) } }
    }
}

@Composable
private fun ThreadRow(row: InboxThread, onClick: () -> Unit) {
    val last = row.summary?.lastMessage
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).background(
                if (row.target.isGroup) row.target.color.copy(alpha = 0.14f) else row.target.color,
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.target.initials,
                color = if (row.target.isGroup) row.target.color else Color.White,
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.target.label,
                color = TextPrimary, fontSize = 12.5.sp,
                fontWeight = if (row.unread > 0) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                // Whose voice it was matters as much as the words — an inbox that
                // says only "ok, tomorrow works" tells you nothing about who to answer.
                if (last == null) "No messages yet"
                else "${last.senderName.trim().substringBefore(' ').ifBlank { "Someone" }}: ${last.message}",
                color = TextSecondary, fontSize = 11.sp, maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(shortAgo(last?.createdAt), color = TextSecondary, fontSize = 10.sp)
            if (row.unread > 0) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.background(row.target.color, RoundedCornerShape(9.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text("${row.unread}", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private val UnreadAccent = Color(0xFF0A9CB5)

/**
 * "now" / "12m" / "5h" / "3d" / "7 Aug" from an ISO-8601 timestamp.
 *
 * An inbox needs finer granularity than the ledger's day slicing: "3d" and
 * "12m" are the difference between a stale thread and a live one.
 */
fun shortAgo(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val then = runCatching { java.time.Instant.parse(iso) }.getOrNull()
        ?: runCatching {
            java.time.LocalDateTime.parse(iso.take(19)).toInstant(java.time.ZoneOffset.UTC)
        }.getOrNull()
        ?: return ""
    val minutes = java.time.temporal.ChronoUnit.MINUTES.between(then, java.time.Instant.now())
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
        else -> java.time.format.DateTimeFormatter.ofPattern("d MMM")
            .withZone(java.time.ZoneId.systemDefault()).format(then)
    }
}
