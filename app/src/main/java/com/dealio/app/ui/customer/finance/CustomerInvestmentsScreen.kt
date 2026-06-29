package com.dealio.app.ui.customer.finance

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush

private data class Investment(
    val name: String, val category: String, val returnMin: Double, val returnMax: Double,
    val lockInYears: Int, val minAmount: Long, val risk: String, val description: String,
)

private val opportunities = listOf(
    Investment("NRE Fixed Deposit", "Banking", 6.5, 7.5, 1, 10000, "Very Low", "Tax-free in India and fully repatriable. Start immediately."),
    Investment("EV Charging Stations", "Infrastructure", 15.0, 22.0, 3, 200000, "Medium", "5M EVs, only 12,000 charging points. Revenue from per-charge fees."),
    Investment("Retail Space Leasing", "Real Estate", 8.0, 12.0, 5, 1000000, "Low-Medium", "Buy a small retail shop in a new township. Dealio manages tenants."),
    Investment("Co-working Space", "Real Estate", 10.0, 15.0, 2, 300000, "Medium", "Co-working market growing 35% YoY in Hyderabad IT corridors."),
    Investment("Solar Rooftop Commercial", "Energy", 14.0, 18.0, 5, 150000, "Low", "Earn from power units sold + government subsidy. Zero maintenance."),
    Investment("Fractional CRE", "Real Estate", 12.0, 16.0, 3, 500000, "Low-Medium", "Own a fraction of Grade-A office space leased to MNCs."),
    Investment("Student Housing / PG", "Real Estate", 10.0, 14.0, 2, 800000, "Medium", "40M students, 30% in PGs. Buy a PG unit, Dealio manages it."),
    Investment("Cold Storage Units", "Infrastructure", 16.0, 20.0, 5, 500000, "Medium-High", "India wastes 40% of food. Government-backed cold-chain contracts."),
    Investment("Medical Equipment Leasing", "Healthcare", 15.0, 18.0, 3, 300000, "Medium", "Lease equipment to clinics. Healthcare demand is recession-proof."),
    Investment("Warehouse / Logistics", "Infrastructure", 12.0, 15.0, 5, 1000000, "Low", "E-commerce needs 3x more warehouse space by 2028. 3–5 yr contracts."),
)

@Composable
fun CustomerInvestmentsScreen(nav: NavController) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var monthlyInvest by remember { mutableFloatStateOf(50_000f) }
    var expectedReturn by remember { mutableFloatStateOf(15f) }

    val loanOutstanding = 1_58_00_000.0
    val yearsRemaining = 19
    val monthlyEmi = 1_38_500.0
    val monthlyReturn = (monthlyInvest * expectedReturn / 100 / 12).toDouble()
    val interestSaved = monthlyReturn * 12 * yearsRemaining * 0.45
    val yearsSaved = if (monthlyReturn > 0) (monthlyReturn / monthlyEmi * yearsRemaining * 0.8) else 0.0

    SubScreenScaffold("Investments", nav) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            // Tabs
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Active", "Planner", "Calculator").forEachIndexed { i, label ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                            .clickable { tab = i }.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    0 -> {
                        DealioCard {
                            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(54.dp).background(tintBrush(Teal), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Wallet, null, tint = Teal, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("No active investments yet", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("Once you start investing, your portfolio and returns appear here.", color = TextSecondary, fontSize = 12.sp)
                                Spacer(Modifier.height(14.dp))
                                GradientButton(text = "Explore investment planner", onClick = { tab = 1 })
                            }
                        }
                    }
                    1 -> {
                        Box(Modifier.fillMaxWidth().background(Color(0xFFF1F4F8), RoundedCornerShape(12.dp)).padding(14.dp)) {
                            Text("Home loan at 8.5% → invest idle savings at 15% → net benefit 6.5%/yr → prepay loan → save 3–5 years of EMIs.", color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                        opportunities.forEach { InvestmentCard(it) }
                        Box(Modifier.fillMaxWidth().background(Color(0xFFFDF3E7), RoundedCornerShape(12.dp)).padding(14.dp)) {
                            Column {
                                Text("💡 Combine strategies", color = StatusColors.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text("₹2L in EV Charging (18%) + ₹1L in NRE FD (7.25%) = 14.5% blended. Use returns to close your loan ~4 years early.", color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                        GradientButton(text = "Talk to an investment advisor", onClick = { Toast.makeText(ctx, "Our advisor will call you within 24 hours.", Toast.LENGTH_SHORT).show() })
                        Spacer(Modifier.height(4.dp))
                    }
                    else -> {
                        DealioCard {
                            Text("Loan Offset Calculator", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Invest idle savings above your 8.5% loan rate and prepay your EMI.", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(14.dp))
                            SliderRow("Monthly investment", formatINR(monthlyInvest.toDouble()), monthlyInvest, 5_000f..2_00_000f) { monthlyInvest = it }
                            SliderRow("Expected return", "${expectedReturn.toInt()}% p.a.", expectedReturn, 8f..22f) { expectedReturn = it }
                            Spacer(Modifier.height(8.dp))
                            Column(Modifier.fillMaxWidth().background(Color(0xFFF1F4F8), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                KvRow("Loan outstanding", formatINR(loanOutstanding))
                                KvRow("Years remaining", "$yearsRemaining yrs")
                                KvRow("Monthly EMI", formatINR(monthlyEmi))
                            }
                            Spacer(Modifier.height(12.dp))
                            Column(Modifier.fillMaxWidth().background(tintBrush(Teal), RoundedCornerShape(14.dp)).padding(16.dp)) {
                                Text("MONTHLY INVESTMENT RETURNS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("${formatINR(monthlyReturn)}/mo", color = Teal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Bolt, null, tint = StatusColors.Amber, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Loan closes ${"%.1f".format(yearsSaved)} years early", color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.TrendingUp, null, tint = StatusColors.Green, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save ${formatINR(interestSaved)} in interest", color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentCard(inv: Investment) {
    DealioCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(inv.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(inv.category, color = TextSecondary, fontSize = 11.sp)
            }
            Box(Modifier.background(tintBrush(Teal), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("${inv.returnMin.toInt()}–${inv.returnMax.toInt()}%", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(inv.description, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("${inv.lockInYears} yr lock-in")
            Chip(inv.risk)
            Chip("Min ${formatINR(inv.minAmount.toDouble())}")
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(Modifier.background(Color(0xFFF1F4F8), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KvRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = current, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal, inactiveTrackColor = Teal.copy(alpha = 0.18f)))
    }
}
