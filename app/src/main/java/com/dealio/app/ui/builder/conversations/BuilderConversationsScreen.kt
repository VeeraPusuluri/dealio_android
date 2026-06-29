package com.dealio.app.ui.builder.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.StatusChip
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.tools.BuilderToolsViewModel
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush

@Composable
fun BuilderConversationsScreen(nav: NavController, vm: BuilderToolsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    SubScreenScaffold("Conversations", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val deals = state.deals

        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("${deals.size} ${if (deals.size == 1) "conversation" else "conversations"}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            if (deals.isEmpty()) {
                item { DealioCard { EmptyState(Icons.Outlined.ChatBubbleOutline, "No conversations yet", "When a customer books a site visit, your chat with them and the channel partner appears here.") } }
            } else {
                items(deals.size) { i ->
                    val d = deals[i]
                    DealioCard(onClick = { nav.navigate(BuilderRoutes.dealDetail(d.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(tintBrush(Teal), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Text(initialsOf(d.customerName), color = Teal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.customerName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Person, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(listOfNotNull(d.projectName, d.cpName?.let { "via $it" }).joinToString(" · "), color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            if (d.status.isNotBlank()) StatusChip(d.status)
                        }
                    }
                }
            }
        }
    }
}
