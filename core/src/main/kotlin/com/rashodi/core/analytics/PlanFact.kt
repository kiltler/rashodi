package com.rashodi.core.analytics

import com.rashodi.core.money.Money

/** Строка план-факта по категории. */
data class PlanFactRow(
    val category: String,
    val plannedKop: Long,
    val actualKop: Long,
) {
    /** План − Факт. Положительное — экономия, отрицательное — перерасход. */
    val diffKop: Long get() = plannedKop - actualKop
    val overspent: Boolean get() = actualKop > plannedKop
    /** Доля исполнения факт/план, null если план = 0. */
    val usage: Double? get() = Money.share(actualKop, plannedKop)
}

object PlanFact {
    /**
     * Сводит план и факт по категориям. В результат попадают все категории,
     * встречающиеся хотя бы в плане или в факте.
     */
    fun build(
        planned: Map<String, Long>,
        actual: Map<String, Long>,
    ): List<PlanFactRow> {
        val cats = planned.keys + actual.keys
        return cats.map { c ->
            PlanFactRow(c, planned[c] ?: 0L, actual[c] ?: 0L)
        }.sortedByDescending { it.actualKop }
    }
}
