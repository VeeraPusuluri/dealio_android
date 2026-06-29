package com.dealio.app.ui.cp.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Facebook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CpLead
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class MonthPoint(val label: String, val count: Int)

private fun monthlyTrend(leads: List<CpLead>): List<MonthPoint> {
    val ymFmt = SimpleDateFormat("yyyy-MM", Locale.US)
    val labelFmt = SimpleDateFormat("MMM", Locale.US)
    return (5 downTo 0).map { back ->
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -back) }
        val ym = ymFmt.format(cal.time)
        MonthPoint(labelFmt.format(cal.time), leads.count { it.createdAt.take(7) == ym })
    }
}

@Composable
fun SocialAnalyticsScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    SubScreenScaffold("Social Analytics", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }

        val leads = state.leads
        val total = leads.size
        val booked = leads.count { it.status == "Booked" }
        val active = leads.count { it.status != "Booked" && it.status != "Closed" }
        val conv = if (total > 0) "%.1f".format(booked * 100.0 / total) else "0"
        val trend = monthlyTrend(leads)
        val peak = trend.maxByOrNull { it.count }

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Total Leads", total.toString(), TextPrimary, Modifier.weight(1f))
                Metric("Active", active.toString(), Color(0xFFB45309), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Deals Closed", booked.toString(), Color(0xFF059669), Modifier.weight(1f))
                Metric("Conversion", "$conv%", Teal, Modifier.weight(1f))
            }

            DealioCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Monthly Lead Trend", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if ((peak?.count ?: 0) > 0) Text("Peak: ${peak!!.label} (${peak.count})", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Text("Last 6 months", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                LineChart(trend)
                Spacer(Modifier.height(8.dp))
                MonthLabels(trend)
            }

            DealioCard {
                Text("Leads per Month", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Bar view for comparison", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                BarChart(trend)
                Spacer(Modifier.height(8.dp))
                MonthLabels(trend)
            }

            // Platform insights
            DealioCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Social Platform Insights", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Connect accounts to track reach and leads per platform", color = TextSecondary, fontSize = 11.sp)
                    }
                    ComingSoonPill()
                }
                Spacer(Modifier.height(12.dp))
                val platforms = listOf(
                    Triple("WhatsApp", Color(0xFF25D366), "Share projects directly with clients"),
                    Triple("Instagram", Color(0xFFE4405F), "Reach buyers through visual stories"),
                    Triple("Facebook", Color(0xFF1877F2), "Target audiences with property ads"),
                    Triple("LinkedIn", Color(0xFF0A66C2), "Connect with investors and HNI buyers"),
                )
                platforms.forEachIndexed { i, (name, color, desc) ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFF7FAFB), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(34.dp).background(color, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Facebook, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(desc, color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("Connect", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, accent: Color, modifier: Modifier) {
    DealioCard(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LineChart(data: List<MonthPoint>) {
    val maxV = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val n = data.size
        if (n < 2) return@Canvas
        val stepX = size.width / (n - 1)
        val pts = data.mapIndexed { i, p -> Offset(i * stepX, size.height - (p.count.toFloat() / maxV) * size.height) }
        for (i in 0 until pts.size - 1) {
            drawLine(Teal, pts[i], pts[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        pts.forEach { drawCircle(Teal, radius = 8f, center = it); drawCircle(Color.White, radius = 3f, center = it) }
    }
}

@Composable
private fun BarChart(data: List<MonthPoint>) {
    val maxV = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val slot = size.width / data.size
        val barW = slot * 0.5f
        data.forEachIndexed { i, p ->
            val h = (p.count.toFloat() / maxV) * size.height
            drawRoundRect(
                Teal,
                topLeft = Offset(i * slot + (slot - barW) / 2, size.height - h),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            )
        }
    }
}

@Composable
private fun MonthLabels(data: List<MonthPoint>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        data.forEach { Text(it.label, color = TextSecondary, fontSize = 10.sp) }
    }
}
