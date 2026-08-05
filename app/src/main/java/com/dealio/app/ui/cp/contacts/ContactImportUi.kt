package com.dealio.app.ui.cp.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/** Add one by hand, pull the phone's address book, or read a sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactChooser(
    onManual: () -> Unit,
    onFromPhone: () -> Unit,
    onFromFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            Text("Add contacts", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("One at a time, or bring in a whole list.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            ChooserRow(
                "Enter details", "Type a single contact in yourself.",
                Icons.Outlined.Edit, Teal, onManual,
            )
            Spacer(Modifier.height(10.dp))
            ChooserRow(
                "Import from phone", "Pick people from your address book.",
                Icons.Outlined.Contacts, IconGreen, onFromPhone,
            )
            Spacer(Modifier.height(10.dp))
            ChooserRow(
                "Import from Excel", "An .xlsx or .csv with name and phone columns.",
                Icons.Outlined.TableChart, IconBlue, onFromFile,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ChooserRow(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(tint.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

/**
 * Review before committing. Import writes one row per API call, so a 200-row
 * sheet is 200 requests — worth letting the CP untick the rows they don't want
 * first rather than importing everything and deleting afterwards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheet(
    title: String,
    items: List<ImportContact>,
    working: Boolean,
    progress: Int,
    onToggle: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chosen = items.count { it.selected }
    ModalBottomSheet(onDismissRequest = { if (!working) onDismiss() }, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Nothing usable found. The sheet needs a header row with a name column and a phone column.",
                    color = TextSecondary, fontSize = 13.sp,
                )
            } else {
                if (items.any { it.investment != null }) {
                    Text(
                        "Where a salary was given, investment capacity is set to 20% of it. " +
                            "Edit any contact afterwards to correct the figure.",
                        color = TextSecondary, fontSize = 12.sp,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$chosen of ${items.size} selected", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clickable { onSelectAll(chosen < items.size) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Checklist, null, tint = Teal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (chosen < items.size) "Select all" else "Clear all",
                            color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(items.size) { i ->
                        val c = items[i]
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !working) { onToggle(i) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = c.selected, onCheckedChange = { onToggle(i) }, enabled = !working,
                                colors = CheckboxDefaults.colors(checkedColor = Teal),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(c.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    listOfNotNull(c.phone, c.designation, c.address).joinToString(" · "),
                                    color = TextSecondary, fontSize = 11.sp, maxLines = 1,
                                )
                                // The seeded figure is what makes the book sortable, so show
                                // it now rather than letting the CP discover it after import.
                                c.investment?.let {
                                    Text(
                                        "Can invest ${formatSalaryShort(it)}/yr",
                                        color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onConfirm,
                enabled = !working && chosen > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, disabledContainerColor = CardBorder),
            ) {
                if (working) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Importing $progress of $chosen…", color = Color.White, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        if (chosen > 0) "Import $chosen contact${if (chosen == 1) "" else "s"}" else "Nothing selected",
                        color = Color.White, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.navigationBarsPadding())
        }
    }
}
