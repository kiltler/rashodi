package com.rashodi.app.data.repo

import com.rashodi.app.data.db.AppDatabase
import com.rashodi.app.data.db.BudgetPlanEntity
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.FundEntity
import com.rashodi.app.data.db.FundTxnEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.seed.SeedData
import com.rashodi.core.model.ExpenseRow
import com.rashodi.core.model.IncomeRow
import kotlinx.coroutines.flow.Flow

/** Единая точка доступа к данным. Бизнес-расчёты живут в модуле :core. */
class FinanceRepository(private val db: AppDatabase) {

    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()
    val incomes: Flow<List<IncomeEntity>> = db.incomeDao().observeAll()
    val categories: Flow<List<CategoryEntity>> = db.categoryDao().observeAll()
    val funds: Flow<List<FundEntity>> = db.fundDao().observeFunds()
    val fundTxns: Flow<List<FundTxnEntity>> = db.fundDao().observeTxns()
    val plans: Flow<List<BudgetPlanEntity>> = db.budgetPlanDao().observeAll()

    // --- Операции ---
    suspend fun addExpense(e: ExpenseEntity) = db.expenseDao().insert(e)
    suspend fun updateExpense(e: ExpenseEntity) = db.expenseDao().update(e)
    suspend fun deleteExpense(e: ExpenseEntity) = db.expenseDao().delete(e)

    suspend fun addIncome(e: IncomeEntity) = db.incomeDao().insert(e)
    suspend fun updateIncome(e: IncomeEntity) = db.incomeDao().update(e)
    suspend fun deleteIncome(e: IncomeEntity) = db.incomeDao().delete(e)

    // --- Категории ---
    suspend fun upsertCategory(c: CategoryEntity) = db.categoryDao().insert(c)
    suspend fun updateCategory(c: CategoryEntity) = db.categoryDao().update(c)
    suspend fun deleteCategory(c: CategoryEntity) = db.categoryDao().delete(c)

    // --- Фонды ---
    suspend fun addFund(f: FundEntity) = db.fundDao().insertFund(f)
    suspend fun updateFund(f: FundEntity) = db.fundDao().updateFund(f)
    suspend fun deleteFund(f: FundEntity) {
        db.fundDao().deleteTxnsForFund(f.id)
        db.fundDao().deleteFund(f)
    }
    suspend fun addFundTxn(t: FundTxnEntity) = db.fundDao().insertTxn(t)
    suspend fun deleteFundTxn(t: FundTxnEntity) = db.fundDao().deleteTxn(t)

    // --- План-факт ---
    suspend fun upsertPlan(p: BudgetPlanEntity) = db.budgetPlanDao().upsert(p)
    suspend fun deletePlan(category: String, year: Int, month: Int) =
        db.budgetPlanDao().delete(category, year, month)

    // --- Импорт/экспорт/демо ---
    suspend fun insertExpenses(list: List<ExpenseEntity>) = db.expenseDao().insertAll(list)
    suspend fun insertIncomes(list: List<IncomeEntity>) = db.incomeDao().insertAll(list)

    suspend fun snapshotExpenses(): List<ExpenseEntity> = db.expenseDao().getAll()
    suspend fun snapshotIncomes(): List<IncomeEntity> = db.incomeDao().getAll()
    suspend fun snapshotCategories(): List<CategoryEntity> = db.categoryDao().getAll()
    suspend fun snapshotFunds(): List<FundEntity> = db.fundDao().getFunds()
    suspend fun snapshotFundTxns(): List<FundTxnEntity> = db.fundDao().getTxns()
    suspend fun snapshotPlans(): List<BudgetPlanEntity> = db.budgetPlanDao().getAll()

    suspend fun ensureSeeded() {
        if (db.categoryDao().count() == 0) {
            db.categoryDao().insertAll(SeedData.categories())
        }
    }

    suspend fun replaceCategories(list: List<CategoryEntity>) {
        db.categoryDao().clear()
        db.categoryDao().insertAll(list)
    }

    suspend fun seedDemo() {
        val seed = SeedData.demo()
        db.expenseDao().insertAll(seed.first)
        db.incomeDao().insertAll(seed.second)
    }

    suspend fun removeDemo() {
        db.expenseDao().deleteDemo()
        db.incomeDao().deleteDemo()
    }

    suspend fun clearAllData() {
        db.expenseDao().clear()
        db.incomeDao().clear()
    }

    /** Полное восстановление из бэкапа: очищает все таблицы и пишет переданные данные. */
    suspend fun restore(
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        categories: List<CategoryEntity>,
        funds: List<FundEntity>,
        fundTxns: List<FundTxnEntity>,
        plans: List<BudgetPlanEntity>,
    ) {
        db.expenseDao().clear()
        db.incomeDao().clear()
        db.fundDao().clearTxns()
        db.fundDao().clearFunds()
        db.budgetPlanDao().clear()
        db.categoryDao().clear()
        db.categoryDao().insertAll(categories)
        db.expenseDao().insertAll(expenses)
        db.incomeDao().insertAll(incomes)
        db.fundDao().insertFunds(funds)
        db.fundDao().insertTxns(fundTxns)
        db.budgetPlanDao().insertAll(plans)
    }
}

// --- Маппинг сущностей в чистые core-модели для расчётов ---

fun ExpenseEntity.toRow(): ExpenseRow =
    ExpenseRow(epochDay, year, month, category, subcategory, amountKop)

fun IncomeEntity.toRow(): IncomeRow =
    IncomeRow(epochDay, year, month, source, category, amountKop, toBudgetKop, toSavingsKop)

fun List<ExpenseEntity>.toRows(): List<ExpenseRow> = map { it.toRow() }

@JvmName("incomeToRows")
fun List<IncomeEntity>.toRows(): List<IncomeRow> = map { it.toRow() }
