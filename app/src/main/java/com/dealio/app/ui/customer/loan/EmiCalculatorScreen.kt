package com.dealio.app.ui.customer.loan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrendingDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlin.math.pow

private data class YearBreak(val year: Int, val principal: Double, val interest: Double)
private data class SchedRow(val month: Int, val principal: Double, val interest: Double, val outstanding: Double)

private fun emiOf(principal: Double, annualRate: Double, months: Int): Double {
    val r = annualRate / 12.0 / 100.0
    return if (r > 0) principal * r * (1 + r).pow(months.toDouble()) / ((1 + r).pow(months.toDouble()) - 1)
    else principal / months
}

private fun buildSchedule(amount: Double, monthlyRate: Double, emi: Double, months: Int): List<SchedRow> {
    var outstanding = amount
    return (1..months).map { m ->
        val interest = outstanding * monthlyRate
        val principal = emi - interest
        outstanding = (outstanding - principal).coerceAtLeast(0.0)
        SchedRow(m, principal, interest, outstanding)
    }
}

private fun buildYearly(amount: Double, monthlyRate: Double, emi: Double, years: Int): List<YearBreak> {
    var balance = amount
    return (1..years).map { y ->
        var principal = 0.0; var interest = 0.0
        repeat(12) {
            val intPart = balance * monthlyRate
            val prinPart = emi - intPart
            principal += prinPart; interest += intPart; balance -= prinPart
        }
        YearBreak(y, principal, interest)
    }
}

@Composable
fun EmiCalculatorScreen(nav: NavController) {
    var amount by remember { mutableFloatStateOf(50_00_000f) }
    var rate by remember { mutableFloatStateOf(8.65f) }
    var tenure by remember { mutableFloatStateOf(20f) }
    var tab by remember { mutableIntStateOf(0) }

    val months = (tenure * 12).toInt().coerceAtLeast(1)
    val monthlyRate = rate / 12.0 / 100.0
    val emi = emiOf(amount.toDouble(), rate.toDouble(), months)
    val totalPayable = emi * months
    val totalInterest = (totalPayable - amount).coerceAtLeast(0.0)
    val interestPct = if (totalPayable > 0) totalInterest / totalPayable * 100 else 0.0
    val principalPct = 100.0 - interestPct

    val yearly = remember(amount, rate, tenure) { buildYearly(amount.toDouble(), monthlyRate, emi, tenure.toInt()) }
    val schedule = remember(amount, rate, tenure) { buildSchedule(amount.toDouble(), monthlyRate, emi, months) }

    SubScreenScaffold("EMI Calculator", nav) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── EMI hero ──
            item {
                Box(
                    Modifier.fillMaxWidth()
                        .background(NavyTealGradient, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                ) {
                    Column {
                        Text("MONTHLY EMI", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(formatINR(emi), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("per month for ${tenure.toInt()} years", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            // ── Total payable / interest ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Total Payable", formatINRShort(totalPayable), Icons.Outlined.AccountBalance, Teal, Modifier.weight(1f))
                    MetricCard("Total Interest", formatINRShort(totalInterest), Icons.Outlined.TrendingDown, Orange, Modifier.weight(1f))
                }
            }

            // ── Inputs + composition ──
            item {
                DealioCard {
                    SectionLabel("Loan parameters")
                    Spacer(Modifier.height(12.dp))
                    SliderRow("Loan amount", formatINRShort(amount.toDouble()), amount, 10_00_000f..10_00_00_000f, 500_000f) { amount = it }
                    SliderRow("Interest rate (p.a.)", "${"%.2f".format(rate)}%", rate, 7f..15f, 0.05f) { rate = it }
                    SliderRow("Loan tenure", "${tenure.toInt()} yr", tenure, 5f..30f, 1f) { tenure = it }

                    Spacer(Modifier.height(8.dp))
                    Text("LOAN COMPOSITION", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                            CompositionDonut(principalPct.toFloat())
                            Text("${principalPct.toInt()}%", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendRow(Teal, "Principal", "${principalPct.toInt()}%")
                            LegendRow(Orange, "Interest", "${interestPct.toInt()}%")
                        }
                    }
                }
            }

            // ── Repayment breakdown with tabs ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Repayment breakdown")
                    Row(
                        Modifier.background(Color(0xFFEDF1F7), RoundedCornerShape(9.dp)).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SegTab("Chart", tab == 0) { tab = 0 }
                        SegTab("Schedule", tab == 1) { tab = 1 }
                    }
                }
            }

            if (tab == 0) {
                item {
                    DealioCard {
                        YearlyBars(yearly)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            LegendDot(Teal, "Principal")
                            LegendDot(Orange, "Interest")
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    DealioCard(contentPadding = 0.dp) {
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFFF1F4F8)).padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            SchedHead("Mo", 0.6f); SchedHead("Principal", 1.3f); SchedHead("Interest", 1.3f); SchedHead("Balance", 1.4f)
                        }
                    }
                }
                items(schedule.size) { i ->
                    val row = schedule[i]
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${row.month}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                        Text(formatINRShort(row.principal), color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.3f))
                        Text(formatINRShort(row.interest), color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.3f))
                        Text(formatINRShort(row.outstanding), color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1.4f))
                    }
                }
            }

            // ── Info strip ──
            item {
                Column(
                    Modifier.fillMaxWidth().background(Teal.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoLine(Icons.Outlined.CalendarMonth, Teal, "Loan closes in ${tenure.toInt()} years ($months EMIs)")
                    InfoLine(Icons.Outlined.Info, Orange, "Interest is ${interestPct.toInt()}% of your total outflow")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    DealioCard(modifier = modifier, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 11.sp)
                Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, step: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        val steps = (((range.endInclusive - range.start) / step).toInt() - 1).coerceAtLeast(0)
        Slider(
            value = current, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal, inactiveTrackColor = Teal.copy(alpha = 0.18f)),
        )
    }
}

@Composable
private fun CompositionDonut(principalPct: Float) {
    Canvas(Modifier.size(96.dp)) {
        val stroke = Stroke(width = 20.dp.toPx())
        val inset = 20.dp.toPx() / 2
        val arcSize = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx())
        val topLeft = Offset(inset, inset)
        val sweepP = principalPct / 100f * 360f
        drawArc(Orange, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke, topLeft = topLeft, size = arcSize)
        drawArc(Teal, startAngle = -90f, sweepAngle = sweepP, useCenter = false, style = stroke, topLeft = topLeft, size = arcSize)
    }
}

@Composable
private fun YearlyBars(data: List<YearBreak>) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { it.principal + it.interest }.coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val slot = size.width / data.size
        val barW = slot * 0.6f
        data.forEachIndexed { i, d ->
            val x = i * slot + (slot - barW) / 2
            val totalH = ((d.principal + d.interest) / maxVal * size.height).toFloat()
            val pH = (d.principal / maxVal * size.height).toFloat()
            drawRect(Orange, topLeft = Offset(x, size.height - totalH), size = Size(barW, (totalH - pH).coerceAtLeast(0f)))
            drawRect(Teal, topLeft = Offset(x, size.height - pH), size = Size(barW, pH))
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, pct: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Text(pct, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun SegTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SchedHead(text: String, weight: Float) {
    Text(text, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(weight))
}

@Composable
private fun InfoLine(icon: ImageVector, accent: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}
