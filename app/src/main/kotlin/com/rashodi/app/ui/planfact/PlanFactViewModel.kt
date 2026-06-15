package com.rashodi.app.ui.planfact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.BudgetPlanEntity
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.core.analytics.PlanFact
import com.rashodi.core.analytics.PlanFactRow
import com.rashodi.core.time.YearMonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanEdit(val category: String, val amount: String)

data class PlanFactUiState(
    val month: YearMonthKey = YearMonthKey.now(),
    val rows: List<PlanFactRow> = emptyList(),
    val totalPlannedKop: Long = 0,
    val totalActualKop: Long = 0,
    val colorMap: Map<String, Int> = emptyMap(),
)

class PlanFactViewModel(private val repo: FinanceRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonthKey.of(LocalDate.now()))
    private val _edit = MutableStateFlow<PlanEdit?>(null)
    val edit: StateFlow<PlanEdit?> = _edit

    fun setMonth(m: YearMonthKey) { month.value = m }
    fun openEdit(category: String, currentKop: Long) {
        _edit.value = PlanEdit(category, if (currentKop > 0) com.rashodi.core.money.Money.toDecimalString(currentKop) else "")
    }
    fun setEditAmount(v: String) { _edit.value = _edit.value?.copy(amount = v) }
    fun closeEdit() { _edit.value = null }

    val state: StateFlow<PlanFactUiState> = combine(
        repo.expenses,
        repo.plans,
        repo.categories,
        month,
    ) { expenses, plans, categories, m ->
        build(expenses, plans, categories, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanFactUiState())

    private fun build(
        expenses: List<ExpenseEntity>,
        plans: List<BudgetPlanEntity>,
        categories: List<CategoryEntity>,
        m: YearMonthKey,
    ): PlanFactUiState {
        val monthExpenses = expenses.filter { it.year == m.year && it.month == m.month }
        val actual = monthExpenses.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amountKop } }
        val monthPlans = plans.filter { it.year == m.year && it.month == m.month }
            .associate { it.category to it.plannedKop }

        val expenseCats = categories.filter { it.type == TYPE_EXPENSE }.map { it.name }
        val planned = (expenseCats + actual.keys + monthPlans.keys).toSet()
            .associateWith { monthPlans[it] ?: 0L }

        val rows = PlanFact.build(planned, actual)
            .sortedWith(compareByDescending<PlanFactRow> { it.plannedKop > 0 || it.actualKop > 0 }.thenByDescending { it.actualKop })

        return PlanFactUiState(
            month = m,
            rows = rows,
            totalPlannedKop = rows.sumOf { it.plannedKop },
            totalActualKop = rows.sumOf { it.actualKop },
            colorMap = categories.associate { it.name to it.colorArgb },
        )
    }

    fun savePlan() {
        val e = _edit.value ?: return
        val kop = com.rashodi.core.money.Money.parseToKop(e.amount)?.let { Math.abs(it) } ?: 0L
        val m = month.value
        viewModelScope.launch {
            if (kop <= 0L) {
                repo.deletePlan(e.category, m.year, m.month)
            } else {
                repo.upsertPlan(BudgetPlanEntity(category = e.category, year = m.year, month = m.month, plannedKop = kop))
            }
            _edit.value = null
        }
    }
}
