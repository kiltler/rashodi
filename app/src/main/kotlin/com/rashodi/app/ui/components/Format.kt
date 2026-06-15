package com.rashodi.app.ui.components

import com.rashodi.core.time.RuMonths
import java.time.LocalDate
import java.util.Locale

/** Дата для списков: "1 мая 2026". */
fun dateLabel(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val month = RuMonths.short(d.monthValue).lowercase()
    return "${d.dayOfMonth} $month ${d.year}"
}

/** Доля 0..1 в проценты: 0.153 -> "15%". null -> "—". */
fun percentLabel(value: Double?, dash: String = "—"): String =
    if (value == null) dash else "${Math.round(value * 100)}%"

/** Проценты со знаком (для роста/нормы сбережений): 0.31 -> "+31%". */
fun signedPercentLabel(value: Double?, dash: String = "—"): String {
    if (value == null) return dash
    val pct = Math.round(value * 100)
    val sign = if (pct > 0) "+" else ""
    return "$sign$pct%"
}

fun percentPrecise(value: Double?, dash: String = "—"): String =
    if (value == null) dash else String.format(Locale.US, "%.1f%%", value * 100)
