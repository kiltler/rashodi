package com.rashodi.app.ui.funds

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.rashodi.app.ui.categories.CategoriesViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.ColorDot
import com.rashodi.app.ui.components.ColorSwatchRow
import com.rashodi.app.ui.components.EmptyState
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.MoneyText
import com.rashodi.app.ui.components.TrackBar
import com.rashodi.app.ui.components.dateLabel
import com.rashodi.app.ui.components.percentLabel
import com.rashodi.app.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundsScreen(onBack: () -> Unit) {
    val vm = appViewModel { FundsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val fundDraft by vm.fundDraft.collectAsStateWithLifecycle()
    val txnDraft by vm.txnDraft.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фонды и цели") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::openNewFund) { Icon(Icons.Rounded.Add, contentDescription = "Новый фонд") }
        },
    ) { inner ->
        if (state.funds.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Savings,
                title = "Фондов пока нет",
                subtitle = "Создайте цель — «Подушка безопасности», «Отпуск», «Крупная покупка» — и отслеживайте прогресс.",
                modifier = Modifier.padding(inner).padding(top = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AppCard {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Всего накоплено", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(state.totalCurrentKop, style = MaterialTheme.typography.titleLarge, color = LocalAppColors.current.income, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Цель суммарно", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(state.totalTargetKop, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                items(state.funds, key = { it.fund.id }) { fundUi ->
                    FundCard(
                        fundUi = fundUi,
                        onDeposit = { vm.openTxn(fundUi.fund, deposit = true) },
                        onWithdraw = { vm.openTxn(fundUi.fund, deposit = false) },
                        onEdit = { vm.openEditFund(fundUi.fund) },
                        onDelete = { vm.deleteFund(fundUi.fund) },
                    )
                }
            }
        }
    }

    fundDraft?.let { d ->
        AlertDialog(
            onDismissRequest = vm::closeFund,
            title = { Text(if (d.isNew) "Новый фонд" else "Изменить фонд") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = d.name, onValueChange = vm::setFundName, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = d.target, onValueChange = vm::setFundTarget, label = { Text("Цель, ₽") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    ColorSwatchRow(palette = CategoriesViewModel.PALETTE, selected = d.colorArgb, onSelect = vm::setFundColor)
                }
            },
            confirmButton = { TextButton(onClick = vm::saveFund) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = vm::closeFund) { Text("Отмена") } },
        )
    }

    txnDraft?.let { d ->
        AlertDialog(
            onDismissRequest = vm::closeTxn,
            title = { Text(d.fundName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = d.deposit, onClick = { vm.setTxnDeposit(true) }, label = { Text("Пополнить") })
                        FilterChip(selected = !d.deposit, onClick = { vm.setTxnDeposit(false) }, label = { Text("Списать") })
                    }
                    OutlinedTextField(value = d.amount, onValueChange = vm::setTxnAmount, label = { Text("Сумма, ₽") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = d.note, onValueChange = vm::setTxnNote, label = { Text("Комментарий") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = vm::saveTxn, enabled = d.canSave) { Text("Готово") } },
            dismissButton = { TextButton(onClick = vm::closeTxn) { Text("Отмена") } },
        )
    }
}

@Composable
private fun FundCard(
    fundUi: FundUi,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = Color(fundUi.fund.colorArgb)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color, 14)
            HGap(12)
            Text(fundUi.fund.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Изменить") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить") }
        }
        Gap(8)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoneyText(fundUi.currentKop, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.SemiBold)
            HGap(6)
            Text("из", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HGap(6)
            MoneyText(fundUi.fund.targetKop, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HGap(8)
            Text(percentLabel(fundUi.progress), style = MaterialTheme.typography.labelMedium, color = color)
        }
        Gap(8)
        TrackBar(fraction = (fundUi.progress ?: 0.0).toFloat(), color = color)
        Gap(12)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDeposit, modifier = Modifier.weight(1f)) { Text("Пополнить") }
            OutlinedButton(onClick = onWithdraw, modifier = Modifier.weight(1f)) { Text("Списать") }
        }
        if (fundUi.txns.isNotEmpty()) {
            Gap(12)
            Text("История", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Gap(4)
            fundUi.txns.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (t.note.isBlank()) (if (t.amountKop >= 0) "Пополнение" else "Списание") else t.note, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(dateLabel(t.epochDay), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MoneyText(t.amountKop, style = MaterialTheme.typography.bodyMedium, signColored = true, alwaysSign = true)
                }
            }
        }
    }
}
