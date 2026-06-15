package com.rashodi.core

import com.rashodi.core.analytics.Analytics
import com.rashodi.core.analytics.Distribution
import com.rashodi.core.analytics.DistributionBucket
import com.rashodi.core.analytics.LeakConfig
import com.rashodi.core.analytics.LeakDetector
import com.rashodi.core.analytics.PlanFact
import com.rashodi.core.model.ExpenseRow
import com.rashodi.core.model.IncomeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTest {

    private fun exp(cat: String, sub: String, kop: Long, y: Int = 2026, m: Int = 5) =
        ExpenseRow(0, y, m, cat, sub, kop)

    private fun inc(kop: Long, cat: String = "Зарплата", y: Int = 2026, m: Int = 5, savings: Long = 0) =
        IncomeRow(0, y, m, "", cat, kop, 0, savings)

    @Test fun month_summary_empty_is_honest() {
        val s = Analytics.monthSummary(emptyList(), emptyList())
        assertEquals(0L, s.incomeKop)
        assertEquals(0L, s.expenseKop)
        assertEquals(0L, s.netKop)
        assertNull(s.savingsRate)      // нулевой доход -> "—"
        assertNull(s.avgExpenseKop)
        assertFalse(s.hasData)
    }

    @Test fun month_summary_computes_kpis() {
        val expenses = listOf(exp("Продукты", "Магнит", 200000), exp("Транспорт", "Бензин", 200000))
        val incomes = listOf(inc(1000000, savings = 100000))
        val s = Analytics.monthSummary(expenses, incomes)
        assertEquals(1000000L, s.incomeKop)
        assertEquals(400000L, s.expenseKop)
        assertEquals(600000L, s.netKop)
        assertEquals(0.6, s.savingsRate!!, 1e-9)
        assertEquals(2, s.expenseCount)
        assertEquals(200000L, s.avgExpenseKop)
        assertEquals(100000L, s.toSavingsKop)
        assertTrue(s.hasData)
    }

    @Test fun expense_by_category_sorted_with_shares() {
        val expenses = listOf(
            exp("Продукты", "Магнит", 600000),
            exp("Транспорт", "Бензин", 300000),
            exp("Транспорт", "Такси", 100000),
        )
        val byCat = Analytics.expenseByCategory(expenses)
        assertEquals("Продукты", byCat[0].name)
        assertEquals(600000L, byCat[0].amountKop)
        assertEquals(0.6, byCat[0].share!!, 1e-9)
        assertEquals("Транспорт", byCat[1].name)
        assertEquals(400000L, byCat[1].amountKop)
        assertEquals(0.4, byCat[1].share!!, 1e-9)
    }

    @Test fun leak_detector_by_share() {
        val current = listOf(
            exp("Рестораны", "Кафе", 800000),  // 0.80
            exp("Продукты", "Магнит", 50000),  // 0.05
            exp("Транспорт", "Бензин", 150000), // 0.15 ровно -> не флагуется
        )
        val flags = LeakDetector.detect(
            current = current,
            previousMonths = emptyList(),
            config = LeakConfig(shareThreshold = 0.15),
            discretionaryCategories = setOf("Рестораны"),
        )
        assertEquals(1, flags.size)
        assertEquals("Рестораны", flags[0].category)
        assertTrue(flags[0].flaggedByShare)
        assertFalse(flags[0].flaggedByGrowth)
        assertTrue(flags[0].discretionary)
    }

    @Test fun leak_detector_by_growth() {
        val current = listOf(exp("Развлечения", "Кино", 200000), exp("Продукты", "Магнит", 100000))
        val prev = listOf(
            listOf(exp("Развлечения", "Кино", 100000), exp("Продукты", "Магнит", 100000)),
            listOf(exp("Развлечения", "Кино", 100000), exp("Продукты", "Магнит", 100000)),
            listOf(exp("Развлечения", "Кино", 100000), exp("Продукты", "Магнит", 100000)),
        )
        // высокий порог доли, чтобы сработал только рост
        val flags = LeakDetector.detect(current, prev, LeakConfig(shareThreshold = 0.95, growthThreshold = 0.30))
        assertEquals(1, flags.size)
        assertEquals("Развлечения", flags[0].category)
        assertTrue(flags[0].flaggedByGrowth)
        assertEquals(1.0, flags[0].growth!!, 1e-9) // (200000-100000)/100000
    }

    @Test fun plan_fact_diff_and_overspend() {
        val planned = mapOf("Продукты" to 1000000L, "Транспорт" to 500000L)
        val actual = mapOf("Продукты" to 1200000L, "Развлечения" to 300000L)
        val rows = PlanFact.build(planned, actual).associateBy { it.category }

        val prod = rows.getValue("Продукты")
        assertEquals(-200000L, prod.diffKop)
        assertTrue(prod.overspent)
        assertEquals(1.2, prod.usage!!, 1e-9)

        val trans = rows.getValue("Транспорт")
        assertEquals(500000L, trans.diffKop)
        assertFalse(trans.overspent)

        val ent = rows.getValue("Развлечения")
        assertTrue(ent.overspent)         // план 0, факт 300000
        assertNull(ent.usage)             // деление на 0 -> null
    }

    @Test fun distribution_keeps_every_kopeck() {
        val alloc = Distribution.allocate(8000000, Distribution.DEFAULT)
        assertEquals(listOf(6000000L, 1200000L, 800000L), alloc.map { it.amountKop })
        assertEquals(8000000L, alloc.sumOf { it.amountKop })
    }

    @Test fun distribution_remainder_to_last_bucket() {
        val buckets = listOf(DistributionBucket("a", 50), DistributionBucket("b", 50))
        val alloc = Distribution.allocate(10001, buckets)
        assertEquals(5000L, alloc[0].amountKop)
        assertEquals(5001L, alloc[1].amountKop) // остаток уходит в последнюю
        assertEquals(10001L, alloc.sumOf { it.amountKop })
    }

    @Test fun distribution_validates_percent_sum() {
        assertTrue(Distribution.isValid(Distribution.DEFAULT))
        assertFalse(Distribution.isValid(listOf(DistributionBucket("a", 50), DistributionBucket("b", 60))))
        assertThrows(IllegalArgumentException::class.java) {
            Distribution.allocate(1000, listOf(DistributionBucket("a", 90)))
        }
    }
}
