package com.rashodi.core.model

/**
 * Лёгкие, не зависящие от Android записи операций — вход для аналитики и формул.
 * Все суммы — копейки (Long).
 */
data class ExpenseRow(
    val epochDay: Long,
    val year: Int,
    val month: Int,
    val category: String,
    val subcategory: String,
    val amountKop: Long,
    val discretionary: Boolean = false,
)

data class IncomeRow(
    val epochDay: Long,
    val year: Int,
    val month: Int,
    val source: String,
    val category: String,
    val amountKop: Long,
    val toBudgetKop: Long = 0L,
    val toSavingsKop: Long = 0L,
)
