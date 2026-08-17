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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.ThreadRepository
import com.dealio.app.data.api.Conversation
import com.dealio.app.data.api.ConversationMessage
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Messaging, for all three portals.
 *
 * One inbox and one thread screen serve the builder, the CP and the buyer. That
 * is possible now because a conversation is between *people*: the backend
 * resolves who the caller is from their token and hands back the conversations
 * they may hold, so nothing on this screen is role-specific except which colour
 * the other person's avatar is.
 *
 * What this replaces: an inbox that listed one card per deal, with a row per
 * thread inside it. A buyer enquiring about three towers from one builder got
 * three cards, each with the same two people in it, and a reply could arrive in
 * any of them. The deal was never what the conversation was about.
 */

// ─── Reading a conversation's shape ──────────────────────────────────────────

/** The two roles named in a kind, e.g. "builder-cp" → builder and cp. */
private fun rolesOf(kind: String): List<DealRole> = kind.split("-").mapNotNull {
    when (it) {
        "builder" -> DealRole.BUILDER
        "cp" -> DealRole.CP
        "customer" -> DealRole.CUSTOMER
        else -> null
    }
}

/**
 * Who the viewer is talking to, or null for the group.
 *
 * Derived from the kind rather than sent by the server, because the kind
 * already carries it: a pair names two roles and one of them is you.
 */
fun counterpartOf(kind: String, viewer: DealRole): DealRole? =
    rolesOf(kind).firstOrNull { it != viewer }

/** The accent for a conversation row — the other person's colour, navy for a group. */
private fun accentOf(c: Conversation, viewer: DealRole): Color =
    if (c.isGroup) Navy else counterpartOf(c.kind, viewer)?.let { roleColor(it) } ?: Teal

private fun initialsOf(name: String): String =
    name.trim().split(" ", "&").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

/** What the other side is, in a word — the second line of a picker row. */
private fun kindLabel(kind: String, viewer: DealRole): String = when {
    kind == "group" -> "All three — you, the builder and the buyer"
    else -> when (counterpartOf(kind, viewer)) {
        DealRole.BUILDER -> "Builder"
        DealRole.CP -> "Channel partner"
        DealRole.CUSTOMER -> "Customer"
        else -> "Conversation"
    }
}

// ─── The inbox ───────────────────────────────────────────────────────────────

data class ConversationsState(
    val loading: Boolean = true,
    val error: String? = null,
    val conversations: List<Conversation> = emptyList(),
    /** Everyone the user could talk to — loaded lazily, when "+" is tapped. */
    val candidates: List<Conversation> = emptyList(),
    val loadingCandidates: Boolean = false,
    val picking: Boolean = false,
    /** Set once a candidate has been opened, so the screen can navigate to it. */
    val opened: Long? = null,
)

class ConversationsViewModel : ViewModel() {
    private val repo = ThreadRepository()
    private val _state = MutableStateFlow(ConversationsState())
    val state: StateFlow<ConversationsState> = _state.asStateFlow()

    // No load() in init: the screen loads on entry *and* on every return, so an
    // init here would only duplicate the first request.
    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.list()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, conversations = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    /**
     * Open the "+" sheet and fetch who is available.
     *
     * Fetched on demand rather than alongside the inbox: the roster is derived
     * from every lead the user has, which is the more expensive of the two
     * queries and is worth nothing until the sheet is actually open.
     */
    fun startPicking() {
        _state.update { it.copy(picking = true, loadingCandidates = true) }
        viewModelScope.launch {
            when (val r = repo.candidates()) {
                is ApiResult.Success -> _state.update { it.copy(loadingCandidates = false, candidates = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loadingCandidates = false, error = r.message) }
            }
        }
    }

    fun stopPicking() = _state.update { it.copy(picking = false) }

    /** Open (or resume) the picked conversation, then hand its id to the screen. */
    fun open(key: String) {
        viewModelScope.launch {
            when (val r = repo.open(key)) {
                is ApiResult.Success -> _state.update {
                    it.copy(picking = false, opened = r.data.id)
                }
                is ApiResult.Error -> _state.update { it.copy(picking = false, error = r.message) }
            }
        }
    }

