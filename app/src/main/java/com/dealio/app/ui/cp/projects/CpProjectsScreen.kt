package com.dealio.app.ui.cp.projects

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.priceHigh
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.builder.titleCase
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.PortalHeaderSurface
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.JakartaFamily
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TealBright
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.softShadow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpProjectsState(
    val loading: Boolean = true,
    val error: String? = null,
    val all: List<Project> = emptyList(),
    val query: String = "",
    /** Project type the partner has narrowed to; null means every type. */
    val type: String? = null,
) {
    /**
     * Type label → how much inventory carries it, biggest first.
     *
     * Derived rather than hardcoded, so the filter row only ever offers types a
     * partner can actually find something under, and the counts are the real
     * ones rather than a promise the list can't keep.
     */
    val typeCounts: List<Pair<String, Int>> get() = all
        .mapNotNull { it.projectType?.takeIf(String::isNotBlank)?.let(::titleCase) }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    val filtered: List<Project> get() = all.filter { p ->
        val byType = type == null || titleCase(p.projectType) == type
        val byQuery = query.isBlank() ||
            p.name.contains(query, true) ||
            (p.city ?: "").contains(query, true) ||
            (p.locality ?: "").contains(query, true)
        byType && byQuery
    }

    val narrowed: Boolean get() = query.isNotBlank() || type != null
}

class CpProjectsViewModel(app: Application) : CpViewModel(app) {
    private val _state = MutableStateFlow(CpProjectsState())
    val state: StateFlow<CpProjectsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getProjects()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, all = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setType(t: String?) = _state.update { it.copy(type = t) }
    fun clearFilters() = _state.update { it.copy(query = "", type = null) }
}

@Composable
fun CpProjectsScreen(nav: NavController, vm: CpProjectsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        // Search belongs to the header, not below it. As a standalone Material
        // field it read as a form control dropped onto the page, and put a band
        // of dead space between the hero and the first project.
        PortalHeaderSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Projects", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Share & refer to earn", color = TealBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (state.all.isNotEmpty()) {
                    Text(
                        "${state.all.size} LIVE",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            HeaderSearchField(state.query, vm::setQuery)
        }

        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load)
            else -> {
                // Only worth offering when there is more than one kind of thing to
                // sell; a single-type list filters to itself.
                val counts = state.typeCounts
                if (counts.size > 1) {
                    TypeFilterRow(
                        counts = counts,
                        total = state.all.size,
                        selected = state.type,
                        onSelect = vm::setType,
                    )
                }

                when {
                    // Nothing to show splits two ways: filters that missed, or no
                    // inventory at all. They need different words and different ways out.
                    state.filtered.isEmpty() && state.narrowed -> PortalEmptyState(
                        icon = Icons.Outlined.Search,
                        title = "Nothing matches",
                        subtitle = "Try a locality or city, or widen the type filter.",
                        actionLabel = "Clear filters",
                        onAction = vm::clearFilters,
                    )
                    state.filtered.isEmpty() -> PortalEmptyState(
                        icon = Icons.Outlined.Apartment,
                        title = "No projects yet",
                        subtitle = "Once builders publish inventory you can refer, it shows up here.",
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.filtered.size) { i ->
                            CpProjectCard(state.filtered[i]) { nav.navigate(CpRoutes.projectDetail(state.filtered[i].id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSearchField(query: String, onQuery: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Project, locality or city", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = JakartaFamily),
                cursorBrush = SolidColor(TealBright),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close, "Clear search",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onQuery("") },
            )
        }
    }
}

@Composable
private fun TypeFilterRow(
    counts: List<Pair<String, Int>>,
    total: Int,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TypeChip("All", total, selected == null) { onSelect(null) }
        counts.forEach { (label, n) ->
            TypeChip(label, n, selected == label) { onSelect(label) }
        }
    }
}

@Composable
private fun TypeChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) NavyDeep else Color.White)
            .border(1.dp, if (selected) NavyDeep else CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            color = if (selected) Color.White else TextPrimary,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "$count",
            color = if (selected) TealBright else TextSecondary,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
        )
    }
}

// ─── The inventory card ──────────────────────────────────────────────────────
//
// The customer's card stacks name, place and price in a column below the photo.
// A channel partner is not shopping — they are picking what to take to a client
// — so the photo carries the identity (place above name, engraved, on a scrim)
// and the strip below carries the two numbers that decide it: the client's
// price and the partner's payout. Payout is the only orange on the page,
// matching the rule that orange means money.

