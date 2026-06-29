package com.dealio.app.ui.cp.growth

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush

private data class CommFeature(val icon: ImageVector, val title: String, val desc: String)

private val communityFeatures = listOf(
    CommFeature(Icons.Outlined.NotificationsActive, "Society Notices", "Broadcast updates, events and maintenance alerts to all residents in a project community."),
    CommFeature(Icons.Outlined.CardGiftcard, "Group Deals", "Negotiate bulk discounts with interior designers, modular-kitchen vendors and appliance brands."),
    CommFeature(Icons.Outlined.Forum, "Resident Forum", "A private WhatsApp-style forum for residents to connect, ask questions and share move-in experiences."),
    CommFeature(Icons.Outlined.Storefront, "Vendor Marketplace", "Vetted vendors for painting, carpentry, plumbing and interior work — at pre-negotiated rates."),
    CommFeature(Icons.Outlined.UploadFile, "Resident Onboarding", "Upload a CSV of flat owners to invite them and enable group-buying power."),
)

@Composable
fun CommunityScreen(nav: NavController) {
    val ctx = LocalContext.current
    SubScreenScaffold("Community", nav) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Hero
            Column(
                Modifier.fillMaxWidth().background(NavyTealGradient, RoundedCornerShape(20.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(56.dp).background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Groups, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Community Hub", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "After a deal closes, your role evolves. Stay connected with buyers even after possession — creating referrals and long-term loyalty.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                ComingSoonPill("Launching Soon")
            }

            // Upcoming features
            DealioCard {
                SectionLabel("What's coming")
                Spacer(Modifier.height(14.dp))
                communityFeatures.forEachIndexed { i, f ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    Row {
                        Box(Modifier.size(38.dp).background(tintBrush(Teal), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                            Icon(f.icon, null, tint = Teal, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(f.desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // Early access
            DealioCard {
                Text("Get early access", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("We're rolling out Community to select CPs first. Request access and we'll notify you when it's ready.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = "Request early access",
                    onClick = { openWhatsApp(ctx, "9000000000", "Hi Dealio team! I'd like early access to the Community Hub feature.") },
                )
            }
        }
    }
}

@Composable
fun JvScreen(nav: NavController) {
    SubScreenScaffold("JV Opportunities", nav) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
            DealioCard {
                EmptyState(
                    icon = Icons.Outlined.Handshake,
                    title = "No JV listings yet",
                    subtitle = "Joint-venture opportunities between channel partners and landowners will appear here once available.",
                )
            }
        }
    }
}
