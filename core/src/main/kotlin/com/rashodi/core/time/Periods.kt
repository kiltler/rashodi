package com.rashodi.core.time

import java.time.LocalDate

/** Год+месяц как сравнимый ключ периода. month: 1..12. */
data class YearMonthKey(val year: Int, val month: Int) : Comparable<YearMonthKey> {

    init {
        require(month in 1..12) { "month must be 1..12, was $month" }
    }

    val ordinal: Int get() = year * 12 + (month - 1)

    override fun compareTo(other: YearMonthKey): Int = ordinal.compareTo(other.ordinal)

    /** Предыдущие [count] месяцев (не включая текущий), от ближайшего к дальнему. */
    fun previous(count: Int): List<YearMonthKey> =
        (1..count).map { minusMonths(it) }

    fun minusMonths(n: Int): YearMonthKey {
        val total = ordinal - n
        return YearMonthKey(Math.floorDiv(total, 12), Math.floorMod(total, 12) + 1)
    }

    fun plusMonths(n: Int): YearMonthKey {
        val total = ordinal + n
        return YearMonthKey(Math.floorDiv(total, 12), Math.floorMod(total, 12) + 1)
    }

    fun label(): String = "${RuMonths.nominative(month)} $year"
    fun shortLabel(): String = "${RuMonths.short(month)} $year"

    companion object {
        fun of(date: LocalDate): YearMonthKey = YearMonthKey(date.year, date.monthValue)
        fun now(today: LocalDate = LocalDate.now()): YearMonthKey = of(today)
    }
}

/** Русские названия месяцев. */
object RuMonths {
    private val NOMINATIVE = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )
    private val SHORT = arrayOf(
        "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек",
    )

    /** Именительный падеж: 1 -> "Январь". */
    fun nominative(month: Int): String = NOMINATIVE[(month - 1).coerceIn(0, 11)]

    fun short(month: Int): String = SHORT[(month - 1).coerceIn(0, 11)]

    // Именительный + родительный падежи (как в датах "1 мая") в нижнем регистре.
    private val LOOKUP: Map<String, Int> = buildMap {
        val genitive = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря",
        )
        for (i in NOMINATIVE.indices) {
            put(NOMINATIVE[i].lowercase(), i + 1)
            put(genitive[i], i + 1)
        }
    }

    /** Индекс месяца (1..12) по русскому названию (имен./род. падеж, любой регистр), либо null. */
    fun indexOf(name: String?): Int? {
        val n = name?.trim()?.lowercase() ?: return null
        if (n.isEmpty()) return null
        LOOKUP[n]?.let { return it }
        // Фолбэк: число "1".."12".
        n.toIntOrNull()?.let { if (it in 1..12) return it }
        return null
    }
}
