package com.rashodi.app.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.db.TYPE_INCOME
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.core.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditState(
    val kind: String = KIND_EXPENSE,
    val amount: String = "",
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val category: String = "",
    val subcategory: String = "",
    val source: String = "",
    val description: String = "",
    val comment: String = "",
    val toSavings: String = "",
    val editingId: Long = -1L,
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = editingId >= 0
    val canSave: Boolean get() = (Money.parseToKop(amount)?.let { it != 0L } == true) && category.isNotBlank()
}

class AddEditViewModel(
    private val repo: FinanceRepository,
    initialKind: String,
    private val id: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState(kind = initialKind, editingId = id))
    val state: StateFlow<AddEditState> = _state

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (id >= 0) loadForEdit(initialKind, id)
    }

    private fun loadForEdit(kind: String, id: Long) {
        viewModelScope.launch {
            if (kind == KIND_INCOME) {
                repo.snapshotIncomes().firstOrNull { it.id == id }?.let { e ->
                    _state.value = AddEditState(
                        kind = KIND_INCOME, amount = Money.toDecimalString(e.amountKop),
                        epochDay = e.epochDay, category = e.category, source = e.source,
                        description = e.description, comment = e.comment,
                        toSavings = if (e.toSavingsKop != 0L) Money.toDecimalString(e.toSavingsKop) else "",
                        editingId = id,
                    )
                }
            } else {
                repo.snapshotExpenses().firstOrNull { it.id == id }?.let { e ->
                    _state.value = AddEditState(
                        kind = KIND_EXPENSE, amount = Money.toDecimalString(e.amountKop),
                        epochDay = e.epochDay, category = e.category, subcategory = e.subcategory,
                        description = e.description, comment = e.comment, editingId = id,
                    )
                }
            }
        }
    }

    fun setKind(kind: String) { if (!_state.value.isEditing) _state.value = _state.value.copy(kind = kind, category = "", subcategory = "") }
    fun setAmount(v: String) { _state.value = _state.value.copy(amount = v) }
    fun setDate(epochDay: Long) { _state.value = _state.value.copy(epochDay = epochDay) }
    fun setCategory(v: String) { _state.value = _state.value.copy(category = v, subcategory = "") }
    fun setSubcategory(v: String) { _state.value = _state.value.copy(subcategory = v) }
    fun setSource(v: String) { _state.value = _state.value.copy(source = v) }
    fun setDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun setComment(v: String) { _state.value = _state.value.copy(comment = v) }
    fun setToSavings(v: String) { _state.value = _state.value.copy(toSavings = v) }

    fun subcategoriesFor(category: String): List<String> =
        categories.value.firstOrNull { it.type == TYPE_EXPENSE && it.name == category }?.subcategories ?: emptyList()

    fun categoryNamesFor(kind: String): List<String> {
        val type = if (kind == KIND_INCOME) TYPE_INCOME else TYPE_EXPENSE
        return categories.value.filter { it.type == type }.map { it.name }
    }

    fun save() {
        val s = _state.value
        val amountKop = Money.parseToKop(s.amount)?.let { Math.abs(it) } ?: return
        val date = LocalDate.ofEpochDay(s.epochDay)
        viewModelScope.launch {
            if (s.kind == KIND_INCOME) {
                val savings = Money.parseToKop(s.toSavings)?.let { Math.abs(it) } ?: 0L
                val entity = IncomeEntity(
                    id = if (s.isEditing) s.editingId else 0L,
                    epochDay = s.epochDay, year = date.year, month = date.monthValue,
                    source = s.source.trim(), category = s.category.trim(),
                    description = s.description.trim(), amountKop = amountKop,
                    toBudgetKop = (amountKop - savings).coerceAtLeast(0L),
                    toSavingsKop = savings, comment = s.comment.trim(),
                )
                if (s.isEditing) repo.updateIncome(entity) else repo.addIncome(entity)
            } else {
                val entity = ExpenseEntity(
                    id = if (s.isEditing) s.editingId else 0L,
                    epochDay = s.epochDay, year = date.year, month = date.monthValue,
                    category = s.category.trim(), subcategory = s.subcategory.trim(),
                    description = s.description.trim(), amountKop = amountKop,
                    comment = s.comment.trim(),
                )
                if (s.isEditing) repo.updateExpense(entity) else repo.addExpense(entity)
            }
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        val s = _state.value
        if (!s.isEditing) return
        viewModelScope.launch {
            if (s.kind == KIND_INCOME) {
                repo.snapshotIncomes().firstOrNull { it.id == s.editingId }?.let { repo.deleteIncome(it) }
            } else {
                repo.snapshotExpenses().firstOrNull { it.id == s.editingId }?.let { repo.deleteExpense(it) }
            }
            _state.value = _state.value.copy(saved = true)
        }
    }
}
