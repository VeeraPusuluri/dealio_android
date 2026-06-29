package com.dealio.app.ui.cp.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MONTHLY_GOAL = 5

private data class TierStyle(val fg: Color, val bg: Color)
private val tierStyles = mapOf(
    "Platinum" to TierStyle(Color(0xFF7C3AED), Color(0xFFF1ECFD)),
    "Gold" to TierStyle(Color(0xFFD97706), Color(0xFFFDF3E7)),
    "Silver" to TierStyle(Color(0xFF64748B), Color(0xFFF1F4F8)),
    "Bronze" to TierStyle(Color(0xFFEA580C), Color(0xFFFFF1E9)),
)

@Composable
fun LeaderboardScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    SubScreenScaffold("Leaderboard", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }

        val leads = state.leads
        val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val bookedTotal = leads.count { it.status == "Booked" }
        val bookedMonth = leads.count { it.status == "Booked" && it.createdAt.take(7) == ym }
        val activeLeads = leads.count { it.status != "Booked" && it.status != "Closed" }
        val totalEarnings = leads.filter { it.status == "Booked" }.sumOf { it.estimatedCommission ?: 0.0 }
        val goalPct = (bookedMonth.toFloat() / MONTHLY_GOAL).coerceAtMost(1f)
        val tier = when {
            bookedTotal >= 20 -> "Platinum"; bookedTotal >= 10 -> "Gold"; bookedTotal >= 5 -> "Silver"; else -> "Bronze"
        }
        val ts = tierStyles.getValue(tier)
        val name = state.profile?.fullName ?: "Channel Partner"

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Performance card
            DealioCard(contentPadding = 0.dp) {
                Row(
                    Modifier.fillMaxWidth().background(NavyTealGradient, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(54.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text(initialsOf(name), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(state.profile?.cp?.city ?: "Channel Partner", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.background(ts.bg, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text("$tier Tier", color = ts.fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Earnings", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                        Text(fmtShortRupee(totalEarnings), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    PerfStat("Total Deals", bookedTotal.toString(), Icons.Outlined.CheckCircle, StatusColors.Green, Modifier.weight(1f))
                    PerfStat("This Month", bookedMonth.toString(), Icons.Outlined.GpsFixed, Teal, Modifier.weight(1f))
                    PerfStat("Active Leads", activeLeads.toString(), Icons.Outlined.AccessTime, StatusColors.Amber, Modifier.weight(1f))
                }
            }

            // Monthly challenge
            DealioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TrendingUp, null, tint = StatusColors.Amber, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Monthly Challenge", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Close $MONTHLY_GOAL deals this month to unlock the next tier and earn a bonus.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(10.dp).background(Color(0xFFEDF1F7), RoundedCornerShape(5.dp))) {
                        Box(Modifier.fillMaxWidth(goalPct).height(10.dp).background(StatusColors.Amber, RoundedCornerShape(5.dp)))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("$bookedMonth/$MONTHLY_GOAL", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (bookedMonth >= MONTHLY_GOAL) {
                    Spacer(Modifier.height(8.dp))
                    Text("Goal achieved this month! 🎉", color = StatusColors.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tier guide
            DealioCard {
                Text("Tier requirements", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                listOf("Bronze" to "0–4", "Silver" to "5–9", "Gold" to "10–19", "Platinum" to "20+").forEachIndexed { i, (t, deals) ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    val style = tierStyles.getValue(t)
                    val current = t == tier
                    Row(
                        Modifier.fillMaxWidth()
                            .background(style.bg, RoundedCornerShape(12.dp))
                            .then(if (current) Modifier.border(2.dp, Teal.copy(alpha = 0.4f), RoundedCornerShape(12.dp)) else Modifier)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t, color = style.fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("$deals deals", color = style.fg, fontSize = 11.sp)
                        if (current) {
                            Spacer(Modifier.width(8.dp))
                            Text("Current", color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Platform leaderboard placeholder
            DealioCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.EmojiEvents, null, tint = StatusColors.Amber, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Platform Leaderboard", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Rankings across all channel partners are coming soon. Your real-time rank will appear here.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    ComingSoonPill()
                }
            }
        }
    }
}

@Composable
private fun PerfStat(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier) {
    Column(modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}
