package com.dealio.app.ui.customer.explore

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.KingBed
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.customer.CustomerProjectCard
import com.dealio.app.ui.customer.CustomerRoutes
import com.dealio.app.ui.customer.FeaturedCard
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.ErrorRed
import com.dealio.app.ui.theme.NavyTealGradient
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun ExploreScreen(nav: NavController, vm: ExploreViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    RefreshOnResume { vm.load(silent = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Navy hero with search (scrolls with the list) ──
        item { ExploreHero(state, vm, nav) }

        when {
            state.loading -> item { LoadingState(Modifier.height(220.dp)) }
            state.error != null -> item { ErrorState(state.error!!, onRetry = { vm.load() }, modifier = Modifier.height(220.dp)) }
            else -> {
                // City chips
                item {
                    FilterRow {
                        FilterChip("All", Icons.Outlined.LocationCity, state.selectedCity == null) { vm.setCity(null) }
                        state.cities.forEach { c ->
                            FilterChip(c, Icons.Outlined.LocationCity, state.selectedCity == c) { vm.setCity(c) }
                        }
                    }
                }

                // BHK chips
                if (state.bhkOptions.isNotEmpty()) {
                    item {
                        FilterRow {
                            FilterChip("Any BHK", Icons.Outlined.KingBed, state.selectedBhk == null) { vm.setBhk(null) }
                            state.bhkOptions.forEach { n ->
                                FilterChip(if (n >= 4) "4+ BHK" else "$n BHK", null, state.selectedBhk == n) { vm.setBhk(n) }
                            }
                        }
                    }
                }

                // Budget chips + clear-all
                item {
                    FilterRow {
                        FilterChip("Any budget", Icons.Outlined.CurrencyRupee, state.selectedBudget == null) { vm.setBudget(null) }
                        BudgetBucket.entries.forEach { b ->
                            FilterChip(b.label, null, state.selectedBudget == b) { vm.setBudget(b) }
                        }
                        if (state.hasActiveFilters) {
                            Row(
                                Modifier
                                    .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { vm.clearFilters() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
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

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel(
                            if (state.selectedCity != null) "Homes in ${state.selectedCity}" else "All homes",
                            Modifier.weight(1f),
                        )
                        if (state.filtered.isNotEmpty()) {
                            Text("${state.filtered.size}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
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
                            CustomerProjectCard(state.filtered[i]) { nav.navigate(CustomerRoutes.projectDetail(state.filtered[i].id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreHero(state: ExploreState, vm: ExploreViewModel, nav: NavController) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(NavyTealGradient),
    ) {
        Column(Modifier.systemBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hi ${state.name.substringBefore(' ')} 👋", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Find your next home", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier.size(40.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { nav.navigate(CustomerRoutes.NOTIFICATIONS) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search projects, localities…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
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
                    cursorColor = Teal,
                ),
            )
        }
    }
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun FilterChip(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(if (selected) Teal else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) Teal else CardBorder, RoundedCornerShape(10.dp))
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
