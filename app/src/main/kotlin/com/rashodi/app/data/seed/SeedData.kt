package com.rashodi.app.data.seed

import com.rashodi.app.data.db.CategoryEntity
import com.rashodi.app.data.db.ExpenseEntity
import com.rashodi.app.data.db.IncomeEntity
import com.rashodi.app.data.db.TYPE_EXPENSE
import com.rashodi.app.data.db.TYPE_INCOME
import java.time.LocalDate

/**
 * Засев справочника реальными категориями из листа «Справочники» шаблона (очищено)
 * и опциональные демо-данные (по умолчанию выключены, помечены isDemo).
 */
object SeedData {

    private data class Cat(
        val name: String,
        val subs: List<String>,
        val color: Int,
        val discretionary: Boolean = false,
    )

    private val EXPENSE_CATS = listOf(
        Cat("Транспорт", listOf("Бензин", "Такси", "Автобус", "Метро", "Электричка", "Парковка", "Разное"), 0xFF8FB4DE.toInt()),
        Cat("Продукты", listOf("Магнит", "Пятёрочка", "Чижик", "Перекрёсток", "Глобус", "Спар", "Фикс Прайс", "Ашан", "Красное и Белое", "Разное"), 0xFF5BC8B0.toInt()),
        Cat("Бытовое", listOf("Чистящие", "Моющие", "Гигиена", "Разное"), 0xFF6FB1C9.toInt()),
        Cat("Рестораны", listOf("Роллы", "Пиццерия", "Бургерная", "Шаурмичка", "Хинкальная", "Доставка еды", "Кафе", "Ресторан", "Разное"), 0xFFE0707E.toInt(), discretionary = true),
        Cat("Развлечения", listOf("Кальянная", "Шашлыки", "Концерт", "Кино", "Музей", "Стим", "Квиз", "Разное"), 0xFF9C8FD9.toInt(), discretionary = true),
        Cat("Здоровье_Спорт", listOf("Абонемент в спортзал", "Бассейн", "Пилатес", "Таблетки", "Мази/Крема"), 0xFF7FC8E0.toInt()),
        Cat("Подарки", listOf("Родственники", "Друзья", "Работа", "Разное"), 0xFFD98FB0.toInt()),
        Cat("Вредности", listOf("Алкоголь", "Кальян", "Разное"), 0xFFB07FD9.toInt(), discretionary = true),
        Cat("Ежемесячное", listOf("ВПН", "Сервер для ВПН", "Домашний интернет", "Сим-карта", "Подписки", "Коммуналка", "Разное"), 0xFFB0BAC9.toInt()),
        Cat("Ежегодное", listOf("Облачный диск", "Разное"), 0xFF8FA0B0.toInt()),
        Cat("Крупное_Разное", listOf("Техника", "ДНС", "Разное"), 0xFF6F8FC9.toInt()),
        Cat("Путешествия", listOf("Билеты", "Жильё", "Разное"), 0xFF5BC8C8.toInt()),
    )

    private val INCOME_CATS = listOf(
        Cat("Зарплата", emptyList(), 0xFF5BC8B0.toInt()),
        Cat("Аванс", emptyList(), 0xFF6FB1C9.toInt()),
        Cat("Подработка", emptyList(), 0xFF8FB4DE.toInt()),
        Cat("Маркетплейсы", emptyList(), 0xFF9C8FD9.toInt()),
        Cat("Возвраты/Кэшбэк", emptyList(), 0xFF7FC8E0.toInt()),
        Cat("Соцвыплаты", emptyList(), 0xFFB0BAC9.toInt()),
        Cat("Аренда", emptyList(), 0xFFD98FB0.toInt()),
        Cat("Прочее", emptyList(), 0xFF8FA0B0.toInt()),
    )

    fun categories(): List<CategoryEntity> {
        val out = ArrayList<CategoryEntity>()
        EXPENSE_CATS.forEachIndexed { i, c ->
            out.add(CategoryEntity(name = c.name, type = TYPE_EXPENSE, subcategories = c.subs, colorArgb = c.color, isDiscretionary = c.discretionary, sortOrder = i))
        }
        INCOME_CATS.forEachIndexed { i, c ->
            out.add(CategoryEntity(name = c.name, type = TYPE_INCOME, subcategories = c.subs, colorArgb = c.color, isDiscretionary = false, sortOrder = i))
        }
        return out
    }

    /** Демо-операции (помечены isDemo). Пара: (расходы, доходы). */
    fun demo(): Pair<List<ExpenseEntity>, List<IncomeEntity>> {
        val exp = ArrayList<ExpenseEntity>()
        val inc = ArrayList<IncomeEntity>()

        fun e(m: Int, d: Int, cat: String, sub: String, rub: Double, desc: String = sub) {
            val date = LocalDate.of(2026, m, d)
            exp.add(
                ExpenseEntity(
                    epochDay = date.toEpochDay(), year = 2026, month = m,
                    category = cat, subcategory = sub, description = desc,
                    amountKop = Math.round(rub * 100), isDemo = true,
                ),
            )
        }
        fun i(m: Int, d: Int, cat: String, rub: Double, savings: Double = 0.0) {
            val date = LocalDate.of(2026, m, d)
            inc.add(
                IncomeEntity(
                    epochDay = date.toEpochDay(), year = 2026, month = m,
                    source = "", category = cat, description = cat,
                    amountKop = Math.round(rub * 100),
                    toSavingsKop = Math.round(savings * 100), isDemo = true,
                ),
            )
        }

        for (m in 3..5) {
            i(m, 5, "Зарплата", 45000.0, savings = 5000.0)
            i(m, 20, "Аванс", 25000.0)
            e(m, 2, "Продукты", "Пятёрочка", 2280.0)
            e(m, 4, "Продукты", "Магнит", 1640.0)
            e(m, 6, "Транспорт", "Бензин", 2000.0)
            e(m, 8, "Рестораны", "Кафе", 1450.0)
            e(m, 12, "Ежемесячное", "Коммуналка", 4300.0)
            e(m, 14, "Развлечения", "Кино", 800.0)
            e(m, 18, "Продукты", "Чижик", 920.0)
            e(m, 22, "Здоровье_Спорт", "Абонемент в спортзал", 2500.0)
        }
        // в мае искусственно завышены «необязательные» — чтобы детектор утечек сработал
        i(6, 5, "Маркетплейсы", 12000.0)
        e(5, 25, "Рестораны", "Доставка еды", 3900.0)
        e(5, 27, "Вредности", "Кальян", 1800.0)
        e(5, 28, "Развлечения", "Концерт", 4500.0)

        return exp to inc
    }
}
