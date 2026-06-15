package com.rashodi.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert
    suspend fun insert(e: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ExpenseEntity>)

    @Update
    suspend fun update(e: ExpenseEntity)

    @Delete
    suspend fun delete(e: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE isDemo = 1")
    suspend fun deleteDemo()

    @Query("DELETE FROM expenses")
    suspend fun clear()
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes")
    suspend fun getAll(): List<IncomeEntity>

    @Insert
    suspend fun insert(e: IncomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<IncomeEntity>)

    @Update
    suspend fun update(e: IncomeEntity)

    @Delete
    suspend fun delete(e: IncomeEntity)

    @Query("DELETE FROM incomes WHERE isDemo = 1")
    suspend fun deleteDemo()

    @Query("DELETE FROM incomes")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(c: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CategoryEntity>)

    @Update
    suspend fun update(c: CategoryEntity)

    @Delete
    suspend fun delete(c: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface FundDao {
    @Query("SELECT * FROM funds ORDER BY sortOrder, name")
    fun observeFunds(): Flow<List<FundEntity>>

    @Query("SELECT * FROM fund_txns ORDER BY epochDay DESC, id DESC")
    fun observeTxns(): Flow<List<FundTxnEntity>>

    @Query("SELECT * FROM funds")
    suspend fun getFunds(): List<FundEntity>

    @Query("SELECT * FROM fund_txns")
    suspend fun getTxns(): List<FundTxnEntity>

    @Insert
    suspend fun insertFund(f: FundEntity): Long

    @Update
    suspend fun updateFund(f: FundEntity)

    @Delete
    suspend fun deleteFund(f: FundEntity)

    @Query("DELETE FROM fund_txns WHERE fundId = :fundId")
    suspend fun deleteTxnsForFund(fundId: Long)

    @Insert
    suspend fun insertTxn(t: FundTxnEntity): Long

    @Delete
    suspend fun deleteTxn(t: FundTxnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFunds(list: List<FundEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxns(list: List<FundTxnEntity>)

    @Query("DELETE FROM funds")
    suspend fun clearFunds()

    @Query("DELETE FROM fund_txns")
    suspend fun clearTxns()
}

@Dao
interface BudgetPlanDao {
    @Query("SELECT * FROM budget_plans")
    fun observeAll(): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans")
    suspend fun getAll(): List<BudgetPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: BudgetPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<BudgetPlanEntity>)

    @Query("DELETE FROM budget_plans WHERE category = :category AND year = :year AND month = :month")
    suspend fun delete(category: String, year: Int, month: Int)

    @Query("DELETE FROM budget_plans")
    suspend fun clear()
}
