package com.rashodi.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.app.data.settings.AppSettings
import com.rashodi.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore,
    private val repo: FinanceRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDark(value: Boolean) = viewModelScope.launch { store.setDarkTheme(value) }
    fun setLeakShare(value: Float) = viewModelScope.launch { store.setLeakShare(value) }
    fun setLeakGrowth(value: Float) = viewModelScope.launch { store.setLeakGrowth(value) }

    fun setDemo(enabled: Boolean) = viewModelScope.launch {
        if (enabled) repo.seedDemo() else repo.removeDemo()
        store.setDemoEnabled(enabled)
    }
}
