package com.dealio.app.ui.cp.growth

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

private fun broadcastMessage(contactName: String, p: Project): String {
    val price = p.priceLow()?.let { "₹${shortPriceWa(it)}+" } ?: "Price on request"
    val configs = p.configurations?.joinToString(" / ")?.takeIf { it.isNotBlank() }?.let { "$it options available" } ?: "Multiple options"
    return "Hi $contactName! 👋\n\nI wanted to share an exciting property with you:\n\n🏠 *${p.name}*\n📍 ${p.city ?: ""}\n💰 Starting $price\n🏗️ $configs\n\nI'd love to arrange a site visit at your convenience. Reply or call me anytime!"
}

private fun shortPriceWa(n: Double): String = when {
    n >= 1_00_00_000 -> "${"%.1f".format(n / 1_00_00_000)}Cr"
    n >= 1_00_000 -> "${(n / 1_00_000).toInt()}L"
    else -> n.toLong().toString()
}

@Composable
fun WhatsAppBroadcastScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val selected = remember { mutableStateListOf<Long>() }
    var projectId by remember { mutableLongStateOf(-1L) }
    var custom by remember { mutableStateOf("") }

    SubScreenScaffold("WhatsApp Broadcast", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val contacts = state.contacts
        val project = state.projects.firstOrNull { it.id == projectId }

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Contacts
            DealioCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("1. Contacts")
                        if (selected.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(20.dp).background(Teal, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Text("${selected.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (contacts.isNotEmpty()) {
                        Text(
                            if (selected.size == contacts.size) "Clear all" else "Select all",
                            color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                if (selected.size == contacts.size) selected.clear()
                                else { selected.clear(); selected.addAll(contacts.map { it.id }) }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (contacts.isEmpty()) {
                    Text("No contacts yet. Add them in Contacts.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                        contacts.forEach { c ->
                            val on = selected.contains(c.id)
                            Row(
                                Modifier.fillMaxWidth().clickable { if (on) selected.remove(c.id) else selected.add(c.id) }.padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(if (on) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null, tint = if (on) Teal else TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(c.phone + (c.bhkPreference?.let { " · $it" } ?: ""), color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Project
            DealioCard {
                SectionLabel("2. Project")
                Spacer(Modifier.height(8.dp))
                if (state.projects.isEmpty()) {
                    Text("No projects available.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        state.projects.forEach { p ->
                            val on = p.id == projectId
                            Row(
                                Modifier.fillMaxWidth().clickable { projectId = p.id; custom = "" }.padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(if (on) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null, tint = if (on) Teal else TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("${p.city ?: ""} · ${p.priceLow()?.let { "from ₹${shortPriceWa(it)}" } ?: "Price on request"}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Composer
            if (project != null) {
                DealioCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionLabel("3. Message preview")
                        Text("${selected.size} recipient${if (selected.size != 1) "s" else ""}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    val preview = custom.ifBlank { broadcastMessage(contacts.firstOrNull()?.name ?: "[Name]", project) }
                    OutlinedTextField(
                        value = preview, onValueChange = { custom = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Each contact is greeted by their own name automatically.", color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    val canSend = selected.isNotEmpty()
                    Box(
                        Modifier.fillMaxWidth()
                            .background(if (canSend) Color(0xFF25D366) else Color(0xFFB7E4C7), RoundedCornerShape(12.dp))
                            .clickable(enabled = canSend) { sendBroadcast(ctx, selected, contacts, project, custom) }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Send, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send via WhatsApp (${selected.size})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else if (selected.isEmpty()) {
                DealioCard { EmptyState(Icons.Outlined.Apartment, "Pick contacts and a project", "A personalised WhatsApp message is composed for each contact.") }
            }
        }
    }
}

private fun sendBroadcast(
    ctx: android.content.Context,
    selectedIds: List<Long>,
    contacts: List<CpContact>,
    project: Project,
    custom: String,
) {
    val chosen = contacts.filter { selectedIds.contains(it.id) }.take(3)
    chosen.forEach { c ->
        val msg = custom.ifBlank { broadcastMessage(c.name, project) }
        if (c.phone.isNotBlank()) openWhatsApp(ctx, c.phone, msg)
    }
}
