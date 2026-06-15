package com.rashodi.app.ui.planfact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.MoneyText
import com.rashodi.app.ui.components.MonthSelector
import com.rashodi.app.ui.components.Pill
import com.rashodi.app.ui.components.TrackBar
import com.rashodi.app.ui.theme.LocalAppColors
import com.rashodi.core.analytics.PlanFactRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFactScreen(onBack: () -> Unit) {
    val vm = appViewModel { PlanFactViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val edit by vm.edit.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("План-факт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") } },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { MonthSelector(current = state.month, onChange = vm::setMonth) }
            item {
                AppCard {
                    Row {
                        Totals("План", state.totalPlannedKop, Modifier.weight(1f))
                        Totals("Факт", state.totalActualKop, Modifier.weight(1f), color = colors.expense)
                        Totals("Разница", state.totalPlannedKop - state.totalActualKop, Modifier.weight(1f), signColored = true)
                    }
                }
            }
            item {
                Text(
                    "Нажмите на категорию, чтобы задать план на месяц.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.rows, key = { it.category }) { row ->
                PlanRow(row, color = state.colorMap[row.category]?.let { Color(it) }) {
                    vm.openEdit(row.category, row.plannedKop)
                }
            }
        }
    }

    edit?.let { e ->
        AlertDialog(
            onDismissRequest = vm::closeEdit,
            title = { Text("План: ${e.category}") },
            text = {
                OutlinedTextField(
                    value = e.amount,
                    onValueChange = vm::setEditAmount,
                    label = { Text("Сумма плана, ₽ (0 — убрать)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = { TextButton(onClick = vm::savePlan) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = vm::closeEdit) { Text("Отмена") } },
        )
    }
}

@Composable
private fun Totals(label: String, kop: Long, modifier: Modifier = Modifier, color: Color = Color.Unspecified, signColored: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Gap(4)
        MoneyText(kop, style = MaterialTheme.typography.titleSmall, color = color, signColored = signColored, withFraction = false, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlanRow(row: PlanFactRow, color: Color?, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    AppCard(modifier = Modifier.clickable { onClick() }, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.category, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            if (row.plannedKop > 0) {
                Pill(
                    text = if (row.overspent) "перерасход" else "в плане",
                    container = (if (row.overspent) colors.expense else colors.income).copy(alpha = 0.16f),
                    content = if (row.overspent) colors.expense else colors.income,
                )
            }
        }
        Gap(8)
        Row {
            LabelValue("План", row.plannedKop, Modifier.weight(1f))
            LabelValue("Факт", row.actualKop, Modifier.weight(1f), color = colors.expense)
            Column(Modifier.weight(1f)) {
                Text("Разница", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(row.diffKop, style = MaterialTheme.typography.bodyMedium, signColored = true, alwaysSign = true)
            }
        }
        if (row.plannedKop > 0) {
            Gap(8)
            TrackBar(
                fraction = (row.usage ?: 0.0).toFloat().coerceIn(0f, 1f),
                color = if (row.overspent) colors.expense else (color ?: colors.income),
            )
        }
    }
}

@Composable
private fun LabelValue(label: String, kop: Long, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(kop, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
