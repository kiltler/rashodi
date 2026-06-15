package com.rashodi.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.BarGroup
import com.rashodi.app.ui.components.ColorDot
import com.rashodi.app.ui.components.DonutChart
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.GroupedBarChart
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.MoneyText
import com.rashodi.app.ui.components.MonthSelector
import com.rashodi.app.ui.components.SectionHeader
import com.rashodi.app.ui.components.TrackBar
import com.rashodi.app.ui.components.percentLabel
import com.rashodi.app.ui.theme.LocalAppColors
import com.rashodi.core.analytics.NamedAmount
import com.rashodi.core.money.Money
import com.rashodi.core.time.RuMonths

@Composable
fun AnalyticsScreen() {
    val vm = appViewModel { AnalyticsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val chartColors = LocalAppColors.current.chart

    fun colorFor(name: String, index: Int): Color =
        state.colorMap[name]?.let { Color(it) } ?: chartColors[index % chartColors.size]

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Аналитика", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(selected = state.period.mode == PeriodMode.MONTH, onClick = { vm.setMode(PeriodMode.MONTH) }, label = { Text("Месяц") })
                androidx.compose.material3.FilterChip(selected = state.period.mode == PeriodMode.YEAR, onClick = { vm.setMode(PeriodMode.YEAR) }, label = { Text("Год") })
                androidx.compose.material3.FilterChip(selected = state.period.mode == PeriodMode.ALL, onClick = { vm.setMode(PeriodMode.ALL) }, label = { Text("Всё время") })
            }
        }
        if (state.period.mode != PeriodMode.ALL) {
            item { MonthSelector(current = state.period.month, onChange = vm::setMonth) }
        }
        item {
            Text(state.periodLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Расходы по категориям
        item {
            AppCard {
                SectionHeader("Расходы по категориям")
                Gap(12)
                if (state.byCategory.isEmpty()) {
                    NoData()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            values = state.byCategory.map { it.amountKop.toFloat() },
                            colors = state.byCategory.mapIndexed { i, na -> colorFor(na.name, i) },
                            modifier = Modifier.size(140.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Расход", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(state.expenseTotalKop, style = MaterialTheme.typography.titleSmall, withFraction = false, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HGap(16)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.byCategory.take(6).forEachIndexed { i, na ->
                                LegendRow(na, colorFor(na.name, i))
                            }
                        }
                    }
                }
            }
        }

        // Подкатегории
        item {
            AppCard {
                SectionHeader("Расходы по подкатегориям")
                Gap(12)
                if (state.bySubcategory.isEmpty()) NoData() else
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.bySubcategory.forEachIndexed { i, na -> BarRow(na, LocalAppColors.current.chart[i % LocalAppColors.current.chart.size]) }
                    }
            }
        }

        // Доходы по источникам
        item {
            AppCard {
                SectionHeader("Доходы по источникам")
                Gap(12)
                if (state.bySource.isEmpty()) NoData() else
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.bySource.forEachIndexed { i, na -> BarRow(na, colorFor(na.name, i)) }
                    }
            }
        }

        // Динамика по месяцам
        if (state.byMonth.size > 1) {
            item {
                AppCard {
                    SectionHeader("Динамика расходов по месяцам")
                    Gap(12)
                    GroupedBarChart(
                        groups = state.byMonth.map { BarGroup(monthLabel(it.name), listOf(it.amountKop)) },
                        colors = listOf(LocalAppColors.current.expense),
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(na: NamedAmount, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color, 9)
        HGap(8)
        Text(na.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(percentLabel(na.share), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BarRow(na: NamedAmount, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(na.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(percentLabel(na.share), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HGap(10)
            MoneyText(na.amountKop, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Gap(6)
        TrackBar(fraction = (na.share ?: 0.0).toFloat(), color = color)
    }
}

@Composable
private fun NoData() {
    Text("Нет данных за период", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun monthLabel(key: String): String {
    val parts = key.split("-")
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return key
    return RuMonths.short(m)
}
