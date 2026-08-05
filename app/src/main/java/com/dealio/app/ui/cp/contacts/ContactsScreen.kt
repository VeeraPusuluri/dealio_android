package com.dealio.app.ui.cp.contacts

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.Spreadsheet
import com.dealio.app.data.api.CpContact
import com.dealio.app.data.api.CpContactPayload
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.FormSheet
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.IconOrange
import com.dealio.app.ui.components.IconPurple
import com.dealio.app.ui.components.SheetChip
import com.dealio.app.ui.components.SheetField
import com.dealio.app.ui.components.SheetGhostButton
import com.dealio.app.ui.components.SheetSection
import com.dealio.app.ui.components.SheetSubmitButton
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.cp.growth.dial
import com.dealio.app.ui.cp.growth.openWhatsApp
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.FieldFill
import com.dealio.app.ui.theme.Mist
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How the book is ordered. Newest first day to day; by spend when a project launches. */
enum class ContactSort(val label: String) { RECENT("Recent"), INVESTMENT("Top investors") }

data class ContactsState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<CpContact> = emptyList(),
    val sort: ContactSort = ContactSort.RECENT,
    val message: String? = null,
    // Rows waiting to be reviewed before import. Held here rather than in the
    // composable so a rotation mid-review doesn't discard a parsed sheet.
    val staged: List<ImportContact>? = null,
    val stagedTitle: String = "",
    val importing: Boolean = false,
    val importProgress: Int = 0,
)

class ContactsViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getContacts()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun save(existing: CpContact?, p: CpContactPayload) {
        viewModelScope.launch {
            val r = if (existing == null) repo.addContact(p) else repo.updateContact(existing.id, p)
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Saved") }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val r = repo.deleteContact(id)
            _state.update { it.copy(message = (r as? ApiResult.Error)?.message ?: "Contact deleted") }
            if (r is ApiResult.Success) load(silent = true)
        }
    }

    fun setSort(sort: ContactSort) = _state.update { it.copy(sort = sort) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    // ── Bulk import ──────────────────────────────────────────────────────────

    /** Parse a picked .xlsx/.csv off the main thread and stage what it yielded. */
    fun stageFromSheet(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val staged = withContext(Dispatchers.IO) {
                runCatching {
                    val name = displayName(ctx, uri)
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    rowsToContacts(Spreadsheet.read(bytes, name))
                }.getOrDefault(emptyList())
            }
            _state.update { it.copy(staged = staged, stagedTitle = "Import from Excel") }
        }
    }

    fun stageFromPhone() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val staged = withContext(Dispatchers.IO) {
                runCatching { readDeviceContacts(ctx) }.getOrDefault(emptyList())
            }
            // Everyone starts unticked — a 500-contact address book should not
            // default to importing all of it.
            _state.update { it.copy(staged = staged, stagedTitle = "Import from phone") }
        }
    }

    fun toggleStaged(index: Int) = _state.update { s ->
        s.copy(staged = s.staged?.mapIndexed { i, c -> if (i == index) c.copy(selected = !c.selected) else c })
    }

    fun selectAllStaged(select: Boolean) = _state.update { s ->
        s.copy(staged = s.staged?.map { it.copy(selected = select) })
    }

    fun clearStaged() = _state.update { it.copy(staged = null, importProgress = 0) }

    /**
     * There is no bulk endpoint, so this is one POST per row. Failures are
     * counted rather than aborting — one bad number shouldn't strand the rest.
     */
    fun importStaged() {
        val chosen = _state.value.staged?.filter { it.selected }.orEmpty()
        if (chosen.isEmpty()) return
        _state.update { it.copy(importing = true, importProgress = 0) }
        viewModelScope.launch {
            var ok = 0
            chosen.forEachIndexed { i, c ->
                if (repo.addContact(c.toPayload()) is ApiResult.Success) ok++
                _state.update { it.copy(importProgress = i + 1) }
            }
            val failed = chosen.size - ok
            _state.update {
                it.copy(
                    importing = false, staged = null, importProgress = 0,
                    message = if (failed == 0) "Imported $ok contact${if (ok == 1) "" else "s"}."
                    else "Imported $ok of ${chosen.size} — $failed could not be saved.",
                )
            }
            load(silent = true)
        }
    }
}

