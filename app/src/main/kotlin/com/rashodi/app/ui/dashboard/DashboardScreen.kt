package com.rashodi.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.TrendingUp
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
import com.rashodi.app.ui.components.ColorDot
import com.rashodi.app.ui.components.EmptyState
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.GroupedBarChart
import com.rashodi.app.ui.components.BarGroup
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.LegendDot
import com.rashodi.app.ui.components.MoneyText
import com.rashodi.app.ui.components.MonthSelector
import com.rashodi.app.ui.components.Pill
import com.rashodi.app.ui.components.SectionHeader
import com.rashodi.app.ui.components.TrackBar
import com.rashodi.app.ui.components.percentLabel
import com.rashodi.app.ui.components.signedPercentLabel
import com.rashodi.app.ui.theme.LocalAppColors
import com.rashodi.core.analytics.LeakFlag
import com.rashodi.core.analytics.MonthSummary
import com.rashodi.core.analytics.NamedAmount

@Composable
fun DashboardScreen(onOpenLeaks: () -> Unit, onAdd: () -> Unit) {
    val vm = appViewModel { DashboardViewModel(it.repository, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Дашборд",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item { MonthSelector(current = state.month, onChange = vm::setMonth) }

        if (!state.summary.hasData) {
            item {
                AppCard {
                    EmptyState(
                        icon = Icons.Rounded.Inbox,
                        title = "Нет данных за ${state.month.label().lowercase()}",
                        subtitle = "Добавьте первую операцию кнопкой «+», и здесь появятся показатели и графики.",
                    )
                }
            }
        } else {
            item { KpiSection(state.summary) }
            item { TrendCard(state.trend) }
            item { TopCategoriesCard(state.topCategories, state.categoryColors) }
            item { LeakCard(state.leaks, onOpenLeaks) }
        }
    }
}

@Composable
private fun KpiSection(s: MonthSummary) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Доход", Modifier.weight(1f)) {
                MoneyText(s.incomeKop, style = MaterialTheme.typography.titleLarge, color = colors.income, fontWeight = FontWeight.SemiBold)
            }
            StatCard("Расход", Modifier.weight(1f)) {
                MoneyText(s.expenseKop, style = MaterialTheme.typography.titleLarge, color = colors.expense, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Чистыми", Modifier.weight(1f)) {
                MoneyText(s.netKop, style = MaterialTheme.typography.titleLarge, signColored = true, alwaysSign = true, fontWeight = FontWeight.SemiBold)
            }
            StatCard("Норма сбережений", Modifier.weight(1f), caption = "расчётная") {
                Text(
                    signedPercentLabel(s.savingsRate),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Средний чек", Modifier.weight(1f)) {
                if (s.avgExpenseKop == null) Dash() else
                    MoneyText(s.avgExpenseKop!!, style = MaterialTheme.typography.titleMedium)
            }
            StatCard("В накопления", Modifier.weight(1f)) {
                MoneyText(s.toSavingsKop, style = MaterialTheme.typography.titleMedium, color = colors.income)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    value: @Composable () -> Unit,
) {
    AppCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Gap(8)
        value()
        if (caption != null) {
            Gap(4)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Dash() {
    Text("—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TrendCard(trend: List<TrendPoint>) {
    val colors = LocalAppColors.current
    AppCard {
        SectionHeader("Доход и расход", subtitle = "по месяцам")
        Gap(12)
        GroupedBarChart(
            groups = trend.map { BarGroup(it.label, listOf(it.incomeKop, it.expenseKop)) },
            colors = listOf(colors.income, colors.expense),
            modifier = Modifier.fillMaxWidth().height(160.dp),
        )
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(colors.income, "Доход")
            LegendDot(colors.expense, "Расход")
        }
    }
}

@Composable
private fun TopCategoriesCard(top: List<NamedAmount>, colorMap: Map<String, Int>) {
    AppCard {
        SectionHeader("Топ категорий расходов")
        Gap(12)
        if (top.isEmpty()) {
            Text("Нет расходов за месяц", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                top.forEach { cat ->
                    val color = colorMap[cat.name]?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(color, 10)
                            HGap(8)
                            Text(cat.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(percentLabel(cat.share), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HGap(10)
                            MoneyText(cat.amountKop, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Gap(6)
                        TrackBar(fraction = (cat.share ?: 0.0).toFloat(), color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeakCard(leaks: List<LeakFlag>, onOpenLeaks: () -> Unit) {
    val colors = LocalAppColors.current
    AppCard(modifier = Modifier.clickable { onOpenLeaks() }) {
        SectionHeader(
            "Детектор утечек",
            subtitle = "куда незаметно утекают деньги",
            trailing = { Icon(Icons.Rounded.TrendingUp) },
        )
        Gap(12)
        if (leaks.isEmpty()) {
            Text(
                "За этот месяц заметных утечек не найдено.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                leaks.take(4).forEach { leak ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(leak.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (leak.discretionary) {
                                    HGap(8)
                                    Pill("необязательная")
                                }
                            }
                            Gap(2)
                            Text(
                                buildReason(leak),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyText(leak.amountKop, style = MaterialTheme.typography.bodyMedium, color = colors.expense, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun buildReason(leak: LeakFlag): String {
    val parts = mutableListOf<String>()
    if (leak.flaggedByShare) parts.add("доля ${percentLabel(leak.share)} расходов")
    if (leak.flaggedByGrowth) parts.add("рост ${signedPercentLabel(leak.growth)} к среднему за 3 мес.")
    return parts.joinToString(" · ")
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.material3.Icon(
        imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
