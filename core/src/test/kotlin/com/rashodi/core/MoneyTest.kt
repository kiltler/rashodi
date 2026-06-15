package com.rashodi.core

import com.rashodi.core.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    private val NBSP = " "

    @Test fun parse_basic() {
        assertEquals(123456L, Money.parseToKop("1 234,56"))
        assertEquals(123450L, Money.parseToKop("1234.5"))
        assertEquals(1000L, Money.parseToKop("10"))
        assertEquals(1L, Money.parseToKop("0,01"))
        assertEquals(100000000L, Money.parseToKop("1 000 000"))
    }

    @Test fun parse_handles_float_artifacts_from_excel() {
        // 63372.90000000001 -> 6 337 290 копеек (без float-мусора)
        assertEquals(6337290L, Money.parseToKop("63372.90000000001"))
    }

    @Test fun parse_signs() {
        assertEquals(-5000L, Money.parseToKop("-50"))
        assertEquals(10000L, Money.parseToKop("+100"))
    }

    @Test fun parse_nbsp_and_ruble_sign() {
        assertEquals(123456L, Money.parseToKop("1 234,56 ₽"))
    }

    @Test fun parse_invalid_returns_null() {
        assertNull(Money.parseToKop(""))
        assertNull(Money.parseToKop("   "))
        assertNull(Money.parseToKop("abc"))
        assertNull(Money.parseToKop("-"))
        assertNull(Money.parseToKop(null))
    }

    @Test fun parse_rounding_half_up() {
        assertEquals(101L, Money.parseToKop("1.005")) // 1.005 -> 1.01
    }

    @Test fun format_groups_and_fraction() {
        assertEquals("1${NBSP}234,56${NBSP}₽", Money.format(123456))
        assertEquals("1${NBSP}000${NBSP}000,00${NBSP}₽", Money.format(100000000))
        assertEquals("0,05${NBSP}₽", Money.format(5))
        assertEquals("0,00${NBSP}₽", Money.format(0))
    }

    @Test fun format_negative_and_sign() {
        assertEquals("-50,00${NBSP}₽", Money.format(-5000))
        assertEquals("+50,00${NBSP}₽", Money.format(5000, alwaysSign = true))
    }

    @Test fun format_without_symbol_and_fraction() {
        assertEquals("1${NBSP}234,56", Money.format(123456, withSymbol = false))
        assertEquals("1${NBSP}234${NBSP}₽", Money.format(123456, withFraction = false))
    }

    @Test fun savings_rate_null_when_no_income() {
        assertNull(Money.savingsRate(0, 1000))
        assertNull(Money.savingsRate(-100, 1000))
        assertEquals(0.6, Money.savingsRate(10000, 4000)!!, 1e-9)
    }

    @Test fun share_and_growth_guard_zero_denominator() {
        assertNull(Money.share(100, 0))
        assertEquals(0.25, Money.share(2500, 10000)!!, 1e-9)
        assertNull(Money.growth(100, 0))
        assertEquals(0.3, Money.growth(130, 100)!!, 1e-9)
    }

    @Test fun average_half_up() {
        assertEquals(200L, Money.average(listOf(100, 200, 300)))
        assertEquals(2L, Money.average(listOf(1, 2))) // 1.5 -> 2
        assertEquals(0L, Money.average(emptyList()))
    }

    @Test fun decimal_string_for_csv() {
        assertEquals("2450.00", Money.toDecimalString(245000))
        assertEquals("0.05", Money.toDecimalString(5))
        assertEquals("-50.00", Money.toDecimalString(-5000))
        assertEquals("1234.56", Money.toDecimalString(123456))
    }

    @Test fun progress_guard_zero_target() {
        assertNull(Money.progress(100, 0))
        assertEquals(0.5, Money.progress(50, 100)!!, 1e-9)
    }
}
