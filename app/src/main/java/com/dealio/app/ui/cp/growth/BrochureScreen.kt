package com.dealio.app.ui.cp.growth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

private fun brochureText(p: Project, name: String, phone: String?, rera: String?): String {
    val loc = listOfNotNull(p.locality, p.city).joinToString(", ")
    val configs = p.configurations?.joinToString(" / ") ?: ""
    val price = p.priceLow()?.let { "₹${shortPrice(it)}" } ?: "Price on request"
    return listOfNotNull(
        "🏠 *${p.name}*",
        loc.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        configs.takeIf { it.isNotBlank() }?.let { "🛏 $it" },
        "💰 Starting $price",
        p.commissionValue?.let { "💼 Commission: $it%" },
        "",
        "📞 Contact: *$name*${phone?.let { " — $it" } ?: ""}",
        rera?.let { "✅ RERA: $it" },
    ).joinToString("\n")
}

private fun shortPrice(n: Double): String = when {
    n >= 1_00_00_000 -> "${"%.1f".format(n / 1_00_00_000)} Cr"
    n >= 1_00_000 -> "${(n / 1_00_000).toInt()} L"
    else -> n.toLong().toString()
}

@Composable
fun BrochureScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var selectedId by remember { mutableLongStateOf(-1L) }

    SubScreenScaffold("Brochure Generator", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val projects = state.projects
        val selected = projects.firstOrNull { it.id == selectedId } ?: projects.firstOrNull()
        val name = state.profile?.fullName ?: "Your Agent"
        val phone = state.profile?.phone
        val rera = state.profile?.cp?.reraNumber

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (projects.isEmpty()) {
                DealioCard { EmptyState(Icons.Outlined.Apartment, "No projects available", "Brochures are generated from published projects.") }
                return@Column
            }

            // Project selector
            DealioCard {
                SectionLabel("Select project")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    projects.forEach { p ->
                        val sel = p.id == selected?.id
                        Box(
                            Modifier.background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                                .border(1.dp, if (sel) Teal else Color(0xFFE3E9F1), RoundedCornerShape(10.dp))
                                .clickable { selectedId = p.id }.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(p.name, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Preview
            if (selected != null) {
                DealioCard(contentPadding = 0.dp) {
                    val url = resolveUrl(selected.imageUrl ?: selected.coverUrl)
                    Box(
                        Modifier.fillMaxWidth().height(176.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(NavyMid, Teal))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (url != null) AsyncImage(url, selected.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Outlined.Apartment, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text(selected.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        val loc = listOfNotNull(selected.locality, selected.city).joinToString(", ")
                        if (loc.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(loc, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        selected.configurations?.takeIf { it.isNotEmpty() }?.let { configs ->
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                configs.forEach { c ->
                                    Box(Modifier.background(Color(0xFFF1F4F8), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(c, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                        selected.priceLow()?.let {
                            Spacer(Modifier.height(10.dp))
                            Text("Starting ₹${shortPrice(it)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        selected.commissionValue?.let {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.background(Color(0xFFEAFAFC), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("${it}% Commission", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFFF7FAFB), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(36.dp).background(Orange, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                                Text(initialsOf(name).take(1), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (phone != null) Text(phone, color = TextSecondary, fontSize = 11.sp)
                                if (rera != null) Text("RERA: $rera", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).background(Teal, RoundedCornerShape(12.dp))
                            .clickable { shareText(ctx, brochureText(selected, name, phone, rera)) }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        Modifier.weight(1f).background(Color(0xFF25D366), RoundedCornerShape(12.dp))
                            .clickable { openWhatsApp(ctx, phone, brochureText(selected, name, phone, rera)) }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("WhatsApp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
