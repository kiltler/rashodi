package com.rashodi.app

import com.rashodi.app.io.XlsxWriter
import org.junit.Assert.assertTrue
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Test

class XlsxWriterTest {

    @Test
    fun builds_valid_zip_with_required_parts() {
        val bytes = XlsxWriter.build(
            listOf(
                XlsxWriter.Sheet("Учет расходов", listOf(listOf("Категория", "Стоимость"), listOf("Продукты", "2450.00"))),
                XlsxWriter.Sheet("Учет доходов", listOf(listOf("Категория", "Приход"), listOf("Зарплата", "45000.00"))),
            ),
        )
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                entries.add(e.name)
                e = zip.nextEntry
            }
        }
        assertTrue("[Content_Types].xml" in entries)
        assertTrue("xl/workbook.xml" in entries)
        assertTrue("xl/worksheets/sheet1.xml" in entries)
        assertTrue("xl/worksheets/sheet2.xml" in entries)
    }

    @Test
    fun numbers_and_strings_are_typed() {
        val bytes = XlsxWriter.build(
            listOf(XlsxWriter.Sheet("Л", listOf(listOf("Текст", "100.50")))),
        )
        val sheet = readEntry(bytes, "xl/worksheets/sheet1.xml")
        // число записывается как <v>, строка — как inlineStr
        assertTrue(sheet.contains("<v>100.50</v>"))
        assertTrue(sheet.contains("inlineStr"))
        assertTrue(sheet.contains("Текст"))
    }

    private fun readEntry(bytes: ByteArray, name: String): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == name) return zip.readBytes().toString(Charsets.UTF_8)
                e = zip.nextEntry
            }
        }
        return ""
    }
}
