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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.theme.ErrorRed
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

data class ContactsState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<CpContact> = emptyList(),
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
                items(state.items.size) { i ->
                    val c = state.items[i]
                    DealioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(c.phone, color = TextSecondary, fontSize = 12.sp)
                                // What they do and earn is how a CP decides who to call first.
                                val qualifiers = listOfNotNull(
                                    c.designation?.takeIf { it.isNotBlank() },
                                    c.salary?.takeIf { it > 0 }?.let { formatSalaryShort(it) },
                                    c.address?.takeIf { it.isNotBlank() },
                                )
                                if (qualifiers.isNotEmpty()) {
                                    Text(qualifiers.joinToString(" · "), color = TextSecondary, fontSize = 11.sp, maxLines = 2)
                                }
                                if (!c.bhkPreference.isNullOrBlank() || !c.tags.isNullOrBlank()) {
                                    Text(listOfNotNull(c.bhkPreference, c.tags).joinToString(" · "), color = Teal, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Outlined.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(20.dp).clickable { editing = c; showDialog = true })
                            Spacer(Modifier.width(14.dp))
                            Icon(Icons.Outlined.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp).clickable { vm.delete(c.id) })
                        }
                        if (!c.notes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(c.notes!!, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
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
        ContactDialog(editing, onDismiss = { showDialog = false }) { p ->
            vm.save(editing, p); showDialog = false
        }
    }
    LaunchedEffect(state.message) { state.message?.let { vm.clearMessage() } }
}

@Composable
private fun ContactDialog(existing: CpContact?, onDismiss: () -> Unit, onSave: (CpContactPayload) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var bhk by remember { mutableStateOf(existing?.bhkPreference ?: "") }
    var tags by remember { mutableStateOf(existing?.tags ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var designation by remember { mutableStateOf(existing?.designation ?: "") }
    var salary by remember { mutableStateOf(existing?.salary?.let { formatSalaryInput(it) } ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CpContactPayload(
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.ifBlank { null },
                            notes = notes.ifBlank { null },
                            tags = tags.ifBlank { null },
                            bhkPreference = bhk.ifBlank { null },
                            designation = designation.ifBlank { null },
                            salary = salary.toDoubleOrNull(),
                            address = address.ifBlank { null },
                        ),
                    )
                },
                enabled = name.isNotBlank() && phone.length >= 6,
            ) { Text(if (existing == null) "Add" else "Save", color = Teal, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        title = { Text(if (existing == null) "New contact" else "Edit contact", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            // Nine fields overflow the dialog on a short screen, so let it scroll.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field("Name", name) { name = it }
                Field("Phone", phone) { phone = it.filter(Char::isDigit) }
                Field("Email", email) { email = it }
                Field("Designation", designation) { designation = it }
                Field("Annual salary (₹)", salary, KeyboardType.Number) { salary = it.filter { c -> c.isDigit() } }
                Field("Address / location", address) { address = it }
                Field("BHK preference", bhk) { bhk = it }
                Field("Tags (comma separated)", tags) { tags = it }
                Field("Notes", notes) { notes = it }
            }
        },
    )
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

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
