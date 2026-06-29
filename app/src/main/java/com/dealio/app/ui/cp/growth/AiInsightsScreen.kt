package com.dealio.app.ui.cp.growth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.dealio.app.data.api.CpLead
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val statusScore = mapOf(
    "Negotiation" to 88, "Meeting Done" to 72, "Meeting Confirmed" to 62,
    "Meeting Requested" to 52, "Profile Created" to 36, "New Lead" to 22, "Booked" to 95,
)
private val nextAction = mapOf(
    "New Lead" to "Call to introduce yourself and understand their requirements",
    "Profile Created" to "Share the project brochure and highlight key features via WhatsApp",
    "Meeting Requested" to "Coordinate with the builder to confirm the site-visit date",
    "Meeting Confirmed" to "Send a warm reminder 24 hours before the site visit",
    "Meeting Done" to "Follow up with pricing, payment plan and available configurations",
    "Negotiation" to "Share special offers and flexible payment plans to close the deal",
    "Booked" to "Congratulate and assist with documentation and loan requirements",
)

private data class Heat(val fg: Color, val bg: Color, val bar: Color)
private val hot = Heat(Color(0xFFB91C1C), Color(0xFFFEE2E2), Color(0xFFEF4444))
private val warm = Heat(Color(0xFFB45309), Color(0xFFFEF3C7), Color(0xFFF59E0B))
private val cold = Heat(Color(0xFF1D4ED8), Color(0xFFDBEAFE), Color(0xFF3B82F6))

private data class Scored(val lead: CpLead, val score: Int, val label: String, val heat: Heat)

private fun daysSince(createdAt: String): Long {
    return runCatching {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(createdAt.take(10)) ?: return 0
        (System.currentTimeMillis() - d.time) / 86_400_000L
    }.getOrDefault(0)
}

private fun scoreOf(l: CpLead): Scored {
    val base = statusScore[l.status] ?: 20
    val decay = (daysSince(l.createdAt) * 0.5).coerceAtMost(20.0)
    val score = max(5, (base - decay).roundToInt())
    val label = when { score >= 70 -> "Hot"; score >= 45 -> "Warm"; else -> "Cold" }
    val heat = when (label) { "Hot" -> hot; "Warm" -> warm; else -> cold }
    return Scored(l, score, label, heat)
}

@Composable
fun AiInsightsScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("All") }
    var expandedId by remember { mutableLongStateOf(-1L) }
    val ctx = LocalContext.current

    SubScreenScaffold("AI Lead Intelligence", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }

        val scored = state.leads.filter { it.status != "Closed" }.map { scoreOf(it) }
        val hotN = scored.count { it.label == "Hot" }
        val warmN = scored.count { it.label == "Warm" }
        val coldN = scored.count { it.label == "Cold" }
        val shown = (if (filter == "All") scored else scored.filter { it.label == filter }).sortedByDescending { it.score }

        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Psychology, null, tint = com.dealio.app.ui.theme.Teal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scoring based on lead stage and recency", color = TextSecondary, fontSize = 12.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Active", scored.size.toString(), TextPrimary, Modifier.weight(1f))
                    MiniStat("Hot", hotN.toString(), hot.fg, Modifier.weight(1f))
                    MiniStat("Warm", warmN.toString(), warm.fg, Modifier.weight(1f))
                    MiniStat("Cold", coldN.toString(), cold.fg, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Hot", "Warm", "Cold").forEach { f ->
                        val sel = filter == f
                        Box(
                            Modifier.background(if (sel) com.dealio.app.ui.theme.Teal else Color.White, RoundedCornerShape(10.dp))
                                .border(1.dp, if (sel) com.dealio.app.ui.theme.Teal else Color(0xFFE3E9F1), RoundedCornerShape(10.dp))
                                .clickable { filter = f }.padding(horizontal = 16.dp, vertical = 7.dp),
                        ) {
                            Text(f, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (shown.isEmpty()) {
                item { DealioCard { EmptyState(Icons.Outlined.Psychology, "No active leads", "Add leads from the Projects page to see AI scoring.") } }
            } else {
                items(shown.size) { i ->
                    val s = shown[i]
                    val expanded = expandedId == s.lead.id
                    DealioCard(onClick = { expandedId = if (expanded) -1L else s.lead.id }, contentPadding = 0.dp) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(s.heat.bg, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                                Text(s.lead.customerName.take(1).uppercase(), color = s.heat.fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.lead.customerName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.background(s.heat.bg, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("${s.score} · ${s.label}", color = s.heat.fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("${s.lead.projectName} · ${s.lead.status}", color = TextSecondary, fontSize = 11.sp)
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFEDF1F7), RoundedCornerShape(3.dp))) {
                                    Box(Modifier.fillMaxWidth(s.score / 100f).height(6.dp).background(s.heat.bar, RoundedCornerShape(3.dp)))
                                }
                            }
                            Icon(Icons.Outlined.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        AnimatedVisibility(expanded) {
                            Column(Modifier.fillMaxWidth().background(Color(0xFFF7FAFB)).padding(14.dp)) {
                                nextAction[s.lead.status]?.let { action ->
                                    Row(Modifier.fillMaxWidth().background(Color(0xFFEAFAFC), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                        Icon(Icons.Outlined.Bolt, null, tint = com.dealio.app.ui.theme.Teal, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text("NEXT BEST ACTION", color = com.dealio.app.ui.theme.Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                            Spacer(Modifier.height(2.dp))
                                            Text(action, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                                if (s.lead.customerPhone.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ActionBtn("Call", Icons.Outlined.Call, Color(0xFF059669)) { dial(ctx, s.lead.customerPhone) }
                                        ActionBtn("WhatsApp", Icons.Outlined.Sms, Color(0xFF25D366)) {
                                            openWhatsApp(ctx, s.lead.customerPhone, "Hi ${s.lead.customerName}, ${nextAction[s.lead.status] ?: "just checking in on your property search!"}")
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
}

@Composable
private fun MiniStat(label: String, value: String, accent: Color, modifier: Modifier) {
    DealioCard(modifier = modifier, contentPadding = 12.dp) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.background(color, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
