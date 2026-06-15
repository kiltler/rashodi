package com.rashodi.core

import com.rashodi.core.csv.Csv
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test fun round_trip_with_quotes_commas_and_newlines() {
        val data = listOf(
            listOf("Категория", "Сумма", "Комментарий"),
            listOf("Продукты, еда", "1234,56", "со \"скидкой\""),
            listOf("Перенос", "10", "строка1\nстрока2"),
        )
        val csv = Csv.write(data)
        val parsed = Csv.read(csv)
        assertEquals(data, parsed)
    }

    @Test fun escapes_quotes_correctly() {
        val csv = Csv.write(listOf(listOf("a,b", "e\"f")))
        assertEquals("\"a,b\",\"e\"\"f\"\r\n", csv)
    }

    @Test fun detects_semicolon_separator() {
        val parsed = Csv.read("a;b;c\r\n1;2;3\r\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), parsed)
    }

    @Test fun skips_blank_lines_and_bom() {
        val parsed = Csv.read("﻿a,b\r\n\r\nc,d\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parsed)
    }
}
