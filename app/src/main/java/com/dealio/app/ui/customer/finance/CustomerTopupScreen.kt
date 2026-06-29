package com.dealio.app.ui.customer.finance

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
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
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlin.math.pow

private const val TOPUP_RATE = 9.25
private const val TOPUP_TENURE = 19
private val PURPOSES = listOf("Home Renovation", "Education", "Medical", "Personal", "Business", "Repay Other Loan", "Investment")

private fun topupEmi(amount: Double): Double {
    if (amount <= 0) return 0.0
    val r = TOPUP_RATE / 1200.0
    val n = TOPUP_TENURE * 12
    return amount * r / (1 - (1 + r).pow(-n.toDouble()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTopupScreen(nav: NavController) {
    val ctx = LocalContext.current
    var outstanding by remember { mutableStateOf("") }
    var propertyValue by remember { mutableStateOf("") }
    var yearsPaid by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var amount by remember { mutableFloatStateOf(20_00_000f) }
    var purpose by remember { mutableStateOf(PURPOSES.first()) }
    var purposeOpen by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val outNum = outstanding.toDoubleOrNull() ?: 0.0
    val propNum = propertyValue.toDoubleOrNull() ?: 0.0
    val yearsNum = yearsPaid.toDoubleOrNull() ?: 0.0
    val maxTopup = (propNum * 0.8 - outNum).coerceAtLeast(0.0)
    val eligible = maxTopup > 0 && yearsNum >= 1
    val emi = topupEmi(amount.toDouble())

    SubScreenScaffold("Loan Top-up", nav) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Info banner
            Row(Modifier.fillMaxWidth().background(Color(0xFFEAF0FE), RoundedCornerShape(14.dp)).padding(14.dp)) {
                Icon(Icons.Outlined.Info, null, tint = StatusColors.Blue, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("What is a Top-up Loan?", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("Borrow additional funds against your existing home loan at attractive rates (8.5–9.5%) — for renovation, education, medical or personal needs. No new property paperwork.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }

            // Eligibility checker
            DealioCard {
                Text("Check your eligibility", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                NumField("Current loan outstanding (₹)", outstanding, "e.g. 15800000") { outstanding = it; checked = false }
                Spacer(Modifier.height(10.dp))
                NumField("Current property value (₹)", propertyValue, "e.g. 25200000") { propertyValue = it; checked = false }
                Spacer(Modifier.height(10.dp))
                NumField("Years of EMI paid on time", yearsPaid, "e.g. 2") { yearsPaid = it; checked = false }
                Spacer(Modifier.height(10.dp))
                NumField("Monthly income (₹)", income, "e.g. 180000") { income = it; checked = false }
                Spacer(Modifier.height(14.dp))
                GradientButton(text = "Check my eligibility", onClick = {
                    if (outstanding.isBlank() || propertyValue.isBlank() || yearsPaid.isBlank() || income.isBlank()) {
                        Toast.makeText(ctx, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        checked = true
                        if (eligible) amount = maxTopup.coerceAtMost(20_00_000.0).toFloat()
                    }
                })
            }

            // Result
            if (checked) {
                if (eligible) {
                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = StatusColors.Green, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("You're eligible!", color = StatusColors.Green, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Maximum top-up: ${formatINR(maxTopup)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("(${formatINR(propNum)} × 80%) − ${formatINR(outNum)}", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniInfo("Rate", "$TOPUP_RATE%", Modifier.weight(1f))
                            MiniInfo("Tenure", "$TOPUP_TENURE yr", Modifier.weight(1f))
                            MiniInfo("EMI", formatINR(emi), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        // Apply
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount required", color = TextSecondary, fontSize = 12.sp)
                            Text(formatINR(amount.toDouble()), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = amount, onValueChange = { amount = it },
                            valueRange = 1_00_000f..maxTopup.toFloat().coerceAtLeast(1_00_000f),
                            colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal, inactiveTrackColor = Teal.copy(alpha = 0.18f)),
                        )
                        // Purpose dropdown
                        ExposedDropdownMenuBox(expanded = purposeOpen, onExpandedChange = { purposeOpen = it }) {
                            OutlinedTextField(
                                value = purpose, onValueChange = {}, readOnly = true,
                                label = { Text("Purpose") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(purposeOpen) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1)),
                            )
                            DropdownMenu(expanded = purposeOpen, onDismissRequest = { purposeOpen = false }) {
                                PURPOSES.forEach { p ->
                                    DropdownMenuItem(text = { Text(p) }, onClick = { purpose = p; purposeOpen = false })
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().background(Color(0xFFF1F4F8), RoundedCornerShape(12.dp)).padding(14.dp)) {
                            Column {
                                Text("Monthly EMI", color = TextSecondary, fontSize = 11.sp)
                                Text("${formatINR(emi)}/month", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${formatINR(amount.toDouble())} at $TOPUP_RATE% · $TOPUP_TENURE yr", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (submitted) {
                            Row(Modifier.fillMaxWidth().background(Color(0xFFE8F6F1), RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = StatusColors.Green, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Application submitted!", color = StatusColors.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Your bank will contact you within 48 hours.", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        } else {
                            GradientButton(text = "Apply for top-up", onClick = { submitted = true })
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().background(Color(0xFFFCEBEB), RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = StatusColors.Red, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Not eligible yet", color = StatusColors.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("You need at least 1 year of timely EMI payments and positive equity (property value must exceed 125% of the outstanding loan).", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            // Advisor CTA
            DealioCard {
                Text("Have questions about top-up loans?", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Our loan experts can guide you through the process.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                GradientButton(text = "Talk to an advisor", onClick = { Toast.makeText(ctx, "A loan advisor will call you within 24 hours.", Toast.LENGTH_SHORT).show() })
            }
        }
    }
}

@Composable
private fun MiniInfo(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Color(0xFFE8F6F1), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, color = StatusColors.Green, fontSize = 10.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun NumField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            singleLine = true, shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
        )
    }
}
