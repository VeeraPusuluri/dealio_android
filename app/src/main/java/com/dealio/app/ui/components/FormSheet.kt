package com.dealio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.ButtonDisabled
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealGradient
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush

/**
 * The shell every data-entry bottom sheet in the app shares: a titled header
 * with a way out, a body that scrolls on its own, and an action bar pinned to
 * the bottom.
 *
 * Pinning matters — these forms are long enough to scroll, and a submit button
 * that scrolls away with the last field reads as a form with no way to finish.
 * The bar clears the keyboard *and* the system navigation bar with a single
 * union of both insets, since when the keyboard is up it already covers the
 * navigation bar and padding for both would leave a visible gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    title: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    accent: Color = Teal,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Created here rather than defaulted into the signature: a default argument
    // is evaluated at the call site, which would force every caller to opt in to
    // the experimental sheet API.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetGrabber() },
        // Top inset only. The status bar still has to clear a form tall enough
        // to expand to the full screen, but the bottom is handled per-section
        // below so the action bar can hug the keyboard instead.
        contentWindowInsets = { WindowInsets.statusBars },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(42.dp).background(tintBrush(accent), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, color = TextSecondary, fontSize = 12.5.sp, lineHeight = 17.sp)
                    }
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .background(CardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Close, "Close", tint = TextSecondary, modifier = Modifier.size(16.dp)) }
            }

            HorizontalDivider(color = CardBorder)

            // fill = false so a short form wraps to its content instead of
            // stretching the sheet to full height.
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                content = content,
            )

            HorizontalDivider(color = CardBorder)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), content = footer)
        }
    }
}

/** Slimmer, quieter grab handle than the Material default. */
@Composable
private fun SheetGrabber() {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(38.dp).height(4.dp).background(CardBorder, RoundedCornerShape(2.dp)))
    }
}

/**
 * Groups a handful of fields under a caption. Long forms read as a wall of
 * identical boxes without them.
 */
@Composable
fun SheetSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(), color = TextSecondary, fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(CardBorder))
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** The one text field shape these sheets use, so every row lines up. */
@Composable
fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    supporting: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = TextSecondary.copy(alpha = 0.6f)) } },
        supportingText = supporting?.let { { Text(it, color = TextSecondary, fontSize = 11.sp) } },
        leadingIcon = icon?.let { { Icon(it, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) } },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        colors = dealioFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

/** Full-width gradient submit. Greys out rather than disappearing when blocked. */
@Composable
fun SheetSubmitButton(
    text: String,
    enabled: Boolean,
    working: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = TealGradient,
) {
    val live = enabled && !working
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                if (live) gradient else Brush.linearGradient(listOf(ButtonDisabled, ButtonDisabled)),
                RoundedCornerShape(16.dp),
            )
            .clickable(enabled = live, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (working) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Text(
                text,
                color = if (live) Color.White else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Secondary, low-emphasis action for the footer row. */
@Composable
fun RowScope.SheetGhostButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .height(52.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

/** Small selectable pill — quick-pick shortcuts above a free-text field. */
@Composable
fun SheetChip(text: String, selected: Boolean, onClick: () -> Unit, accent: Color = Teal) {
    Text(
        text,
        color = if (selected) Color.White else TextSecondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) accent else Color.White, RoundedCornerShape(11.dp))
            .border(1.dp, if (selected) accent else CardBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    )
}
