package com.rashodi.core.csv

/** Минимальный, но корректный CSV (RFC 4180): кавычки, экранирование "", переводы строк в полях. */
object Csv {

    /** Сериализует строки в CSV-текст. Разделитель по умолчанию — запятая (для Google Таблиц). */
    fun write(rows: List<List<String>>, separator: Char = ','): String {
        val sb = StringBuilder()
        for (row in rows) {
            for (i in row.indices) {
                if (i > 0) sb.append(separator)
                sb.append(escape(row[i], separator))
            }
            sb.append("\r\n")
        }
        return sb.toString()
    }

    private fun escape(value: String, separator: Char): String {
        val needsQuote = value.any { it == separator || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    /** Разбирает CSV-текст в строки. Автоопределение разделителя (',' или ';') по первой строке. */
    fun read(text: String, separator: Char? = null): List<List<String>> {
        val sep = separator ?: detectSeparator(text)
        val rows = ArrayList<List<String>>()
        var field = StringBuilder()
        var row = ArrayList<String>()
        var inQuotes = false
        var i = 0
        // пропускаем BOM
        var start = 0
        if (text.isNotEmpty() && text[0] == '﻿') start = 1
        i = start
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            field.append('"'); i++
                        } else {
                            inQuotes = false
                        }
                    } else field.append(c)
                }
                c == '"' -> inQuotes = true
                c == sep -> { row.add(field.toString()); field = StringBuilder() }
                c == '\r' -> { /* пропускаем, перевод строки обработает \n */ }
                c == '\n' -> {
                    row.add(field.toString()); field = StringBuilder()
                    rows.add(row); row = ArrayList()
                }
                else -> field.append(c)
            }
            i++
        }
        // последнее поле/строка, если файл без завершающего перевода строки
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        // отбрасываем полностью пустые строки
        return rows.filterNot { it.size == 1 && it[0].isBlank() }
    }

    private fun detectSeparator(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        val semis = firstLine.count { it == ';' }
        val commas = firstLine.count { it == ',' }
        val tabs = firstLine.count { it == '\t' }
        return when {
            tabs > semis && tabs > commas -> '\t'
            semis > commas -> ';'
            else -> ','
        }
    }
}
