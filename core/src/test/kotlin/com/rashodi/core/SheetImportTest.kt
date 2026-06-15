package com.rashodi.core

import com.rashodi.core.csv.SheetImport
import com.rashodi.core.csv.SheetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SheetImportTest {

    private val expenseHeader = listOf(
        "Месяц", "Год", "Дата", "Категория", "Подкатегория",
        "Подробное описание", "Стоимость", "Комментарий",
    )
    private val incomeHeader = listOf(
        "Месяц", "Год", "Дата", "От кого пришло", "Категория",
        "Подробное описание", "Приход", "Вложено в бюджет",
        "Остаток на накопления", "Комментарий",
    )

    @Test fun detect_kind() {
        assertEquals(SheetKind.EXPENSE, SheetImport.detectKind(expenseHeader))
        assertEquals(SheetKind.INCOME, SheetImport.detectKind(incomeHeader))
        assertEquals(SheetKind.UNKNOWN, SheetImport.detectKind(listOf("a", "b")))
    }

    @Test fun parse_expense_rows() {
        val rows = listOf(
            expenseHeader,
            listOf("Май", "2026", "2026-05-01 00:00:00", "Развлечения", "Шашлыки", "Шашлыки", "849.27", "ок"),
            listOf("", "", "", "Кат", "Под", "опис", "100", ""),                 // нет даты -> skip
            listOf("Май", "2026", "2026-05-02", "Кат", "Под", "опис", "", ""),   // нет суммы -> skip
            listOf("Июнь", "2026", "", "Кат", "Под", "опис", "500", ""),         // дата из месяц+год
        )
        val res = SheetImport.parse(rows)
        assertEquals(SheetKind.EXPENSE, res.kind)
        assertEquals(2, res.recognized)
        assertEquals(2, res.skipped)
        assertEquals(2, res.expenses.size)

        val first = res.expenses[0]
        assertEquals(84927L, first.amountKop)
        assertEquals("Развлечения", first.category)
        assertEquals("Шашлыки", first.subcategory)
        assertEquals(2026, first.year)
        assertEquals(5, first.month)
        assertEquals(LocalDate.of(2026, 5, 1).toEpochDay(), first.epochDay)

        val fallback = res.expenses[1]
        assertEquals(LocalDate.of(2026, 6, 1).toEpochDay(), fallback.epochDay)
        assertTrue(res.skipReasons.isNotEmpty())
    }

    @Test fun parse_income_rows() {
        val rows = listOf(
            incomeHeader,
            listOf("Январь", "2026", "2026-01-12", "", "Яндекс Маркет", "", "12220.96", "", "12220.96", ""),
        )
        val res = SheetImport.parse(rows)
        assertEquals(SheetKind.INCOME, res.kind)
        assertEquals(1, res.incomes.size)
        val inc = res.incomes[0]
        assertEquals(1222096L, inc.amountKop)
        assertEquals("Яндекс Маркет", inc.category)
        assertEquals(1222096L, inc.toSavingsKop)
        assertEquals(0L, inc.toBudgetKop)
    }

    @Test fun parse_excel_serial_date() {
        // Excel-сериал 45292 == 2024-01-01
        assertEquals(LocalDate.of(2024, 1, 1), SheetImport.parseDate("45292", "", ""))
    }
}
