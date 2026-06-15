package com.rashodi.app.ui.importexport

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.io.ShareExport
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(onBack: () -> Unit) {
    val vm = appViewModel { ImportExportViewModel(it.repository) }
    val context = LocalContext.current
    val preview by vm.preview.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }

    val openImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.previewImport(context, it) }
    }
    val createXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ShareExport.MIME_XLSX)) { uri ->
        uri?.let { vm.exportXlsx(context, it) }
    }
    val createCsvExpenses = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.exportExpensesCsv(context, it) }
    }
    val createCsvIncomes = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.exportIncomesCsv(context, it) }
    }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportJson(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт и экспорт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") } },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Google Таблицы — главная кнопка
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        HGap(10)
                        Text("Google Таблицы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Gap(8)
                    Text(
                        "Одной кнопкой подготовим файл и откроем «Поделиться» → выберите Google Таблицы или Диск. Файл импортируется как полноценная таблица.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Gap(14)
                    Button(
                        onClick = { vm.exportToGoogleSheets(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null)
                        HGap(8)
                        Text("Выгрузить в Google Таблицы")
                    }
                }
            }

            SectionHeader("Экспорт на устройство")
            ActionRow("Excel (.xlsx)", "Книга с листами «Учет расходов» и «Учет доходов»", Icons.Rounded.GridOn) {
                createXlsx.launch("Расходы.xlsx")
            }
            ActionRow("CSV — расходы", "Таблица расходов в CSV", Icons.Rounded.Description) {
                createCsvExpenses.launch("Учет_расходов.csv")
            }
            ActionRow("CSV — доходы", "Таблица доходов в CSV", Icons.Rounded.Description) {
                createCsvIncomes.launch("Учет_доходов.csv")
            }
            ActionRow("Полный бэкап (JSON)", "Все данные для восстановления", Icons.Rounded.FileDownload) {
                createJson.launch("rashodi_backup.json")
            }

            SectionHeader("Импорт")
            ActionRow("Выбрать файл (CSV или JSON)", "CSV из листов «Учет расходов»/«Учет доходов» или JSON-бэкап", Icons.Rounded.FileUpload) {
                openImport.launch(arrayOf("*/*"))
            }
            Text(
                "Подсказка: чтобы перенести данные из Excel, один раз сохраните лист как CSV и импортируйте его здесь. Перед записью покажем предпросмотр.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(12)
        }
    }

    preview?.let { p ->
        AlertDialog(
            onDismissRequest = vm::cancelPreview,
            title = { Text(if (p.isBackup) "Восстановить из бэкапа?" else "Предпросмотр импорта") },
            text = {
                Column {
                    if (p.isBackup) {
                        Text("Будет восстановлено:", style = MaterialTheme.typography.bodyMedium)
                        Gap(6)
                        Text("• Расходы: ${p.expenseCount}", style = MaterialTheme.typography.bodyMedium)
                        Text("• Доходы: ${p.incomeCount}", style = MaterialTheme.typography.bodyMedium)
                        Gap(8)
                        Text("Внимание: текущие данные будут заменены.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Распознано:", style = MaterialTheme.typography.bodyMedium)
                        Gap(6)
                        if (p.expenseCount > 0) Text("• Расходы: ${p.expenseCount}", style = MaterialTheme.typography.bodyMedium)
                        if (p.incomeCount > 0) Text("• Доходы: ${p.incomeCount}", style = MaterialTheme.typography.bodyMedium)
                        if (p.skipped > 0) {
                            Gap(8)
                            Text("Пропущено строк: ${p.skipped}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            p.reasons.forEach { r ->
                                Text("• $r", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::confirmImport) { Text(if (p.isBackup) "Восстановить" else "Импортировать") } },
            dismissButton = { TextButton(onClick = vm::cancelPreview) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable { onClick() }, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            HGap(14)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
