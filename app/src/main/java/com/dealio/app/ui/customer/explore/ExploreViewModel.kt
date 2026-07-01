package com.dealio.app.ui.customer.explore

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.dealio.app.data.ApiResult
import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.customer.CustomerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BudgetBucket(val label: String) {
    UNDER_50L("< ₹50L"),
    L50_1CR("₹50L–1Cr"),
    CR1_2("₹1–2Cr"),
    CR2_PLUS("₹2Cr+");

    fun contains(price: Double): Boolean = when (this) {
        UNDER_50L -> price < 50_00_000
        L50_1CR -> price >= 50_00_000 && price < 1_00_00_000
        CR1_2 -> price >= 1_00_00_000 && price < 2_00_00_000
        CR2_PLUS -> price >= 2_00_00_000
    }
}

/** Leading BHK count parsed from a configuration string like "2 BHK" / "3.5 BHK". */
fun bhkValue(config: String): Int? = config.trim().takeWhile { it.isDigit() }.toIntOrNull()

data class ExploreState(
    val loading: Boolean = true,
    val error: String? = null,
    val cities: List<String> = emptyList(),
    val selectedCity: String? = null,
    val selectedBhk: Int? = null,
    val selectedBudget: BudgetBucket? = null,
    val query: String = "",
    val all: List<Project> = emptyList(),
    val name: String = "Customer",
) {
    val hasActiveFilters: Boolean
        get() = selectedCity != null || selectedBhk != null || selectedBudget != null || query.isNotBlank()

    /** BHK chip options actually present in the catalogue (4+ collapsed to 4). */
    val bhkOptions: List<Int>
        get() = all.flatMap { it.configurations ?: emptyList() }
            .mapNotNull { bhkValue(it) }
            .map { minOf(it, 4) }
            .distinct()
            .sorted()

    val featured: List<Project>
        get() = all.filter { it.featured && (selectedCity == null || (it.city ?: "").equals(selectedCity, true)) }

    val showFeatured: Boolean
        get() = featured.isNotEmpty() && query.isBlank() && selectedBhk == null && selectedBudget == null

    val filtered: List<Project>
        get() = all.filter { p -> matchesCity(p) && matchesQuery(p) && matchesBhk(p) && matchesBudget(p) }

    private fun matchesCity(p: Project) = selectedCity == null || (p.city ?: "").equals(selectedCity, true)

    private fun matchesQuery(p: Project) = query.isBlank() ||
        p.name.contains(query, true) ||
        (p.locality ?: "").contains(query, true) ||
        (p.city ?: "").contains(query, true)

    private fun matchesBhk(p: Project): Boolean {
        val target = selectedBhk ?: return true
        val values = (p.configurations ?: emptyList()).mapNotNull { bhkValue(it) }
        return if (target >= 4) values.any { it >= 4 } else values.any { it == target }
    }

    private fun matchesBudget(p: Project): Boolean {
        val bucket = selectedBudget ?: return true
        val price = p.priceLow() ?: return false
        return price > 0 && bucket.contains(price)
    }
}

class ExploreViewModel(app: Application) : CustomerViewModel(app) {

    private val _state = MutableStateFlow(ExploreState(name = repo.name))
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    init { load() }

    fun load(silent: Boolean = false) {
        if (!silent) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val projects = repo.getProjects(null)
            val cities = repo.getCities()
            when (projects) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        all = projects.data,
                        cities = (cities as? ApiResult.Success)?.data ?: emptyList(),
                        name = repo.name,
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = projects.message) }
            }
        }
    }

    fun setCity(city: String?) = _state.update { it.copy(selectedCity = city) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setBhk(bhk: Int?) = _state.update { it.copy(selectedBhk = if (it.selectedBhk == bhk) null else bhk) }
    fun setBudget(b: BudgetBucket?) = _state.update { it.copy(selectedBudget = if (it.selectedBudget == b) null else b) }
    fun clearFilters() = _state.update {
        it.copy(selectedCity = null, selectedBhk = null, selectedBudget = null, query = "")
    }
}