    fun consumeOpened() = _state.update { it.copy(opened = null) }
}

/**
 * The conversations screen, complete — list, "+" picker and empty state.
 *
 * @param viewer whose portal this is, which decides only the avatar colours and
 *        which side of a pair counts as "the other person".
 * @param emptyHint what to say when there is nothing yet, in this role's terms.
 * @param onOpen where to send the user for a conversation. Every portal has its
 *        own route for the thread screen, which is the only thing they differ on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    nav: NavController,
    viewer: DealRole,
    emptyHint: String,
    onOpen: (conversationId: Long) -> Unit,
    vm: ConversationsViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Reload on return, so a conversation read on the thread screen comes back
    // without its badge rather than keeping it until the app restarts.
    LaunchedEffect(Unit) { vm.load() }

    // Opening a candidate lands here one recomposition later, once the backend
    // has minted (or found) the row and returned its id.
    LaunchedEffect(state.opened) {
        state.opened?.let { onOpen(it); vm.consumeOpened() }
    }

    SubScreenScaffold(
        "Conversations",
        nav,
        actions = {
            IconButton(onClick = vm::startPicking) {
                Icon(Icons.Default.Add, "New conversation", tint = Navy)
            }
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load, modifier = Modifier.padding(inner))
            else -> {
                val total = state.conversations.sumOf { it.unreadCount }
                LazyColumn(
                    Modifier.padding(inner).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            buildString {
                                append("${state.conversations.size} ")
                                append(if (state.conversations.size == 1) "conversation" else "conversations")
                                if (total > 0) append(" · $total unread")
                            },
                            color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                    if (state.conversations.isEmpty()) {
                        item {
                            DealioCard {
                                EmptyState(Icons.Outlined.ChatBubbleOutline, "No conversations yet", emptyHint)
                            }
                        }
                    } else {
                        items(state.conversations.size) { i ->
                            val c = state.conversations[i]
                            ConversationRow(c, viewer) { c.id?.let(onOpen) }
                        }
                    }
                }
            }
        }
    }

    if (state.picking) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = vm::stopPicking,
            sheetState = sheet,
            containerColor = Color.White,
        ) {
            CandidatePicker(
                candidates = state.candidates,
                loading = state.loadingCandidates,
                viewer = viewer,
                onPick = { c -> if (c.id != null) { vm.stopPicking(); onOpen(c.id) } else vm.open(c.key) },
            )
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, viewer: DealRole, onClick: () -> Unit) {
    val accent = accentOf(c, viewer)
    val unread = c.unreadCount > 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(if (unread) 1.5.dp else 1.dp, if (unread) accent else CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).background(
                if (c.isGroup) accent.copy(alpha = 0.14f) else accent,
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (c.isGroup) "3" else initialsOf(c.title),
                color = if (c.isGroup) accent else Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                c.title,
                color = TextPrimary, fontSize = 14.sp,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
            )
            val last = c.lastMessage
            Text(
                // Whose voice it was matters as much as the words — an inbox
                // that says only "ok, tomorrow works" tells you nothing about
                // who to answer.
                if (last == null) kindLabel(c.kind, viewer)
                else "${last.senderName.trim().substringBefore(' ').ifBlank { "Someone" }}: ${last.message}",
                color = TextSecondary, fontSize = 11.5.sp, maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(shortAgo(c.lastMessage?.createdAt), color = TextSecondary, fontSize = 10.sp)
            if (unread) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.background(accent, RoundedCornerShape(9.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text("${c.unreadCount}", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * The "+" sheet: everyone on this user's leads, and the rooms they can hold.
 *
 * Rooms already open are listed too rather than hidden — the point of the
 * picker is "who can I talk to", and hiding the answers you already have makes
 * that list read as if those people were unavailable.
 */
