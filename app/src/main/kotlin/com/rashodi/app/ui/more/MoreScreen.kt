package com.rashodi.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rashodi.app.ui.components.AppCard
import com.rashodi.app.ui.components.Gap
import com.rashodi.app.ui.components.HGap
import com.rashodi.app.ui.nav.Routes

private data class MoreItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val entries = listOf(
        MoreItem(Routes.CATEGORIES, "Категории", "Справочник категорий и подкатегорий", Icons.Rounded.Category),
        MoreItem(Routes.PLAN_FACT, "План-факт", "Плановые и фактические расходы", Icons.Rounded.TableChart),
        MoreItem(Routes.FUNDS, "Фонды и цели", "Накопления и прогресс по целям", Icons.Rounded.Savings),
        MoreItem(Routes.CALCULATOR, "Калькулятор распределения", "Разбивка дохода по долям", Icons.Rounded.Calculate),
        MoreItem(Routes.IMPORT_EXPORT, "Импорт и экспорт", "CSV, JSON, выгрузка в Google Таблицы", Icons.Rounded.ImportExport),
        MoreItem(Routes.SETTINGS, "Настройки", "Тема, детектор утечек, демо-данные", Icons.Rounded.Settings),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Ещё", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Gap(4)
        }
        entries.forEach { entry ->
            item {
                AppCard(
                    modifier = Modifier.clickable { onNavigate(entry.route) },
                    padding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HGap(14)
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
