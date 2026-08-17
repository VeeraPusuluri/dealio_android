package com.dealio.app.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.customer.CustomerRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "DealioDeepLink"

/**
 * Turning a notification into the screen it is about.
 *
 * Every notification the backend persists carries a `link`: the web app's path
 * for the thing that happened — "/builder/deals/12", "/cp/leads",
 * "/customer/journey". The FCM push repeats that string in its data payload, so
 * the tray entry and the in-app bell list describe the same destination in the
 * same words. This file is the single place those words are read and answered
 * with an Android route, which is why the two entry points can never disagree
 * about where a given alert leads.
 *
 * Links reach us in three shapes and all three are handled: a bare section
 * ("/cp/leads"), an id in the path ("/customer/deals/12"), and an id as a query
 * hint ("/builder/deals?dealId=12") — the last being the form the backend can
 * add without changing where the web app navigates.
 */
enum class Portal(val segment: String) {
    BUILDER("builder"),
    CP("cp"),
    CUSTOMER("customer"),
}

/** A resolved destination: the route, and whether it is one of the pill-nav tabs. */
data class DeepLinkTarget(val route: String, val isTab: Boolean)

object DeepLink {

    /**
     * Intent extra carrying the link, named for the FCM data key the backend
     * sends. Keeping the two names identical matters: when the app is in the
     * background the tray entry is drawn by the FCM SDK, not by us, and it
     * copies every data key straight onto the launch intent's extras.
     */
    const val EXTRA_LINK = "link"

    /**
     * A tap waiting for somewhere to land.
     *
     * The tap can arrive long before there is anywhere to send it — on a cold
     * start the intent is read while the splash screen is still up, and if the
     * user is signed out there is no portal at all until they sign in. So the
     * link is held here until a portal composes and claims it.
     */
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(link: String?) {
        if (!link.isNullOrBlank()) _pending.value = link
    }

    /** Drops any unclaimed link — on sign-out, so it can't follow the next user in. */
    fun clear() {
        _pending.value = null
    }

    /**
     * Maps a backend link to a route inside [portal], or null when it belongs
     * somewhere else. A link is written for one recipient's portal, so a path
     * under another one is not ours to follow.
     */
    fun resolve(portal: Portal, link: String?): DeepLinkTarget? {
        val raw = link?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val query = raw.substringAfter('?', "")
        val segments = raw.substringBefore('?').split('/').filter { it.isNotEmpty() }
        if (segments.firstOrNull() != portal.segment) return null

        val section = segments.getOrNull(1).orEmpty()
        val pathId = segments.getOrNull(2)?.toLongOrNull()
        return when (portal) {
            Portal.BUILDER -> builderTarget(section, pathId, query)
            Portal.CP -> cpTarget(section, pathId, query)
            Portal.CUSTOMER -> customerTarget(section, pathId, query)
        }
    }
}

// ─── Per-portal mapping ──────────────────────────────────────────────────────
//
// A section the app doesn't have falls through to that portal's notification
// list rather than nowhere: the alert is listed there in full, so the tap still
// answers "what was this about?" instead of dropping the user on the home tab.

private fun builderTarget(section: String, pathId: Long?, query: String) = when (section) {
    "", "home", "dashboard", "overview" -> tab(BuilderRoutes.HOME)
    "deals" -> (pathId ?: query.longValue("dealId"))
        ?.let { page(BuilderRoutes.dealDetail(it)) } ?: tab(BuilderRoutes.DEALS)
    "leads", "pipeline" -> tab(BuilderRoutes.PIPELINE)
    "projects" -> (pathId ?: query.longValue("projectId"))
        ?.let { page(BuilderRoutes.projectDetail(it)) } ?: tab(BuilderRoutes.PROJECTS)
    "conversations" -> (pathId ?: query.longValue("conversationId"))
        ?.let { page(BuilderRoutes.conversation(it)) } ?: page(BuilderRoutes.CONVERSATIONS)
    "meetings" -> page(BuilderRoutes.MEETINGS)
    "units" -> page(BuilderRoutes.UNITS)
    "commissions" -> page(BuilderRoutes.COMMISSIONS)
    "documents" -> page(BuilderRoutes.DOCUMENTS)
    "loans" -> page(BuilderRoutes.LOANS)
    "settings" -> page(BuilderRoutes.SETTINGS)
    else -> page(BuilderRoutes.NOTIFICATIONS)
}

