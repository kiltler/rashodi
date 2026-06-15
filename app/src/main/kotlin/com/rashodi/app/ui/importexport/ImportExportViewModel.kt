package com.rashodi.app.ui.importexport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.app.io.FileIo
import com.rashodi.app.io.JsonBackup
import com.rashodi.app.io.ShareExport
import com.rashodi.app.io.TableExport
import com.rashodi.app.io.XlsxWriter
import com.rashodi.core.csv.Csv
import com.rashodi.core.csv.SheetImport
import com.rashodi.core.csv.SheetKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImportPreview(
    val isBackup: Boolean,
    val expenseCount: Int,
    val incomeCount: Int,
    val skipped: Int,
    val reasons: List<String>,
    val pendingExpenses: List<ExpenseEntity> = emptyList(),
    val pendingIncomes: List<IncomeEntity> = emptyList(),
    val backup: JsonBackup.Backup? = null,
)

class ImportExportViewModel(private val repo: FinanceRepository) : ViewModel() {

    private val _preview = MutableStateFlow<ImportPreview?>(null)
    val preview: StateFlow<ImportPreview?> = _preview

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun cancelPreview() { _preview.value = null }

    // ---- Импорт ----
    fun previewImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val text = FileIo.readText(context, uri)
                val trimmed = text.trimStart()
                if (trimmed.startsWith("{")) {
                    val backup = JsonBackup.parse(text)
                    _preview.value = ImportPreview(
                        isBackup = true,
                        expenseCount = backup.expenses.size,
                        incomeCount = backup.incomes.size,
                        skipped = 0,
                        reasons = listOf("Полное восстановление заменит все текущие данные"),
                        backup = backup,
                    )
                } else {
                    val rows = Csv.read(text)
                    val result = SheetImport.parse(rows)
                    val expenses = if (result.kind == SheetKind.EXPENSE) result.expenses.map {
                        ExpenseEntity(
                            epochDay = it.epochDay, year = it.year, month = it.month,
                            category = it.category, subcategory = it.subcategory,
                            description = it.description, amountKop = it.amountKop, comment = it.comment,
                        )
                    } else emptyList()
                    val incomes = if (result.kind == SheetKind.INCOME) result.incomes.map {
                        IncomeEntity(
                            epochDay = it.epochDay, year = it.year, month = it.month,
                            source = it.source, category = it.category, description = it.description,
                            amountKop = it.amountKop, toBudgetKop = it.toBudgetKop,
                            toSavingsKop = it.toSavingsKop, comment = it.comment,
                        )
                    } else emptyList()

                    if (result.kind == SheetKind.UNKNOWN) {
                        _message.value = "Не удалось распознать таблицу. Нужны столбцы «Учет расходов» или «Учет доходов»."
                    } else {
                        _preview.value = ImportPreview(
                            isBackup = false,
                            expenseCount = expenses.size,
                            incomeCount = incomes.size,
                            skipped = result.skipped,
                            reasons = result.skipReasons,
                            pendingExpenses = expenses,
                            pendingIncomes = incomes,
                        )
                    }
                }
            } catch (e: Exception) {
                _message.value = "Ошибка чтения файла: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun confirmImport() {
        val p = _preview.value ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                if (p.isBackup && p.backup != null) {
                    val b = p.backup
                    repo.restore(b.expenses, b.incomes, b.categories, b.funds, b.fundTxns, b.plans)
                    _message.value = "Данные восстановлены из бэкапа."
                } else {
                    if (p.pendingExpenses.isNotEmpty()) repo.insertExpenses(p.pendingExpenses)
                    if (p.pendingIncomes.isNotEmpty()) repo.insertIncomes(p.pendingIncomes)
                    _message.value = "Импортировано: ${p.pendingExpenses.size + p.pendingIncomes.size} операций."
                }
            } catch (e: Exception) {
                _message.value = "Ошибка импорта: ${e.message}"
            } finally {
                _preview.value = null
                _busy.value = false
            }
        }
    }

    // ---- Экспорт на устройство (SAF) ----
    fun exportExpensesCsv(context: Context, uri: Uri) = writeText(context, uri) {
        TableExport.expensesCsv(repo.snapshotExpenses())
    }

    fun exportIncomesCsv(context: Context, uri: Uri) = writeText(context, uri) {
        TableExport.incomesCsv(repo.snapshotIncomes())
    }

    fun exportJson(context: Context, uri: Uri) = writeText(context, uri) {
        JsonBackup.export(fullBackup())
    }

    fun exportXlsx(context: Context, uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val bytes = buildWorkbook()
                FileIo.writeBytes(context, uri, bytes)
                _message.value = "Файл Excel сохранён."
            } catch (e: Exception) {
                _message.value = "Ошибка экспорта: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    // ---- Выгрузка в Google Таблицы одной кнопкой ----
    fun exportToGoogleSheets(context: Context) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val bytes = buildWorkbook()
                val uri = ShareExport.cacheFile(context, "Расходы.xlsx", bytes)
                ShareExport.shareToSheets(context, uri, ShareExport.MIME_XLSX, "Выгрузка в Google Таблицы")
            } catch (e: Exception) {
                _message.value = "Не удалось подготовить выгрузку: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun buildWorkbook(): ByteArray {
        val expenses = repo.snapshotExpenses()
        val incomes = repo.snapshotIncomes()
        return XlsxWriter.build(
            listOf(
                XlsxWriter.Sheet("Учет расходов", TableExport.expenseRows(expenses)),
                XlsxWriter.Sheet("Учет доходов", TableExport.incomeRows(incomes)),
            ),
        )
    }

    private suspend fun fullBackup(): JsonBackup.Backup = JsonBackup.Backup(
        expenses = repo.snapshotExpenses(),
        incomes = repo.snapshotIncomes(),
        categories = repo.snapshotCategories(),
        funds = repo.snapshotFunds(),
        fundTxns = repo.snapshotFundTxns(),
        plans = repo.snapshotPlans(),
    )

    private fun writeText(context: Context, uri: Uri, build: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                FileIo.writeText(context, uri, build())
                _message.value = "Файл сохранён."
            } catch (e: Exception) {
                _message.value = "Ошибка экспорта: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }
}
