package com.dealio.app.ui.customer.notifications

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.BuilderNotification
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.customer.CustomerViewModel
import com.dealio.app.ui.navigation.Portal
import com.dealio.app.ui.navigation.openNotificationLink
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotifState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<BuilderNotification> = emptyList(),
)

class CustomerNotificationsViewModel(app: Application) : CustomerViewModel(app) {
    private val _state = MutableStateFlow(NotifState())
    val state: StateFlow<NotifState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getNotifications()) {
                // Deliberately does NOT mark everything read here. Marking on open
                // meant the unread dots were decoration -- they could never survive
                // the load that drew them -- and it silently cleared alerts the user
                // had not looked at. Read is now an explicit act: tap one, or use
                // "Mark all read".
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repo.markAllNotificationsRead()
            _state.update { s -> s.copy(items = s.items.map { it.copy(read = true) }) }
        }
    }

    /** Persist the read *before* redrawing: the list endpoint serves unread-only,
     *  so a purely local flag would be undone by the next load. */
    fun markRead(id: Long) {
        if (_state.value.items.none { it.id == id && !it.read }) return
        viewModelScope.launch {
            repo.markNotificationRead(id)
            _state.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(read = true) else it }) }
        }
    }
}

@Composable
fun CustomerNotificationsScreen(nav: NavController, vm: CustomerNotificationsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    SubScreenScaffold(
        "Notifications", nav,
        actions = {
            if (state.items.any { !it.read }) {
                Text("Mark all read", color = CustomerAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 14.dp).clickable { vm.markAllRead() })
            }
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load, modifier = Modifier.padding(inner))
            state.items.isEmpty() -> Box(Modifier.padding(inner)) {
                EmptyState(Icons.Outlined.NotificationsNone, "You're all caught up", "Updates about your visits and deals will appear here.")
            }
            else -> LazyColumn(
                modifier = Modifier.padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items.size) { i ->
                    val n = state.items[i]
                    // Reading an alert and going to what it is about are one act:
                    // the same link the tray entry carries takes you there.
                    DealioCard(onClick = {
                        vm.markRead(n.id)
                        nav.openNotificationLink(Portal.CUSTOMER, n.link)
                    }) {
                        Text(n.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(n.message, color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(formatDate(n.createdAt), color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
