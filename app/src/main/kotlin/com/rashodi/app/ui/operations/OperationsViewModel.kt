package com.rashodi.app.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.db.TYPE_INCOME
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.core.time.YearMonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TypeFilter { ALL, EXPENSE, INCOME }

const val KIND_EXPENSE = "expense"
const val KIND_INCOME = "income"

data class OperationItem(
    val id: Long,
    val kind: String,
    val epochDay: Long,
    val title: String,
    val subtitle: String,
    val amountKop: Long,
    val isIncome: Boolean,
    val colorArgb: Int?,
)

data class OperationsFilter(
    val query: String = "",
    val type: TypeFilter = TypeFilter.ALL,
    val month: YearMonthKey = YearMonthKey.now(),
    val monthEnabled: Boolean = true,
)

data class OperationsUiState(
    val items: List<OperationItem> = emptyList(),
    val totalIncomeKop: Long = 0,
    val totalExpenseKop: Long = 0,
    val filter: OperationsFilter = OperationsFilter(),
)

class OperationsViewModel(private val repo: FinanceRepository) : ViewModel() {

    private val filter = MutableStateFlow(OperationsFilter(month = YearMonthKey.of(LocalDate.now())))

    fun setQuery(q: String) { filter.value = filter.value.copy(query = q) }
    fun setType(t: TypeFilter) { filter.value = filter.value.copy(type = t) }
    fun setMonth(m: YearMonthKey) { filter.value = filter.value.copy(month = m) }
    fun toggleMonthEnabled() { filter.value = filter.value.copy(monthEnabled = !filter.value.monthEnabled) }

    val state: StateFlow<OperationsUiState> = combine(
        repo.expenses,
        repo.incomes,
        repo.categories,
        filter,
    ) { expenses, incomes, categories, f ->
        build(expenses, incomes, categories, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperationsUiState())

    private fun build(
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        categories: List<CategoryEntity>,
        f: OperationsFilter,
    ): OperationsUiState {
        val expColor = categories.filter { it.type == TYPE_EXPENSE }.associate { it.name to it.colorArgb }
        val incColor = categories.filter { it.type == TYPE_INCOME }.associate { it.name to it.colorArgb }
        val q = f.query.trim().lowercase()

        fun monthOk(y: Int, m: Int) = !f.monthEnabled || (y == f.month.year && m == f.month.month)

        val expItems = if (f.type == TypeFilter.INCOME) emptyList() else expenses
            .filter { monthOk(it.year, it.month) }
            .filter { q.isEmpty() || it.description.lowercase().contains(q) || it.category.lowercase().contains(q) || it.subcategory.lowercase().contains(q) }
            .map {
                OperationItem(
                    id = it.id, kind = KIND_EXPENSE, epochDay = it.epochDay,
                    title = it.category,
                    subtitle = listOf(it.subcategory, it.description).filter { s -> s.isNotBlank() }.joinToString(" · "),
                    amountKop = it.amountKop, isIncome = false, colorArgb = expColor[it.category],
                )
            }

        val incItems = if (f.type == TypeFilter.EXPENSE) emptyList() else incomes
            .filter { monthOk(it.year, it.month) }
            .filter { q.isEmpty() || it.description.lowercase().contains(q) || it.category.lowercase().contains(q) || it.source.lowercase().contains(q) }
            .map {
                OperationItem(
                    id = it.id, kind = KIND_INCOME, epochDay = it.epochDay,
                    title = it.category,
                    subtitle = listOf(it.source, it.description).filter { s -> s.isNotBlank() }.joinToString(" · "),
                    amountKop = it.amountKop, isIncome = true, colorArgb = incColor[it.category],
                )
            }

        val items = (expItems + incItems).sortedWith(compareByDescending<OperationItem> { it.epochDay }.thenByDescending { it.id })
        return OperationsUiState(
            items = items,
            totalIncomeKop = incItems.sumOf { it.amountKop },
            totalExpenseKop = expItems.sumOf { it.amountKop },
            filter = f,
        )
    }

    fun delete(item: OperationItem) {
        viewModelScope.launch {
            if (item.isIncome) {
                repo.snapshotIncomes().firstOrNull { it.id == item.id }?.let { repo.deleteIncome(it) }
            } else {
                repo.snapshotExpenses().firstOrNull { it.id == item.id }?.let { repo.deleteExpense(it) }
            }
        }
    }
}
