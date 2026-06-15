package com.rashodi.app.io

import com.rashodi.app.data.db.BudgetPlanEntity
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.FundEntity
import com.rashodi.app.data.db.FundTxnEntity
import com.rashodi.app.data.db.IncomeEntity
import org.json.JSONArray
import org.json.JSONObject

/** Полный бэкап/восстановление в JSON. Все суммы — копейки (целые). */
object JsonBackup {

    const val VERSION = 1

    data class Backup(
        val expenses: List<ExpenseEntity>,
        val incomes: List<IncomeEntity>,
        val categories: List<CategoryEntity>,
        val funds: List<FundEntity>,
        val fundTxns: List<FundTxnEntity>,
        val plans: List<BudgetPlanEntity>,
    )

    fun export(b: Backup): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("app", "rashodi")

        root.put("expenses", JSONArray().apply {
            b.expenses.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("epochDay", e.epochDay); put("year", e.year); put("month", e.month)
                    put("category", e.category); put("subcategory", e.subcategory); put("description", e.description)
                    put("amountKop", e.amountKop); put("comment", e.comment); put("isDemo", e.isDemo)
                })
            }
        })
        root.put("incomes", JSONArray().apply {
            b.incomes.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("epochDay", e.epochDay); put("year", e.year); put("month", e.month)
                    put("source", e.source); put("category", e.category); put("description", e.description)
                    put("amountKop", e.amountKop); put("toBudgetKop", e.toBudgetKop); put("toSavingsKop", e.toSavingsKop)
                    put("comment", e.comment); put("isDemo", e.isDemo)
                })
            }
        })
        root.put("categories", JSONArray().apply {
            b.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("type", c.type)
                    put("subcategories", JSONArray(c.subcategories))
                    put("colorArgb", c.colorArgb); put("isDiscretionary", c.isDiscretionary); put("sortOrder", c.sortOrder)
                })
            }
        })
        root.put("funds", JSONArray().apply {
            b.funds.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id); put("name", f.name); put("targetKop", f.targetKop)
                    put("colorArgb", f.colorArgb); put("note", f.note); put("sortOrder", f.sortOrder)
                })
            }
        })
        root.put("fundTxns", JSONArray().apply {
            b.fundTxns.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("fundId", t.fundId); put("epochDay", t.epochDay)
                    put("amountKop", t.amountKop); put("note", t.note)
                })
            }
        })
        root.put("plans", JSONArray().apply {
            b.plans.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("category", p.category); put("year", p.year)
                    put("month", p.month); put("plannedKop", p.plannedKop)
                })
            }
        })
        return root.toString(2)
    }

    fun parse(text: String): Backup {
        val root = JSONObject(text)
        return Backup(
            expenses = root.optJSONArray("expenses").map { o ->
                ExpenseEntity(
                    id = o.optLong("id"), epochDay = o.optLong("epochDay"),
                    year = o.optInt("year"), month = o.optInt("month"),
                    category = o.optString("category"), subcategory = o.optString("subcategory"),
                    description = o.optString("description"), amountKop = o.optLong("amountKop"),
                    comment = o.optString("comment"), isDemo = o.optBoolean("isDemo"),
                )
            },
            incomes = root.optJSONArray("incomes").map { o ->
                IncomeEntity(
                    id = o.optLong("id"), epochDay = o.optLong("epochDay"),
                    year = o.optInt("year"), month = o.optInt("month"),
                    source = o.optString("source"), category = o.optString("category"),
                    description = o.optString("description"), amountKop = o.optLong("amountKop"),
                    toBudgetKop = o.optLong("toBudgetKop"), toSavingsKop = o.optLong("toSavingsKop"),
                    comment = o.optString("comment"), isDemo = o.optBoolean("isDemo"),
                )
            },
            categories = root.optJSONArray("categories").map { o ->
                val subsArr = o.optJSONArray("subcategories")
                val subs = ArrayList<String>()
                if (subsArr != null) for (i in 0 until subsArr.length()) subs.add(subsArr.optString(i))
                CategoryEntity(
                    id = o.optLong("id"), name = o.optString("name"), type = o.optString("type"),
                    subcategories = subs, colorArgb = o.optInt("colorArgb"),
                    isDiscretionary = o.optBoolean("isDiscretionary"), sortOrder = o.optInt("sortOrder"),
                )
            },
            funds = root.optJSONArray("funds").map { o ->
                FundEntity(
                    id = o.optLong("id"), name = o.optString("name"), targetKop = o.optLong("targetKop"),
                    colorArgb = o.optInt("colorArgb"), note = o.optString("note"), sortOrder = o.optInt("sortOrder"),
                )
            },
            fundTxns = root.optJSONArray("fundTxns").map { o ->
                FundTxnEntity(
                    id = o.optLong("id"), fundId = o.optLong("fundId"), epochDay = o.optLong("epochDay"),
                    amountKop = o.optLong("amountKop"), note = o.optString("note"),
                )
            },
            plans = root.optJSONArray("plans").map { o ->
                BudgetPlanEntity(
                    id = o.optLong("id"), category = o.optString("category"),
                    year = o.optInt("year"), month = o.optInt("month"), plannedKop = o.optLong("plannedKop"),
                )
            },
        )
    }

    private inline fun <T> JSONArray?.map(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            out.add(transform(o))
        }
        return out
    }
}
