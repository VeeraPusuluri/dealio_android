package com.dealio.app.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.data.api.Project
import com.dealio.app.data.api.UnitRow
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * A project's units, and the grid that shows them.
 *
 * The website has always let a buyer pick an actual unit — Tower A, floor 7,
 * unit 703 — from the project's stored `unitMatrix`, and it is the unit id that
 * travels on the shortlist, the pricing request and eventually the booking. The
 * app only ever offered the *configuration* ("2 BHK"), so a buyer could say
 * what shape of flat they wanted but never which one, and the builder received
 * a shortlist with nothing to reserve.
 *
 * Shared by all three portals deliberately: the buyer picks from this grid, and
 * the builder and the partner read the same grid, so all three are looking at
 * one inventory rather than three renderings of it.
 */

// ─── Status ──────────────────────────────────────────────────────────────────

enum class UnitStatus(val label: String, val fg: Color, val bg: Color) {
    AVAILABLE("Available", Color(0xFF0F766E), Color(0xFFDCF5F1)),
    BOOKED("Booked", Color(0xFFB45309), Color(0xFFFDF3E7)),
    SOLD("Sold", Color(0xFF9F1239), Color(0xFFFDE7EC)),
    HELD("On hold", Color(0xFF4B5563), Color(0xFFEDEFF2));

    val isPickable get() = this == AVAILABLE
}

/**
 * Fold a stored status onto the four the grid draws.
 *
 * The column is free text written by the project wizard, the booking flow and
 * the admin console, so it carries several spellings of the same three states.
 * Anything unrecognised reads as available, which matches the website — a unit
 * with a status nobody set is one nobody has taken.
 */
fun unitStatusOf(raw: String?): UnitStatus = when (raw?.trim()?.lowercase()) {
    "booked", "blocked", "reserved" -> UnitStatus.BOOKED
    "sold", "registered" -> UnitStatus.SOLD
    "hold", "on hold", "on-hold" -> UnitStatus.HELD
    else -> UnitStatus.AVAILABLE
}

/** "Floor 7" / "Ground" — the ground floor is not floor 0 to a buyer. */
fun floorLabel(floor: Int?): String = when (floor) {
    null -> "—"
    0 -> "Ground"
    else -> "Floor $floor"
}

/** "Unit A-703 · 2 BHK · 1240 sqft", with the parts that are missing left out. */
fun unitSummary(u: UnitRow): String = listOfNotNull(
    "Unit ${u.id}",
    u.bhk?.takeIf { it.isNotBlank() },
    u.areaSqft?.takeIf { it > 0 }?.let { "$it sqft" },
).joinToString(" · ")

// ─── Deriving the matrix ─────────────────────────────────────────────────────

/**
 * The units of a project, stored or derived.
 *
 * Most projects carry a real `unitMatrix`. Older ones — created before the
 * wizard collected it — carry only a total, a tower count and a configuration
 * list, and the website synthesizes a plausible grid from those rather than
 * showing a buyer nothing. This mirrors that synthesis exactly, so the same
 * project offers the same unit ids on the web and in the app; a shortlist made
 * on one is a unit the other can find.
 *
 * Synthesized rows are all available, which is the truth available: without a
 * matrix there is no per-unit status to know.
 */
fun unitsOf(p: Project): List<UnitRow> {
    p.unitMatrix?.takeIf { it.isNotEmpty() }?.let { return it }

    val total = p.totalUnits ?: 0
    if (total <= 0) return emptyList()
    val configs = p.configurations?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: listOf("2 BHK")
    val floors = p.floorsPerTower ?: ((total + 3) / 4).coerceIn(1, 15)
    val perFloor = ((total + floors - 1) / floors).coerceAtLeast(1)

    val out = ArrayList<UnitRow>(total)
    for (f in 1..floors) {
        for (u in 1..perFloor) {
            if (out.size >= total) return out
            out += UnitRow(
                // The website's exact expression, `A-${floor}0${unit}`, not a
                // zero-padded equivalent. They agree up to nine units a floor
                // and part company at ten: the web mints "A-1010" where padding
                // would give "A-110". Either is a defensible name; only one is
                // the *same* name, and a shortlist made in the browser has to be
                // the unit this grid highlights.
                id = "A-$f" + "0" + u,
                tower = "A",
                floor = f,
                unit = u,
                bhk = configs[(u - 1) % configs.size],
                status = "Available",
            )
        }
    }
    return out
}

/** Only what a buyer may actually take. */
fun availableUnits(p: Project): List<UnitRow> =
    unitsOf(p).filter { unitStatusOf(it.status).isPickable }

// ─── The grid ────────────────────────────────────────────────────────────────

