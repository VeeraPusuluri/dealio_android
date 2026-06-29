package com.dealio.app.ui.cp.growth

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.CpProfile
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

private fun referralCodeOf(p: CpProfile?): String {
    if (p == null) return "—"
    val first = (p.fullName ?: "Partner").split(" ").firstOrNull()?.uppercase()?.take(6) ?: "PARTNER"
    return "CP-$first-${p.id}"
}

@Composable
fun ReferralScreen(nav: NavController, vm: CpGrowthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val code = referralCodeOf(state.profile)
    val link = "https://dealio.app/login?ref=$code"
    val waMsg = "Join Dealio as a Channel Partner using my referral code: $code\n\nEarn commissions on real-estate deals across India.\n\nSign up: $link"

    SubScreenScaffold("Referral Tree", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Code card
            DealioCard {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("YOUR REFERRAL CODE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.background(Color(0xFFEDF1F7), RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(code, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                Icons.Outlined.ContentCopy, "Copy", tint = TextSecondary,
                                modifier = Modifier.size(18.dp).clickable { copyToClipboard(ctx, "Referral code", code) },
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.background(Color(0xFF25D366), RoundedCornerShape(12.dp))
                                    .clickable { openWhatsApp(ctx, null, waMsg) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share on WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Box(
                                Modifier.background(Color(0xFFEDF1F7), RoundedCornerShape(12.dp))
                                    .clickable { copyToClipboard(ctx, "Referral link", link) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ContentCopy, null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy link", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // How it works
            DealioCard {
                Text("How referrals work", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                val steps = listOf(
                    "Share your code" to "Share your referral code or link with fellow real-estate agents.",
                    "They sign up" to "When they register on Dealio with your code, they become your Level 1 referral.",
                    "Earn bonuses" to "Earn ₹500 for each deal they close, and ₹200 for their referrals' deals (Level 2).",
                    "Track earnings" to "Referral earnings appear in your commissions dashboard once payouts are processed.",
                )
                steps.forEachIndexed { i, (title, desc) ->
                    if (i > 0) Spacer(Modifier.height(12.dp))
                    Row {
                        Box(Modifier.size(28.dp).background(Color(0xFFD9F4F8), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color(0xFF0A818A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // Earnings structure
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EarnCard("₹500", "Per deal from Level 1 referral", Icons.Outlined.CardGiftcard, Color(0xFF0A9CB5), Modifier.weight(1f))
                EarnCard("₹200", "Per deal from Level 2 referral", Icons.Outlined.Groups, Color(0xFF6366F1), Modifier.weight(1f))
            }

            // Empty tree
            DealioCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Groups, null, tint = TextSecondary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No referrals yet", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Share your code above. Once other agents join and add deals, your referral tree appears here.",
                        color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun EarnCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier) {
    DealioCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}
