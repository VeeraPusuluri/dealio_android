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
import com.dealio.app.ui.customer.explore.ExploreScreen
import com.dealio.app.ui.customer.documents.DocumentsScreen
import com.dealio.app.ui.customer.journey.DealDetailScreen
import com.dealio.app.ui.customer.journey.JourneyScreen
import com.dealio.app.ui.customer.conversations.CustomerConversationsScreen
import com.dealio.app.ui.customer.finance.CustomerInvestmentsScreen
import com.dealio.app.ui.customer.finance.CustomerTopupScreen
import com.dealio.app.ui.customer.handover.CustomerPossessionScreen
import com.dealio.app.ui.customer.handover.CustomerSnaggingScreen
import com.dealio.app.ui.customer.loan.EmiCalculatorScreen
import com.dealio.app.ui.customer.loan.LoanApplyScreen
import com.dealio.app.ui.customer.support.CustomerContactScreen
import com.dealio.app.ui.customer.loan.LoanEligibilityScreen
import com.dealio.app.ui.customer.loan.LoansScreen
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
    const val POSSESSION = "c_possession"
    const val SNAGGING = "c_snagging"
    const val PROPERTY = "c_property"
    const val DOCUMENTS = "c_documents"
    const val NOTIFICATIONS = "c_notifications"

    fun projectDetail(id: Long) = "$PROJECT_DETAIL/$id"
    fun dealDetail(id: Long) = "$DEAL_DETAIL/$id"
    fun loanApply(projectId: Long? = null, builderId: Long? = null) =
        "$LOAN_APPLY?projectId=${projectId ?: -1}&builderId=${builderId ?: -1}"
}

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
            ) { e -> DealDetailScreen(nav, e.arguments?.getLong("id") ?: 0) }

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
            composable(CustomerRoutes.POSSESSION) { CustomerPossessionScreen(nav) }
            composable(CustomerRoutes.SNAGGING) { CustomerSnaggingScreen(nav) }
            composable(CustomerRoutes.PROPERTY) { PropertyScreen(nav) }
            composable(CustomerRoutes.DOCUMENTS) { DocumentsScreen(nav) }
            composable(CustomerRoutes.NOTIFICATIONS) { CustomerNotificationsScreen(nav) }
        }
    }
}