/** Best-effort file name for the picked document, used to pick the parser. */
private fun displayName(ctx: Context, uri: Uri): String {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0) c.getString(i)?.let { return it }
        }
    }
    return uri.lastPathSegment.orEmpty()
}

@Composable
fun ContactsScreen(nav: NavController, vm: ContactsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CpContact?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CpContact?>(null) }

    val pickSheet = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.stageFromSheet(it) }
    }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.stageFromPhone()
    }
    val context = LocalContext.current
    fun importFromPhone() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.stageFromPhone() else askContacts.launch(Manifest.permission.READ_CONTACTS)
    }

    SubScreenScaffold(
        "Contacts", nav,
        actions = {
            Row(
                Modifier.padding(end = 8.dp).background(Teal, RoundedCornerShape(10.dp))
                    .clickable { showChooser = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    ) { inner ->
        when {
            state.loading -> LoadingState(Modifier.padding(inner))
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() }, modifier = Modifier.padding(inner))
            state.items.isEmpty() -> Box(Modifier.padding(inner)) {
                EmptyState(Icons.Outlined.Contacts, "No contacts yet", "Tap Add to enter one, or import your phone book or a spreadsheet.")
            }
            else -> LazyColumn(
                modifier = Modifier.padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SortRow(state.sort, vm::setSort)
                }
                // Nobody with an unknown capacity outranks someone with a known one,
                // so unset investments sink to the bottom rather than sorting as zero.
                val ordered = when (state.sort) {
                    ContactSort.RECENT -> state.items
                    ContactSort.INVESTMENT -> state.items.sortedByDescending { it.investment ?: -1.0 }
                }
                items(ordered.size) { i ->
                    val c = ordered[i]
                    ContactCard(
                        c,
                        onEdit = { editing = c; showDialog = true },
                        onDelete = { deleting = c },
                    )
                }
            }
        }
    }

    if (showChooser) {
        AddContactChooser(
            onManual = { showChooser = false; editing = null; showDialog = true },
            onFromPhone = { showChooser = false; importFromPhone() },
            onFromFile = {
                showChooser = false
                // Some providers report .xlsx/.csv under generic types, so accept
                // the specific ones plus a catch-all rather than showing a picker
                // where the user's own file is greyed out.
                pickSheet.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/comma-separated-values",
                        "text/plain",
                        "*/*",
                    ),
                )
            },
            onDismiss = { showChooser = false },
        )
    }

    state.staged?.let { staged ->
        ImportPreviewSheet(
            title = state.stagedTitle,
            items = staged,
            working = state.importing,
            progress = state.importProgress,
            onToggle = vm::toggleStaged,
            onSelectAll = vm::selectAllStaged,
            onConfirm = vm::importStaged,
            onDismiss = vm::clearStaged,
        )
    }

    if (showDialog) {
        ContactSheet(editing, onDismiss = { showDialog = false }) { p ->
            vm.save(editing, p); showDialog = false
        }
    }

    // Delete sits in the same action row as Call, so a mis-tap is a plausible
    // way to lose a contact the CP spent months collecting. Ask first.
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            confirmButton = {
                TextButton(onClick = { vm.delete(target.id); deleting = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = TextSecondary) } },
            title = { Text("Delete contact?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("${target.name} will be removed from your book.", color = TextSecondary, fontSize = 13.sp) },
        )
    }
    LaunchedEffect(state.message) { state.message?.let { vm.clearMessage() } }
}

// Accents rotate by name so a long book is scannable by colour as well as by
// text — two Rameshes still land on different tiles.
private val CONTACT_ACCENTS = listOf(Teal, IconBlue, IconPurple, IconOrange, IconGreen)

private fun accentFor(name: String): Color {
    val i = name.hashCode() % CONTACT_ACCENTS.size
    return CONTACT_ACCENTS[if (i < 0) i + CONTACT_ACCENTS.size else i]
}

