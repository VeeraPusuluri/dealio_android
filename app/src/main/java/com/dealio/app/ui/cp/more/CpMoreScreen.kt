package com.dealio.app.ui.cp.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.BuildConfig
import com.dealio.app.ui.components.ActionGroup
import com.dealio.app.ui.components.ActionItem
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.IconOrange
import com.dealio.app.ui.components.IconPurple
import com.dealio.app.ui.components.IconRed
import com.dealio.app.ui.components.PortalHeader
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun CpMoreScreen(nav: NavController, onLogout: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PortalHeader(title = "More", subtitle = "Your workspace and growth tools") },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ActionGroup(
                "Workspace",
                listOf(
                    ActionItem("Conversations", Icons.Outlined.ChatBubbleOutline, Teal) { nav.navigate(CpRoutes.CONVERSATIONS) },
                    ActionItem("Contacts", Icons.Outlined.Contacts, IconBlue) { nav.navigate(CpRoutes.CONTACTS) },
                    ActionItem("Follow-ups", Icons.Outlined.EventRepeat, IconOrange) { nav.navigate(CpRoutes.FOLLOWUPS) },
                    // "Meetings" and "Meetups" read as synonyms, so the labels have
                    // to carry the difference: a site meeting is one builder
                    // appointment for one customer; a meetup is the partner's own
                    // gathering, with a guest list.
                    ActionItem("Site meetings", Icons.Outlined.CalendarMonth, IconPurple) { nav.navigate(CpRoutes.MEETINGS) },
                    ActionItem("Your meetups", Icons.Outlined.Groups, Teal) { nav.navigate(CpRoutes.MEETUPS) },
                    ActionItem("Profile & verification", Icons.Outlined.Person, IconBlue) { nav.navigate(CpRoutes.PROFILE) },
                    ActionItem("Notifications", Icons.Outlined.Notifications, IconRed) { nav.navigate(CpRoutes.NOTIFICATIONS) },
                ),
            )

            ActionGroup(
                "Grow your business",
                listOf(
                    ActionItem("AI Lead Intelligence", Icons.Outlined.Psychology, IconPurple) { nav.navigate(CpRoutes.AI_INSIGHTS) },
                    ActionItem("Content Studio", Icons.Outlined.AutoAwesome, IconPurple) { nav.navigate(CpRoutes.CONTENT_STUDIO) },
                    ActionItem("Brochure Generator", Icons.Outlined.Description, IconBlue) { nav.navigate(CpRoutes.BROCHURE) },
                    ActionItem("WhatsApp Broadcast", Icons.Outlined.Campaign, IconGreen) { nav.navigate(CpRoutes.WHATSAPP_BROADCAST) },
                    ActionItem("Social Analytics", Icons.Outlined.InsertChart, IconBlue) { nav.navigate(CpRoutes.SOCIAL_ANALYTICS) },
                    ActionItem("Referrals", Icons.Outlined.CardGiftcard, IconRed) { nav.navigate(CpRoutes.REFERRAL) },
                    ActionItem("Loan Assist", Icons.Outlined.AccountBalance, IconGreen) { nav.navigate(CpRoutes.LOAN_ASSIST) },
                    ActionItem("Community", Icons.Outlined.Groups, Teal) { nav.navigate(CpRoutes.COMMUNITY) },
                    ActionItem("JV Opportunities", Icons.Outlined.Handshake, IconOrange) { nav.navigate(CpRoutes.JV) },
                ),
            )

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f), contentColor = ErrorRed),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }

            Text(
                "Dealio v${BuildConfig.VERSION_NAME}",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
