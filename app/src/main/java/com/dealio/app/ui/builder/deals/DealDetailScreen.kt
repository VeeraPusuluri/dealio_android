package com.dealio.app.ui.builder.deals

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.DealDetail
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.flow.ActivityLedger
import com.dealio.app.ui.flow.DEAL_STAGES
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.flow.DealSpine
import com.dealio.app.ui.flow.PartyRail
import com.dealio.app.ui.flow.ThreadTarget
import com.dealio.app.ui.flow.canonicalStage
import com.dealio.app.ui.flow.rosterFor
import com.dealio.app.ui.flow.threadKeyFor
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun DealDetailScreen(
    nav: NavController,
    dealId: Long,
    /** Which party's thread to open on, when arrived at from the inbox. */
    initialThread: String? = null,
    vm: DealDetailViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(dealId) { vm.load(dealId) }

    SubScreenScaffold(title = state.deal?.customerName ?: "Deal", nav = nav) { pad ->
        when {
            state.loading -> LoadingState(Modifier.padding(pad))
            state.error != null -> ErrorState(state.error!!, { vm.load(dealId) }, Modifier.padding(pad))
            state.deal != null -> {
                val d = state.deal!!
                var message by remember { mutableStateOf("") }
                // Which thread the builder is looking at. Pre-booking with a CP
                // attached the private customer pair is withheld, so the roster
                // offers the group instead — matching the backend exactly.
                val roster = rosterFor(
                    viewer = DealRole.BUILDER,
                    hasCp = d.cpName != null,
                    rawStatus = d.status,
                    cpName = d.cpName,
                    customerName = d.customerName,
                )
                var target by remember(dealId) { mutableStateOf<ThreadTarget?>(null) }
                val selected = target
                    ?: roster.firstOrNull { it.recipientRole == initialThread }
                    ?: roster.firstOrNull()

                val threadKeys = roster.map { threadKeyFor(DealRole.BUILDER, it) }
                LaunchedEffect(d.id, d.messages.size) { vm.refreshUnread(threadKeys) }
                LaunchedEffect(selected?.recipientRole, d.id) {
                    if (selected != null) vm.markThreadRead(threadKeyFor(DealRole.BUILDER, selected))
                }
                Column(
                    Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Header
                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(d.customerName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(d.projectName, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                        if ((d.dealValue ?: 0.0) > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("Deal value ${formatINRShort(d.dealValue)}", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // The spine, then the builder's own controls. The stepper this
                    // replaces ran on a six-stage list starting at "Meeting Done",
                    // so an early deal matched nothing: no progress shown, and no
                    // advance button at all. The canonical ladder covers all ten.
                    DealSpine(
                        rawStatus = d.status,
                        viewer = DealRole.BUILDER,
                        cpAgreed = d.cpAgreed,
                        customerConfirmed = d.customerConfirmed,
                        onNudge = { vm.nudge() },
                    )

                    DealioCard {
                        SectionLabel("Advance")
                        Spacer(Modifier.height(8.dp))
                        val idx = DEAL_STAGES.indexOf(canonicalStage(d.status))
                        val next = if (idx in 0 until DEAL_STAGES.lastIndex) DEAL_STAGES[idx + 1] else null
                        if (next != null) {
                            ActionButton("Advance to $next", Navy, enabled = !state.working) { vm.updateStatus(next) }
                        } else {
                            Text("This deal is complete.", color = TextSecondary, fontSize = 13.sp)
                        }
                        if (d.status.equals("Pending Booking", true) || d.status.equals("Booked", true)) {
                            Spacer(Modifier.height(8.dp))
                            ActionButton("Mark unit SOLD", StatusColors.Green, enabled = !state.working) { vm.markSold() }
                        }
                    }

                    // Parties
                    DealioCard {
                        SectionLabel("Parties")
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Customer", d.customerName)
                        InfoRow("Phone", d.customerPhone.ifBlank { "Contact via channel partner" })
                        InfoRow("Channel partner", d.cpName ?: "Direct")
                        InfoRow("CP phone", d.cpPhone)
                        InfoRow("CP tier", d.cpTier)
                        if (d.customerPhone.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth().height(44.dp).border(1.dp, Teal, RoundedCornerShape(12.dp))
                                    .clickable { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${d.customerPhone}"))) },
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Call, null, tint = Teal, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Call customer", color = Teal, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    // Commission
                    DealioCard {
                        SectionLabel("Commission")
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Rate", d.commissionPercent?.let { "$it%" })
                        InfoRow("Amount", d.commissionAmount?.let { formatINRShort(it) })
                        InfoRow("Status", d.commissionStatus)
                        InfoRow("CP agreed", if (d.cpAgreed) "Yes" else "No")
                        InfoRow("Customer confirmed", if (d.customerConfirmed) "Yes" else "No")
                    }

                    // Payment schedule
                    if (!d.paymentSchedule.isNullOrEmpty()) {
                        DealioCard {
                            SectionLabel("Payment schedule")
                            Spacer(Modifier.height(8.dp))
                            d.paymentSchedule!!.forEach { p ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.installment, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(com.dealio.app.ui.builder.formatDate(p.dueDate), color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Text(formatINRShort(p.amount), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(8.dp))
                                    StatusChip(p.status)
                                }
                            }
                        }
                    }

                    // Documents
                    DealioCard {
                        SectionLabel("Documents (${d.dealDocuments.size})")
                        Spacer(Modifier.height(8.dp))
                        if (d.dealDocuments.isEmpty()) {
                            Text("No documents shared yet.", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            d.dealDocuments.forEach { doc ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Description, null, tint = Teal, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(doc.name.ifBlank { doc.docType }, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(doc.docType, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    if (doc.sharedWithCustomer) StatusChip("Customer")
                                    if (doc.sharedWithCp) { Spacer(Modifier.width(4.dp)); StatusChip("CP") }
                                }
                            }
                        }
                    }

                    if (d.events.isNotEmpty()) {
                        DealioCard { ActivityLedger(d.events) }
                    }

                    // Messages — one thread at a time. The backend only returns
                    // threads this builder is party to; the rail picks between them.
                    DealioCard {
                        SectionLabel("Conversation")
                        Spacer(Modifier.height(8.dp))
                        PartyRail(
                            targets = roster,
                            selected = selected,
                            onSelect = { target = it },
                            unreadOf = { state.unread[threadKeyFor(DealRole.BUILDER, it)] ?: 0 },
                        )
                        Spacer(Modifier.height(12.dp))
                        val thread = selected?.let { threadKeyFor(DealRole.BUILDER, it) }
                        val shown = d.messages.filter { thread == null || it.threadKey == thread }
                        if (shown.isEmpty()) {
                            Text(
                                if (selected?.isGroup == true) "No messages in the group thread yet."
                                else "No messages with ${selected?.label ?: "this party"} yet.",
                                color = TextSecondary, fontSize = 13.sp,
                            )
                        } else {
                            shown.forEach { m ->
                                val mine = m.senderRole.equals("builder", true)
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                                ) {
                                    Text("${m.senderName} · ${m.senderRole}", color = TextSecondary, fontSize = 10.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        Modifier
                                            .background(if (mine) Teal.copy(alpha = 0.14f) else CardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) { Text(m.message, color = TextPrimary, fontSize = 13.sp) }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = message, onValueChange = { message = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        if (selected?.isGroup == true) "Message all three…"
                                        else "Message ${selected?.label ?: "…"}",
                                    )
                                },
                                singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(48.dp).background(Navy, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !state.sending && message.isNotBlank()) {
                                        vm.sendMessage(message, selected?.recipientRole ?: "cp"); message = ""
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(46.dp)
            .background(if (enabled) color else color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}