private fun cpTarget(section: String, pathId: Long?, query: String) = when (section) {
    "", "home", "dashboard", "overview" -> tab(CpRoutes.HOME)
    // The CP portal calls its deals "leads" on the web and in the tab bar; both
    // spellings arrive, and both open the same pipeline.
    "leads", "pipeline", "deals" -> (pathId ?: query.longValue("dealId"))
        ?.let { page(CpRoutes.dealDetail(it)) } ?: tab(CpRoutes.LEADS)
    "projects" -> (pathId ?: query.longValue("projectId"))
        ?.let { page(CpRoutes.projectDetail(it)) } ?: tab(CpRoutes.PROJECTS)
    "conversations" -> (pathId ?: query.longValue("conversationId"))
        ?.let { page(CpRoutes.conversation(it)) } ?: page(CpRoutes.CONVERSATIONS)
    "earnings", "commissions" -> tab(CpRoutes.EARNINGS)
    "meetings" -> page(CpRoutes.MEETINGS)
    "meetups" -> (pathId ?: query.longValue("meetupId"))
        ?.let { page(CpRoutes.meetupDetail(it)) } ?: page(CpRoutes.MEETUPS)
    "followups" -> page(CpRoutes.FOLLOWUPS)
    "contacts" -> page(CpRoutes.CONTACTS)
    "referral" -> page(CpRoutes.REFERRAL)
    "community" -> page(CpRoutes.COMMUNITY)
    // Verification results are the only thing sent to /cp/settings, and the
    // profile screen is where a partner's verification state lives on Android.
    "settings", "profile" -> page(CpRoutes.PROFILE)
    else -> page(CpRoutes.NOTIFICATIONS)
}

private fun customerTarget(section: String, pathId: Long?, query: String) = when (section) {
    "", "home", "explore" -> tab(CustomerRoutes.EXPLORE)
    // A buyer's deal lives under the Journey tab, so a link to either — with an
    // id or without — belongs to the same pair of screens.
    "journey", "deals" -> (pathId ?: query.longValue("dealId"))
        ?.let { page(CustomerRoutes.dealDetail(it)) } ?: tab(CustomerRoutes.JOURNEY)
    "projects", "project" -> (pathId ?: query.longValue("projectId"))
        ?.let { page(CustomerRoutes.projectDetail(it)) } ?: tab(CustomerRoutes.EXPLORE)
    "conversations" -> (pathId ?: query.longValue("conversationId"))
        ?.let { page(CustomerRoutes.conversation(it)) } ?: page(CustomerRoutes.CONVERSATIONS)
    // Site visits are "meeting" on the web and "Visits" here.
    "meeting", "meetings", "visits" -> tab(CustomerRoutes.VISITS)
    "property" -> page(CustomerRoutes.PROPERTY)
    "loan", "loans" -> page(CustomerRoutes.LOANS)
    "documents" -> page(CustomerRoutes.DOCUMENTS)
    "possession" -> page(CustomerRoutes.POSSESSION)
    "snagging" -> page(CustomerRoutes.SNAGGING)
    "meetups" -> (pathId ?: query.longValue("meetupId"))
        ?.let { page(CustomerRoutes.meetupDetail(it)) } ?: page(CustomerRoutes.MEETUPS)
    "saved" -> tab(CustomerRoutes.SAVED)
    "profile" -> tab(CustomerRoutes.PROFILE)
    else -> page(CustomerRoutes.NOTIFICATIONS)
}

private fun tab(route: String) = DeepLinkTarget(route, isTab = true)
private fun page(route: String) = DeepLinkTarget(route, isTab = false)

/** Reads one numeric query value out of a raw query string ("tab=status&dealId=12"). */
private fun String.longValue(key: String): Long? = split('&')
    .firstOrNull { it.substringBefore('=') == key }
    ?.substringAfter('=', "")
    ?.toLongOrNull()

// ─── Navigating ──────────────────────────────────────────────────────────────

/**
 * Opens the screen a notification's [link] points at. Returns false when the
 * link resolves nowhere, so a caller can fall back to its own behaviour.
 *
 * A tab is entered the way the pill bar enters it — anything else pushes onto
 * the current stack, so Back returns to where the notification was tapped.
 */
fun NavController.openNotificationLink(portal: Portal, link: String?): Boolean {
    val target = DeepLink.resolve(portal, link) ?: return false
    return runCatching {
        if (target.isTab) {
            // Almost the options the bar uses — a plain navigate() would push the
            // tab outside the save/restore bookkeeping and leave the bar unable to
            // switch back (see the note on navigateToCpTab) — but deliberately
            // *without* restoreState.
            //
            // Restoring is right for the bar, where returning to a tab means
            // returning to where you were inside it. It is wrong here: a tap on
            // "your home is booked" restored the pages stacked over Journey last
            // time and left the user staring at the same notification list they
            // had tapped from. A notification names one screen and has to land
            // on it.
            navigate(target.route) {
                popUpTo(graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
        } else {
            navigate(target.route) { launchSingleTop = true }
        }
        true
    }.getOrElse { e ->
        Log.w(TAG, "Could not open \"$link\" (${target.route})", e)
        false
    }
}

/**
 * Claims whatever notification tap is waiting and follows it into [portal].
 *
 * Placed in each portal shell, so a tap that arrived before sign-in — or before
 * the nav graph existed — is honoured the moment there is somewhere to send it.
 */
@Composable
fun FollowPendingDeepLink(nav: NavController, portal: Portal) {
    val pending by DeepLink.pending.collectAsState()
    LaunchedEffect(pending) {
        val link = pending ?: return@LaunchedEffect
        // Cleared before navigating, not after: a link this portal has no route
        // for must not sit in the queue waiting to fire on the next screen.
        DeepLink.clear()
        nav.openNotificationLink(portal, link)
    }
}