/**
 * One entry in the book.
 *
 * A CP reads this list to decide who to ring next, so the card leads with who
 * the person is and what they can spend, demotes the qualifiers to chips that
 * wrap instead of a dot-joined line that truncates, and puts call and WhatsApp
 * on the card — previously the number could only be read off and re-typed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactCard(c: CpContact, onEdit: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val accent = remember(c.name) { accentFor(c.name) }

    DealioCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).background(tintBrush(accent), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsOf(c.name), color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    c.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                c.designation?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(3.dp))
                // The flag carries the country at a glance; the code is still
                // spelled out so it can be read off and dialled.
                Text(
                    "${flagFor(c.countryCode)} ${formatPhone(c.countryCode, c.phone)}",
                    color = TextPrimary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
            }
            // Capacity is what the investor sort ranks on, so it gets the one
            // spot the eye lands on after the name rather than a line of body text.
            c.investment?.takeIf { it > 0 }?.let {
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.background(IconGreen.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Savings, null, tint = IconGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${formatSalaryShort(it)}/yr", color = IconGreen, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                }
            }
        }

        val tags = c.tags?.split(",")?.map(String::trim)?.filter { it.isNotBlank() }.orEmpty()
        val hasChips = !c.bhkPreference.isNullOrBlank() || tags.isNotEmpty() ||
            (c.salary ?: 0.0) > 0 || !c.address.isNullOrBlank() || !c.email.isNullOrBlank()
        if (hasChips) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                c.bhkPreference?.takeIf { it.isNotBlank() }?.let { MetaChip(it, Icons.Outlined.Sell, Teal) }
                tags.forEach { MetaChip(it, null, accent) }
                c.salary?.takeIf { it > 0 }?.let { MetaChip("Earns ${formatSalaryShort(it)}", Icons.Outlined.Payments, TextSecondary) }
                c.address?.takeIf { it.isNotBlank() }?.let { MetaChip(it, Icons.Outlined.LocationOn, TextSecondary) }
                c.email?.takeIf { it.isNotBlank() }?.let { MetaChip(it, Icons.Outlined.MailOutline, TextSecondary) }
            }
        }

        if (!c.notes.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().background(Mist, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Text(
                    c.notes!!, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = CardBorder.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (c.phone.isNotBlank()) {
                // Dial the international form: a UAE contact rung as a bare
                // national number reaches the wrong person, or nobody.
                ActionPill("Call", Icons.Outlined.Phone, IconGreen) {
                    dial(ctx, "+${dialable(c.countryCode, c.phone)}")
                }
                Spacer(Modifier.width(8.dp))
                ActionPill("WhatsApp", Icons.Outlined.ChatBubbleOutline, Teal) {
                    openWhatsApp(ctx, c.phone, "Hi ${c.name.trim()}, ", c.countryCode)
                }
            }
            Spacer(Modifier.weight(1f))
            IconAction(Icons.Outlined.Edit, "Edit", TextSecondary, onEdit)
            Spacer(Modifier.width(8.dp))
            IconAction(Icons.Outlined.Delete, "Delete", ErrorRed, onDelete)
        }
    }
}

/** A qualifier — BHK, tag, salary, locality — sized to be skimmed, not read. */
@Composable
private fun MetaChip(text: String, icon: ImageVector?, color: Color) {
    Row(
        Modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

/** Labelled tap target for the two things a CP does from this list. */
@Composable
private fun ActionPill(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Edit and delete: present, but not competing with the call actions. */
@Composable
private fun IconAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
            .background(FieldFill)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/**
 * When a project launches a CP wants to work the book top-down by who can afford
 * it, not by who they happened to add last. Two chips is the whole feature.
 */
@Composable
private fun SortRow(sort: ContactSort, onSort: (ContactSort) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Sort", color = TextSecondary, fontSize = 12.sp)
        ContactSort.entries.forEach { option ->
            SheetChip(option.label, selected = sort == option, onClick = { onSort(option) })
        }
    }
}

/**
 * Dial-code picker. A scrolling row of flags rather than a dropdown: India is
 * the answer almost every time and stays first, so the common case is already
 * selected and the rest are one tap away without opening a menu.
 */
@Composable
private fun CountryCodeRow(selected: String, onSelect: (String) -> Unit) {
    val chosen = normalizeDialCode(selected)
    // A code that came off an import isn't necessarily one the picker offers —
    // show it anyway, or editing a British contact would silently make them Indian.
    val options = remember(chosen) {
        if (PICKER_DIAL_CODES.any { it.code == chosen }) PICKER_DIAL_CODES
        else PICKER_DIAL_CODES + (DIAL_CODES.firstOrNull { it.code == chosen } ?: DialCode(chosen, "🌐", "Other"))
    }
    Column {
        Text("Country code", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { dc ->
                SheetChip(
                    text = "${dc.flag} ${dc.code}",
                    selected = dc.code == chosen,
                    onClick = { onSelect(dc.code) },
                )
            }
        }
    }
}

private val BHK_CHOICES = listOf("1 BHK", "2 BHK", "3 BHK", "4 BHK", "Villa", "Plot")

/**
 * Nine fields in an AlertDialog left the form scrolling inside a box a third of
 * the screen tall, with the Save button hidden behind the keyboard. A sheet
 * gives them room and groups them by the question each one answers.
 */
@Composable
private fun ContactSheet(existing: CpContact?, onDismiss: () -> Unit, onSave: (CpContactPayload) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var countryCode by remember { mutableStateOf(normalizeDialCode(existing?.countryCode)) }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var bhk by remember { mutableStateOf(existing?.bhkPreference ?: "") }
    var tags by remember { mutableStateOf(existing?.tags ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var designation by remember { mutableStateOf(existing?.designation ?: "") }
    var salary by remember { mutableStateOf(existing?.salary?.let { formatSalaryInput(it) } ?: "") }
    var investment by remember { mutableStateOf(existing?.investment?.let { formatSalaryInput(it) } ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    // A figure the CP typed is theirs to keep — only a still-seeded one follows
    // the salary. Matches how the backend decides whether to re-seed on save.
    var investmentEdited by remember {
        mutableStateOf(existing?.investment != null && existing.investment != seedInvestment(existing.salary))
    }

    val canSave = name.isNotBlank() && phone.count(Char::isDigit) >= 6

    FormSheet(
        title = if (existing == null) "New contact" else "Edit contact",
        subtitle = if (existing == null) {
            "Name and phone are enough — the rest sharpens your follow-up."
        } else {
            "Changes save to your contact book only."
        },
        icon = if (existing == null) Icons.Outlined.PersonAdd else Icons.Outlined.Edit,
        onDismiss = onDismiss,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetGhostButton("Cancel", onDismiss)
                Box(Modifier.weight(1.6f)) {
                    SheetSubmitButton(
                        text = if (existing == null) "Add contact" else "Save changes",
                        enabled = canSave,
                        working = false,
                        onClick = {
                            // The field can still hold a raw "+971…" if the CP
                            // never typed enough digits to resolve it; settle it here.
                            val (saveCode, savePhone) = splitDialCode(phone.trim(), fallback = countryCode)
                            onSave(
                                CpContactPayload(
                                    name = name.trim(),
                                    phone = savePhone,
                                    countryCode = saveCode,
                                    email = email.ifBlank { null },
                                    notes = notes.ifBlank { null },
                                    tags = tags.ifBlank { null },
                                    bhkPreference = bhk.ifBlank { null },
                                    designation = designation.ifBlank { null },
                                    salary = salary.toDoubleOrNull(),
                                    investment = investment.toDoubleOrNull(),
                                    address = address.ifBlank { null },
                                ),
                            )
                        },
                    )
                }
            }
        },
    ) {
        SheetSection("Who they are") {
            SheetField(
                value = name, onValueChange = { name = it },
                label = "Full name", icon = Icons.Outlined.Person, placeholder = "e.g. Ramesh Kumar",
            )
            // Pasting a number with its own code retags the picker rather than
            // stripping the code — that is how a number arrives off WhatsApp.
            CountryCodeRow(selected = countryCode, onSelect = { countryCode = it })
            SheetField(
                value = phone,
                onValueChange = { typed ->
                    // The raw text may still be mid-way through a "+971…" the CP
                    // is typing, so hold it until it resolves to a real code.
                    val raw = typed.filter { it.isDigit() || it == '+' }
                    val split = trySplitDialCode(raw, fallback = countryCode)
                    if (split != null) {
                        countryCode = split.first
                        phone = split.second
                    } else {
                        phone = raw
                    }
                },
                label = "Phone", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone,
                placeholder = if (countryCode == DEFAULT_DIAL_CODE) "10-digit mobile" else "Number without $countryCode",
                supporting = phone.takeIf { it.length >= 6 && it.all(Char::isDigit) }
                    ?.let { "Saves as ${formatPhone(countryCode, it)}" },
            )
            SheetField(
                value = email, onValueChange = { email = it },
                label = "Email (optional)", icon = Icons.Outlined.MailOutline, keyboardType = KeyboardType.Email,
            )
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("What they earn") {
            SheetField(
                value = designation, onValueChange = { designation = it },
                label = "Designation", icon = Icons.Outlined.Work, placeholder = "e.g. Senior Engineer",
            )
            SheetField(
                value = salary,
                onValueChange = {
                    salary = it.filter(Char::isDigit)
                    if (!investmentEdited) {
                        investment = seedInvestment(salary.toDoubleOrNull())?.let(::formatSalaryInput).orEmpty()
                    }
                },
                label = "Annual salary (₹)", icon = Icons.Outlined.Payments,
                keyboardType = KeyboardType.Number, placeholder = "1200000",
                // Seven digits are hard to read back; echo them as the CP thinks.
                supporting = salary.toDoubleOrNull()?.takeIf { it > 0 }?.let { "That's ${formatSalaryShort(it)} a year" },
            )
            SheetField(
                value = investment,
                onValueChange = { investment = it.filter(Char::isDigit); investmentEdited = true },
                label = "Investment capacity (₹/yr)", icon = Icons.Outlined.Savings,
                keyboardType = KeyboardType.Number, placeholder = "240000",
                supporting = when {
                    investment.isBlank() -> "Filled in at 20% of salary — change it if you know better"
                    investmentEdited -> investment.toDoubleOrNull()?.takeIf { it > 0 }
                        ?.let { "${formatSalaryShort(it)} a year — your figure, not the 20% default" }
                    else -> investment.toDoubleOrNull()?.takeIf { it > 0 }
                        ?.let { "${formatSalaryShort(it)} a year — 20% of salary. Tap to change." }
                },
            )
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("What they're looking for") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BHK_CHOICES.forEach { choice ->
                    SheetChip(
                        text = choice,
                        selected = bhk.equals(choice, ignoreCase = true),
                        onClick = { bhk = if (bhk.equals(choice, ignoreCase = true)) "" else choice },
                    )
                }
            }
            SheetField(
                value = bhk, onValueChange = { bhk = it },
                label = "Configuration", placeholder = "Tap a chip above, or type your own",
            )
            SheetField(
                value = address, onValueChange = { address = it },
                label = "Preferred area", icon = Icons.Outlined.LocationOn, placeholder = "e.g. Gachibowli",
            )
            SheetField(
                value = tags, onValueChange = { tags = it },
                label = "Tags", icon = Icons.Outlined.Sell, placeholder = "hot, nri, referral",
                supporting = "Separate with commas",
            )
        }
        Spacer(Modifier.height(20.dp))

        SheetSection("Notes") {
            SheetField(
                value = notes, onValueChange = { notes = it },
                label = "Anything worth remembering", singleLine = false, minLines = 3,
            )
        }
    }
}

/** The stored value is a Double; show it back as the plain integer the CP typed. */
private fun formatSalaryInput(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

/** 1200000 -> "₹12 L", 25000000 -> "₹2.5 Cr" — read at a glance, not audited. */
internal fun formatSalaryShort(v: Double): String = when {
    v >= 1_00_00_000 -> "₹${trimZero(v / 1_00_00_000)} Cr"
    v >= 1_00_000 -> "₹${trimZero(v / 1_00_000)} L"
    else -> "₹${v.toLong()}"
}

private fun trimZero(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
