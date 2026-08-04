package com.dealio.app.ui.cp.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
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
import com.dealio.app.data.api.Project
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.cp.contacts.ImportContact
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/** Log one lead by hand, or bring a sheet of them in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLeadChooser(onManual: () -> Unit, onFromFile: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("Add leads", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("One at a time, or a whole spreadsheet.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Choice("Enter details", "Log a single lead against a project.", Icons.Outlined.Edit, Teal, onManual)
            Spacer(Modifier.height(10.dp))
            Choice("Import from Excel", "An .xlsx or .csv with name and phone columns.", Icons.Outlined.TableChart, IconBlue, onFromFile)
        }
    }
}

@Composable
private fun Choice(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(4.dp),
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
 * Review before importing. Unlike contacts, a lead has to belong to a project,
 * so the whole batch is filed against one picked here — a sheet of buyers is
 * almost always for the project the CP is currently pushing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadImportSheet(
    items: List<ImportContact>,
    projects: List<Project>,
    selectedProjectId: Long?,
    working: Boolean,
    progress: Int,
    onPickProject: (Long) -> Unit,
    onToggle: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chosen = items.count { it.selected }
    ModalBottomSheet(onDismissRequest = { if (!working) onDismiss() }, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Import leads", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            if (items.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Nothing usable found. The sheet needs a header row with a name column and a phone column.",
                    color = TextSecondary, fontSize = 13.sp,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text("File these against", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    projects.forEach { p ->
                        val sel = p.id == selectedProjectId
                        Text(
                            p.name,
                            color = if (sel) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier
                                .background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                                .clickable(enabled = !working) { onPickProject(p.id) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
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
                                    listOfNotNull(c.phone, c.email).joinToString(" · "),
                                    color = TextSecondary, fontSize = 11.sp, maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onConfirm,
                enabled = !working && chosen > 0 && selectedProjectId != null,
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
                        when {
                            chosen == 0 -> "Nothing selected"
                            selectedProjectId == null -> "Pick a project"
                            else -> "Import $chosen lead${if (chosen == 1) "" else "s"}"
                        },
                        color = Color.White, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.navigationBarsPadding())
        }
    }
}
