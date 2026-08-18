package com.dealio.app.ui.customer

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
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
import com.dealio.app.ui.components.CustomerHeroAccent
import com.dealio.app.ui.components.FloatingPillNav
import com.dealio.app.ui.components.LocalHeroAccent
import com.dealio.app.ui.components.PillTab
import com.dealio.app.ui.components.selectTab
import com.dealio.app.ui.navigation.FollowPendingDeepLink
import com.dealio.app.ui.navigation.Portal
import com.dealio.app.ui.customer.explore.ExploreScreen
import com.dealio.app.ui.customer.documents.DocumentsScreen
import com.dealio.app.ui.customer.journey.DealDetailScreen
import com.dealio.app.ui.customer.journey.JourneyScreen
import com.dealio.app.ui.customer.conversations.CustomerConversationsScreen
import com.dealio.app.ui.flow.ConversationScreen
import com.dealio.app.ui.flow.DealRole
import com.dealio.app.ui.customer.finance.CustomerInvestmentsScreen
import com.dealio.app.ui.customer.finance.CustomerTopupScreen
import com.dealio.app.ui.customer.handover.CustomerPossessionScreen
import com.dealio.app.ui.customer.handover.CustomerSnaggingScreen
import com.dealio.app.ui.customer.loan.EmiCalculatorScreen
import com.dealio.app.ui.customer.loan.LoanApplyScreen
import com.dealio.app.ui.customer.support.CustomerContactScreen
import com.dealio.app.ui.customer.loan.LoanEligibilityScreen
import com.dealio.app.ui.customer.loan.LoansScreen
import com.dealio.app.ui.customer.meetups.CustomerMeetupDetailScreen
import com.dealio.app.ui.customer.meetups.CustomerMeetupsScreen
import com.dealio.app.ui.customer.notifications.CustomerNotificationsScreen
import com.dealio.app.ui.customer.property.PropertyScreen
import com.dealio.app.ui.customer.profile.ProfileScreen
import com.dealio.app.ui.customer.project.ProjectDetailScreen
import com.dealio.app.ui.customer.saved.SavedScreen
import com.dealio.app.ui.customer.visits.VisitsScreen

object CustomerRoutes {
    const val EXPLORE = "c_explore"
    const val VISITS = "c_visits"
    const val JOURNEY = "c_journey"
    const val SAVED = "c_saved"
    const val PROFILE = "c_profile"

    const val PROJECT_DETAIL = "c_project_detail"
    const val DEAL_DETAIL = "c_deal_detail"
    const val LOAN_APPLY = "c_loan_apply"
    const val LOANS = "c_loans"
    const val EMI = "c_emi"
    const val LOAN_ELIGIBILITY = "c_loan_eligibility"
    const val TOPUP = "c_topup"
    const val INVESTMENTS = "c_investments"
    const val CONTACT = "c_contact"
    const val CONVERSATIONS = "c_conversations"
    // Deliberately not "c_conversation": BOTTOM_OWNING_ROUTES matches by prefix,
    // and that spelling is a prefix of the inbox route above — which would have
    // hidden the tab bar on the list as well as on the thread.
    const val CONVERSATION = "c_thread"
    const val POSSESSION = "c_possession"
    const val SNAGGING = "c_snagging"
    const val PROPERTY = "c_property"
    const val DOCUMENTS = "c_documents"
    const val NOTIFICATIONS = "c_notifications"
    const val MEETUPS = "c_meetups"
    const val MEETUP_DETAIL = "c_meetup_detail"

    fun meetupDetail(id: Long) = "$MEETUP_DETAIL/$id"
    fun projectDetail(id: Long) = "$PROJECT_DETAIL/$id"
    fun dealDetail(id: Long) = "$DEAL_DETAIL/$id"
    /** One conversation: the only place messages are read and written. */
    fun conversation(id: Long) = "$CONVERSATION/$id"
    fun loanApply(projectId: Long? = null, builderId: Long? = null) =
        "$LOAN_APPLY?projectId=${projectId ?: -1}&builderId=${builderId ?: -1}"
}

/**
 * Nested routes that pin their own bar at the bottom — see [showBottomBar].
 *
 * Only the conversation screen qualifies now. The project page also pins a bar,
 * but it is a page a buyer opens constantly and closes rarely, so taking the
 * tabs away there stranded them on it with Back as the only exit. Its actions
 * now stack above the tabs instead — see the bottom bar in ProjectDetailScreen,
 * which gives up its own navigation-bar inset because the pill nav below it
 * already holds one.
 */
