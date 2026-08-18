package com.dealio.app.ui.builder

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dealio.app.ui.builder.ai.AiAssistantScreen
import com.dealio.app.ui.builder.analytics.AnalyticsScreen
import com.dealio.app.ui.builder.broadcast.BroadcastScreen
import com.dealio.app.ui.builder.conversations.BuilderConversationsScreen
import com.dealio.app.ui.flow.ConversationScreen
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.builder.demandletters.DemandLettersScreen
import com.dealio.app.ui.builder.documents.BuilderDocumentsScreen
import com.dealio.app.ui.builder.possession.BuilderPossessionScreen
import com.dealio.app.ui.builder.snagging.BuilderSnaggingScreen
import com.dealio.app.ui.builder.virtualtours.VirtualToursScreen
import com.dealio.app.ui.builder.commissions.CommissionsScreen
import com.dealio.app.ui.builder.cp.CPPerformanceScreen
import com.dealio.app.ui.builder.deals.DealDetailScreen
import com.dealio.app.ui.builder.deals.DealsScreen
import com.dealio.app.ui.builder.loans.LoansScreen
import com.dealio.app.ui.builder.meetings.MeetingsScreen
import com.dealio.app.ui.builder.more.MoreScreen
import com.dealio.app.ui.builder.notifications.NotificationsScreen
import com.dealio.app.ui.builder.overview.OverviewScreen
import com.dealio.app.ui.builder.pipeline.PipelineScreen
import com.dealio.app.ui.builder.projects.ProjectDetailScreen
import com.dealio.app.ui.builder.projects.ProjectFormScreen
import com.dealio.app.ui.builder.projects.ProjectsScreen
import com.dealio.app.ui.builder.rera.ReraScreen
import com.dealio.app.ui.builder.settings.BuilderSettingsScreen
import com.dealio.app.ui.builder.shortlists.ShortlistsScreen
import com.dealio.app.ui.builder.units.UnitMatrixScreen
import com.dealio.app.ui.components.BuilderHeroAccent
import com.dealio.app.ui.components.FloatingPillNav
import com.dealio.app.ui.components.LocalHeroAccent
import com.dealio.app.ui.components.PillTab
import com.dealio.app.ui.components.selectTab
import com.dealio.app.ui.navigation.FollowPendingDeepLink
import com.dealio.app.ui.navigation.Portal

object BuilderRoutes {
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val PIPELINE = "pipeline"
    const val DEALS = "deals"
    const val MORE = "more"
    const val PROJECT_DETAIL = "project_detail"
    const val PROJECT_FORM = "project_form"
    const val DEAL_DETAIL = "deal_detail"
    const val MEETINGS = "meetings"
    const val UNITS = "units"
    const val COMMISSIONS = "commissions"
    const val BROADCAST = "broadcast"
    const val CP_PERFORMANCE = "cp_performance"
    const val ANALYTICS = "analytics"
    const val LOANS = "loans"
    const val RERA = "rera"
    const val SHORTLISTS = "shortlists"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val AI = "ai_assistant"
    const val VIRTUAL_TOURS = "virtual_tours"
    const val DOCUMENTS = "documents"
    const val DEMAND_LETTERS = "demand_letters"
    const val POSSESSION = "possession"
    const val SNAGGING = "snagging"
    const val CONVERSATIONS = "conversations"
    // Deliberately not "conversation": BOTTOM_OWNING_ROUTES matches by prefix,
    // and that spelling is a prefix of the inbox route above — which would have
    // hidden the tab bar on the list as well as on the thread.
    const val CONVERSATION = "builder_thread"

    fun projectDetail(id: Long) = "$PROJECT_DETAIL/$id"
    fun dealDetail(id: Long) = "$DEAL_DETAIL/$id"
    /** One conversation: the only place messages are read and written. */
    fun conversation(id: Long) = "$CONVERSATION/$id"
    fun projectForm(id: Long? = null) = if (id == null) PROJECT_FORM else "$PROJECT_FORM?id=$id"
}

