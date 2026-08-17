package com.dealio.app.ui.customer.conversations

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.flow.ConversationsScreen
import com.dealio.app.ui.flow.DealRole

/**
 * The buyer's inbox — their advisor, and the room with the builder in it.
 *
 * The buyer was misled most by the old per-deal list: enquiring about three
 * towers from one builder produced three cards, each carrying the same two
 * people, and an unanswered question could be sitting in any of them. There is
 * now one advisor, one room, however many towers they looked at.
 *
 * Everything below the role is shared — see ui/flow/Conversations.kt.
 */
@Composable
fun CustomerConversationsScreen(nav: NavController) {
    ConversationsScreen(
        nav = nav,
        viewer = DealRole.CUSTOMER,
        emptyHint = "Book a site visit, then tap + to start talking to your advisor or the builder.",
        onOpen = { id -> nav.navigate(CustomerRoutes.conversation(id)) },
    )
}
