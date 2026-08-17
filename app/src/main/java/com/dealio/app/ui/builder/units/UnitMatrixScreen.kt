package com.dealio.app.ui.builder.units

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.BuilderRoutes
import com.dealio.app.ui.builder.BuilderViewModel
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.ErrorState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.StatusColors
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.flow.UnitMatrixGrid
import com.dealio.app.ui.flow.tallyOf
import com.dealio.app.ui.flow.unitsOf
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnitsState(val loading: Boolean = true, val error: String? = null, val projects: List<Project> = emptyList())

class UnitsViewModel(app: Application) : BuilderViewModel(app) {
    private val _state = MutableStateFlow(UnitsState())
    val state: StateFlow<UnitsState> = _state.asStateFlow()
    init { load() }
    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.getProjects()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, projects = r.data) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }
}

/**
 * The builder's inventory, project by project — and, on tap, the actual matrix.
 *
 * This screen used to be four numbers per project and a link to the project
 * page. The numbers came from the hand-maintained `availableUnits`/`bookedUnits`
 * counters, so a builder could read "18 available" without being able to say
 * *which* eighteen — and nothing in the app ever drew the matrix the buyer is
 * now picking from. Expanding a card renders the same grid the buyer sees.
 */
@Composable
fun UnitMatrixScreen(nav: NavController, vm: UnitsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<Long?>(null) }
    SubScreenScaffold("Inventory", nav) { pad ->
        when {
            state.loading -> LoadingState(Modifier.padding(pad))
            state.error != null -> ErrorState(state.error!!, vm::load, Modifier.padding(pad))
            state.projects.isEmpty() -> EmptyState(Icons.Outlined.Grid4x4, "No inventory", "Create a project to manage its unit inventory.", Modifier.padding(pad))
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.projects.size) { i ->
                    val p = state.projects[i]
                    InventoryCard(
                        p = p,
                        expanded = expanded == p.id,
                        onToggle = { expanded = if (expanded == p.id) null else p.id },
                        onOpenProject = { nav.navigate(BuilderRoutes.projectDetail(p.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(
    p: Project,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenProject: () -> Unit,
) {
    // Counted off the matrix where there is one — the stored counters drift the
    // moment anyone edits units directly, and this screen is where that shows.
    val tally = tallyOf(p)
    val total = tally.total.coerceAtLeast(1)
    DealioCard(Modifier.clickable { onToggle() }) {
        Text(p.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(p.configurations?.joinToString(", ")?.ifBlank { "—" } ?: "—", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        // Stacked bar
        Row(Modifier.fillMaxWidth().height(10.dp)) {
            if (tally.sold > 0) Box(Modifier.weight(tally.sold.toFloat()).fillMaxWidth().height(10.dp).background(StatusColors.Green, RoundedCornerShape(2.dp)))
            if (tally.booked > 0) Box(Modifier.weight(tally.booked.toFloat()).fillMaxWidth().height(10.dp).background(StatusColors.Amber, RoundedCornerShape(2.dp)))
            if (tally.available > 0) Box(Modifier.weight(tally.available.toFloat()).fillMaxWidth().height(10.dp).background(Color(0xFFE3E9F1), RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendItem("Total", total.toString(), TextPrimary)
            LegendItem("Available", tally.available.toString(), TextSecondary)
            LegendItem("Booked", tally.booked.toString(), StatusColors.Amber)
            LegendItem("Sold", tally.sold.toString(), StatusColors.Green)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (expanded) "Hide unit matrix" else "Show unit matrix",
            color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        )
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            UnitMatrixGrid(unitsOf(p))
            Spacer(Modifier.height(10.dp))
            Text(
                "Open project to edit",
                color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onOpenProject() },
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label.uppercase(), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}
