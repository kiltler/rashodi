package com.rashodi.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val TYPE_EXPENSE = "EXPENSE"
const val TYPE_INCOME = "INCOME"

@Entity(
    tableName = "expenses",
    indices = [Index("year", "month"), Index("category")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val year: Int,
    val month: Int,
    val category: String,
    val subcategory: String = "",
    val description: String = "",
    val amountKop: Long,
    val comment: String = "",
    val isDemo: Boolean = false,
)

@Entity(
    tableName = "incomes",
    indices = [Index("year", "month")],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val year: Int,
    val month: Int,
    val source: String = "",
    val category: String,
    val description: String = "",
    val amountKop: Long,
    val toBudgetKop: Long = 0,
    val toSavingsKop: Long = 0,
    val comment: String = "",
    val isDemo: Boolean = false,
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // TYPE_EXPENSE | TYPE_INCOME
    val subcategories: List<String> = emptyList(),
    val colorArgb: Int,
    val isDiscretionary: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetKop: Long,
    val colorArgb: Int,
    val note: String = "",
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "fund_txns",
    indices = [Index("fundId")],
)
data class FundTxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fundId: Long,
    val epochDay: Long,
    val amountKop: Long, // + пополнение, − списание
    val note: String = "",
)

@Entity(
    tableName = "budget_plans",
    indices = [Index(value = ["category", "year", "month"], unique = true)],
)
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val year: Int,
    val month: Int,
    val plannedKop: Long,
)
