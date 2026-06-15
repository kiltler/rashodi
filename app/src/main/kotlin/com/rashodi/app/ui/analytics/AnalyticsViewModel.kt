package com.rashodi.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.repo.toRows
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.core.analytics.Analytics
import com.rashodi.core.analytics.NamedAmount
import com.rashodi.core.time.YearMonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class PeriodMode { MONTH, YEAR, ALL }

data class AnalyticsPeriod(val mode: PeriodMode = PeriodMode.MONTH, val month: YearMonthKey = YearMonthKey.now())

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod(),
    val periodLabel: String = "",
    val expenseTotalKop: Long = 0,
    val incomeTotalKop: Long = 0,
    val byCategory: List<NamedAmount> = emptyList(),
    val bySubcategory: List<NamedAmount> = emptyList(),
    val bySource: List<NamedAmount> = emptyList(),
    val byMonth: List<NamedAmount> = emptyList(),
    val colorMap: Map<String, Int> = emptyMap(),
)

class AnalyticsViewModel(private val repo: FinanceRepository) : ViewModel() {

    private val period = MutableStateFlow(AnalyticsPeriod(month = YearMonthKey.of(LocalDate.now())))

    fun setMode(mode: PeriodMode) { period.value = period.value.copy(mode = mode) }
    fun setMonth(m: YearMonthKey) { period.value = period.value.copy(month = m) }

    val state: StateFlow<AnalyticsUiState> = combine(
        repo.expenses,
        repo.incomes,
        repo.categories,
        period,
    ) { expenses, incomes, categories, p ->
        build(expenses, incomes, categories, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    private fun build(
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        categories: List<CategoryEntity>,
        p: AnalyticsPeriod,
    ): AnalyticsUiState {
        val exp = expenses.filter { inPeriod(it.year, it.month, p) }
        val inc = incomes.filter { inPeriod(it.year, it.month, p) }
        val label = when (p.mode) {
            PeriodMode.MONTH -> p.month.label()
            PeriodMode.YEAR -> "${p.month.year} год"
            PeriodMode.ALL -> "Всё время"
        }
        return AnalyticsUiState(
            period = p,
            periodLabel = label,
            expenseTotalKop = exp.sumOf { it.amountKop },
            incomeTotalKop = inc.sumOf { it.amountKop },
            byCategory = Analytics.expenseByCategory(exp.toRows()),
            bySubcategory = Analytics.expenseBySubcategory(exp.toRows()).take(8),
            bySource = Analytics.incomeBySource(inc.toRows()),
            byMonth = Analytics.expenseByMonth(exp.toRows()),
            colorMap = categories.associate { it.name to it.colorArgb },
        )
    }

    private fun inPeriod(year: Int, month: Int, p: AnalyticsPeriod): Boolean = when (p.mode) {
        PeriodMode.MONTH -> year == p.month.year && month == p.month.month
        PeriodMode.YEAR -> year == p.month.year
        PeriodMode.ALL -> true
    }
}