@Composable
private fun CpProjectCard(p: Project, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(radius = 20.dp)
            .clip(shape)
            .background(Color.White, shape)
            .border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .clickable { onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(196.dp).background(Brush.linearGradient(listOf(NavyMid, Teal)))) {
            val url = resolveUrl(p.imageUrl ?: p.coverUrl)
            if (url != null) {
                AsyncImage(model = url, contentDescription = p.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Apartment, null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(44.dp))
                }
            }
            // Scrim only across the lower half — a full-height wash would dull the
            // photograph, which is the reason the card is image-led at all.
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1.0f to NavyDeep.copy(alpha = 0.90f),
                    ),
                ),
            )

            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // What kind of thing this is decides how a partner pitches it, so it
                // is worth stating on the photo rather than leaving to the detail page.
                val type = titleCase(p.projectType).takeIf { it.isNotBlank() }
                if (type != null) ImageTag(type, Color.White.copy(alpha = 0.92f), NavyDeep)
                if (p.closingSoon) ImageTag("Closing soon", Orange, Color.White)
                else if (p.featured) ImageTag("Featured", TealBright.copy(alpha = 0.92f), NavyDeep)
            }

            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                val place = listOfNotNull(p.locality, p.city).joinToString(", ")
                if (place.isNotBlank()) {
                    Text(
                        place.uppercase(),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    p.name,
                    color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MicroLabel("Price")
                Spacer(Modifier.height(3.dp))
                Text(cpPriceRange(p), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            val payout = cpPayout(p)
            if (payout != null) {
                // A rule between the two figures: they are different kinds of money —
                // what the customer pays and what the partner keeps — and reading
                // straight across without a break invites confusing one for the other.
                Box(Modifier.width(1.dp).height(30.dp).background(CardBorder))
                Spacer(Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.End) {
                    MicroLabel("Your payout")
                    Spacer(Modifier.height(3.dp))
                    Text(payout, color = Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        val configs = configSummary(p.configurations)
        if (configs != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder.copy(alpha = 0.7f)))
            Text(
                configs,
                color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun ImageTag(text: String, background: Color, content: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(background).padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(
            text.uppercase(), color = content,
            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun MicroLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
    )
}

/**
 * What's on sale, in one line.
 *
 * Listing every size spelled it out four times over — "200 sq yd · 300 sq yd ·
 * 400 sq yd · 500 sq yd" — which runs past the card and says little more than
 * its own span. When the sizes share a unit and all carry a number, the span is
 * the useful fact, so they collapse to "200 – 500 sq yd".
 *
 * A list with anything unparseable in it ("Penthouse") has no span to state and
 * is left as written, rather than quietly dropping the odd one out.
 */
private fun configSummary(configs: List<String>?): String? {
    val list = configs?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: return null

    val parsed = list.map { Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*(.+)$").find(it.trim()) }
    if (parsed.any { it == null }) return list.joinToString("  ·  ")

    val units = parsed.map { it!!.groupValues[2].trim() }.distinct()
    if (units.size != 1) return list.joinToString("  ·  ")

    val numbers = parsed.mapNotNull { it!!.groupValues[1].toDoubleOrNull() }
    if (numbers.size != list.size) return list.joinToString("  ·  ")

    val lo = numbers.min()
    val hi = numbers.max()
    fun trim(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
    return if (lo == hi) "${trim(lo)} ${units[0]}" else "${trim(lo)} – ${trim(hi)} ${units[0]}"
}

private fun cpPriceRange(p: Project): String {
    val lo = p.priceLow()
    val hi = p.priceHigh()
    return when {
        (lo ?: 0.0) > 0 && (hi ?: 0.0) > 0 && hi != lo -> "${formatINRShort(lo)} – ${formatINRShort(hi)}"
        (lo ?: 0.0) > 0 -> "${formatINRShort(lo)}+"
        else -> "On request"
    }
}

/**
 * What the partner earns.
 *
 * `commissionValue` is always a percentage: the builder form asks for "CP
 * commission %" and writes that number here. It also writes the string "FLAT"
 * into `commissionStructure`, which describes nothing — reading that field as
 * the unit rendered a 2.5% commission as "₹3".
 */
private fun cpPayout(p: Project): String? {
    val percent = p.commissionValue
    if (percent != null && percent > 0) {
        val trimmed = if (percent % 1.0 == 0.0) percent.toInt().toString() else percent.toString()
        return "$trimmed%"
    }
    return p.cpIncentive?.trim()?.takeIf { it.isNotBlank() }
}
