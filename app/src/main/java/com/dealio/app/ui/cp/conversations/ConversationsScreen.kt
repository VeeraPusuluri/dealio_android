package com.dealio.app.ui.cp.conversations

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.flow.ConversationsScreen as SharedConversationsScreen
import com.dealio.app.ui.flow.DealRole

/**
 * The CP's inbox.
 *
 * A channel partner is the only party who always has two counterparties, so
 * they felt the old per-deal model worst: one row per deal per thread meant a
 * builder they work with across six projects appeared six times, and a reply
 * could land in any of them. They now have one row per person, plus the
 * three-way room for each buyer.
 *
 * Everything below the role is shared — see ui/flow/Conversations.kt.
 */
@Composable
fun ConversationsScreen(nav: NavController) {
    SharedConversationsScreen(
        nav = nav,
        viewer = DealRole.CP,
        emptyHint = "Refer a lead from the Projects page, then tap + to start talking to the customer or the builder.",
        onOpen = { id -> nav.navigate(CpRoutes.conversation(id)) },
    )
}
