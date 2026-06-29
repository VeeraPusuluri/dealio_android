package com.dealio.app.ui.customer.handover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

private data class Snag(
    val id: String, val location: String, val category: String,
    val description: String, val priority: String, var status: String,
)

private val LOCATIONS = listOf("Living Room", "Master Bedroom", "Bedroom 2", "Kitchen", "Bathroom 1", "Balcony", "Entrance", "Other")
private val CATEGORIES = listOf("Structural", "Plumbing", "Electrical", "Painting", "Flooring", "Fixtures", "Other")
private val PRIORITIES = listOf("High", "Medium", "Low")

private fun priorityColors(p: String): Pair<Color, Color> = when (p) {
    "High" -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
    "Medium" -> Color(0xFFB45309) to Color(0xFFFEF3C7)
    else -> Color(0xFF1D4ED8) to Color(0xFFDBEAFE)
}

private fun statusColors(s: String): Pair<Color, Color> = when (s) {
    "Resolved" -> Color(0xFF047857) to Color(0xFFD1FAE5)
    "In Progress" -> Color(0xFFB45309) to Color(0xFFFEF3C7)
    "Reopened", "Open" -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
    else -> TextSecondary to Color(0xFFF1F4F8)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerSnaggingScreen(nav: NavController) {
    val snags = remember { mutableStateListOf<Snag>() }
    var showAdd by remember { mutableStateOf(false) }
    var loc by remember { mutableStateOf(LOCATIONS.first()) }
    var cat by remember { mutableStateOf(CATEGORIES.first()) }
    var priority by remember { mutableStateOf("Medium") }
    var desc by remember { mutableStateOf("") }

    val total = snags.size
    val resolved = snags.count { it.status == "Resolved" }
    val pending = snags.count { it.status in listOf("Open", "In Progress", "Reopened") }

    SubScreenScaffold("Snagging Report", nav, actions = {
        Row(
            Modifier.padding(end = 8.dp).background(Teal, RoundedCornerShape(10.dp))
                .clickable { showAdd = !showAdd }.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.width(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Raise", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total", total.toString(), TextPrimary, Modifier.weight(1f))
                    StatTile("Resolved", resolved.toString(), Color(0xFF047857), Modifier.weight(1f))
                    StatTile("Pending", pending.toString(), Color(0xFFB45309), Modifier.weight(1f))
                }
            }

            if (showAdd) {
                item {
                    DealioCard {
                        Text("New snag item", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        ChipRow("Location", LOCATIONS, loc) { loc = it }
                        Spacer(Modifier.height(8.dp))
                        ChipRow("Category", CATEGORIES, cat) { cat = it }
                        Spacer(Modifier.height(8.dp))
                        ChipRow("Priority", PRIORITIES, priority) { priority = it }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = desc, onValueChange = { desc = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Describe the defect clearly…", fontSize = 13.sp) },
                            minLines = 2, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (desc.isNotBlank()) Color(0xFF16A34A) else Color(0xFFB7E4C7), RoundedCornerShape(12.dp))
                                .clickable(enabled = desc.isNotBlank()) {
                                    val id = "SNG-${(snags.size + 1).toString().padStart(4, '0')}"
                                    snags.add(Snag(id, loc, cat, desc.trim(), priority, "Open"))
                                    desc = ""; showAdd = false
                                }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Submit snag", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            if (snags.isEmpty()) {
                item {
                    DealioCard {
                        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CameraAlt, null, tint = TextSecondary, modifier = Modifier.width(30.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No snags reported yet", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("After possession, tap \"Raise\" to report any defects to your builder for resolution.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(snags.size) { i ->
                    val s = snags[i]
                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.id, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Pill(s.priority, priorityColors(s.priority))
                            Spacer(Modifier.width(6.dp))
                            Pill(s.status, statusColors(s.status))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(s.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${s.location} · ${s.category}", color = TextSecondary, fontSize = 11.sp)
                        if (s.status == "Resolved") {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier.background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                                    .clickable { snags[i] = s.copy(status = "Reopened") }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) { Text("Reopen", color = Color(0xFFB91C1C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier) {
    DealioCard(modifier = modifier, contentPadding = 12.dp) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Pill(text: String, colors: Pair<Color, Color>) {
    Box(Modifier.background(colors.second, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, color = colors.first, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val sel = opt == selected
                Box(
                    Modifier.background(if (sel) Teal else Color.White, RoundedCornerShape(20.dp))
                        .clickable { onSelect(opt) }.padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text(opt, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
    }
}
