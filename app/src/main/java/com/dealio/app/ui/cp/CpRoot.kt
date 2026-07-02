package com.dealio.app.ui.cp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dealio.app.ui.components.FloatingPillNav
import com.dealio.app.ui.components.PillTab
import com.dealio.app.ui.cp.calllogs.CallLogsScreen
import com.dealio.app.ui.cp.contacts.ContactsScreen
import com.dealio.app.ui.cp.conversations.ConversationsScreen
import com.dealio.app.ui.cp.earnings.EarningsScreen
import com.dealio.app.ui.cp.loan.CpLoanAssistScreen
import com.dealio.app.ui.cp.followups.FollowUpsScreen
import com.dealio.app.ui.cp.growth.AiInsightsScreen
import com.dealio.app.ui.cp.growth.BrochureScreen
import com.dealio.app.ui.cp.growth.CommunityScreen
import com.dealio.app.ui.cp.growth.ContentStudioScreen
import com.dealio.app.ui.cp.growth.JvScreen
import com.dealio.app.ui.cp.growth.LeaderboardScreen
import com.dealio.app.ui.cp.growth.ReferralScreen
import com.dealio.app.ui.cp.growth.SocialAnalyticsScreen
import com.dealio.app.ui.cp.growth.WhatsAppBroadcastScreen
import com.dealio.app.ui.cp.leads.CpDealDetailScreen
import com.dealio.app.ui.cp.leads.LeadsScreen
import com.dealio.app.ui.cp.meetings.CpMeetingsScreen
import com.dealio.app.ui.cp.more.CpMoreScreen
import com.dealio.app.ui.cp.notifications.CpNotificationsScreen
import com.dealio.app.ui.cp.overview.CpOverviewScreen
import com.dealio.app.ui.cp.profile.CpProfileScreen
import com.dealio.app.ui.cp.projects.CpProjectDetailScreen
import com.dealio.app.ui.cp.projects.CpProjectsScreen

object CpRoutes {
    const val HOME = "cp_home"
    const val LEADS = "cp_leads"
    const val PROJECTS = "cp_projects"
    const val EARNINGS = "cp_earnings"
    const val MORE = "cp_more"

    const val DEAL_DETAIL = "cp_deal_detail"
    const val PROJECT_DETAIL = "cp_project_detail"
    const val CONTACTS = "cp_contacts"
    const val FOLLOWUPS = "cp_followups"
    const val CALLLOGS = "cp_calllogs"
    const val MEETINGS = "cp_meetings"
    const val PROFILE = "cp_profile"
    const val NOTIFICATIONS = "cp_notifications"

    // Growth tools
    const val LEADERBOARD = "cp_leaderboard"
    const val REFERRAL = "cp_referral"
    const val AI_INSIGHTS = "cp_ai_insights"
    const val SOCIAL_ANALYTICS = "cp_social_analytics"
    const val CONTENT_STUDIO = "cp_content_studio"
    const val BROCHURE = "cp_brochure"
    const val WHATSAPP_BROADCAST = "cp_whatsapp_broadcast"
    const val COMMUNITY = "cp_community"
    const val JV = "cp_jv"
    const val CONVERSATIONS = "cp_conversations"
    const val LOAN_ASSIST = "cp_loan_assist"

    fun dealDetail(id: Long) = "$DEAL_DETAIL/$id"
    fun projectDetail(id: Long) = "$PROJECT_DETAIL/$id"
}

private val tabs = listOf(
    PillTab(CpRoutes.HOME, "Home", Icons.Filled.SpaceDashboard, Icons.Outlined.SpaceDashboard),
    PillTab(CpRoutes.LEADS, "Leads", Icons.Outlined.Groups, Icons.Outlined.Groups),
    PillTab(CpRoutes.PROJECTS, "Projects", Icons.Outlined.Apartment, Icons.Outlined.Apartment),
    PillTab(CpRoutes.EARNINGS, "Earnings", Icons.Outlined.Payments, Icons.Outlined.Payments),
    PillTab(CpRoutes.MORE, "More", Icons.Filled.GridView, Icons.Outlined.GridView),
)

/** The channel-partner app shell: floating pill navigation + nested route host. */
@Composable
fun CpRoot(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                FloatingPillNav(
                    tabs = tabs,
                    selectedRoute = currentRoute,
                    onSelect = { tab ->
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = CpRoutes.HOME,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            composable(CpRoutes.HOME) { CpOverviewScreen(nav) }
            composable(CpRoutes.LEADS) { LeadsScreen(nav) }
            composable(CpRoutes.PROJECTS) { CpProjectsScreen(nav) }
            composable(CpRoutes.EARNINGS) { EarningsScreen(nav) }
            composable(CpRoutes.MORE) { CpMoreScreen(nav, onLogout) }

            composable(
                "${CpRoutes.DEAL_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e -> CpDealDetailScreen(nav, e.arguments?.getLong("id") ?: 0) }

            composable(
                "${CpRoutes.PROJECT_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e -> CpProjectDetailScreen(nav, e.arguments?.getLong("id") ?: 0) }

            composable(CpRoutes.CONTACTS) { ContactsScreen(nav) }
            composable(CpRoutes.FOLLOWUPS) { FollowUpsScreen(nav) }
            composable(CpRoutes.CALLLOGS) { CallLogsScreen(nav) }
            composable(CpRoutes.MEETINGS) { CpMeetingsScreen(nav) }
            composable(CpRoutes.PROFILE) { CpProfileScreen(nav) }
            composable(CpRoutes.NOTIFICATIONS) { CpNotificationsScreen(nav) }

            composable(CpRoutes.LEADERBOARD) { LeaderboardScreen(nav) }
            composable(CpRoutes.REFERRAL) { ReferralScreen(nav) }
            composable(CpRoutes.AI_INSIGHTS) { AiInsightsScreen(nav) }
            composable(CpRoutes.SOCIAL_ANALYTICS) { SocialAnalyticsScreen(nav) }
            composable(CpRoutes.CONTENT_STUDIO) { ContentStudioScreen(nav) }
            composable(CpRoutes.BROCHURE) { BrochureScreen(nav) }
            composable(CpRoutes.WHATSAPP_BROADCAST) { WhatsAppBroadcastScreen(nav) }
            composable(CpRoutes.COMMUNITY) { CommunityScreen(nav) }
            composable(CpRoutes.JV) { JvScreen(nav) }
            composable(CpRoutes.CONVERSATIONS) { ConversationsScreen(nav) }
            composable(CpRoutes.LOAN_ASSIST) { CpLoanAssistScreen(nav) }
        }
    }
}
