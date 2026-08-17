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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.dealio.app.ui.flow.StageActionCard
import com.dealio.app.ui.flow.stageActionFor
import com.dealio.app.ui.flow.StageTarget
import com.dealio.app.ui.flow.canonicalStage
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.theme.Navy
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun DealDetailScreen(
    nav: NavController,
    dealId: Long,
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

                    // What the builder may do at this stage, from the table all
                    // three portals read. It matters most around the visit: the
                    // slot the CP is waiting on is confirmed on the meetings
                    // screen, and nothing on this page said so.
                    StageActionCard(
                        rawStatus = d.status,
                        viewer = DealRole.BUILDER,
                        enabled = !state.working,
                    ) { target ->
                        when (target) {
                            StageTarget.BUILDER_MEETINGS -> nav.navigate(BuilderRoutes.MEETINGS)
                            StageTarget.BUILDER_SHORTLISTS -> nav.navigate(BuilderRoutes.SHORTLISTS)
                            StageTarget.BUILDER_COMMISSIONS -> nav.navigate(BuilderRoutes.COMMISSIONS)
                            StageTarget.BUILDER_ACCEPT_AGREEMENT -> vm.acceptAgreement()
                            else -> Unit
                        }
                    }

                    DealioCard {
                        SectionLabel("Advance")
                        Spacer(Modifier.height(8.dp))
                        val idx = DEAL_STAGES.indexOf(canonicalStage(d.status))
                        val next = if (idx in 0 until DEAL_STAGES.lastIndex) DEAL_STAGES[idx + 1] else null
                        // When the stage card above already offers the proper move
                        // for this stage, the generic advance is not a second way
                        // to do it — it is a way to do it *without* the checks that
                        // move carries. At Agreement it would move the deal to
                        // Pending Booking with no signed copy on the row and nobody
                        // told, so it stands down and leaves the countersign to it.
                        val stageOwnsAdvance =
                            stageActionFor(d.status, DealRole.BUILDER).cta?.target == StageTarget.BUILDER_ACCEPT_AGREEMENT
                        if (stageOwnsAdvance) {
                            Text(
                                "Countersign the agreement above to move this deal on.",
                                color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp,
                            )
                        } else if (next != null) {
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

                    // Messaging is one tap away rather than embedded, the way the
                    // CP page already does it. The party rail and composer that
                    // used to sit here were wedged between the commission figures
                    // and the activity ledger, and they belonged to a model where
                    // a conversation was part of a deal. It is not: the buyer and
                    // the partner on this deal are the same people on every other
                    // one, and there is one conversation with each of them.
                    OutlinedButton(
                        onClick = { nav.navigate(BuilderRoutes.CONVERSATIONS) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Teal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Message", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
