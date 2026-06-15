package com.rashodi.core.analytics

import com.rashodi.core.model.ExpenseRow
import com.rashodi.core.model.IncomeRow
import com.rashodi.core.money.Money

/** Ключевые показатели за период. Производные метрики null при нулевом знаменателе. */
data class MonthSummary(
    val incomeKop: Long,
    val expenseKop: Long,
    val netKop: Long,
    val savingsRate: Double?,
    val expenseCount: Int,
    val avgExpenseKop: Long?,
    val toSavingsKop: Long,
) {
    val hasData: Boolean get() = incomeKop != 0L || expenseKop != 0L
}

/** Сумма по сущности (категория/подкатегория/источник/месяц) с долей в общем. */
data class NamedAmount(
    val name: String,
    val amountKop: Long,
    val share: Double?,
)

object Analytics {

    fun monthSummary(expenses: List<ExpenseRow>, incomes: List<IncomeRow>): MonthSummary {
        val income = incomes.sumOf { it.amountKop }
        val expense = expenses.sumOf { it.amountKop }
        val toSavings = incomes.sumOf { it.toSavingsKop }
        return MonthSummary(
            incomeKop = income,
            expenseKop = expense,
            netKop = income - expense,
            savingsRate = Money.savingsRate(income, expense),
            expenseCount = expenses.size,
            avgExpenseKop = if (expenses.isEmpty()) null
            else Money.average(expenses.map { it.amountKop }),
            toSavingsKop = toSavings,
        )
    }

    private fun group(
        amounts: Map<String, Long>,
        total: Long,
    ): List<NamedAmount> = amounts.entries
        .map { NamedAmount(it.key, it.value, Money.share(it.value, total)) }
        .sortedByDescending { it.amountKop }

    fun expenseByCategory(expenses: List<ExpenseRow>): List<NamedAmount> {
        val total = expenses.sumOf { it.amountKop }
        val sums = expenses.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amountKop } }
        return group(sums, total)
    }

    fun expenseBySubcategory(expenses: List<ExpenseRow>): List<NamedAmount> {
        val total = expenses.sumOf { it.amountKop }
        val sums = expenses.groupBy { "${it.category} · ${it.subcategory}" }
            .mapValues { (_, v) -> v.sumOf { it.amountKop } }
        return group(sums, total)
    }

    fun incomeBySource(incomes: List<IncomeRow>): List<NamedAmount> {
        val total = incomes.sumOf { it.amountKop }
        val sums = incomes.groupBy { it.category.ifBlank { it.source.ifBlank { "Прочее" } } }
            .mapValues { (_, v) -> v.sumOf { it.amountKop } }
        return group(sums, total)
    }

    /** Расходы по месяцам (ключ "yyyy-MM") по возрастанию. */
    fun expenseByMonth(expenses: List<ExpenseRow>): List<NamedAmount> =
        byMonth(expenses.groupBy { ymKey(it.year, it.month) }
            .mapValues { (_, v) -> v.sumOf { it.amountKop } })

    fun incomeByMonth(incomes: List<IncomeRow>): List<NamedAmount> =
        byMonth(incomes.groupBy { ymKey(it.year, it.month) }
            .mapValues { (_, v) -> v.sumOf { it.amountKop } })

    private fun byMonth(sums: Map<String, Long>): List<NamedAmount> =
        sums.entries.sortedBy { it.key }.map { NamedAmount(it.key, it.value, null) }

    private fun ymKey(year: Int, month: Int): String =
        year.toString() + "-" + month.toString().padStart(2, '0')
}
