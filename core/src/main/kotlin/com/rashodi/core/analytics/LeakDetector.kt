package com.rashodi.core.analytics

import com.rashodi.core.model.ExpenseRow
import com.rashodi.core.money.Money

/** Настройки «детектора утечек». */
data class LeakConfig(
    /** Доля категории в расходах месяца, выше которой категория подсвечивается (0..1). */
    val shareThreshold: Double = 0.15,
    /** Относительный рост к среднему за предыдущие месяцы, выше которого подсветка (0..1). */
    val growthThreshold: Double = 0.30,
)

/** Найденная «утечка» по категории. */
data class LeakFlag(
    val category: String,
    val amountKop: Long,
    val share: Double?,
    val growth: Double?,
    val flaggedByShare: Boolean,
    val flaggedByGrowth: Boolean,
    val discretionary: Boolean,
) {
    val flagged: Boolean get() = flaggedByShare || flaggedByGrowth
}

object LeakDetector {

    /**
     * @param current расходы текущего месяца.
     * @param previousMonths список расходов предыдущих месяцев (каждый элемент — один месяц),
     *        для расчёта среднего. Пустой список → рост не считается.
     * @param discretionaryCategories категории, помеченные как «необязательные траты».
     */
    fun detect(
        current: List<ExpenseRow>,
        previousMonths: List<List<ExpenseRow>>,
        config: LeakConfig = LeakConfig(),
        discretionaryCategories: Set<String> = emptySet(),
    ): List<LeakFlag> {
        val total = current.sumOf { it.amountKop }
        val currentByCat = current.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amountKop } }

        // Среднее по категории за предыдущие месяцы (среднее месячных сумм).
        val avgByCat: Map<String, Long> = if (previousMonths.isEmpty()) {
            emptyMap()
        } else {
            val perCat = mutableMapOf<String, MutableList<Long>>()
            for (month in previousMonths) {
                val sums = month.groupBy { it.category }
                    .mapValues { (_, v) -> v.sumOf { it.amountKop } }
                // учитываем 0 для категорий, которых в этом месяце не было, чтобы среднее не завышалось
                val cats = currentByCat.keys + sums.keys
                for (c in cats) perCat.getOrPut(c) { mutableListOf() }.add(sums[c] ?: 0L)
            }
            perCat.mapValues { (_, list) -> Money.average(list) }
        }

        return currentByCat.entries.map { (cat, amount) ->
            val share = Money.share(amount, total)
            val avg = avgByCat[cat]
            val growth = if (avg != null) Money.growth(amount, avg) else null
            val byShare = share != null && share > config.shareThreshold
            val byGrowth = growth != null && growth > config.growthThreshold
            LeakFlag(
                category = cat,
                amountKop = amount,
                share = share,
                growth = growth,
                flaggedByShare = byShare,
                flaggedByGrowth = byGrowth,
                discretionary = cat in discretionaryCategories,
            )
        }
            .filter { it.flagged }
            .sortedByDescending { it.amountKop }
    }
}