private val BOTTOM_OWNING_ROUTES = listOf(CustomerRoutes.CONVERSATION)

private val tabs = listOf(
    PillTab(CustomerRoutes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    PillTab(CustomerRoutes.VISITS, "Visits", Icons.Outlined.CalendarMonth, Icons.Outlined.CalendarMonth),
    PillTab(CustomerRoutes.JOURNEY, "Journey", Icons.Outlined.Timeline, Icons.Outlined.Timeline),
    PillTab(CustomerRoutes.SAVED, "Saved", Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
    PillTab(CustomerRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

/** The consumer app shell: floating pill navigation + nested route host. */
@Composable
fun CustomerRoot(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // The nav stays up on nested pages. It used to be shown only on the five tab
    // routes, so opening anything — a project, a deal, a visit — took the tabs
    // away and left Back as the only way to reach another section.
    //
    // The exception is a screen that owns the bottom of the display itself: the
    // project page pins its "Book a site visit" bar there, and stacking a second
    // bar under it would put two competing controls in the same thumb zone.
    val showBottomBar = currentRoute?.let { route ->
        BOTTOM_OWNING_ROUTES.none { route.startsWith(it) }
    } ?: false

    // A notification tapped in the tray lands here, on the screen it is about.
    FollowPendingDeepLink(nav, Portal.CUSTOMER)

    // Every hero below this point is lit buyer-green — see LocalHeroAccent.
    CompositionLocalProvider(LocalHeroAccent provides CustomerHeroAccent) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                FloatingPillNav(
                    tabs = tabs,
                    selectedRoute = currentRoute,
                    onSelect = { tab -> nav.selectTab(tab.route, tabs) },
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = CustomerRoutes.EXPLORE,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            composable(CustomerRoutes.EXPLORE) { ExploreScreen(nav) }
            composable(CustomerRoutes.VISITS) { VisitsScreen(nav) }
            composable(CustomerRoutes.JOURNEY) { JourneyScreen(nav) }
            composable(CustomerRoutes.SAVED) { SavedScreen(nav) }
            composable(CustomerRoutes.PROFILE) { ProfileScreen(nav, onLogout) }

            composable(
                "${CustomerRoutes.PROJECT_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e -> ProjectDetailScreen(nav, e.arguments?.getLong("id") ?: 0) }

            composable(
                "${CustomerRoutes.DEAL_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                DealDetailScreen(nav, e.arguments?.getLong("id") ?: 0)
            }

            composable(
                "${CustomerRoutes.LOAN_APPLY}?projectId={projectId}&builderId={builderId}",
                arguments = listOf(
                    navArgument("projectId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("builderId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { e ->
                val pid = e.arguments?.getLong("projectId") ?: -1L
                val bid = e.arguments?.getLong("builderId") ?: -1L
                LoanApplyScreen(nav, if (pid > 0) pid else null, if (bid > 0) bid else null)
            }

            composable(CustomerRoutes.LOANS) { LoansScreen(nav) }
            composable(CustomerRoutes.EMI) { EmiCalculatorScreen(nav) }
            composable(CustomerRoutes.LOAN_ELIGIBILITY) { LoanEligibilityScreen(nav) }
            composable(CustomerRoutes.TOPUP) { CustomerTopupScreen(nav) }
            composable(CustomerRoutes.INVESTMENTS) { CustomerInvestmentsScreen(nav) }
            composable(CustomerRoutes.CONTACT) { CustomerContactScreen(nav) }
            composable(CustomerRoutes.CONVERSATIONS) { CustomerConversationsScreen(nav) }
            composable(
                "${CustomerRoutes.CONVERSATION}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                ConversationScreen(nav, DealRole.CUSTOMER, e.arguments?.getLong("id") ?: 0)
            }
            composable(CustomerRoutes.POSSESSION) { CustomerPossessionScreen(nav) }
            composable(CustomerRoutes.SNAGGING) { CustomerSnaggingScreen(nav) }
            composable(CustomerRoutes.PROPERTY) { PropertyScreen(nav) }
            composable(CustomerRoutes.DOCUMENTS) { DocumentsScreen(nav) }
            composable(CustomerRoutes.NOTIFICATIONS) { CustomerNotificationsScreen(nav) }

            composable(CustomerRoutes.MEETUPS) { CustomerMeetupsScreen(nav) }
            composable(
                "${CustomerRoutes.MEETUP_DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e -> CustomerMeetupDetailScreen(nav, e.arguments?.getLong("id") ?: 0) }
        }
    }
    }
}
