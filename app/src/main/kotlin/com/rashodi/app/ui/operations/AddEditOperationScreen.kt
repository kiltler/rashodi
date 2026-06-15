package com.rashodi.app.ui.operations

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.db.TYPE_INCOME
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.LabeledDropdown
import com.rashodi.app.ui.components.dateLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOperationScreen(initialKind: String, id: Long, onDone: () -> Unit) {
    val vm = appViewModel { AddEditViewModel(it.repository, initialKind, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }

    if (state.saved) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onDone() }
    }

    val type = if (state.kind == KIND_INCOME) TYPE_INCOME else TYPE_EXPENSE
    val categoryNames = cats.filter { it.type == type }.map { it.name }
    val subOptions = cats.firstOrNull { it.type == TYPE_EXPENSE && it.name == state.category }?.subcategories ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Изменить операцию" else "Новая операция") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить")
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = state.kind == KIND_EXPENSE,
                    onClick = { vm.setKind(KIND_EXPENSE) },
                    enabled = !state.isEditing,
                    label = { Text("Расход") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.kind == KIND_INCOME,
                    onClick = { vm.setKind(KIND_INCOME) },
                    enabled = !state.isEditing,
                    label = { Text("Доход") },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.amount,
                onValueChange = vm::setAmount,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сумма, ₽") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
            )

            // Дата (системный выбор даты)
            DateField(epochDay = state.epochDay) {
                val d = LocalDate.ofEpochDay(state.epochDay)
                DatePickerDialog(
                    context,
                    { _, y, m, day -> vm.setDate(LocalDate.of(y, m + 1, day).toEpochDay()) },
                    d.year, d.monthValue - 1, d.dayOfMonth,
                ).show()
            }

            LabeledDropdown(
                label = "Категория",
                value = state.category,
                options = categoryNames,
                onSelect = vm::setCategory,
                placeholder = "Выберите категорию",
            )

            if (state.kind == KIND_EXPENSE && subOptions.isNotEmpty()) {
                LabeledDropdown(
                    label = "Подкатегория",
                    value = state.subcategory,
                    options = subOptions,
                    onSelect = vm::setSubcategory,
                    placeholder = "Выберите подкатегорию",
                )
            }

            if (state.kind == KIND_INCOME) {
                OutlinedTextField(
                    value = state.source,
                    onValueChange = vm::setSource,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("От кого (источник)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = state.toSavings,
                    onValueChange = vm::setToSavings,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("В накопления, ₽ (необязательно)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Описание") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = state.comment,
                onValueChange = vm::setComment,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий") },
                shape = MaterialTheme.shapes.medium,
            )

            Gap(4)
            Button(
                onClick = vm::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEditing) "Сохранить" else "Добавить")
            }
            Gap(12)
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Удалить операцию?") },
            text = { Text("Действие нельзя отменить.") },
            confirmButton = { TextButton(onClick = { showDelete = false; vm.delete() }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun DateField(epochDay: Long, onClick: () -> Unit) {
    Column {
        Text("Дата", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Gap(6)
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        ) {
            Text(
                dateLabel(epochDay),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
