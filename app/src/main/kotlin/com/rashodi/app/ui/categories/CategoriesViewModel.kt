package com.rashodi.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.db.TYPE_INCOME
import com.rashodi.app.data.repo.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryDraft(
    val id: Long = 0,
    val name: String = "",
    val type: String = TYPE_EXPENSE,
    val colorArgb: Int = PALETTE.first(),
    val discretionary: Boolean = false,
    val subcategories: List<String> = emptyList(),
    val newSub: String = "",
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank()
}

class CategoriesViewModel(private val repo: FinanceRepository) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow<CategoryDraft?>(null)
    val draft: StateFlow<CategoryDraft?> = _draft

    fun openNew(type: String) { _draft.value = CategoryDraft(type = type) }

    fun openEdit(c: CategoryEntity) {
        _draft.value = CategoryDraft(
            id = c.id, name = c.name, type = c.type, colorArgb = c.colorArgb,
            discretionary = c.isDiscretionary, subcategories = c.subcategories,
        )
    }

    fun close() { _draft.value = null }

    fun setName(v: String) { _draft.update { it.copy(name = v) } }
    fun setColor(v: Int) { _draft.update { it.copy(colorArgb = v) } }
    fun setDiscretionary(v: Boolean) { _draft.update { it.copy(discretionary = v) } }
    fun setNewSub(v: String) { _draft.update { it.copy(newSub = v) } }

    fun addSub() {
        _draft.update { d ->
            val s = d.newSub.trim()
            if (s.isEmpty() || d.subcategories.any { it.equals(s, ignoreCase = true) }) d.copy(newSub = "")
            else d.copy(subcategories = d.subcategories + s, newSub = "")
        }
    }

    fun removeSub(sub: String) {
        _draft.update { it.copy(subcategories = it.subcategories - sub) }
    }

    fun save() {
        val d = _draft.value ?: return
        if (!d.canSave) return
        viewModelScope.launch {
            val entity = CategoryEntity(
                id = d.id, name = d.name.trim(), type = d.type,
                subcategories = if (d.type == TYPE_EXPENSE) d.subcategories else emptyList(),
                colorArgb = d.colorArgb,
                isDiscretionary = d.type == TYPE_EXPENSE && d.discretionary,
                sortOrder = 100,
            )
            if (d.isNew) repo.upsertCategory(entity) else repo.updateCategory(entity)
            _draft.value = null
        }
    }

    fun delete(c: CategoryEntity) {
        viewModelScope.launch { repo.deleteCategory(c) }
    }

    private inline fun MutableStateFlow<CategoryDraft?>.update(transform: (CategoryDraft) -> CategoryDraft) {
        val current = value ?: return
        value = transform(current)
    }

    companion object {
        val PALETTE = listOf(
            0xFF8FB4DE.toInt(), 0xFF5BC8B0.toInt(), 0xFF9C8FD9.toInt(), 0xFF6FB1C9.toInt(),
            0xFFB0BAC9.toInt(), 0xFFD98FB0.toInt(), 0xFF7FC8E0.toInt(), 0xFFE0707E.toInt(),
            0xFFB07FD9.toInt(), 0xFF5BC8C8.toInt(), 0xFF8FA0B0.toInt(), 0xFF6F8FC9.toInt(),
        )

        fun expenseOf(list: List<CategoryEntity>) = list.filter { it.type == TYPE_EXPENSE }
        fun incomeOf(list: List<CategoryEntity>) = list.filter { it.type == TYPE_INCOME }
    }
}