@Composable
private fun CandidatePicker(
    candidates: List<Conversation>,
    loading: Boolean,
    viewer: DealRole,
    onPick: (Conversation) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text("Start a conversation", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            "Everyone on your leads, and the rooms you share with them.",
            color = TextSecondary, fontSize = 12.sp,
        )
        Spacer(Modifier.height(14.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Teal, strokeWidth = 2.dp)
            }
            candidates.isEmpty() -> Text(
                "Nobody yet. Once you have a lead, the people on it appear here.",
                color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 20.dp),
            )
            else -> LazyColumn(
                Modifier.fillMaxWidth().height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(candidates.size) { i ->
                    val c = candidates[i]
                    val accent = accentOf(c, viewer)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { onPick(c) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(32.dp).background(
                                if (c.isGroup) accent.copy(alpha = 0.14f) else accent,
                                CircleShape,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (c.isGroup) "3" else initialsOf(c.title),
                                color = if (c.isGroup) accent else Color.White,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(kindLabel(c.kind, viewer), color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                        if (c.id != null) {
                            Text("Open", color = accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─── The thread ──────────────────────────────────────────────────────────────

data class ThreadState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val conversation: Conversation? = null,
    val messages: List<ConversationMessage> = emptyList(),
)

class ConversationThreadViewModel : ViewModel() {
    private val repo = ThreadRepository()
    private val _state = MutableStateFlow(ThreadState())
    val state: StateFlow<ThreadState> = _state.asStateFlow()

    fun load(conversationId: Long) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.messages(conversationId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, conversation = r.data.conversation, messages = r.data.messages)
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun send(conversationId: Long, text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            when (val r = repo.send(conversationId, text.trim())) {
                // Append rather than reload: the transcript is already correct
                // and a full refetch would jump the list out from under a reader.
                is ApiResult.Success -> _state.update {
                    it.copy(sending = false, messages = it.messages + r.data)
                }
                is ApiResult.Error -> _state.update { it.copy(sending = false, toast = r.message) }
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
}

/**
 * One conversation, read and written.
 *
 * The only role-dependent thing here is which bubbles are yours, which follows
 * from [viewer] — the sender's role is on every message.
 */
@Composable
fun ConversationScreen(
    nav: NavController,
    viewer: DealRole,
    conversationId: Long,
    vm: ConversationThreadViewModel = viewModel(),
) {
    LaunchedEffect(conversationId) { vm.load(conversationId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.toast) { state.toast?.let { snackbar.showSnackbar(it); vm.clearToast() } }

    // A chat that opens at the top of a long history is a chat you have to
    // scroll before you can read it — and again after every send.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    val c = state.conversation
    SubScreenScaffold(c?.title ?: "Conversation", nav) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            // Who else is in the room. On a group this is the whole point; on a
            // pair it is a quiet confirmation you are writing to the right person.
            if (c != null && c.participants.isNotEmpty()) {
                Text(
                    c.participants.joinToString(" · "),
                    color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> LoadingState()
                    state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(conversationId) })
                    state.messages.isEmpty() -> Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            if (c?.isGroup == true) "No messages in this room yet."
                            else "No messages with ${c?.title ?: "this party"} yet.",
                            color = TextSecondary, fontSize = 13.sp,
                        )
                    }
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.messages.size) { i -> MessageBubble(state.messages[i], viewer) }
                    }
                }
            }
            SnackbarHost(snackbar)
            Row(
                // Whichever is taller — the keyboard when open, the navigation
                // bar otherwise. imePadding() alone leaves the composer sitting
                // under the nav bar with the keyboard down.
                Modifier.fillMaxWidth().background(Color.White)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (c?.isGroup == true) "Message all three…"
                            else "Message ${c?.title ?: "…"}…",
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = dealioFieldColors(), maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { vm.send(conversationId, draft); draft = "" },
                    enabled = draft.isNotBlank() && !state.sending,
                    modifier = Modifier.size(48.dp).background(Teal, RoundedCornerShape(24.dp)),
                ) {
                    if (state.sending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ConversationMessage, viewer: DealRole) {
    val mine = m.senderRole.equals(viewer.wireName, true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.background(if (mine) Teal else Color.White, RoundedCornerShape(14.dp))
                .border(if (mine) 0.dp else 1.dp, if (mine) Teal else CardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!mine) Text(m.senderName, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(m.message, color = if (mine) Color.White else TextPrimary, fontSize = 13.sp)
        }
    }
}

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
