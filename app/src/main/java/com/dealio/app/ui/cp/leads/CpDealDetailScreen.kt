package com.dealio.app.ui.cp.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CpDealDetail
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.flow.ActivityLedger
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.DealSpine
import com.dealio.app.ui.flow.batonOf
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.cp.projects.CpBookingSheet
import com.dealio.app.ui.flow.canonicalStage
import com.dealio.app.ui.flow.stageIndex
import com.dealio.app.ui.flow.rosterFor
import com.dealio.app.ui.flow.threadKeyFor
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpDealDetailScreen(
    nav: NavController,
    dealId: Long,
    vm: CpDealDetailViewModel = viewModel(),
) {
    LaunchedEffect(dealId) { vm.load(dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showFollowUp by remember { mutableStateOf(false) }
    var showCallLog by remember { mutableStateOf(false) }
    var showBooking by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val d = state.deal
    // Messaging lives in Conversations, not here. This page still wants to know
    // whether anyone is waiting on a reply, so it keeps the counts — one number
    // on a button — and hands the actual talking off to the thread screen.
    val roster = rosterFor(
        viewer = DealRole.CP,
        hasCp = true,
        rawStatus = d?.status,
        customerName = d?.customerName,
    )
    val threadKeys = roster.map { threadKeyFor(DealRole.CP, it) }
    LaunchedEffect(d?.id) { if (d != null) vm.refreshUnread(threadKeys) }
    val unread = threadKeys.sumOf { state.unread[it] ?: 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(d?.projectName ?: "Lead", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = { nav.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Navy),
            )
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(dealId) }, modifier = Modifier.padding(inner))
            d != null -> LazyColumn(
                modifier = Modifier.padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The spine: where the deal is, and whether it is waiting on this
                // CP or on someone else. Previously this screen showed only a raw
                // status chip, so a CP could not tell a deal that needed them from
                // one that had been sitting with the builder for a fortnight.
                // When the baton is genuinely on the CP to agree, the spine owns
                // that action — one primary CTA per screen. Everywhere else
                // agreeing is still possible but demoted below, because it is not
                // what the deal is waiting for.
                val ownsAgree = batonOf(d.status, d.cpAgreed, d.customerConfirmed)
                    .heldBy(DealRole.CP) && !d.cpAgreed &&
                    canonicalStage(d.status) == "Agreement"
                item {
                    DealSpine(
                        rawStatus = d.status,
                        viewer = DealRole.CP,
                        cpAgreed = d.cpAgreed,
                        customerConfirmed = d.customerConfirmed,
                        actionLabel = if (ownsAgree) "Agree" else null,
                        onAction = if (ownsAgree) ({ vm.agree() }) else null,
                        onNudge = { vm.nudge() },
                    )
                }

                item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.customerName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Customer phone", d.customerPhone)
                        InfoRow("Deal value", d.dealValue?.let { formatINR(it) })
                        InfoRow("Your commission", d.commissionAmount?.let { "${formatINR(it)} (${d.commissionPercent ?: 0.0}%)" })
                        InfoRow("Commission status", d.commissionStatus)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            AgreedPill("You", d.cpAgreed)
                            Spacer(Modifier.width(8.dp))
                            AgreedPill("Customer", d.customerConfirmed)
                        }
                    }
                }

                // Booking the visit is how a lead actually moves — the spine says
                // "Request a site visit" from New Lead through Meeting Confirmed,
                // and until now there was nothing here to do it with. The CP had
                // to leave the lead, open the project and retype the customer's
                // name and phone. Same gate as the web journey tab.
                if (stageIndex(d.status) in 0..3) {
                    item {
                        Button(
                            onClick = { showBooking = true }, enabled = !state.working,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Schedule site visit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Agreeing early is legitimate — the endpoint sets cpAgreed *and*
                // moves the deal to Agreement — so this stays available. It is
                // just no longer a full-width primary competing with a spine that
                // says the deal is waiting on someone else.
                if (!d.cpAgreed && !ownsAgree) {
                    item {
                        OutlinedButton(
                            onClick = vm::agree, enabled = !state.working,
                            modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                "Agree and move to Agreement",
                                color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Messaging is one tap away rather than embedded: this opens the
                // deal's threads in Conversations, where the party is chosen and
                // the conversation is actually held.
                item {
                    OutlinedButton(
                        onClick = { nav.navigate(CpRoutes.conversations(d.id)) },
                        modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Teal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (unread > 0) "Message · $unread new" else "Message",
                            color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showFollowUp = true }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.EventRepeat, null, tint = Navy, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Follow-up", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { showCallLog = true }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.Phone, null, tint = Navy, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Log call", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (d.events.isNotEmpty()) {
                    item { ActivityLedger(d.events) }
                }
            }
        }
    }

    if (showFollowUp && d != null) {
        FollowUpDialog(working = state.working, onDismiss = { showFollowUp = false }) { date, time, reason ->
            vm.addFollowUp(date, time, reason); showFollowUp = false
        }
    }
    if (showCallLog && d != null) {
        CallLogDialog(working = state.working, onDismiss = { showCallLog = false }) { outcome, duration, notes ->
            vm.logCall(outcome, duration, notes, null, null); showCallLog = false
        }
    }
    if (showBooking && d != null) {
        val bookingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBooking = false },
            sheetState = bookingSheetState,
            containerColor = Color.White,
        ) {
            // The customer is fixed on a lead, so the sheet shows them rather
            // than asking the CP to type a name it already has.
            CpBookingSheet(
                projectName = d.projectName,
                working = state.working,
                initialName = d.customerName,
                initialPhone = d.customerPhone,
                customerLocked = true,
            ) { _, _, date, time, type, notes ->
                vm.bookVisit(date, time, type, notes) { showBooking = false }
            }
        }
    }
}

@Composable
private fun AgreedPill(who: String, agreed: Boolean) {
    val (fg, bg) = if (agreed) StatusColors.Green to StatusColors.GreenBg else StatusColors.Grey to StatusColors.GreyBg
    Text(
        "$who: ${if (agreed) "Agreed" else "Pending"}",
        color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}
