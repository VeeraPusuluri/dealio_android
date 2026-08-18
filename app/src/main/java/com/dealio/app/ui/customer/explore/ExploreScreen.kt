package com.dealio.app.ui.customer.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.KingBed
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.RefreshOnResume
import com.dealio.app.ui.builder.greetingName
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.customer.CustomerProjectCard
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.components.LocalHeroAccent
import com.dealio.app.ui.components.PortalHeaderSurface
import com.dealio.app.ui.customer.FeaturedCard
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.CustomerAccent
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun ExploreScreen(nav: NavController, vm: ExploreViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    RefreshOnResume { vm.load(silent = true) }

    var showFilters by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Navy hero with search (scrolls with the list) ──
        item { ExploreHero(state, vm, nav) }

        when {
            state.loading -> item { LoadingState(Modifier.height(220.dp)) }
            state.error != null -> item { ErrorState(state.error!!, onRetry = { vm.load() }, modifier = Modifier.height(220.dp)) }
            else -> {
                // City is the first cut a buyer makes and the one they change most,
                // so it sits on the surface as a rail rather than two taps deep in
                // the filter sheet with budget and configuration. Below two cities
                // there is nothing to switch between — "All cities | Hyderabad" is
                // a control whose two settings return the same list.
                if (state.cities.size > 1) {
                    item { CityRail(state, vm) }
                }

                // Featured carousel
                if (state.showFeatured) {
                    item { SectionLabel("Featured", Modifier.padding(horizontal = 16.dp)) }
                    item {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.featured.forEach { p ->
                                FeaturedCard(p) { nav.navigate(CustomerRoutes.projectDetail(p.id)) }
                            }
                        }
                    }
                }

                // Meetups used to sit here, between the featured homes and the
                // list. They have their own page now (profile → Around you), and
                // this screen is for property: someone scrolling homes is not
                // shopping for an event, and the strip pushed the listings down.

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel(
                                if (state.selectedCity != null) "Homes in ${state.selectedCity}" else "All homes",
                            )
                            // The count used to be a bare "5" floating between the
                            // heading and the Filters button, which reads as an
                            // unlabelled number rather than as how many results.
                            if (state.filtered.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (state.filtered.size == 1) "1 home" else "${state.filtered.size} homes",
                                    color = TextSecondary, fontSize = 12.sp,
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        FiltersButton(activeCount = state.activeFilterCount) { showFilters = true }
                    }
                }

                if (state.filtered.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Outlined.Apartment,
                            "No homes found",
                            if (state.hasActiveFilters) "Try adjusting or clearing your filters." else "Try a different city or search term.",
                        )
                    }
                } else {
                    items(state.filtered.size) { i ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            CustomerProjectCard(
                                state.filtered[i],
                                saved = state.filtered[i].id in state.savedIds,
                                onToggleSave = { vm.toggleSaved(state.filtered[i].id) },
                            ) { nav.navigate(CustomerRoutes.projectDetail(state.filtered[i].id)) }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(state, vm, onDismiss = { showFilters = false })
    }
}

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    val active = activeCount > 0
    Row(
        Modifier
            .background(if (active) CustomerAccent else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (active) CustomerAccent else CardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Tune, null, tint = if (active) Color.White else TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (active) "Filters ($activeCount)" else "Filters",
            color = if (active) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(state: ExploreState, vm: ExploreViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (state.activeFilterCount > 0) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { vm.clearFacetFilters() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = ErrorRed, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear all", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // City is not repeated here — the rail above the list owns it.

            if (state.bhkOptions.isNotEmpty()) {
                FilterGroup("Configuration") {
                    FilterChip("Any BHK", Icons.Outlined.KingBed, state.selectedBhk == null) { vm.setBhk(null) }
                    state.bhkOptions.forEach { n ->
                        FilterChip(if (n >= 4) "4+ BHK" else "$n BHK", null, state.selectedBhk == n) { vm.setBhk(n) }
                    }
                }
            }

            FilterGroup("Budget") {
                FilterChip("Any budget", Icons.Outlined.CurrencyRupee, state.selectedBudget == null) { vm.setBudget(null) }
                BudgetBucket.entries.forEach { b ->
                    FilterChip(b.label, null, state.selectedBudget == b) { vm.setBudget(b) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .background(CustomerAccent, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (state.filtered.isNotEmpty()) "Show ${state.filtered.size} homes" else "Show homes",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Text(title.uppercase(), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ExploreHero(state: ExploreState, vm: ExploreViewModel, nav: NavController) {
    // The shared portal surface — same gradient, radius, inset and buyer-green
    // tint as the rest of the customer portal. The buyer's home is the first
    // screen after the role picker, so it is the one that most has to look like
    // the card they just signed in on.
    PortalHeaderSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hi ${greetingName(state.name, "there")} 👋", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Find your next home", color = LocalHeroAccent.current, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            HeroIconButton(Icons.Outlined.Notifications, "Notifications") { nav.navigate(CustomerRoutes.NOTIFICATIONS) }
        }
        // Search sits in the bar rather than behind an icon that toggles it open.
        // This screen exists to find a home, and the hero was otherwise a quarter
        // of the display carrying a greeting and two buttons.
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search projects, localities…", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(19.dp)) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp).clickable { vm.setQuery("") },
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLeadingIconColor = TextSecondary,
                unfocusedLeadingIconColor = TextSecondary,
                focusedPlaceholderColor = TextSecondary,
                unfocusedPlaceholderColor = TextSecondary,
                cursorColor = CustomerAccent,
            ),
        )
    }
}

/** Horizontal city switch — "All" plus every city the catalogue actually has. */
@Composable
private fun CityRail(state: ExploreState, vm: ExploreViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CityChip("All cities", state.selectedCity == null) { vm.setCity(null) }
        state.cities.forEach { c ->
            CityChip(c, state.selectedCity == c) { vm.setCity(c) }
        }
    }
}

@Composable
private fun CityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else TextSecondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .background(if (selected) CustomerAccent else Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, if (selected) CustomerAccent else CardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun HeroIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp)
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun FilterChip(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(if (selected) CustomerAccent else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) CustomerAccent else CardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Color.White else TextSecondary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
