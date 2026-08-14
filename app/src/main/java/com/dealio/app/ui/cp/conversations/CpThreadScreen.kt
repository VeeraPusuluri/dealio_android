package com.dealio.app.ui.cp.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.DealMessage
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.leads.CpDealDetailViewModel
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.ThreadTarget
import com.dealio.app.ui.flow.rosterFor
import com.dealio.app.ui.flow.threadKeyFor
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * One thread of one deal — the builder, the buyer, or the three-way group.
 *
 * Messaging used to live on the deal page, which meant a CP reading a deal saw a
 * party picker and a composer wedged between the commission figures and the
 * activity ledger, and the Conversations inbox could do nothing but bounce them
 * back to that same page. Now the inbox picks the party and this screen holds
 * the conversation; the deal page links here and keeps to the deal.
 */
@Composable
fun CpThreadScreen(
    nav: NavController,
    dealId: Long,
    /** Which party's thread to open: "builder" | "customer" | "group". */
    recipientRole: String?,
    vm: CpDealDetailViewModel = viewModel(),
) {
    LaunchedEffect(dealId) { vm.load(dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val d = state.deal
    // The roster is derived the same way the inbox derives it, so the thread the
    // row opened is the thread this screen sends on.
    val roster = rosterFor(
        viewer = DealRole.CP,
        hasCp = true,
        rawStatus = d?.status,
        customerName = d?.customerName,
    )
    val target: ThreadTarget? = roster.firstOrNull { it.recipientRole == recipientRole } ?: roster.firstOrNull()
    val threadKey = target?.let { threadKeyFor(DealRole.CP, it) }

    // Opening a thread reads it.
    LaunchedEffect(d?.id, threadKey) {
        if (d != null && threadKey != null) {
            vm.refreshUnread(listOf(threadKey))
            vm.markThreadRead(threadKey)
        }
    }

    val shown = d?.messages.orEmpty().filter { threadKey == null || it.threadKey == threadKey }

    // A chat that opens at the top of a long history is a chat you have to scroll
    // before you can read it — and again after every send.
    LaunchedEffect(shown.size) {
        if (shown.isNotEmpty()) listState.animateScrollToItem(shown.lastIndex)
    }

    SubScreenScaffold(target?.label ?: "Conversation", nav) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            // Which deal you are talking about, since the title is the party.
            if (d != null) {
                Text(
                    listOfNotNull(
                        d.customerName.takeIf { it.isNotBlank() },
                        d.projectName.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> LoadingState()
                    state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(dealId) })
                    shown.isEmpty() -> Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            if (target?.isGroup == true) "No messages in the group thread yet."
                            else "No messages with ${target?.label ?: "this party"} yet.",
                            color = TextSecondary, fontSize = 13.sp,
                        )
                    }
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(shown.size) { i -> MessageBubble(shown[i]) }
                    }
                }
            }
            SnackbarHost(snackbar)
            if (d != null) {
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
                                if (target?.isGroup == true) "Message all three…"
                                else "Message ${target?.label ?: "the builder"}…",
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = dealioFieldColors(), maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            vm.sendMessage(draft, target?.recipientRole ?: "builder"); draft = ""
                        },
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
}

@Composable
private fun MessageBubble(m: DealMessage) {
    val mine = m.senderRole.equals("cp", true)
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
