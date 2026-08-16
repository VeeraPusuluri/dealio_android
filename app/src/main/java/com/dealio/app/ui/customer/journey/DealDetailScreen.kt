package com.dealio.app.ui.customer.journey

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.customer.CustomerRoutes
import androidx.compose.material3.OutlinedButton
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.DealSpine
import com.dealio.app.ui.flow.StageActionCard
import com.dealio.app.ui.flow.StageTarget
import com.dealio.app.ui.flow.batonOf
import com.dealio.app.ui.flow.ThreadTarget
import com.dealio.app.ui.flow.rosterFor
import com.dealio.app.ui.flow.threadKeyFor
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailScreen(
    nav: NavController,
    dealId: Long,
    /** Which party's thread to open on, when arrived at from the inbox. */
    initialThread: String? = null,
    vm: DealDetailViewModel = viewModel(),
) {
    LaunchedEffect(dealId) { vm.load(dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

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
    val selected = target
        ?: roster.firstOrNull { it.recipientRole == initialThread }
        ?: roster.firstOrNull()

    val pickAgreement = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.uploadSignedAgreement(it) }
    }

    // True when the deal is genuinely waiting on the buyer and there is a
    // confirm to make — the one case where the spine should carry the action.
    val ownsConfirm = d != null &&
        batonOf(d.dealStatus, d.cpAgreed, d.customerConfirmed).heldBy(DealRole.CUSTOMER) &&
        showConfirmFor(d)

    val threadKeys = roster.map { threadKeyFor(DealRole.CUSTOMER, it) }
    // One number on one button, the way the CP page does it — the page keeps the
    // counts but hands the actual talking off to the thread screen.
    val unread = threadKeys.sumOf { state.unread[it] ?: 0 }
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

                // What the buyer may do at this stage, from the table all three
                // portals read — in the buyer's register, and never a second
                // booking: once the visit is requested or confirmed the only
                // thing offered is the visit itself, on the Visits screen.
                item {
                    StageActionCard(
                        rawStatus = d.dealStatus,
                        viewer = DealRole.CUSTOMER,
                        enabled = !state.working,
                    ) { target ->
                        when (target) {
                            StageTarget.CUSTOMER_VISITS -> nav.navigate(CustomerRoutes.VISITS)
                            StageTarget.CUSTOMER_PROJECT -> nav.navigate(CustomerRoutes.projectDetail(d.projectId))
                            StageTarget.CUSTOMER_LOAN -> nav.navigate(CustomerRoutes.loanApply(d.projectId))
                            // Any document type: buyers send back a scan, a photo
                            // of the signed pages, or the PDF they were sent.
                            StageTarget.UPLOAD_SIGNED_AGREEMENT ->
                                pickAgreement.launch(arrayOf("application/pdf", "image/*"))
                            else -> Unit
                        }
                    }
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

                // Messaging is one tap away rather than embedded: this opens
                // the deal's threads in Conversations, where the party is chosen
                // and the conversation is actually held. The buyer's page used to
                // carry the whole thread plus a composer pinned to the bottom,
                // which is why this screen — alone among the three — could not
                // show anything else down there.
                item {
                    OutlinedButton(
                        onClick = { nav.navigate(CustomerRoutes.CONVERSATIONS) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Teal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (unread > 0) "Message · $unread new" else "Message",
                            color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
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
