package com.rashodi.app

import android.app.Application
import com.rashodi.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RashodiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Засев справочника категориями при первом запуске.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.repository.ensureSeeded()
        }
    }
}
