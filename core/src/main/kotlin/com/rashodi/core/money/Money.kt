package com.rashodi.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Денежная арифметика. Все суммы — целые копейки ([Long]).
 *
 * Никогда не используем Double/Float для денег: парсинг и округление идут через
 * [BigDecimal] в одном месте, чтобы исключить ошибки вида 63372.90000000001,
 * встречающиеся в исходных Excel-данных.
 */
object Money {

    const val KOP_IN_RUB: Long = 100L
    const val RUBLE_SIGN: String = "₽" // ₽

    private const val NBSP = ' '        // неразрывный пробел — разделитель тысяч
    private val STRIP_CHARS = charArrayOf(' ', ' ', ' ', ' ')

    /**
     * Разбирает пользовательский ввод суммы в копейки.
     * Принимает "1 234,56", "1234.5", "1 234,56", "63372.90000000001", "-50", "+100".
     * Возвращает null, если строку нельзя интерпретировать как число.
     */
    fun parseToKop(input: String?): Long? {
        if (input == null) return null
        var s = input.trim()
        if (s.isEmpty()) return null
        for (c in STRIP_CHARS) s = s.replace(c.toString(), "")
        s = s.replace(RUBLE_SIGN, "").replace(",", ".")
        if (s.startsWith("+")) s = s.substring(1)
        if (s.isEmpty() || s == "-" || s == ".") return null
        return try {
            BigDecimal(s).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Делит рубли (как BigDecimal-совместимую строку) — не используется для денег напрямую. */
    fun kopToRubBigDecimal(kop: Long): BigDecimal =
        BigDecimal(kop).movePointLeft(2)

    /**
     * Форматирует копейки в строку "1 234,56 ₽".
     * @param withSymbol добавлять знак рубля.
     * @param alwaysSign показывать "+" для положительных (для дельт).
     * @param withFraction показывать копейки (для крупных сумм можно скрыть).
     */
    fun format(
        kop: Long,
        withSymbol: Boolean = true,
        alwaysSign: Boolean = false,
        withFraction: Boolean = true,
    ): String {
        val negative = kop < 0
        val abs = if (negative) Math.negateExact(kop) else kop
        val rub = abs / KOP_IN_RUB
        val rem = (abs % KOP_IN_RUB).toInt()
        val sb = StringBuilder()
        when {
            negative -> sb.append('-')
            alwaysSign && kop > 0 -> sb.append('+')
        }
        sb.append(groupThousands(rub))
        if (withFraction) {
            sb.append(',')
            if (rem < 10) sb.append('0')
            sb.append(rem)
        }
        if (withSymbol) sb.append(NBSP).append(RUBLE_SIGN)
        return sb.toString()
    }

    /** Только число, без символа валюты: "1 234,56". */
    fun formatPlain(kop: Long): String = format(kop, withSymbol = false)

    /** Машинно-читаемое представление для CSV/JSON: "2450.00" (точка, без группировки и символа). */
    fun toDecimalString(kop: Long): String {
        val negative = kop < 0
        val abs = if (negative) Math.negateExact(kop) else kop
        val sb = StringBuilder()
        if (negative) sb.append('-')
        sb.append(abs / KOP_IN_RUB).append('.')
        val rem = (abs % KOP_IN_RUB).toInt()
        if (rem < 10) sb.append('0')
        sb.append(rem)
        return sb.toString()
    }

    /** Группировка тысяч неразрывным пробелом. */
    fun groupThousands(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        val first = digits.length % 3
        if (first > 0) {
            sb.append(digits, 0, first)
        }
        var i = first
        while (i < digits.length) {
            if (sb.isNotEmpty()) sb.append(NBSP)
            sb.append(digits, i, i + 3)
            i += 3
        }
        return sb.toString()
    }

    /**
     * Норма сбережений = (доход − расход) / доход.
     * Возвращает null при доходе <= 0 (нулевой знаменатель → "—" в UI).
     */
    fun savingsRate(incomeKop: Long, expenseKop: Long): Double? {
        if (incomeKop <= 0L) return null
        return (incomeKop - expenseKop).toDouble() / incomeKop.toDouble()
    }

    /** Доля части в целом. null при нулевом знаменателе. */
    fun share(partKop: Long, wholeKop: Long): Double? {
        if (wholeKop <= 0L) return null
        return partKop.toDouble() / wholeKop.toDouble()
    }

    /**
     * Рост относительно базы (MoM): (текущее − база) / база.
     * null, если база <= 0 (не с чем сравнивать).
     */
    fun growth(currentKop: Long, baseKop: Long): Double? {
        if (baseKop <= 0L) return null
        return (currentKop - baseKop).toDouble() / baseKop.toDouble()
    }

    /** Среднее по списку копеек (целочисленное, HALF_UP). 0 для пустого списка. */
    fun average(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sum = values.fold(BigDecimal.ZERO) { acc, v -> acc.add(BigDecimal(v)) }
        return sum.divide(BigDecimal(values.size), 0, RoundingMode.HALF_UP).toLong()
    }

    /** Прогресс фонда currentKop/targetKop. null при target <= 0. Не ограничивает сверху. */
    fun progress(currentKop: Long, targetKop: Long): Double? {
        if (targetKop <= 0L) return null
        return currentKop.toDouble() / targetKop.toDouble()
    }
}
