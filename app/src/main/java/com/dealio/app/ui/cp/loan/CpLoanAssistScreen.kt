package com.dealio.app.ui.cp.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatINR
import com.dealio.app.ui.cp.growth.openWhatsApp
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlin.math.pow

@Composable
fun CpLoanAssistScreen(nav: NavController) {
    val ctx = LocalContext.current
    var lead by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var propertyValue by remember { mutableStateOf("") }
    var rate by remember { mutableFloatStateOf(8.5f) }
    var tenure by remember { mutableFloatStateOf(20f) }

    val inc = income.toDoubleOrNull() ?: 0.0
    val prop = propertyValue.toDoubleOrNull() ?: 0.0
    val r = rate / 100.0 / 12.0
    val n = (tenure * 12).toInt()
    val byIncome = if (inc > 0) (inc * 0.5) * ((1 + r).pow(n.toDouble()) - 1) / (r * (1 + r).pow(n.toDouble())) else 0.0
    val byLtv = prop * 0.8
    val eligible = minOf(byIncome, byLtv)
    val emi = if (eligible > 0) eligible * r * (1 + r).pow(n.toDouble()) / ((1 + r).pow(n.toDouble()) - 1) else 0.0

    val (fg, bg) = when {
        eligible > 50_00_000 -> StatusColors.Green to Color(0xFFE8F6F1)
        eligible > 20_00_000 -> StatusColors.Amber to Color(0xFFFDF3E7)
        else -> StatusColors.Red to Color(0xFFFCEBEB)
    }

    SubScreenScaffold("Loan Assist", nav) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Outlined.Info, null, tint = Teal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pre-qualify a lead in seconds and share the estimate on WhatsApp.",
                    color = TextSecondary, fontSize = 12.sp,
                )
            }

            DealioCard {
                Text("Lead details", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Field("Lead name (optional)", lead, "e.g. Ramesh", KeyboardType.Text) { lead = it }
                Spacer(Modifier.height(10.dp))
                Field("Gross monthly income (₹)", income, "e.g. 120000", KeyboardType.Number) { income = it }
                Spacer(Modifier.height(10.dp))
                Field("Property value (₹)", propertyValue, "e.g. 9000000", KeyboardType.Number) { propertyValue = it }
                Spacer(Modifier.height(10.dp))
                Field("Interest rate (%)", "%.2f".format(rate), "8.50", KeyboardType.Number) { v -> v.toFloatOrNull()?.let { rate = it.coerceIn(6f, 15f) } }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tenure", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${tenure.toInt()} years", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = tenure, onValueChange = { tenure = it }, valueRange = 5f..30f, steps = 24,
                    colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal, inactiveTrackColor = Teal.copy(alpha = 0.18f)),
                )
            }

            DealioCard {
                Column(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(14.dp)).padding(16.dp)) {
                    Text("Indicative eligibility", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(formatINR(eligible), color = fg, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Estimated EMI ${formatINR(emi)}/month for ${tenure.toInt()} yrs at ${"%.2f".format(rate)}%", color = TextSecondary, fontSize = 12.sp)
                }
            }

            GradientButton(
                text = "Share estimate on WhatsApp",
                enabled = eligible > 0,
                onClick = {
                    val who = lead.ifBlank { "there" }
                    val msg = "Hi $who! 👋 Based on your details, you're eligible for a home loan up to approximately *${formatINR(eligible)}*, with an estimated EMI of ${formatINR(emi)}/month over ${tenure.toInt()} years at ${"%.2f".format(rate)}% p.a.\n\nThis is an indicative estimate — I can help you get a formal sanction. Shall we proceed?"
                    openWhatsApp(ctx, null, msg)
                },
            )
            Text(
                "Indicative only — actual eligibility depends on the bank's credit assessment.",
                color = TextSecondary, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String, placeholder: String, type: KeyboardType, onChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
        )
    }
}
