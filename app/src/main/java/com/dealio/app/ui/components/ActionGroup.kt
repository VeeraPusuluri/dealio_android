package com.dealio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

// Accent palette for action-row icon tiles. Rotating hues makes a long list
// scannable by colour as well as label.
val IconBlue = Color(0xFF2D7FF9)
val IconGreen = Color(0xFF24A148)
val IconOrange = Color(0xFFFF8930)
val IconPurple = Color(0xFF7B61FF)
val IconRed = Color(0xFFE5484D)

data class ActionItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

/**
 * A titled list of navigation rows sharing one rounded container.
 *
 * The alternative — one outlined box per row — turns a long menu into a column
 * of floating cards with a gap between every entry, which reads as many
 * unrelated things rather than one grouped list.
 */
@Composable
fun ActionGroup(title: String, items: List<ActionItem>, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionLabel(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = CardBorder.copy(alpha = 0.7f),
                        // Inset to start under the label, not the icon tile.
                        modifier = Modifier.padding(start = 58.dp),
                    )
                }
                ActionRow(item.label, item.icon, item.tint, item.onClick)
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tinted tile rather than a solid block of colour — saturated squares
        // stacked down the screen fought with the content.
        Box(
            Modifier.size(34.dp).background(tint.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}
