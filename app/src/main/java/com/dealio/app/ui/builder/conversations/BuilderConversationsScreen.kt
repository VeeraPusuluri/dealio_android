package com.dealio.app.ui.builder.conversations

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.flow.ConversationsScreen
import com.dealio.app.ui.flow.DealRole

/**
 * The builder's inbox — one row per person, not per deal.
 *
 * A builder selling four projects through the same channel partner used to see
 * four "Advisor" rows with the same advisor in each. They now see one, plus the
 * three-way room for every buyer that partner brought.
 *
 * Everything below the role is shared — see ui/flow/Conversations.kt.
 */
@Composable
fun BuilderConversationsScreen(nav: NavController) {
    ConversationsScreen(
        nav = nav,
        viewer = DealRole.BUILDER,
        emptyHint = "When a channel partner brings you a buyer, tap + to start talking to either of them.",
        onOpen = { id -> nav.navigate(BuilderRoutes.conversation(id)) },
    )
}