/**
 * The unit matrix, one row per floor, newest floor at the top.
 *
 * Floors descend because that is how a stack plan is read and how every sales
 * office pins it to the wall — the penthouse is at the top of the board, not
 * scrolled to the bottom of it.
 *
 * @param selectable when false the grid is a read-only inventory board, which
 *        is what the builder and the partner see.
 * @param selected the unit id currently picked, if any.
 */
@Composable
fun UnitMatrixGrid(
    units: List<UnitRow>,
    modifier: Modifier = Modifier,
    selectable: Boolean = false,
    selected: String? = null,
    onSelect: (UnitRow) -> Unit = {},
) {
    if (units.isEmpty()) {
        Text(
            "No unit inventory has been published for this project yet.",
            color = TextSecondary, fontSize = 13.sp, modifier = modifier,
        )
        return
    }

    // Towers first, then floors within each — a matrix with two towers rendered
    // as one flat list of floors would stack A-701 next to B-701 with nothing
    // saying they are different buildings.
    val byTower = units.groupBy { it.tower?.takeIf { t -> t.isNotBlank() } ?: "—" }

    Column(modifier.fillMaxWidth()) {
        UnitLegend()
        Spacer(Modifier.height(10.dp))
        byTower.entries.sortedBy { it.key }.forEach { (tower, towerUnits) ->
            if (byTower.size > 1 || tower != "—") {
                Text(
                    "TOWER $tower".uppercase(),
                    color = TextSecondary, fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(6.dp))
            }
            towerUnits.groupBy { it.floor ?: 0 }.entries
                .sortedByDescending { it.key }
                .forEach { (floor, floorUnits) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (floor == 0) "G" else "$floor",
                            color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(22.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        // Horizontal, not wrapped: a floor is a row of units and
                        // wrapping one onto two lines reads as two floors.
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            floorUnits.sortedBy { it.unit ?: 0 }.forEach { u ->
                                UnitCell(
                                    unit = u,
                                    isSelected = selectable && u.id == selected,
                                    selectable = selectable,
                                    onClick = { onSelect(u) },
                                )
                            }
                        }
                    }
                }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UnitCell(unit: UnitRow, isSelected: Boolean, selectable: Boolean, onClick: () -> Unit) {
    val status = unitStatusOf(unit.status)
    // A taken unit is never tappable even in a selectable grid — the buyer must
    // be able to see the whole board, and see what is gone.
    val enabled = selectable && status.isPickable
    Column(
        Modifier
            .width(58.dp)
            .background(if (isSelected) Teal else status.bg, RoundedCornerShape(8.dp))
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) Teal else CardBorder,
                RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            unit.id,
            color = if (isSelected) Color.White else status.fg,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
        unit.bhk?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else status.fg.copy(alpha = 0.75f),
                fontSize = 8.sp, maxLines = 1,
            )
        }
    }
}

@Composable
private fun UnitLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitStatus.entries.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(s.fg, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(s.label, color = TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** A one-line summary of a picked unit, for confirming a choice before sending it. */
@Composable
fun PickedUnitRow(unit: UnitRow, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Teal.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, Teal.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(unitSummary(unit), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(
                    unit.tower?.takeIf { it.isNotBlank() }?.let { "Tower $it" },
                    floorLabel(unit.floor),
                ).joinToString(" · "),
                color = TextSecondary, fontSize = 11.sp,
            )
        }
    }
}

/** The counts a builder reads at a glance, derived rather than stored. */
data class UnitTally(val total: Int, val available: Int, val booked: Int, val sold: Int)

/**
 * Tally a project's units from the matrix, falling back to the stored counters.
 *
 * The matrix is the truth when it exists — `availableUnits`/`bookedUnits` are
 * counters maintained by hand and drift the moment anyone edits the matrix
 * directly. Where there is no matrix the counters are all there is.
 */
fun tallyOf(p: Project): UnitTally {
    val units = p.unitMatrix?.takeIf { it.isNotEmpty() }
    if (units == null) {
        val total = p.totalUnits ?: 0
        val booked = p.bookedUnits ?: 0
        val sold = p.soldUnits ?: 0
        return UnitTally(total, p.availableUnits ?: (total - booked - sold).coerceAtLeast(0), booked, sold)
    }
    val byStatus = units.groupingBy { unitStatusOf(it.status) }.eachCount()
    return UnitTally(
        total = units.size,
        available = byStatus[UnitStatus.AVAILABLE] ?: 0,
        booked = byStatus[UnitStatus.BOOKED] ?: 0,
        sold = byStatus[UnitStatus.SOLD] ?: 0,
    )
}
