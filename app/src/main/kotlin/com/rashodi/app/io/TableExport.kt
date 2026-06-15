package com.rashodi.app.io

import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.core.csv.Csv
import com.rashodi.core.money.Money
import com.rashodi.core.time.RuMonths
import java.time.LocalDate

/** Готовит таблицы расходов/доходов в колонках исходного Excel-шаблона. */
object TableExport {

    val EXPENSE_HEADER = listOf(
        "Месяц", "Год", "Дата", "Категория", "Подкатегория",
        "Подробное описание", "Стоимость", "Комментарий",
    )
    val INCOME_HEADER = listOf(
        "Месяц", "Год", "Дата", "От кого пришло", "Категория",
        "Подробное описание", "Приход", "Вложено в бюджет",
        "Остаток на накопления", "Комментарий",
    )

    private fun dateStr(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

    fun expenseRows(list: List<ExpenseEntity>): List<List<String>> {
        val rows = ArrayList<List<String>>(list.size + 1)
        rows.add(EXPENSE_HEADER)
        for (e in list.sortedBy { it.epochDay }) {
            rows.add(
                listOf(
                    RuMonths.nominative(e.month),
                    e.year.toString(),
                    dateStr(e.epochDay),
                    e.category,
                    e.subcategory,
                    e.description,
                    Money.toDecimalString(e.amountKop),
                    e.comment,
                ),
            )
        }
        return rows
    }

    fun incomeRows(list: List<IncomeEntity>): List<List<String>> {
        val rows = ArrayList<List<String>>(list.size + 1)
        rows.add(INCOME_HEADER)
        for (e in list.sortedBy { it.epochDay }) {
            rows.add(
                listOf(
                    RuMonths.nominative(e.month),
                    e.year.toString(),
                    dateStr(e.epochDay),
                    e.source,
                    e.category,
                    e.description,
                    Money.toDecimalString(e.amountKop),
                    Money.toDecimalString(e.toBudgetKop),
                    Money.toDecimalString(e.toSavingsKop),
                    e.comment,
                ),
            )
        }
        return rows
    }

    fun expensesCsv(list: List<ExpenseEntity>): String = Csv.write(expenseRows(list))
    fun incomesCsv(list: List<IncomeEntity>): String = Csv.write(incomeRows(list))
}
