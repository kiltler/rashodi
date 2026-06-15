package com.rashodi.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.settings, it.repository) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") } },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                SwitchRow(
                    title = "Тёмная тема",
                    subtitle = "По умолчанию включена",
                    checked = settings.darkTheme,
                    onChange = vm::setDark,
                )
            }

            AppCard {
                SectionHeader("Детектор утечек")
                Gap(8)
                SliderRow(
                    title = "Порог доли категории",
                    valueLabel = "${Math.round(settings.leakShareThreshold * 100)}%",
                    value = settings.leakShareThreshold,
                    range = 0.05f..0.50f,
                    onChange = vm::setLeakShare,
                )
                SliderRow(
                    title = "Порог роста к среднему за 3 мес.",
                    valueLabel = "+${Math.round(settings.leakGrowthThreshold * 100)}%",
                    value = settings.leakGrowthThreshold,
                    range = 0.10f..1.00f,
                    onChange = vm::setLeakGrowth,
                )
            }

            AppCard {
                SwitchRow(
                    title = "Демо-данные",
                    subtitle = "Заполнить примером для ознакомления. Помечаются как демо и удаляются при выключении.",
                    checked = settings.demoEnabled,
                    onChange = vm::setDemo,
                )
            }

            AppCard {
                SectionHeader("О приложении")
                Gap(8)
                Text(
                    "Все данные хранятся только на вашем устройстве. Приложение не имеет доступа в интернет и никуда не передаёт данные.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(8)
                Text(
                    "Это инструмент учёта и анализа, а не финансовый или инвестиционный совет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Gap(12)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
