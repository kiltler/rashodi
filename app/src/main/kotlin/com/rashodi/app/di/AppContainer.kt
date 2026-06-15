package com.rashodi.app.di

import android.content.Context
import com.rashodi.app.data.db.AppDatabase
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.app.data.settings.SettingsStore

/**
 * Простой ручной DI-контейнер. Для приложения такого размера это чище и надёжнее
 * генерируемого DI: меньше плагинов-процессоров и меньше точек отказа сборки.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    val repository = FinanceRepository(database)
    val settings = SettingsStore(context)
}
