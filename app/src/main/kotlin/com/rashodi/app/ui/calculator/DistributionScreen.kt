package com.rashodi.app.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashodi.app.ui.appViewModel
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.components.MoneyText
import com.rashodi.app.ui.components.Pill
import com.rashodi.app.ui.components.SectionHeader
import com.rashodi.app.ui.theme.LocalAppColors
import com.rashodi.core.analytics.Distribution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionScreen(onBack: () -> Unit) {
    val vm = appViewModel { DistributionViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калькулятор распределения") },
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
            OutlinedTextField(
                value = state.income,
                onValueChange = vm::setIncome,
                label = { Text("Ежемесячный доход, ₽") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.preset(Distribution.DEFAULT) }, label = { Text("75 / 15 / 10") })
                AssistChip(onClick = { vm.preset(DistributionViewModel.RULE_50_30_20) }, label = { Text("50 / 30 / 20") })
            }

            AppCard {
                SectionHeader("Доли", subtitle = "сумма процентов должна быть 100")
                Gap(12)
                state.buckets.forEachIndexed { i, bucket ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = bucket.label,
                            onValueChange = { vm.setLabel(i, it) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                        )
                        HGap(8)
                        OutlinedTextField(
                            value = bucket.percent.toString(),
                            onValueChange = { vm.setPercent(i, it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(82.dp),
                            suffix = { Text("%") },
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
                Gap(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Сумма долей", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Pill(
                        text = "${state.percentSum}%",
                        container = (if (state.valid) colors.income else colors.expense).copy(alpha = 0.16f),
                        content = if (state.valid) colors.income else colors.expense,
                    )
                }
            }

            AppCard {
                SectionHeader("Разбивка")
                Gap(12)
                when {
                    !state.valid -> Text(
                        "Подгоните проценты так, чтобы сумма была равна 100%.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.allocations.isEmpty() -> Text(
                        "Введите доход, чтобы увидеть разбивку.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.allocations.forEach { a ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(a.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Text("${a.percent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HGap(12)
                                MoneyText(a.amountKop, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Gap(12)
        }
    }
}
