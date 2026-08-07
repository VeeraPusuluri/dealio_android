package com.dealio.app.ui.customer.journey

import android.content.Intent
import androidx.core.net.toUri
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CustomerDeal
import com.dealio.app.data.api.DealMessage
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import androidx.compose.material3.OutlinedButton
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.DealSpine
import com.dealio.app.ui.flow.PartyRail
import com.dealio.app.ui.flow.batonOf
import com.dealio.app.ui.flow.ThreadTarget
import com.dealio.app.ui.flow.rosterFor
import com.dealio.app.ui.flow.threadKeyFor
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailScreen(nav: NavController, dealId: Long, vm: DealDetailViewModel = viewModel()) {
    LaunchedEffect(dealId) { vm.load(dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val d = state.deal
    // The buyer's two counterparties, plus the group. Pre-booking with an advisor
    // attached the private builder pair is withheld and the group stands in for
    // it — the same rule the backend applies, so the rail never offers a thread
    // the server would refuse.
    val roster = rosterFor(
        viewer = DealRole.CUSTOMER,
        hasCp = d?.cpName != null,
        rawStatus = d?.dealStatus,
        builderName = d?.builderName,
        cpName = d?.cpName,
    )
    var target by remember(dealId) { mutableStateOf<ThreadTarget?>(null) }
    val selected = target ?: roster.firstOrNull()

    // True when the deal is genuinely waiting on the buyer and there is a
    // confirm to make — the one case where the spine should carry the action.
    val ownsConfirm = d != null &&
        batonOf(d.dealStatus, d.cpAgreed, d.customerConfirmed).heldBy(DealRole.CUSTOMER) &&
        showConfirmFor(d)

    val threadKeys = roster.map { threadKeyFor(DealRole.CUSTOMER, it) }
    LaunchedEffect(d?.dealId, d?.messages?.size) {
        if (d != null) vm.refreshUnread(threadKeys)
    }
    LaunchedEffect(selected?.recipientRole, d?.dealId) {
        if (d != null && selected != null) vm.markThreadRead(threadKeyFor(DealRole.CUSTOMER, selected))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(d?.projectName ?: "Deal", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White, titleContentColor = Navy),
            )
        },
        bottomBar = {
            if (d != null) {
                Row(
                    // Whichever is taller — the keyboard when open, the navigation
                    // bar otherwise. imePadding() alone left the composer sitting
                    // under the nav bar with the keyboard down.
                    Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White)
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (selected?.isGroup == true) "Message everyone…"
                                else "Message ${selected?.label ?: "the builder"}…",
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = dealioFieldColors(),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            vm.sendMessage(draft, selected?.recipientRole ?: "builder"); draft = ""
                        },
                        enabled = draft.isNotBlank() && !state.sending,
                        modifier = Modifier.size(48.dp).background(Teal, RoundedCornerShape(24.dp)),
                    ) {
                        if (state.sending) CircularProgressIndicator(Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(dealId) }, modifier = Modifier.padding(inner))
            d != null -> LazyColumn(
                modifier = Modifier.padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The spine, in the buyer's register: no stage chip, no pipeline
                // vocabulary. This screen used to show StatusChip(dealStatus) —
                // the sales team's words ("Pending Booking") shown to the person
                // buying the home, and no sense of progress at all.
                item {
                    DealSpine(
                        rawStatus = d.dealStatus,
                        viewer = DealRole.CUSTOMER,
                        customerConfirmed = d.customerConfirmed,
                        buyerRegister = true,
                        // When the deal really is waiting on the buyer, the spine
                        // carries the action instead of a second button below it.
                        actionLabel = if (ownsConfirm) "Confirm" else null,
                        onAction = if (ownsConfirm) ({ vm.confirm() }) else null,
                        onNudge = { vm.nudge() },
                    )
                }

                if (d.customerConfirmed) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = StatusColors.Green, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Confirmed by you", color = StatusColors.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Actions
                if (!ownsConfirm) {
                    item { DealActions(d, working = state.working, onAccept = vm::acceptNegotiation, onConfirm = vm::confirm) }
                }

                // Loan info
                if (d.loanCaseId != null) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(14.dp),
                        ) {
                            SectionLabel("Home loan")
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Amount", d.loanAmount?.let { formatINR(it) })
                            InfoRow("Status", d.loanStatus)
                            InfoRow("Tenure", d.tenureMonths?.let { "${it / 12} years" })
                            InfoRow("Interest", d.interestRate?.let { "$it%" })
                        }
                    }
                }

                // Documents
                if (d.dealDocuments.isNotEmpty()) {
                    item { SectionLabel("Documents") }
                    items(d.dealDocuments.size) { i ->
                        val doc = d.dealDocuments[i]
                        Row(
                            Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    resolveUrl(doc.fileUrl)?.let { url ->
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Description, null, tint = Teal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(doc.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(doc.docType, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Conversation — one thread at a time.
                item { SectionLabel("Conversation") }
                item {
                    PartyRail(
                        targets = roster,
                        selected = selected,
                        onSelect = { target = it },
                        unreadOf = { state.unread[threadKeyFor(DealRole.CUSTOMER, it)] ?: 0 },
                    )
                }
                val thread = selected?.let { threadKeyFor(DealRole.CUSTOMER, it) }
                val shown = d.messages.filter { thread == null || it.threadKey == thread }
                if (shown.isEmpty()) {
                    item {
                        Text(
                            if (selected?.isGroup == true) "No messages here yet — everyone can see this one."
                            else "No messages with ${selected?.label ?: "this party"} yet.",
                            color = TextSecondary, fontSize = 13.sp,
                        )
                    }
                } else {
                    items(shown.size) { i -> MessageBubble(shown[i]) }
                }
            }
        }
    }
}

// A buyer can accept a quote while the deal still reads "Negotiation" — the
// status column has no separate "quote sent" state — so this action is offered
// even when the baton is not on them. It just renders as a secondary control,
// so it never competes with a spine saying the deal is waiting on someone else.
internal fun showAcceptFor(d: CustomerDeal): Boolean =
    d.dealStatus.lowercase().contains("negotiation") && !d.customerConfirmed

internal fun showConfirmFor(d: CustomerDeal): Boolean {
    val status = d.dealStatus.lowercase()
    return !d.customerConfirmed && !showAcceptFor(d) &&
        (status.contains("agreement") || status.contains("pending booking") || status.contains("booked"))
}

@Composable
private fun DealActions(d: CustomerDeal, working: Boolean, onAccept: () -> Unit, onConfirm: () -> Unit) {
    if (showAcceptFor(d)) {
        OutlinedButton(
            onClick = onAccept, enabled = !working,
            modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
        ) { Text("Accept negotiated price", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    } else if (showConfirmFor(d)) {
        OutlinedButton(
            onClick = onConfirm, enabled = !working,
            modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
        ) { Text("Confirm deal", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun MessageBubble(m: DealMessage) {
    val mine = m.senderRole.equals("customer", true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .background(
                    if (mine) Teal else androidx.compose.ui.graphics.Color.White,
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = if (mine) 14.dp else 2.dp, bottomEnd = if (mine) 2.dp else 14.dp),
                )
                .border(if (mine) 0.dp else 1.dp, if (mine) Teal else CardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!mine) Text(m.senderName, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(m.message, color = if (mine) androidx.compose.ui.graphics.Color.White else TextPrimary, fontSize = 13.sp)
        }
    }
}
