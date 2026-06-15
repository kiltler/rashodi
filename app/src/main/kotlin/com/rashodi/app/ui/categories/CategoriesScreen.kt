package com.rashodi.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.db.TYPE_INCOME
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.ColorDot
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.Pill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm = appViewModel { CategoriesViewModel(it.repository) }
    val cats by vm.categories.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Категории") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionAddHeader("Категории расходов") { vm.openNew(TYPE_EXPENSE) } }
            items(CategoriesViewModel.expenseOf(cats), key = { it.id }) { c ->
                CategoryCard(c, onEdit = { vm.openEdit(c) }, onDelete = { pendingDelete = c })
            }

            item { Gap(6); SectionAddHeader("Категории доходов") { vm.openNew(TYPE_INCOME) } }
            items(CategoriesViewModel.incomeOf(cats), key = { it.id }) { c ->
                CategoryCard(c, onEdit = { vm.openEdit(c) }, onDelete = { pendingDelete = c })
            }
        }
    }

    draft?.let { d ->
        CategoryEditDialog(
            draft = d,
            onName = vm::setName,
            onColor = vm::setColor,
            onDiscretionary = vm::setDiscretionary,
            onNewSub = vm::setNewSub,
            onAddSub = vm::addSub,
            onRemoveSub = vm::removeSub,
            onSave = vm::save,
            onCancel = vm::close,
        )
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить категорию?") },
            text = { Text("«${c.name}». Операции с этой категорией останутся, но категория исчезнет из справочника.") },
            confirmButton = { TextButton(onClick = { vm.delete(c); pendingDelete = null }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun SectionAddHeader(title: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onAdd) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            HGap(4)
            Text("Добавить")
        }
    }
}

@Composable
private fun CategoryCard(category: CategoryEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    AppCard(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(Color(category.colorArgb), 14)
            HGap(12)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, style = MaterialTheme.typography.titleSmall)
                    if (category.isDiscretionary) {
                        HGap(8)
                        Pill("необязательная")
                    }
                }
                if (category.subcategories.isNotEmpty()) {
                    Gap(4)
                    Text(
                        category.subcategories.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Изменить") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить") }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    draft: CategoryDraft,
    onName: (String) -> Unit,
    onColor: (Int) -> Unit,
    onDiscretionary: (Boolean) -> Unit,
    onNewSub: (String) -> Unit,
    onAddSub: () -> Unit,
    onRemoveSub: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    if (draft.isNew) "Новая категория" else "Изменить категорию",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = draft.name, onValueChange = onName,
                    label = { Text("Название") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )

                Text("Цвет", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoriesViewModel.PALETTE.forEach { argb ->
                        val selected = argb == draft.colorArgb
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                )
                                .clickable { onColor(argb) },
                        )
                    }
                }

                if (draft.type == TYPE_EXPENSE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Необязательная трата", style = MaterialTheme.typography.bodyLarge)
                            Text("Учитывается детектором утечек", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draft.discretionary, onCheckedChange = onDiscretionary)
                    }

                    Text("Подкатегории", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = draft.newSub, onValueChange = onNewSub,
                            modifier = Modifier.weight(1f), singleLine = true,
                            placeholder = { Text("Добавить подкатегорию") }, shape = MaterialTheme.shapes.medium,
                        )
                        HGap(8)
                        IconButton(onClick = onAddSub) { Icon(Icons.Rounded.Add, contentDescription = "Добавить") }
                    }
                    draft.subcategories.forEach { sub ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sub, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onRemoveSub(sub) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Убрать", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Отмена") }
                    HGap(8)
                    Button(onClick = onSave, enabled = draft.canSave) { Text("Сохранить") }
                }
            }
        }
    }
}
