package com.dealio.app.ui.customer.loan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

private data class BankProduct(val name: String, val rate: Double, val maxTenure: Int, val processingFee: Double, val scheme: String?)

private val bankProducts = listOf(
    BankProduct("HDFC Bank", 8.50, 30, 0.5, "Special NRI rates available"),
    BankProduct("SBI Home Loans", 8.25, 30, 0.35, "Women borrowers get 0.05% concession"),
    BankProduct("ICICI Bank", 8.90, 25, 0.5, null),
    BankProduct("Axis Bank", 8.75, 30, 0.5, "Pre-approved for salaried"),
    BankProduct("Kotak Mahindra", 8.65, 25, 1.0, "Balance transfer at 8.5%"),
)

@Composable
fun LoanEligibilityScreen(nav: NavController) {
    var income by remember { mutableStateOf("") }
    var existingEmi by remember { mutableStateOf("") }
    var propertyValue by remember { mutableStateOf("") }
    var rate by remember { mutableFloatStateOf(8.5f) }
    var tenure by remember { mutableFloatStateOf(20f) }
    var digiState by remember { mutableStateOf(0) } // 0 idle, 1 connecting, 2 done
    val scope = rememberCoroutineScope()

    val inc = income.toDoubleOrNull() ?: 0.0
    val emiOut = existingEmi.toDoubleOrNull() ?: 0.0
    val propVal = propertyValue.toDoubleOrNull() ?: 0.0
    val netAvailable = inc * 0.5 - emiOut
    val r = rate / 100.0 / 12.0
    val n = (tenure * 12).toInt()
    val maxLoanByIncome = if (netAvailable > 0) netAvailable * ((1 + r).pow(n.toDouble()) - 1) / (r * (1 + r).pow(n.toDouble())) else 0.0
    val maxLoanByLtv = propVal * 0.8
    val eligibleLoan = minOf(maxLoanByIncome, maxLoanByLtv)
    val estEmi = if (eligibleLoan > 0) eligibleLoan * r * (1 + r).pow(n.toDouble()) / ((1 + r).pow(n.toDouble()) - 1) else 0.0

    val (eligFg, eligBg) = when {
        eligibleLoan > 50_00_000 -> StatusColors.Green to Color(0xFFE8F6F1)
        eligibleLoan > 20_00_000 -> StatusColors.Amber to Color(0xFFFDF3E7)
        else -> StatusColors.Red to Color(0xFFFCEBEB)
    }

    SubScreenScaffold("Home Loan Eligibility", nav) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Eligibility calculator ──
            DealioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Calculate, null, tint = Teal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check eligibility", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                NumField("Gross monthly income (₹)", income, "e.g. 150000") { income = it }
                Spacer(Modifier.height(10.dp))
                NumField("Existing EMI (₹)", existingEmi, "0 if none") { existingEmi = it }
                Spacer(Modifier.height(10.dp))
                NumField("Property value (₹)", propertyValue, "e.g. 10000000") { propertyValue = it }
                Spacer(Modifier.height(10.dp))
                NumField("Interest rate (%)", "%.2f".format(rate), "8.50") { v -> v.toFloatOrNull()?.let { rate = it.coerceIn(6f, 15f) } }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tenure", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${tenure.toInt()} years", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = tenure, onValueChange = { tenure = it }, valueRange = 5f..30f, steps = 24,
                    colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal, inactiveTrackColor = Teal.copy(alpha = 0.18f)),
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    Modifier.fillMaxWidth().background(eligBg, RoundedCornerShape(14.dp)).padding(16.dp),
                ) {
                    Text("You are eligible for a home loan up to", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(formatINR(eligibleLoan), color = eligFg, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Estimated EMI ${formatINR(estEmi)}/month for ${tenure.toInt()} yrs at ${"%.2f".format(rate)}%",
                        color = TextSecondary, fontSize = 12.sp,
                    )
                }
            }

            // ── DigiLocker ──
            DealioCard {
                Text("Fetch documents from DigiLocker", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                when (digiState) {
                    2 -> {
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFFFDF3E7), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Info, null, tint = StatusColors.Amber, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Demo mode — production integrates the real DigiLocker API", color = StatusColors.Amber, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFFE8F6F1), RoundedCornerShape(12.dp)).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = StatusColors.Green, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Documents connected successfully", color = StatusColors.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Your KYC documents were fetched. An advisor will verify them shortly.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                    else -> GradientButton(
                        text = if (digiState == 1) "Connecting…" else "Connect DigiLocker",
                        onClick = {
                            if (digiState == 0) {
                                digiState = 1
                                scope.launch { delay(2000); digiState = 2 }
                            }
                        },
                        enabled = digiState == 0,
                    )
                }
            }

            // ── Compare banks ──
            SectionLabel("Compare banks")
            bankProducts.forEach { bp ->
                DealioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(Teal.copy(alpha = 0.12f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AccountBalance, null, tint = Teal, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(bp.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${bp.maxTenure} yr max · ${bp.processingFee}% fee", color = TextSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${bp.rate}%", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("p.a.", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                    if (bp.scheme != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(bp.scheme, color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().background(Teal.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .clickable { rate = bp.rate.toFloat() }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Use this rate", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NumField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = Color(0xFFE3E9F1),
                cursorColor = Teal,
            ),
        )
    }
}
