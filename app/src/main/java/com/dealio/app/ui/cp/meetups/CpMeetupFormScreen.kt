package com.dealio.app.ui.cp.meetups

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.CpMeetup
import com.dealio.app.data.api.CreateCpMeetupRequest
import com.dealio.app.data.api.UpdateCpMeetupRequest
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.GradientButton
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.meetups.ChoiceChips
import com.dealio.app.ui.meetups.FormLabel
import com.dealio.app.ui.meetups.MeetupCategory
import com.dealio.app.ui.meetups.MeetupMode
import com.dealio.app.ui.meetups.meetupDayLabel
import com.dealio.app.ui.meetups.meetupTimes
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class MeetupFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val people: List<Invitee> = emptyList(),
    val editing: CpMeetup? = null,
    val message: String? = null,
    val savedId: Long? = null,
    /** A photograph is on its way up. Blocks Save so a half-uploaded cover is never stored. */
    val uploading: Boolean = false,
)

class CpMeetupFormViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(MeetupFormState())
    val state: StateFlow<MeetupFormState> = _state.asStateFlow()

    /**
     * Loads the invite list, and the meetup itself when editing.
     *
     * The invite list is fetched up front rather than when the picker opens: a
     * partner filling this in should never wait on a spinner to answer the one
     * question the form is actually about.
     */
    fun start(meetupId: Long?) {
        if (_state.value.people.isNotEmpty() || _state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val invitable = repo.getInvitable()
            val editing = meetupId?.let { id -> (repo.getMeetup(id) as? ApiResult.Success)?.data }
            _state.update {
                it.copy(
                    loading = false,
                    people = (invitable as? ApiResult.Success)?.data?.let(::mergeInvitable) ?: emptyList(),
                    editing = editing,
                )
            }
        }
    }

    fun create(req: CreateCpMeetupRequest) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.createMeetup(req)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        saving = false,
                        savedId = r.data.id,
                        message = if (req.invitees.isEmpty()) "Meetup created"
                        else "Meetup created — ${req.invitees.size} invited",
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }

    fun update(id: Long, req: UpdateCpMeetupRequest) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.updateMeetup(id, req)) {
                is ApiResult.Success -> _state.update { it.copy(saving = false, savedId = r.data.id, message = "Changes saved") }
                is ApiResult.Error -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }

    /**
     * Sends one picked image up and hands back its URL.
     *
     * The upload happens now rather than on Save so the partner can see what
     * they chose before committing to it — a cover is the one field on this form
     * whose value you cannot judge from its text. The URL is held by the form
     * and travels with the create; abandoning the form leaves a stray file on
     * the server, which is the cheaper of the two mistakes available here.
     */
    fun uploadPhoto(uri: Uri, onDone: (String) -> Unit) {
        _state.update { it.copy(uploading = true) }
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val ctx = getApplication<Application>()
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    bytes?.let { it to (ctx.contentResolver.getType(uri) ?: "image/jpeg") }
                }.getOrNull()
            }
            if (picked == null) {
                _state.update { it.copy(uploading = false, message = "Could not read that image.") }
                return@launch
            }
            val (bytes, mime) = picked
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            when (val r = repo.uploadMeetupPhoto(bytes, "meetup.$ext", mime)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(uploading = false) }
                    onDone(r.data.url)
                }
                is ApiResult.Error -> _state.update { it.copy(uploading = false, message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/**
 * Create or edit a meetup.
 *
 * A full screen rather than the dialog this used to be. Once a meetup carries a
 * category, a visibility, a city and an invite list, a capped-height dialog
 * hides most of the form behind a scroll — and the invite list, the part that
 * decides whether anyone turns up, was the part getting hidden.
 */
@Composable
fun CpMeetupFormScreen(
    nav: NavController,
    meetupId: Long?,
    vm: CpMeetupFormViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(meetupId) { vm.start(meetupId) }

    // Leave once saved. The detail screen reloads on resume, so an edit shows
    // there without this screen having to hand anything back.
    LaunchedEffect(state.savedId) { if (state.savedId != null) nav.popBackStack() }

    val editing = state.editing
    val isEdit = meetupId != null

    SubScreenScaffold(if (isEdit) "Edit meetup" else "New meetup", nav) { inner ->
        if (state.loading) {
            LoadingState(Modifier.padding(inner))
            return@SubScreenScaffold
        }
        // Keyed on the loaded row so the fields seed once it arrives, rather
        // than staying empty because they were remembered before the fetch.
        key(editing?.id ?: 0L) {
            MeetupFormBody(
                inner = inner,
                editing = editing,
                people = state.people,
                saving = state.saving,
                uploading = state.uploading,
                message = state.message,
                onDismissMessage = vm::clearMessage,
                onPickPhoto = vm::uploadPhoto,
                onSubmit = { create, update, invitees ->
                    if (isEdit && editing != null) vm.update(editing.id, update)
                    else vm.create(create.copy(invitees = invitees.map { it.toPayload() }))
                },
            )
        }
    }
}

@Composable
private fun MeetupFormBody(
    inner: androidx.compose.foundation.layout.PaddingValues,
    editing: CpMeetup?,
    people: List<Invitee>,
    saving: Boolean,
    uploading: Boolean,
    message: String?,
    onDismissMessage: () -> Unit,
    onPickPhoto: (Uri, (String) -> Unit) -> Unit,
    onSubmit: (CreateCpMeetupRequest, UpdateCpMeetupRequest, List<Invitee>) -> Unit,
) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var coverImage by remember { mutableStateOf(editing?.coverImage) }
    var photos by remember { mutableStateOf(editing?.photos.orEmpty()) }
    var topics by remember { mutableStateOf(editing?.topics.orEmpty()) }
    // A new meetup starts on the commonest kind, not on OTHER — `from(null)`
    // falls through to OTHER, which is the right answer for an unknown value off
    // the wire and the wrong one for a blank form.
    var category by remember {
        mutableStateOf(editing?.let { MeetupCategory.from(it.category) } ?: MeetupCategory.SITE_VISIT)
    }
    var mode by remember { mutableStateOf(MeetupMode.from(editing?.mode)) }
    var location by remember { mutableStateOf(editing?.location ?: "") }
    var city by remember { mutableStateOf(editing?.city ?: "") }
    // No longer typed here — the form dropped the field. Still carried so that
    // editing a meetup whose link was set elsewhere (the admin CRM sets one)
    // saves it back untouched instead of quietly clearing it.
    val mapsLink = editing?.mapsLink ?: ""
    var onlineLink by remember { mutableStateOf(editing?.onlineLink ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var capacity by remember { mutableStateOf(editing?.capacity?.toString() ?: "") }
    var isPublic by remember { mutableStateOf(editing?.isPublic ?: true) }
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now().plusDays(1).toString()) }
    var time by remember { mutableStateOf(editing?.time ?: "04:00 PM") }
    var picked by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(message) { if (message != null) onDismissMessage() }

    val needsPlace = mode != MeetupMode.ONLINE
    val canSave = title.isNotBlank() && (location.isNotBlank() || !needsPlace) && !saving && !uploading

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { u -> onPickPhoto(u) { coverImage = it } }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { u -> onPickPhoto(u) { url -> photos = (photos + url).take(MAX_PHOTOS) } }
    }

    Column(
        Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // ── Cover ───────────────────────────────────────────────────────────
        // First, because it is the first thing a customer sees. A meetup with a
        // photograph of the site reads as a real event; the category wash below
        // is the fallback, shown here so the partner knows what they are
        // choosing between rather than discovering it on the customer's screen.
        DealioCard {
            FormLabel("Cover photo")
            CoverPicker(
                coverImage = coverImage,
                category = category,
                uploading = uploading,
                onPick = { pickCover.launch("image/*") },
                onRemove = { coverImage = "" },
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── What ────────────────────────────────────────────────────────────
        DealioCard {
            FormLabel("What is it")
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. Nature County site walk-through", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
            Spacer(Modifier.height(14.dp))
            FormLabel("Kind of meetup")
            ChoiceChips(
                options = MeetupCategory.entries,
                selected = category,
                label = { it.label },
                tint = { it.tint },
                onPick = { category = it },
            )
            Spacer(Modifier.height(14.dp))
            FormLabel("Description")
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
                placeholder = { Text("What will happen, and why someone should come", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
            Spacer(Modifier.height(14.dp))
            FormLabel("Topics (optional)")
            TopicEditor(
                topics = topics,
                onAdd = { topics = (topics + it).take(MAX_TOPICS) },
                onRemove = { topics = topics - it },
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── When ────────────────────────────────────────────────────────────
        DealioCard {
            FormLabel("When")
            DayStrip(date) { date = it }
            Spacer(Modifier.height(10.dp))
            ChoiceChips(meetupTimes, time, label = { it }, onPick = { time = it })
        }

        Spacer(Modifier.height(12.dp))

        // ── Where ───────────────────────────────────────────────────────────
        DealioCard {
            FormLabel("Where")
            ChoiceChips(MeetupMode.entries, mode, label = { it.label }, onPick = { mode = it })
            if (needsPlace) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    placeholder = { Text("Address or landmark", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
            }
            if (mode != MeetupMode.IN_PERSON) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = onlineLink, onValueChange = { onlineLink = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Zoom or Meet link", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Only people who say they're going will see this link.",
                    color = TextSecondary, fontSize = 11.sp,
                )
            }
            if (needsPlace) {
                Spacer(Modifier.height(14.dp))
                FormLabel("Photos of the place (optional)")
                PhotoStrip(
                    photos = photos,
                    uploading = uploading,
                    onAdd = { pickPhoto.launch("image/*") },
                    onRemove = { photos = photos - it },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Who can see it ──────────────────────────────────────────────────
        DealioCard {
            FormLabel("Who can see this")
            VisibilityToggle(isPublic = isPublic, onChange = { isPublic = it })
            if (isPublic) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = city, onValueChange = { city = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("City") },
                    placeholder = { Text("e.g. Hyderabad", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (city.isBlank())
                        "Set a city, or customers browsing won't find this."
                    else
                        "Customers who follow $city will see this in their app.",
                    color = if (city.isBlank()) com.dealio.app.ui.theme.Orange else TextSecondary,
                    fontSize = 11.sp, lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            FormLabel("Limit numbers (optional)")
            OutlinedTextField(
                value = capacity, onValueChange = { v -> capacity = v.filter { it.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Leave blank for no limit", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Who to invite. Creation only: on an edit the invite list is
        // managed from the event page, where the RSVPs are.
        if (editing == null) {
            DealioCard {
                FormLabel(invitePickerLabel(picked.size))
                Text(
                    "Everyone you pick gets the same invite. Dealio customers see it in their app too.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                )
                Spacer(Modifier.height(12.dp))
                InvitePicker(
                    people = people,
                    selected = picked,
                    onToggle = { p -> picked = if (picked.contains(p.key)) picked - p.key else picked + p.key },
                )
                if (picked.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SelectedCountBar(picked.size)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Private note ────────────────────────────────────────────────────
        DealioCard {
            FormLabel("Private note")
            Text("Only you see this — it is not part of the invite.", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
                shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
            )
        }

        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = when {
                uploading -> "Uploading photo…"
                saving -> "Saving…"
                editing != null -> "Save changes"
                picked.isEmpty() -> "Create meetup"
                else -> "Create and invite ${picked.size}"
            },
            enabled = canSave,
            onClick = {
                val cap = capacity.toIntOrNull()?.takeIf { it > 0 }
                val cityValue = city.trim().ifBlank { null }
                onSubmit(
                    CreateCpMeetupRequest(
                        title = title.trim(),
                        location = location.trim().ifBlank { if (needsPlace) "" else "Online" },
                        date = date,
                        time = time,
                        description = description.trim().ifBlank { null },
                        category = category.wire,
                        coverImage = coverImage?.trim()?.ifBlank { null },
                        photos = photos,
                        topics = topics,
                        city = cityValue,
                        mapsLink = mapsLink.trim().ifBlank { null },
                        mode = mode.wire,
                        onlineLink = onlineLink.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null },
                        visibility = if (isPublic) "PUBLIC" else "PRIVATE",
                        capacity = cap,
                    ),
                    UpdateCpMeetupRequest(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        category = category.wire,
                        // "" is how the server is told to clear the cover, which
                        // is what Remove leaves behind; null would mean "leave it".
                        coverImage = coverImage ?: "",
                        photos = photos,
                        topics = topics,
                        location = location.trim().ifBlank { if (needsPlace) "" else "Online" },
                        city = cityValue,
                        mapsLink = mapsLink.trim().ifBlank { null },
                        mode = mode.wire,
                        onlineLink = onlineLink.trim().ifBlank { null },
                        date = date,
                        time = time,
                        notes = notes.trim().ifBlank { null },
                        visibility = if (isPublic) "PUBLIC" else "PRIVATE",
                        capacity = cap,
                    ),
                    people.filter { picked.contains(it.key) },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Public or invite-only, spelled out.
 *
 * A switch would make this a setting people skim past. Two labelled cards, each
 * saying what actually happens, make it a decision — which it is, because one of
 * them puts a partner's name and a physical address in front of strangers.
 */
@Composable
private fun VisibilityToggle(isPublic: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VisibilityOption(
            title = "Anyone in the city",
            body = "Customers browsing meetups will find it",
            selected = isPublic,
            modifier = Modifier.weight(1f),
        ) { onChange(true) }
        VisibilityOption(
            title = "Invite only",
            body = "Only the people you pick can see it",
            selected = !isPublic,
            modifier = Modifier.weight(1f),
        ) { onChange(false) }
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    body: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Teal.copy(alpha = 0.08f) else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Teal else CardBorder, shape)
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Text(
            title,
            color = if (selected) Teal else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Text(body, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

// ─── Photographs ─────────────────────────────────────────────────────────────

/** Two more than anyone uploads, and few enough that the strip stays scannable. */
private const val MAX_PHOTOS = 8
/** Enough to say what a meetup is about; past this it reads as tag soup. */
private const val MAX_TOPICS = 6

/**
 * Pick, preview and clear the cover.
 *
 * The frame is always the full-width shape the customer will see, empty or not,
 * so a partner is choosing a photograph rather than filling in a field. When
 * there is none it shows the category wash that will stand in for it — which
 * makes leaving it blank a decision with a visible outcome instead of an
 * omission.
 */
@Composable
private fun CoverPicker(
    coverImage: String?,
    category: MeetupCategory,
    uploading: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val url = coverImage?.takeIf { it.isNotBlank() }

    Box(
        Modifier.fillMaxWidth().height(160.dp).clip(shape)
            .background(category.gradient)
            .clickable(enabled = !uploading) { onPick() },
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = resolveUrl(url),
                contentDescription = "Cover photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AddAPhoto, null,
                    tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add a photo of the place",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Without one, customers see this colour",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp,
                )
            }
        }

        if (uploading) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp)) }
        }
    }

    if (url != null && !uploading) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Replace", Icons.Outlined.AddAPhoto, Teal, onPick)
            SoftButton("Remove", Icons.Outlined.Close, com.dealio.app.ui.theme.ErrorRed, onRemove)
        }
    }
}

/**
 * Venue photographs, as a row of thumbnails with an add tile on the end.
 *
 * Horizontal rather than a grid: this sits inside a form that is already long,
 * and the pictures are supporting evidence for the address above them, not the
 * thing being edited.
 */
@Composable
private fun PhotoStrip(
    photos: List<String>,
    uploading: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photos.forEach { url ->
            Box(Modifier.size(84.dp)) {
                AsyncImage(
                    model = resolveUrl(url),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(shape).background(CardBorder),
                )
                Icon(
                    Icons.Outlined.Close, "Remove photo", tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .size(20.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onRemove(url) }
                        .padding(4.dp),
                )
            }
        }
        if (photos.size < MAX_PHOTOS) {
            Box(
                Modifier.size(84.dp).clip(shape)
                    .background(Teal.copy(alpha = 0.07f))
                    .border(1.dp, CardBorder, shape)
                    .clickable(enabled = !uploading) { onAdd() },
                contentAlignment = Alignment.Center,
            ) {
                if (uploading) {
                    CircularProgressIndicator(color = Teal, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Outlined.AddAPhoto, "Add photo", tint = Teal, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/**
 * Free-text topics, added one at a time.
 *
 * Not a fixed list. The category already says what kind of gathering this is;
 * topics are where an organiser says it is for first-time buyers, or about
 * rental yield — the specifics that make someone recognise their own situation,
 * and the ones an enum would flatten back out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicEditor(topics: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    val commit = {
        val t = draft.trim()
        if (t.isNotEmpty() && !topics.contains(t)) onAdd(t)
        draft = ""
    }

    if (topics.isNotEmpty()) {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            topics.forEach { topic ->
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Teal.copy(alpha = 0.10f))
                        .clickable { onRemove(topic) }
                        .padding(start = 13.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(topic, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.Close, "Remove $topic", tint = Teal, modifier = Modifier.size(13.dp))
                }
            }
        }
    }

    if (topics.size < MAX_TOPICS) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text("e.g. First-time buyers", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
            },
            trailingIcon = {
                if (draft.isNotBlank()) {
                    Text(
                        "Add",
                        color = Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { commit() }.padding(horizontal = 14.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            shape = RoundedCornerShape(14.dp),
            colors = dealioFieldColors(),
        )
    }
}

@Composable
private fun SoftButton(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.10f))
            .clickable { onClick() }.padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The next fortnight. A meetup is arranged days ahead, not months. */
@Composable
private fun DayStrip(selected: String, onPick: (String) -> Unit) {
    val days = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()).toString() } }
    ChoiceChips(days, selected, label = { meetupDayLabel(it) }, onPick = onPick)
}
