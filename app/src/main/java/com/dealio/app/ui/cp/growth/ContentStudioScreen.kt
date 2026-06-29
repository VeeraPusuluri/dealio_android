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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Platform(val id: String, val label: String, val color: Color)
private val platforms = listOf(
    Platform("whatsapp", "WhatsApp", Color(0xFF25D366)),
    Platform("instagram", "Instagram", Color(0xFFE4405F)),
    Platform("facebook", "Facebook", Color(0xFF1877F2)),
    Platform("linkedin", "LinkedIn", Color(0xFF0A66C2)),
)

private fun shortPrice(n: Double?): String = when {
    n == null -> "?"
    n >= 1_00_00_000 -> "${"%.1f".format(n / 1_00_00_000)}Cr"
    n >= 1_00_000 -> "${(n / 1_00_000).toInt()}L"
    else -> n.toLong().toString()
}

private fun caption(p: Project, platform: String): String {
    val price = p.priceLow()?.let { "₹${shortPrice(it)}+" } ?: "Price on request"
    val configs = p.configurations?.joinToString(" / ") ?: "BHK configurations"
    val city = p.city ?: ""
    val builder = p.builderName ?: "a trusted builder"
    val cityTag = city.replace(" ", "")
    return when (platform) {
        "instagram" -> "✨ Your dream home awaits! 🏡\n\n${p.name} — $configs\n📍 $city\n💰 Starting $price\n\nDM me for details, virtual tours and exclusive offers! 🔑\n\n#${cityTag}RealEstate #NewLaunch #DreamHome"
        "linkedin" -> "Exciting real-estate opportunity: ${p.name} by $builder in $city.\n\n• $configs configurations\n• Starting $price\n• Status: ${p.status ?: "Available"}\n\nIdeal for end-users and investors. Reach out for a detailed presentation.\n\n#RealEstate #Investment #$cityTag"
        "facebook" -> "🏠 Introducing ${p.name}!\n\n📍 $city | 💰 $price | 🏗️ $configs\n\nDeveloped by $builder. Limited units available. Comment \"INTERESTED\" or DM for details!\n\n#${cityTag}Homes #RealEstate"
        else -> "🏠 *${p.name}* by $builder\n📍 ${p.locality ?: city}, $city\n💰 Starting $price\n🏗️ $configs\n\nInterested? Reply to this message or call me for more details!\n\n#RealEstate #$cityTag"
    }
}

@Composable
fun ContentStudioScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedId by remember { mutableLongStateOf(-1L) }
    var platform by remember { mutableStateOf("whatsapp") }
    var text by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }

    SubScreenScaffold("Content Studio", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val selected = state.projects.firstOrNull { it.id == selectedId }

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Pick project
            DealioCard {
                SectionLabel("1. Pick a project")
                Spacer(Modifier.height(10.dp))
                if (state.projects.isEmpty()) {
                    Text("No projects yet. They appear here once builders publish them.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.projects.forEach { p ->
                            val sel = p.id == selectedId
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(if (sel) Color(0xFFEAFAFC) else Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, if (sel) Teal else Color(0xFFE3E9F1), RoundedCornerShape(12.dp))
                                    .clickable { selectedId = p.id; text = "" }.padding(12.dp),
                            ) {
                                Text(p.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${p.builderName ?: "—"} · ${p.city ?: ""}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. Platform
            DealioCard {
                SectionLabel("2. Choose platform")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    platforms.forEach { pl ->
                        val sel = platform == pl.id
                        Box(
                            Modifier.background(if (sel) pl.color else Color.White, RoundedCornerShape(10.dp))
                                .border(1.dp, if (sel) pl.color else Color(0xFFE3E9F1), RoundedCornerShape(10.dp))
                                .clickable { platform = pl.id; text = "" }.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(pl.label, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            GradientButton(
                text = if (generating) "Generating…" else "Generate caption",
                icon = Icons.Outlined.AutoAwesome,
                enabled = selected != null && !generating,
                onClick = {
                    val p = selected ?: return@GradientButton
                    generating = true
                    scope.launch { delay(500); text = caption(p, platform); generating = false }
                },
            )

            if (text.isNotBlank()) {
                DealioCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Generated caption")
                        Row(Modifier.clickable { copyToClipboard(ctx, "Caption", text) }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ContentCopy, null, tint = Teal, modifier = Modifier.height(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.weight(1f).background(if (platform == "whatsapp") Color(0xFF25D366) else Teal, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (platform == "whatsapp") openWhatsApp(ctx, null, text) else { copyToClipboard(ctx, "Caption", text); shareText(ctx, text) }
                                }.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (platform == "whatsapp") "Share on WhatsApp" else "Copy & Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        Box(
                            Modifier.background(Color(0xFFEDF1F7), RoundedCornerShape(12.dp)).clickable { text = "" }.padding(horizontal = 18.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Clear", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            // Tips
            Column(Modifier.fillMaxWidth().background(Color(0xFFF7FAFB), RoundedCornerShape(14.dp)).padding(14.dp)) {
                Text("Tips for better engagement", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Post during peak hours: 7–9 AM or 7–9 PM IST",
                    "Add project photos when sharing on Instagram and Facebook",
                    "Always include a clear call-to-action (DM, call, WhatsApp)",
                    "Use local hashtags like #BangaloreHomes or #PuneFlats",
                ).forEach { tip ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(tip, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
