package com.dealio.app.ui.cp.projects

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.formatINRShort
import com.dealio.app.ui.builder.priceHigh
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.components.PortalEmptyState
import com.dealio.app.ui.components.PortalHeader
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.cp.CpRoutes
import com.dealio.app.ui.cp.CpViewModel
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.NavyDeep
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Orange
import com.dealio.app.ui.theme.Teal
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
) {
    val filtered: List<Project> get() = if (query.isBlank()) all else all.filter {
        it.name.contains(query, true) || (it.city ?: "").contains(query, true) || (it.locality ?: "").contains(query, true)
    }
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
}

@Composable
fun CpProjectsScreen(nav: NavController, vm: CpProjectsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        PortalHeader(
            title = "Projects",
            subtitle = "Share & refer to earn",
            stats = buildList {
                if (state.all.isNotEmpty()) {
                    add("${state.all.size}" to "live")
                    val cities = state.all.mapNotNull { it.city?.takeIf(String::isNotBlank) }.distinct().size
                    if (cities > 0) add("$cities" to if (cities == 1) "city" else "cities")
                }
            },
        )
        OutlinedTextField(
            value = state.query, onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search projects…") },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary) },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = dealioFieldColors(),
        )
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!, onRetry = vm::load)
            // Nothing to show splits two ways: a search that missed, or no inventory at
            // all. They need different words and different ways out.
            state.filtered.isEmpty() && state.query.isNotBlank() -> PortalEmptyState(
                icon = Icons.Outlined.Search,
                title = "No match for “${state.query}”",
                subtitle = "Try a locality or city instead of the full project name.",
                actionLabel = "Clear search",
                onAction = { vm.setQuery("") },
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
        Box(Modifier.fillMaxWidth().height(184.dp).background(Brush.linearGradient(listOf(NavyMid, Teal)))) {
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
                        1.0f to NavyDeep.copy(alpha = 0.88f),
                    ),
                ),
            )

            val flag = when {
                p.closingSoon -> "Closing soon"
                p.featured -> "Featured"
                else -> null
            }
            if (flag != null) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (p.closingSoon) Orange else Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        flag.uppercase(),
                        color = if (p.closingSoon) Color.White else NavyDeep,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
                    )
                }
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
                Text("PRICE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(2.dp))
                Text(cpPriceRange(p), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            val payout = cpPayout(p)
            if (payout != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("YOUR PAYOUT", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(payout, color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        val configs = p.configurations?.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        if (configs != null) {
            Text(
                configs,
                color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            )
        }
    }
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