private val bottomTabs = listOf(
    PillTab(BuilderRoutes.HOME, "Home", Icons.Filled.SpaceDashboard, Icons.Outlined.SpaceDashboard),
    PillTab(BuilderRoutes.PROJECTS, "Projects", Icons.Outlined.Apartment, Icons.Outlined.Apartment),
    PillTab(BuilderRoutes.PIPELINE, "Pipeline", Icons.Outlined.Groups, Icons.Outlined.Groups),
    PillTab(BuilderRoutes.DEALS, "Deals", Icons.Filled.Handshake, Icons.Outlined.Handshake),
    PillTab(BuilderRoutes.MORE, "More", Icons.Filled.GridView, Icons.Outlined.GridView),
)

/** Nested routes that pin their own bar at the bottom — see [showBottomBar]. */
private val BOTTOM_OWNING_ROUTES = listOf(BuilderRoutes.CONVERSATION)

/** The builder app shell: floating pill navigation + nested route host. */
@Composable
fun BuilderRoot(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // The nav stays up on nested pages — see the note in CustomerRoot. The
    // conversation screen is the exception: its composer is pinned to the bottom
    // and rides the keyboard, so the tabs stand down rather than stack under it.
    val showBottomBar = currentRoute?.let { route ->
        BOTTOM_OWNING_ROUTES.none { route.startsWith(it) }
    } ?: false

    // A notification tapped in the tray lands here, on the screen it is about.
    FollowPendingDeepLink(nav, Portal.BUILDER)

    // Every hero below this point is lit builder-blue — see LocalHeroAccent.
    CompositionLocalProvider(LocalHeroAccent provides BuilderHeroAccent) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                FloatingPillNav(
                    tabs = bottomTabs,
                    selectedRoute = currentRoute,
                    onSelect = { tab -> nav.selectTab(tab.route, bottomTabs) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = BuilderRoutes.HOME,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(BuilderRoutes.HOME) { OverviewScreen(nav) }
            composable(BuilderRoutes.PROJECTS) { ProjectsScreen(nav) }
            composable(BuilderRoutes.PIPELINE) { PipelineScreen(nav) }
            composable(BuilderRoutes.DEALS) { DealsScreen(nav) }
            composable(BuilderRoutes.MORE) { MoreScreen(nav, onLogout) }

            composable(
                "${BuilderRoutes.PROJECT_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry -> ProjectDetailScreen(nav, entry.arguments?.getLong("id") ?: 0) }

            composable(
                "${BuilderRoutes.PROJECT_FORM}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                ProjectFormScreen(nav, if (id > 0) id else null)
            }

            composable(
                "${BuilderRoutes.DEAL_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                DealDetailScreen(nav, entry.arguments?.getLong("id") ?: 0)
            }

            composable(BuilderRoutes.MEETINGS) { MeetingsScreen(nav) }
            composable(BuilderRoutes.UNITS) { UnitMatrixScreen(nav) }
            composable(BuilderRoutes.COMMISSIONS) { CommissionsScreen(nav) }
            composable(BuilderRoutes.BROADCAST) { BroadcastScreen(nav) }
            composable(BuilderRoutes.CP_PERFORMANCE) { CPPerformanceScreen(nav) }
            composable(BuilderRoutes.ANALYTICS) { AnalyticsScreen(nav) }
            composable(BuilderRoutes.LOANS) { LoansScreen(nav) }
            composable(BuilderRoutes.RERA) { ReraScreen(nav) }
            composable(BuilderRoutes.SHORTLISTS) { ShortlistsScreen(nav) }
            composable(BuilderRoutes.NOTIFICATIONS) { NotificationsScreen(nav) }
            composable(BuilderRoutes.SETTINGS) { BuilderSettingsScreen(nav, onLogout) }

            composable(BuilderRoutes.AI) { AiAssistantScreen(nav) }
            composable(BuilderRoutes.VIRTUAL_TOURS) { VirtualToursScreen(nav) }
            composable(BuilderRoutes.DOCUMENTS) { BuilderDocumentsScreen(nav) }
            composable(BuilderRoutes.DEMAND_LETTERS) { DemandLettersScreen(nav) }
            composable(BuilderRoutes.POSSESSION) { BuilderPossessionScreen(nav) }
            composable(BuilderRoutes.SNAGGING) { BuilderSnaggingScreen(nav) }
            composable(BuilderRoutes.CONVERSATIONS) { BuilderConversationsScreen(nav) }
            composable(
                "${BuilderRoutes.CONVERSATION}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                ConversationScreen(nav, DealRole.BUILDER, e.arguments?.getLong("id") ?: 0)
            }
        }
    }
    }
}
