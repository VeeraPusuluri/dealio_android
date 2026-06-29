package com.dealio.app.ui.builder.demandletters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.dealio.app.data.api.DealSummary
import com.dealio.app.data.api.Installment
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

private val ACTIVE = setOf("Booked", "Negotiation", "Agreement", "Loan Application Created", "Loan Sanctioned")

@Composable
fun DemandLettersScreen(nav: NavController, vm: com.dealio.app.ui.builder.tools.BuilderToolsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableLongStateOf(-1L) }

    SubScreenScaffold("Demand Letters", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val deals = state.deals.filter { it.status in ACTIVE }

        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Payment schedules for active deals (Booked and later).", color = TextSecondary, fontSize = 12.sp)
            }
            if (deals.isEmpty()) {
                item { DealioCard { EmptyState(Icons.Outlined.ReceiptLong, "No active deals", "Demand letters are available for Booked and later stages.") } }
            } else {
                items(deals.size) { i ->
                    val d = deals[i]
                    val sched = d.paymentSchedule ?: emptyList()
                    val paid = sched.filter { it.status.equals("Paid", true) }.sumOf { it.amount }
                    val pending = sched.filter { !it.status.equals("Paid", true) }.sumOf { it.amount }
                    DealioCard(onClick = { expanded = if (expanded == d.id) -1L else d.id }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(d.customerName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${d.projectName} · ${d.status}", color = TextSecondary, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(d.dealValue?.let { formatINRShort(it) } ?: "—", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${sched.count { it.status.equals("Paid", true) }}/${sched.size} paid", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        AnimatedVisibility(expanded == d.id) {
                            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Stat("Value", d.dealValue?.let { formatINRShort(it) } ?: "—", TextPrimary, Modifier.weight(1f))
                                    Stat("Received", formatINRShort(paid), Color(0xFF047857), Modifier.weight(1f))
                                    Stat("Pending", formatINRShort(pending), Color(0xFFB45309), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(10.dp))
                                if (sched.isEmpty()) {
                                    Text("No demand letters recorded for this deal yet.", color = TextSecondary, fontSize = 12.sp)
                                } else {
                                    sched.forEachIndexed { idx, inst ->
                                        if (idx > 0) Spacer(Modifier.height(8.dp))
                                        InstallmentRow(inst)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.background(Color(0xFFF7FAFB), RoundedCornerShape(12.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun InstallmentRow(inst: Installment) {
    val overdue = inst.status.equals("Pending", true) && isPast(inst.dueDate)
    val (fg, bg, label) = when {
        inst.status.equals("Paid", true) -> Triple(Color(0xFF047857), Color(0xFFD1FAE5), "Paid")
        overdue -> Triple(Color(0xFFB91C1C), Color(0xFFFEE2E2), "Overdue")
        else -> Triple(Color(0xFFB45309), Color(0xFFFEF3C7), "Pending")
    }
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF7FAFB), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(inst.installment.ifBlank { "Installment" }, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${formatINR(inst.amount)} · due ${inst.dueDate}", color = TextSecondary, fontSize = 11.sp)
        }
        Box(Modifier.background(bg, RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
            Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun isPast(date: String): Boolean = runCatching {
    val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(date.take(10)) ?: return false
    d.before(java.util.Date())
}.getOrDefault(false)
