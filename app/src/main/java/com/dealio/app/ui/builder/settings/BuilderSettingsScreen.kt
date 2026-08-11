package com.dealio.app.ui.builder.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.TokenStore
import com.dealio.app.data.api.BuilderInfo
import com.dealio.app.data.api.BuilderProfile
import com.dealio.app.data.api.BuilderProfileUpdateRequest
import com.dealio.app.ui.builder.BuilderViewModel
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.InfoRow
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.components.ProfileAvatar
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.components.rememberProfileAvatarState
import com.dealio.app.ui.theme.ButtonDisabled
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuilderSettingsState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: BuilderProfile? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

class BuilderSettingsViewModel(app: Application) : BuilderViewModel(app) {
    private val _state = MutableStateFlow(BuilderSettingsState())
    val state: StateFlow<BuilderSettingsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getProfile()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, profile = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun save(body: BuilderProfileUpdateRequest, onSaved: () -> Unit) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.updateProfile(body)) {
                is ApiResult.Success -> {
                    // The response is the freshly read row, so there is no second
                    // round trip and no window where the card disagrees with it.
                    _state.update { it.copy(saving = false, profile = r.data, error = null, message = "Profile updated") }
                    onSaved()
                }
                is ApiResult.Error -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun BuilderSettingsScreen(
    nav: NavController,
    onLogout: () -> Unit,
    vm: BuilderSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    // Signing out must work whether or not the profile call succeeded, so the
    // page always renders. What the server knows is layered on top of the
    // session the app already holds rather than replacing the screen with a
    // full-page error.
    val cached = remember { TokenStore(context).user() }
    val snackbar = remember { SnackbarHostState() }
    val avatar = rememberProfileAvatarState(rememberCoroutineScope())
    var showEdit by remember { mutableStateOf(false) }

    LaunchedEffect(avatar.message) { avatar.consumeMessage()?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val p = state.profile
    val name = if (p != null) p.fullName else cached?.fullName
    val email = if (p != null) p.email else cached?.email
    val phone = if (p != null) p.phone else cached?.phone
    val company = p?.builder

    SubScreenScaffold("Settings", nav) { pad ->
      Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Profile card
            DealioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        name = name,
                        state = avatar,
                        size = 64.dp,
                        // On white card stock the white ring the branded headers
                        // use would simply vanish.
                        ringColor = CardBorder,
                    )
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(name ?: "Builder", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            company?.companyName?.takeIf { it.isNotBlank() } ?: "Builder account",
                            color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            DealioCard {
                CardHeader("Account", loading = state.loading) { showEdit = true }
                Spacer(Modifier.size(8.dp))
                InfoRow("Name", name ?: "—")
                InfoRow("Phone", phone ?: "—")
                InfoRow("Email", email?.takeIf { it.isNotBlank() } ?: "—")
                InfoRow("Role", "Builder")
            }

            DealioCard {
                CardHeader("Company", loading = state.loading) { showEdit = true }
                Spacer(Modifier.size(8.dp))
                when {
                    state.error != null -> RetryNote(state.error!!) { vm.load() }
                    state.loading -> Text("Loading your company details…", color = TextSecondary, fontSize = 12.sp)
                    // InfoRow draws nothing for a blank value, so a builder who
                    // has filled in none of these would get a heading over empty
                    // space. Say what is missing and where to fix it.
                    company == null || company.isEmpty() -> Text(
                        "Nothing added yet. Tap Edit to add your company name and what you've delivered.",
                        color = TextSecondary, fontSize = 12.sp,
                    )
                    else -> {
                        InfoRow("Company", company.companyName)
                        InfoRow("Website", company.website)
                        InfoRow("Established", company.yearEstablished?.toString())
                        InfoRow("Projects delivered", company.deliveredProjects?.toString())
                        InfoRow("Contact phone", company.contactPhone)
                        InfoRow("Contact email", company.contactEmail)
                        if (!company.about.isNullOrBlank()) {
                            Spacer(Modifier.size(6.dp))
                            Text(company.about!!, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            DealioCard {
                SectionLabel("About")
                Spacer(Modifier.size(8.dp))
                InfoRow("App", "Dealio for Builders")
                InfoRow("Version", "1.0")
            }

            Spacer(Modifier.size(4.dp))
            Row(
                Modifier.fillMaxWidth().height(52.dp)
                    .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { onLogout() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Sign out", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
      }
    }

    if (showEdit) {
        EditProfileSheet(
            name = name.orEmpty(),
            email = email.orEmpty(),
            phone = phone.orEmpty(),
            company = company,
            saving = state.saving,
            onDismiss = { showEdit = false },
            onSave = { body -> vm.save(body) { showEdit = false } },
        )
    }
}

/** Section heading with the Edit affordance, matching the CP profile's Details card. */
@Composable
private fun CardHeader(title: String, loading: Boolean, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(title, Modifier.weight(1f))
        if (loading) {
            CircularProgressIndicator(Modifier.size(14.dp), color = Teal, strokeWidth = 2.dp)
        } else {
            Text(
                "Edit", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Teal.copy(alpha = 0.10f))
                    .clickable { onEdit() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun RetryNote(message: String, onRetry: () -> Unit) {
    Column {
        Text(message, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.size(8.dp))
        Text(
            "Try again", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Teal.copy(alpha = 0.10f))
                .clickable { onRetry() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun BuilderInfo.isEmpty(): Boolean =
    companyName.isNullOrBlank() && about.isNullOrBlank() && website.isNullOrBlank() &&
        contactPhone.isNullOrBlank() && contactEmail.isNullOrBlank() &&
        yearEstablished == null && deliveredProjects == null

/**
 * The edit sheet.
 *
 * Both cards open the same one — the split between "you" and "your company" is
 * how the page reads, not two separate saves, and a builder correcting their
 * name usually wants the company line right there too.
 *
 * Phone is shown but not editable: it is the login identity, and moving it
 * belongs with the OTP that owns it rather than a free-text field that would
 * silently lock the account out.
 */
@Composable
private fun EditProfileSheet(
    name: String,
    email: String,
    phone: String,
    company: BuilderInfo?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (BuilderProfileUpdateRequest) -> Unit,
) {
    var fullName by remember { mutableStateOf(name) }
    var emailValue by remember { mutableStateOf(email) }
    var companyName by remember { mutableStateOf(company?.companyName.orEmpty()) }
    var about by remember { mutableStateOf(company?.about.orEmpty()) }
    var website by remember { mutableStateOf(company?.website.orEmpty()) }
    var established by remember { mutableStateOf(company?.yearEstablished?.toString().orEmpty()) }
    var delivered by remember { mutableStateOf(company?.deliveredProjects?.toString().orEmpty()) }
    var contactPhone by remember { mutableStateOf(company?.contactPhone.orEmpty()) }
    var contactEmail by remember { mutableStateOf(company?.contactEmail.orEmpty()) }
    val canSave = fullName.isNotBlank() && !saving

    // A Dialog does not scroll on its own, so on a short screen this many fields
    // would render past the bottom edge with no way to reach Save.
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                SectionLabel("Edit profile")
                Spacer(Modifier.size(4.dp))
                Text(
                    "This is what customers and channel partners see.",
                    color = TextSecondary, fontSize = 12.sp,
                )

                Spacer(Modifier.size(18.dp))
                FieldLabel("Name")
                OutlinedTextField(
                    value = fullName, onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Your full name", color = TextSecondary.copy(alpha = 0.6f)) },
                    isError = fullName.isBlank(),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                if (fullName.isBlank()) {
                    Spacer(Modifier.size(4.dp))
                    Text("Add a name so buyers know who they're dealing with.", color = ErrorRed, fontSize = 11.sp)
                }

                Spacer(Modifier.size(14.dp))
                FieldLabel("Email")
                OutlinedTextField(
                    value = emailValue, onValueChange = { emailValue = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("you@company.com", color = TextSecondary.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                if (phone.isNotBlank()) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Signed in as $phone — your login number can't be changed here.",
                        color = TextSecondary, fontSize = 11.sp,
                    )
                }

                Spacer(Modifier.size(20.dp))
                SectionLabel("Company")

                Spacer(Modifier.size(12.dp))
                FieldLabel("Company name")
                OutlinedTextField(
                    value = companyName, onValueChange = { companyName = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("e.g. Prestige Estates Ltd.", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(14.dp))
                FieldLabel("About")
                OutlinedTextField(
                    value = about, onValueChange = { about = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    placeholder = { Text("A line about the company and its track record", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(14.dp))
                FieldLabel("Website")
                OutlinedTextField(
                    value = website, onValueChange = { website = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("https://yourcompany.com", color = TextSecondary.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(14.dp))
                FieldLabel("Year established")
                OutlinedTextField(
                    // Filtered rather than validated: the backend rejects a
                    // non-number outright, and a rejected save is a worse way to
                    // learn that than a key that simply does not type.
                    value = established, onValueChange = { established = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("e.g. 1996", color = TextSecondary.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(14.dp))
                FieldLabel("Projects delivered")
                OutlinedTextField(
                    value = delivered, onValueChange = { delivered = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("e.g. 42", color = TextSecondary.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(14.dp))
                FieldLabel("Contact phone")
                OutlinedTextField(
                    value = contactPhone, onValueChange = { contactPhone = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Sales line buyers can call", color = TextSecondary.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(14.dp))
                FieldLabel("Contact email")
                OutlinedTextField(
                    value = contactEmail, onValueChange = { contactEmail = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("sales@yourcompany.com", color = TextSecondary.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )

                Spacer(Modifier.size(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !saving) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                BuilderProfileUpdateRequest(
                                    fullName = fullName.trim(),
                                    email = emailValue.trim(),
                                    companyName = companyName.trim(),
                                    about = about.trim(),
                                    website = website.trim(),
                                    contactPhone = contactPhone.trim(),
                                    contactEmail = contactEmail.trim(),
                                    yearEstablished = established.trim(),
                                    deliveredProjects = delivered.trim(),
                                ),
                            )
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, disabledContainerColor = ButtonDisabled),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save changes", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
