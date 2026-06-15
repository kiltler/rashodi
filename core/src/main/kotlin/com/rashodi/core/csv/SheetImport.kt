package com.rashodi.core.csv

import com.rashodi.core.money.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class SheetKind { EXPENSE, INCOME, UNKNOWN }

data class ImportedExpense(
    val epochDay: Long,
    val year: Int,
    val month: Int,
    val category: String,
    val subcategory: String,
    val description: String,
    val amountKop: Long,
    val comment: String,
)

data class ImportedIncome(
    val epochDay: Long,
    val year: Int,
    val month: Int,
    val source: String,
    val category: String,
    val description: String,
    val amountKop: Long,
    val toBudgetKop: Long,
    val toSavingsKop: Long,
    val comment: String,
)

data class ImportResult(
    val kind: SheetKind,
    val expenses: List<ImportedExpense> = emptyList(),
    val incomes: List<ImportedIncome> = emptyList(),
    val recognized: Int = 0,
    val skipped: Int = 0,
    val skipReasons: List<String> = emptyList(),
)

/**
 * Сопоставление колонок Excel-шаблона («Учет расходов» / «Учет доходов») с моделью.
 * Работает с уже разобранными строками (первая строка — заголовки).
 * Строки без распознанной даты или суммы пропускаются и попадают в счётчик skipped.
 */
object SheetImport {

