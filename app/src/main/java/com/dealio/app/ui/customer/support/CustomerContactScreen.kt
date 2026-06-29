package com.dealio.app.ui.customer.support

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.SurfaceTintTeal
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush

private val CITIES = listOf("Hyderabad", "Bengaluru", "Mumbai", "Pune", "Delhi NCR", "Chennai")
private val INTERESTS = listOf("Buy a home", "Schedule a site visit", "Get loan assistance", "Investment query", "NRI purchase", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerContactScreen(nav: NavController) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    SubScreenScaffold("Contact Us", nav) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text("Tell us what you're looking for", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                Spacer(Modifier.height(6.dp))
                Text("Have a home in mind? Fill in the form and our team will reach out within 24 hours.", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }

            if (submitted) {
                DealioCard {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(52.dp).background(tintBrush(Teal), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Teal, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Message received!", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Thank you${if (name.isNotBlank()) ", $name" else ""}. Our team will get back to you within 24 hours.", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Send another message", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                            submitted = false; name = ""; phone = ""; email = ""; city = ""; interest = ""; message = ""
                        })
                    }
                }
            } else {
                DealioCard {
                    Field("Full name *", name, "e.g. Ravi Kumar", KeyboardType.Text) { name = it }
                    Spacer(Modifier.height(10.dp))
                    Field("Phone *", phone, "+91 98765 43210", KeyboardType.Phone) { phone = it }
                    Spacer(Modifier.height(10.dp))
                    Field("Email", email, "you@example.com", KeyboardType.Email) { email = it }
                    Spacer(Modifier.height(10.dp))
                    ChipPicker("Preferred city", CITIES, city) { city = it }
                    Spacer(Modifier.height(10.dp))
                    ChipPicker("I'm interested in", INTERESTS, interest) { interest = it }
                    Spacer(Modifier.height(10.dp))
                    Field("Message", message, "BHK preference, budget, timeline…", KeyboardType.Text, minLines = 3) { message = it }
                    Spacer(Modifier.height(14.dp))
                    GradientButton(text = "Send message", enabled = name.isNotBlank() && phone.isNotBlank(), onClick = { submitted = true })
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().background(Color(0xFF25D366).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable {
                                val url = "https://wa.me/?text=" + Uri.encode("Hi, I'm looking for a property. Please get in touch.")
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Chat on WhatsApp", color = Color(0xFF1FA855), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.height(6.dp))
                    Text("Your details are kept private and never shared with third parties.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                }
            }

            // Contact details
            DealioCard {
                ContactRow(Icons.Outlined.Phone, "Call us", "+91 40 6688 0000", "Mon–Sat, 9am–7pm IST") {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+914066880000")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                Spacer(Modifier.height(12.dp))
                ContactRow(Icons.Outlined.Email, "Email us", "hello@dealio.in", "We reply within 24 hours") {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hello@dealio.in")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                Spacer(Modifier.height(12.dp))
                ContactRow(Icons.Outlined.LocationOn, "Visit us", "Hyderabad, Telangana", "By appointment", null)
            }
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, value: String, sub: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(SurfaceTintTeal, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Teal, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipPicker(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val sel = opt == selected
                Box(
                    Modifier.background(if (sel) Teal else Color.White, RoundedCornerShape(20.dp))
                        .then(if (!sel) Modifier.padding(0.dp) else Modifier)
                        .clickable { onSelect(if (sel) "" else opt) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(opt, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, placeholder: String, type: KeyboardType, minLines: Int = 1, onChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp) },
            singleLine = minLines == 1, minLines = minLines,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = Color(0xFFE3E9F1), cursorColor = Teal),
        )
    }
}
