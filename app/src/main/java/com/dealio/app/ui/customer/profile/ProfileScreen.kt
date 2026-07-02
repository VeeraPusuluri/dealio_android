package com.dealio.app.ui.customer.profile

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.components.AppLockToggleRow
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.customer.CustomerViewModel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val cities: List<String> = emptyList(),
    val selectedCity: String? = null,
    val message: String? = null,
    val name: String = "Customer",
    val phone: String = "",
    val email: String = "",
)

class ProfileViewModel(app: Application) : CustomerViewModel(app) {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        val u = repo.currentUser
        _state.update { it.copy(name = u?.fullName ?: "Customer", phone = u?.phone ?: "", email = u?.email ?: "") }
        viewModelScope.launch {
            (repo.getCities() as? ApiResult.Success)?.let { r -> _state.update { it.copy(cities = r.data) } }
        }
    }

    fun setCity(city: String) {
        _state.update { it.copy(selectedCity = city) }
        viewModelScope.launch {
            val r = repo.setPreferredCity(city)
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Preferred city set to $city") }
        }
    }

    fun setEmailField(v: String) = _state.update { it.copy(email = v) }

    fun saveEmail() {
        viewModelScope.launch {
            val r = repo.updateProfile(_state.value.email.ifBlank { null })
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Profile updated") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

private val IconBlue = Color(0xFF2D7FF9)
private val IconGreen = Color(0xFF24A148)
private val IconOrange = Color(0xFFFF8930)
private val IconPurple = Color(0xFF7B61FF)
private val IconRed = Color(0xFFE5484D)

@Composable
fun ProfileScreen(nav: NavController, onLogout: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding()).verticalScroll(rememberScrollState()),
        ) {
            // ── Branded header ──
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(NavyTealGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(76.dp).background(TealBright, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initialsOf(state.name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(state.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                if (state.phone.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(state.phone, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                if (state.email.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Email, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(state.email, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Preferences card
                DealioCard {
                    SectionLabel("Preferred city")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.cities.forEach { c ->
                            val sel = state.selectedCity == c
                            Text(
                                c,
                                color = if (sel) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier
                                    .background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                                    .border(1.dp, if (sel) Teal else CardBorder, RoundedCornerShape(10.dp))
                                    .clickable { vm.setCity(c) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    SectionLabel("Email")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = vm::setEmailField,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = dealioFieldColors(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = vm::saveEmail,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Save changes", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }

                // Home & finance
                ActionGroup("Home & finance") {
                    ActionRow("My properties", Icons.Outlined.Home, IconBlue) { nav.navigate(CustomerRoutes.PROPERTY) }
                    ActionRow("Home loans", Icons.Outlined.AccountBalance, IconGreen) { nav.navigate(CustomerRoutes.LOANS) }
                    ActionRow("Loan top-up", Icons.Outlined.AddCard, IconOrange) { nav.navigate(CustomerRoutes.TOPUP) }
                    ActionRow("Investments", Icons.Outlined.TrendingUp, IconPurple) { nav.navigate(CustomerRoutes.INVESTMENTS) }
                }

                // Documents & support
                ActionGroup("Documents & support") {
                    ActionRow("Documents", Icons.Outlined.Description, IconBlue) { nav.navigate(CustomerRoutes.DOCUMENTS) }
                    ActionRow("Conversations", Icons.Outlined.ChatBubbleOutline, Teal) { nav.navigate(CustomerRoutes.CONVERSATIONS) }
                    ActionRow("Possession tracker", Icons.Outlined.HomeWork, IconOrange) { nav.navigate(CustomerRoutes.POSSESSION) }
                    ActionRow("Snagging report", Icons.Outlined.Handyman, IconRed) { nav.navigate(CustomerRoutes.SNAGGING) }
                    ActionRow("Contact us", Icons.Outlined.SupportAgent, IconGreen) { nav.navigate(CustomerRoutes.CONTACT) }
                    ActionRow("Notifications", Icons.Outlined.Notifications, IconPurple) { nav.navigate(CustomerRoutes.NOTIFICATIONS) }
                }

                // Security
                DealioCard {
                    SectionLabel("Security")
                    Spacer(Modifier.height(10.dp))
                    AppLockToggleRow()
                }

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f), contentColor = ErrorRed),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log out", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ActionGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, Modifier.padding(start = 4.dp))
        content()
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(tint, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}
