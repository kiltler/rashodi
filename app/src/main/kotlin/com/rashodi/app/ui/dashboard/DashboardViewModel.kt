package com.rashodi.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.app.data.repo.toRows
import com.rashodi.app.data.settings.AppSettings
import com.rashodi.app.data.settings.SettingsStore
import com.rashodi.core.analytics.Analytics
import com.rashodi.core.analytics.LeakConfig
import com.rashodi.core.analytics.LeakDetector
import com.rashodi.core.analytics.LeakFlag
import com.rashodi.core.analytics.MonthSummary
import com.rashodi.core.analytics.NamedAmount
import com.rashodi.core.time.YearMonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TrendPoint(val label: String, val incomeKop: Long, val expenseKop: Long)

data class DashboardUiState(
    val month: YearMonthKey = YearMonthKey.now(),
    val summary: MonthSummary = MonthSummary(0, 0, 0, null, 0, null, 0),
    val topCategories: List<NamedAmount> = emptyList(),
    val trend: List<TrendPoint> = emptyList(),
    val leaks: List<LeakFlag> = emptyList(),
    val categoryColors: Map<String, Int> = emptyMap(),
    val loading: Boolean = true,
)

class DashboardViewModel(
    private val repo: FinanceRepository,
    settingsStore: SettingsStore,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonthKey.of(LocalDate.now()))

    fun setMonth(m: YearMonthKey) {
        selectedMonth.value = m
    }

    val state: StateFlow<DashboardUiState> = combine(
        repo.expenses,
        repo.incomes,
        repo.categories,
        settingsStore.settings,
        selectedMonth,
    ) { expenses, incomes, categories, settings, month ->
        compute(expenses, incomes, categories, settings, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState(),
    )

    private fun compute(
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        categories: List<CategoryEntity>,
        settings: AppSettings,
        month: YearMonthKey,
    ): DashboardUiState {
        val monthExpenses = expenses.filter { it.year == month.year && it.month == month.month }
        val monthIncomes = incomes.filter { it.year == month.year && it.month == month.month }

        val summary = Analytics.monthSummary(monthExpenses.toRows(), monthIncomes.toRows())
        val topCats = Analytics.expenseByCategory(monthExpenses.toRows()).take(5)

        // Тренд за 6 месяцев, включая выбранный.
        val months = (5 downTo 0).map { month.minusMonths(it) }
        val trend = months.map { mk ->
            val inc = incomes.filter { it.year == mk.year && it.month == mk.month }.sumOf { it.amountKop }
            val exp = expenses.filter { it.year == mk.year && it.month == mk.month }.sumOf { it.amountKop }
            TrendPoint(mk.shortMonthLabel(), inc, exp)
        }

        // Детектор утечек: текущий месяц + 3 предыдущих.
        val prevMonths = (1..3).map { back ->
            val mk = month.minusMonths(back)
            expenses.filter { it.year == mk.year && it.month == mk.month }.toRows()
        }
        val discretionary = categories
            .filter { it.type == TYPE_EXPENSE && it.isDiscretionary }
            .map { it.name }.toSet()
        val leaks = LeakDetector.detect(
            current = monthExpenses.toRows(),
            previousMonths = prevMonths,
            config = LeakConfig(
                shareThreshold = settings.leakShareThreshold.toDouble(),
                growthThreshold = settings.leakGrowthThreshold.toDouble(),
            ),
            discretionaryCategories = discretionary,
        )

        val colors = categories.associate { it.name to it.colorArgb }

        return DashboardUiState(
            month = month,
            summary = summary,
            topCategories = topCats,
            trend = trend,
            leaks = leaks,
            categoryColors = colors,
            loading = false,
        )
    }
}

private fun YearMonthKey.shortMonthLabel(): String =
    com.rashodi.core.time.RuMonths.short(month)
