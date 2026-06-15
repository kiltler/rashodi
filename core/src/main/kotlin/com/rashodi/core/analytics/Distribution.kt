package com.rashodi.core.analytics

/** Доля распределения дохода (проценты — целые, в сумме должны давать 100). */
data class DistributionBucket(val label: String, val percent: Int)

data class Allocation(val label: String, val percent: Int, val amountKop: Long)

object Distribution {

    val DEFAULT: List<DistributionBucket> = listOf(
        DistributionBucket("Постоянные расходы", 75),
        DistributionBucket("Переменные расходы", 15),
        DistributionBucket("Сбережения", 10),
    )

    fun percentSum(buckets: List<DistributionBucket>): Int = buckets.sumOf { it.percent }

    fun isValid(buckets: List<DistributionBucket>): Boolean = percentSum(buckets) == 100

    /**
     * Распределяет доход по долям без потери копеек: первые корзины получают floor,
     * последняя — остаток, поэтому сумма частей всегда равна доходу.
     * Требует, чтобы сумма процентов была равна 100.
     */
    fun allocate(incomeKop: Long, buckets: List<DistributionBucket>): List<Allocation> {
        require(isValid(buckets)) { "Сумма процентов должна быть равна 100, сейчас ${percentSum(buckets)}" }
        if (buckets.isEmpty()) return emptyList()
        val result = ArrayList<Allocation>(buckets.size)
        var allocated = 0L
        for (i in buckets.indices) {
            val b = buckets[i]
            val amount = if (i == buckets.lastIndex) {
                incomeKop - allocated
            } else {
                // целочисленно: incomeKop * percent / 100
                Math.multiplyExact(incomeKop, b.percent.toLong()) / 100L
            }
            allocated += amount
            result.add(Allocation(b.label, b.percent, amount))
        }
        return result
    }
}
