package com.rashodi.app.ui.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.ColorDot
import com.rashodi.app.ui.components.EmptyState
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.MoneyText
import com.rashodi.app.ui.components.MonthSelector
import com.rashodi.app.ui.components.dateLabel
import com.rashodi.app.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsScreen(onEdit: (String, Long) -> Unit) {
    val vm = appViewModel { OperationsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<OperationItem?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Gap(12)
        Text("Операции", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Gap(12)

        OutlinedTextField(
            value = state.filter.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Поиск по описанию") },
            shape = MaterialTheme.shapes.large,
        )
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.filter.type == TypeFilter.ALL, onClick = { vm.setType(TypeFilter.ALL) }, label = { Text("Все") })
            FilterChip(selected = state.filter.type == TypeFilter.EXPENSE, onClick = { vm.setType(TypeFilter.EXPENSE) }, label = { Text("Расходы") })
            FilterChip(selected = state.filter.type == TypeFilter.INCOME, onClick = { vm.setType(TypeFilter.INCOME) }, label = { Text("Доходы") })
            FilterChip(selected = state.filter.monthEnabled, onClick = { vm.toggleMonthEnabled() }, label = { Text("За месяц") })
        }
        Gap(10)
        if (state.filter.monthEnabled) {
            MonthSelector(current = state.filter.month, onChange = vm::setMonth)
            Gap(10)
        }
        TotalsRow(state.totalIncomeKop, state.totalExpenseKop)
        Gap(8)

        if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.ReceiptLong,
                title = "Операций не найдено",
                subtitle = "Измените фильтры или добавьте операцию кнопкой «+».",
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 104.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { "${it.kind}-${it.id}" }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) pending = item
                            false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { DeleteBackground() },
                        content = { OperationRow(item) { onEdit(item.kind, item.id) } },
                    )
                }
            }
        }
    }

    val toDelete = pending
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Удалить операцию?") },
            text = { Text("«${toDelete.title}» на сумму ${com.rashodi.core.money.Money.format(toDelete.amountKop)}. Действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(toDelete); pending = null }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun TotalsRow(incomeKop: Long, expenseKop: Long) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Доходы", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(incomeKop, style = MaterialTheme.typography.titleMedium, color = colors.income, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text("Расходы", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(expenseKop, style = MaterialTheme.typography.titleMedium, color = colors.expense, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OperationRow(item: OperationItem, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorDot(item.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary, 12)
            HGap(12)
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                if (item.subtitle.isNotBlank()) {
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(dateLabel(item.epochDay), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HGap(8)
            MoneyText(
                kop = if (item.isIncome) item.amountKop else -item.amountKop,
                style = MaterialTheme.typography.titleMedium,
                signColored = true,
                alwaysSign = true,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeleteBackground() {
    val colors = LocalAppColors.current
    Box(
        Modifier
            .fillMaxSize()
            .padding(vertical = 1.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        androidx.compose.material3.Surface(
            color = colors.expense.copy(alpha = 0.18f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = colors.expense)
            }
        }
    }
}
