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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.BuildConfig
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.components.ActionGroup
import com.dealio.app.ui.components.ActionItem
import com.dealio.app.ui.components.AppLockToggleRow
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.IconOrange
import com.dealio.app.ui.components.IconPurple
import com.dealio.app.ui.components.IconRed
import com.dealio.app.ui.components.ProfileAvatar
import com.dealio.app.ui.components.rememberProfileAvatarState
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.customer.CustomerViewModel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.CustomerAccentBright
import com.dealio.app.ui.theme.CustomerAccent
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
        _state.update {
            it.copy(
                name = u?.fullName ?: "Customer",
                phone = u?.phone ?: "",
                email = u?.email ?: "",
                selectedCity = repo.preferredCity,
            )
        }
        viewModelScope.launch {
            (repo.getCities() as? ApiResult.Success)?.let { r -> _state.update { it.copy(cities = r.data) } }
        }
    }

    fun setCity(city: String) {
        val previous = _state.value.selectedCity
        _state.update { it.copy(selectedCity = city) }
        viewModelScope.launch {
            when (val r = repo.setPreferredCity(city)) {
                // Put the chip back where it was rather than leaving the UI
                // claiming a city the server never accepted.
                is ApiResult.Error -> _state.update { it.copy(selectedCity = previous, message = r.message) }
                is ApiResult.Success -> _state.update { it.copy(message = "Preferred city set to $city") }
            }
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

@Composable
fun ProfileScreen(nav: NavController, onLogout: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val avatar = rememberProfileAvatarState(rememberCoroutineScope())
    LaunchedEffect(avatar.message) { avatar.consumeMessage()?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding()).verticalScroll(rememberScrollState()),
        ) {
            // ── Branded header ──
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(NavyTealGradient),
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(260.dp)
                        .background(
                            Brush.radialGradient(listOf(CustomerAccentBright.copy(alpha = 0.22f), Color.Transparent)),
                            CircleShape,
                        ),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(name = state.name, state = avatar)
                    // Only worth offering once there is something to remove; the
                    // badge alone can replace a picture but never take one away.
                    if (avatar.url != null && !avatar.uploading) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Remove photo",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { avatar.remove() },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(state.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (state.phone.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(state.phone, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderChip(
                            icon = Icons.Outlined.Email,
                            text = state.email.ifBlank { "Add your email" },
                        )
                        if (state.selectedCity != null) {
                            HeaderChip(icon = Icons.Outlined.LocationOn, text = state.selectedCity!!)
                        }
                    }
                }
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // City and email were sharing one card behind a single "Save
                // changes" button that only ever saved the email — splitting them
                // makes it clear the city applies the moment it's tapped.
                DealioCard {
                    SectionLabel("Preferred city")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Sets which city's new launches you hear about.",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.cities.isEmpty()) {
                        // Without this the card renders as a heading over blank
                        // space whenever the cities call fails.
                        Text(
                            if (state.selectedCity != null) {
                                "Currently ${state.selectedCity}. Other cities couldn't be loaded — pull back later to change it."
                            } else {
                                "Cities couldn't be loaded right now."
                            },
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.cities.forEach { c -> CityChip(c, state.selectedCity == c) { vm.setCity(c) } }
                        }
                    }
                }

                DealioCard {
                    SectionLabel("Email")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Where booking confirmations and documents are sent.",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = vm::setEmailField,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = dealioFieldColors(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = vm::saveEmail,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CustomerAccent),
                    ) { Text("Save email", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }

                // Meetups had no home of their own: the only way in was the strip
                // on Explore, which deliberately renders nothing when nothing is
                // on — so a customer with a quiet week could not reach the page
                // at all, or find the invitations waiting on it.
                ActionGroup(
                    "Around you",
                    listOf(
                        ActionItem("Meetups", Icons.Outlined.Groups, CustomerAccent) { nav.navigate(CustomerRoutes.MEETUPS) },
                    ),
                )

                ActionGroup(
                    "Home & finance",
                    listOf(
                        ActionItem("My properties", Icons.Outlined.Home, IconBlue) { nav.navigate(CustomerRoutes.PROPERTY) },
                        ActionItem("Home loans", Icons.Outlined.AccountBalance, IconGreen) { nav.navigate(CustomerRoutes.LOANS) },
                        ActionItem("Loan top-up", Icons.Outlined.AddCard, IconOrange) { nav.navigate(CustomerRoutes.TOPUP) },
                        ActionItem("Investments", Icons.Outlined.TrendingUp, IconPurple) { nav.navigate(CustomerRoutes.INVESTMENTS) },
                    ),
                )

                ActionGroup(
                    "Documents & support",
                    listOf(
                        ActionItem("Documents", Icons.Outlined.Description, IconBlue) { nav.navigate(CustomerRoutes.DOCUMENTS) },
                        ActionItem("Conversations", Icons.Outlined.ChatBubbleOutline, CustomerAccent) { nav.navigate(CustomerRoutes.CONVERSATIONS) },
                        ActionItem("Possession tracker", Icons.Outlined.HomeWork, IconOrange) { nav.navigate(CustomerRoutes.POSSESSION) },
                        ActionItem("Snagging report", Icons.Outlined.Handyman, IconRed) { nav.navigate(CustomerRoutes.SNAGGING) },
                        ActionItem("Contact us", Icons.Outlined.SupportAgent, IconGreen) { nav.navigate(CustomerRoutes.CONTACT) },
                        ActionItem("Notifications", Icons.Outlined.Notifications, IconPurple) { nav.navigate(CustomerRoutes.NOTIFICATIONS) },
                    ),
                )

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

                Text(
                    "Dealio v${BuildConfig.VERSION_NAME}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Translucent pill in the profile hero. */
@Composable
private fun HeaderChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** City selector chip — ticks the active city so the saved choice is visible. */
@Composable
private fun CityChip(city: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(if (selected) CustomerAccent else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) CustomerAccent else CardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            city,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