    private val DATE_PATTERNS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yy"),
        DateTimeFormatter.ofPattern("d.M.yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    )
    private val EXCEL_EPOCH = LocalDate.of(1899, 12, 30)

    fun detectKind(header: List<String>): SheetKind {
        val h = header.joinToString("|") { it.lowercase() }
        val incomeSignals = listOf("приход", "от кого", "вложено в бюджет", "остаток на накопл")
        val expenseSignals = listOf("стоимость", "расход")
        val incomeScore = incomeSignals.count { h.contains(it) }
        val expenseScore = expenseSignals.count { h.contains(it) }
        return when {
            incomeScore > expenseScore -> SheetKind.INCOME
            expenseScore > 0 -> SheetKind.EXPENSE
            else -> SheetKind.UNKNOWN
        }
    }

    /** Разбирает таблицу, автоопределяя её тип по заголовкам. */
    fun parse(rows: List<List<String>>): ImportResult {
        if (rows.isEmpty()) return ImportResult(SheetKind.UNKNOWN)
        val header = rows.first().map { it.trim() }
        val body = rows.drop(1)
        return when (detectKind(header)) {
            SheetKind.EXPENSE -> parseExpenses(header, body)
            SheetKind.INCOME -> parseIncomes(header, body)
            SheetKind.UNKNOWN -> ImportResult(
                SheetKind.UNKNOWN,
                skipReasons = listOf("Не удалось определить тип таблицы по заголовкам"),
            )
        }
    }

    private fun parseExpenses(header: List<String>, body: List<List<String>>): ImportResult {
        val cDate = findCol(header, "дата")
        val cYear = findCol(header, "год")
        val cMonth = findCol(header, "месяц")
        val cSub = findCol(header, "подкатег")
        val cCat = findCol(header, "категория", exclude = "подкатег")
        val cDesc = findCol(header, "описание", "наименование", "назначение", "подробн")
        val cAmount = findCol(header, "стоимость", "сумма", "расход", "цена")
        val cComment = findCol(header, "коммент", "примеч")

        val out = ArrayList<ImportedExpense>()
        var skipped = 0
        val reasons = sortedReasons()
        for (raw in body) {
            val row = raw
            val date = parseDate(cell(row, cDate), cell(row, cYear), cell(row, cMonth))
            val amount = Money.parseToKop(cell(row, cAmount))
            if (date == null) { skipped++; reasons.bump("нет/неверная дата"); continue }
            if (amount == null) { skipped++; reasons.bump("нет/неверная сумма"); continue }
            out.add(
                ImportedExpense(
                    epochDay = date.toEpochDay(),
                    year = date.year,
                    month = date.monthValue,
                    category = cell(row, cCat).ifBlank { "Без категории" },
                    subcategory = cell(row, cSub),
                    description = cell(row, cDesc),
                    amountKop = amount,
                    comment = cell(row, cComment),
                ),
            )
        }
        return ImportResult(SheetKind.EXPENSE, expenses = out, recognized = out.size,
            skipped = skipped, skipReasons = reasons.toList())
    }

    private fun parseIncomes(header: List<String>, body: List<List<String>>): ImportResult {
        val cDate = findCol(header, "дата")
        val cYear = findCol(header, "год")
        val cMonth = findCol(header, "месяц")
        val cSource = findCol(header, "от кого", "источник")
        val cCat = findCol(header, "категория")
        val cDesc = findCol(header, "описание", "наименование", "подробн")
        val cAmount = findCol(header, "приход", "доход", "сумма")
        val cBudget = findCol(header, "вложено", "бюджет")
        val cSavings = findCol(header, "накопл", "остаток")
        val cComment = findCol(header, "коммент", "примеч")

        val out = ArrayList<ImportedIncome>()
        var skipped = 0
        val reasons = sortedReasons()
        for (row in body) {
            val date = parseDate(cell(row, cDate), cell(row, cYear), cell(row, cMonth))
            val amount = Money.parseToKop(cell(row, cAmount))
            if (date == null) { skipped++; reasons.bump("нет/неверная дата"); continue }
            if (amount == null) { skipped++; reasons.bump("нет/неверная сумма"); continue }
            out.add(
                ImportedIncome(
                    epochDay = date.toEpochDay(),
                    year = date.year,
                    month = date.monthValue,
                    source = cell(row, cSource),
                    category = cell(row, cCat).ifBlank { "Прочее" },
                    description = cell(row, cDesc),
                    amountKop = amount,
                    toBudgetKop = Money.parseToKop(cell(row, cBudget)) ?: 0L,
                    toSavingsKop = Money.parseToKop(cell(row, cSavings)) ?: 0L,
                    comment = cell(row, cComment),
                ),
            )
        }
        return ImportResult(SheetKind.INCOME, incomes = out, recognized = out.size,
            skipped = skipped, skipReasons = reasons.toList())
    }

    // -- helpers --

    private fun cell(row: List<String>, idx: Int): String =
        if (idx in row.indices) row[idx].trim() else ""

    private fun findCol(header: List<String>, vararg keys: String): Int =
        findCol(header, *keys, exclude = null)

    private fun findCol(header: List<String>, vararg keys: String, exclude: String?): Int {
        for (i in header.indices) {
            val h = header[i].lowercase()
            if (exclude != null && h.contains(exclude)) continue
            if (keys.any { h.contains(it) }) return i
        }
        return -1
    }

    /** Дата из колонки даты; при пустой — из колонок месяц+год (1-е число). */
    fun parseDate(dateStr: String, yearStr: String, monthStr: String): LocalDate? {
        val d = parseDateOnly(dateStr)
        if (d != null) return d
        // фолбэк: месяц + год
        val y = yearStr.trim().toIntOrNull()
        val m = com.rashodi.core.time.RuMonths.indexOf(monthStr)
        if (y != null && m != null) return LocalDate.of(y, m, 1)
        return null
    }

    private fun parseDateOnly(s: String): LocalDate? {
        val str = s.trim()
        if (str.isEmpty()) return null
        // Excel-сериал (число дней)
        val serial = str.toDoubleOrNull()
        if (serial != null && serial > 20000 && serial < 80000) {
            return EXCEL_EPOCH.plusDays(serial.toLong())
        }
        for (fmt in DATE_PATTERNS) {
            try {
                return LocalDate.parse(str, fmt)
            } catch (_: Exception) {
            }
        }
        // ISO с временем без секунд и т.п.
        try {
            return LocalDate.parse(str.substring(0, 10))
        } catch (_: Exception) {
        }
        return null
    }

    private fun sortedReasons() = ReasonCounter()

    class ReasonCounter {
        private val map = LinkedHashMap<String, Int>()
        fun bump(reason: String) {
            map[reason] = (map[reason] ?: 0) + 1
        }
        fun toList(): List<String> = map.entries
            .sortedByDescending { it.value }
            .map { "${it.key}: ${it.value}" }
    }
}
