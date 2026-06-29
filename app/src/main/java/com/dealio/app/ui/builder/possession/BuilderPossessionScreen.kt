package com.dealio.app.ui.builder.possession

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

private data class PItem(val name: String, var status: Int) // 0 pending, 1 in-progress, 2 done

@Composable
fun BuilderPossessionScreen(nav: NavController) {
    val items = remember {
        mutableStateListOf(
            PItem("Occupancy Certificate (OC) received", 2),
            PItem("Snagging defects cleared", 1),
            PItem("Final payment & dues settled", 0),
            PItem("Registration & sale deed completed", 0),
            PItem("Key handover scheduled", 0),
        )
    }
    var newItem by remember { mutableStateOf("") }

    val done = items.count { it.status == 2 }
    val total = items.size
    val pct = if (total > 0) done * 100 / total else 0
    val allDone = total > 0 && done == total

    SubScreenScaffold("Possession Tracker", nav) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Progress
            DealioCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Handover progress", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("$pct%", color = if (allDone) StatusColors.Green else Teal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFEDF1F7), RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(pct / 100f).height(10.dp).background(if (allDone) StatusColors.Green else Teal, RoundedCornerShape(5.dp)))
                }
                Spacer(Modifier.height(8.dp))
                if (allDone) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VpnKey, null, tint = StatusColors.Green, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("All items complete — schedule key handover! 🎉", color = StatusColors.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("${total - done} item${if (total - done != 1) "s" else ""} remaining · tap an item to advance its status", color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Checklist
            items.forEachIndexed { i, item ->
                val (icon, tint) = when (item.status) {
                    2 -> Icons.Outlined.CheckCircle to StatusColors.Green
                    1 -> Icons.Outlined.Schedule to StatusColors.Amber
                    else -> Icons.Outlined.RadioButtonUnchecked to TextSecondary
                }
                DealioCard(onClick = { items[i] = item.copy(status = (item.status + 1) % 3) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            item.name, color = if (item.status == 2) TextSecondary else TextPrimary, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (item.status == 2) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                        val labelText = when (item.status) { 2 -> "Completed"; 1 -> "In Progress"; else -> "Pending" }
                        Text(labelText, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Add item
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItem, onValueChange = { newItem = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add checklist item…", fontSize = 13.sp) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions.Default,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.background(if (newItem.isNotBlank()) Teal else Color(0xFFB9D9DE), RoundedCornerShape(12.dp))
                        .clickable(enabled = newItem.isNotBlank()) { items.add(PItem(newItem.trim(), 0)); newItem = "" }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) { Text("Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
