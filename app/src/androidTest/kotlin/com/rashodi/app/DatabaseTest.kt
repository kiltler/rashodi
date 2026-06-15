package com.rashodi.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rashodi.app.data.db.AppDatabase
import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_query_expense() = runBlocking {
        db.expenseDao().insert(
            ExpenseEntity(epochDay = 20000, year = 2026, month = 5, category = "Продукты", amountKop = 123456),
        )
        val all = db.expenseDao().observeAll().first()
        assertEquals(1, all.size)
        assertEquals(123456L, all.first().amountKop)
    }

    @Test
    fun category_subcategories_roundtrip() = runBlocking {
        db.categoryDao().insert(
            CategoryEntity(
                name = "Транспорт", type = TYPE_EXPENSE,
                subcategories = listOf("Бензин", "Такси", "Метро"),
                colorArgb = 0xFF8FB4DE.toInt(), isDiscretionary = false,
            ),
        )
        val loaded = db.categoryDao().getAll().first()
        assertEquals(listOf("Бензин", "Такси", "Метро"), loaded.subcategories)
    }
}
