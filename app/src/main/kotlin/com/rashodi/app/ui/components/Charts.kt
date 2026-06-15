package com.rashodi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Тонкая полоса-индикатор (для топ-категорий, план-факта, прогресса фондов). */
@Composable
fun TrackBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Int = 8,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** Кольцевая диаграмма. Центр можно заполнить через [center]. */
@Composable
fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    holeRatio: Float = 0.64f,
    center: @Composable () -> Unit = {},
) {
    val total = values.sum()
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * (1f - holeRatio) / 2f
            val diameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            if (total <= 0f) {
                drawArc(
                    color = emptyColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = stroke),
                )
                return@Canvas
            }
            var start = -90f
            values.forEachIndexed { i, v ->
                val sweep = v / total * 360f
                drawArc(
                    color = colors[i % colors.size],
                    startAngle = start,
                    sweepAngle = sweep - 1.2f, // зазор между секторами
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        center()
    }
}

data class BarGroup(val label: String, val values: List<Long>)

/** Сгруппированные вертикальные столбцы (например, Доход vs Расход по месяцам). */
@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val maxValue = groups.flatMap { it.values }.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (groups.isEmpty()) return@Canvas
            val groupWidth = size.width / groups.size
            val barCount = groups.first().values.size.coerceAtLeast(1)
            val barAreaWidth = groupWidth * 0.62f
            val barWidth = barAreaWidth / barCount
            val gap = barWidth * 0.18f
            val plotH = size.height
            groups.forEachIndexed { gi, group ->
                val groupStart = gi * groupWidth + (groupWidth - barAreaWidth) / 2f
                group.values.forEachIndexed { bi, value ->
                    val h = (value.toFloat() / maxValue.toFloat()) * (plotH - 6f)
                    val left = groupStart + bi * barWidth + gap / 2f
                    val w = barWidth - gap
                    drawRoundRect(
                        color = colors[bi % colors.size],
                        topLeft = Offset(left, plotH - h),
                        size = Size(w.coerceAtLeast(2f), h),
                        cornerRadius = CornerRadius(w / 3f, w / 3f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            groups.forEach { g ->
                Text(
                    g.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

data class LineSeries(val color: Color, val points: List<Long>)

/** Линейный график тренда с несколькими сериями. */
@Composable
fun LineChart(
    series: List<LineSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val maxValue = series.flatMap { it.points }.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val grid = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // базовая линия
            drawLine(
                color = grid,
                start = Offset(0f, size.height - 1f),
                end = Offset(size.width, size.height - 1f),
                strokeWidth = 1.5f,
            )
            val count = xLabels.size
            if (count == 0) return@Canvas
            val stepX = if (count > 1) size.width / (count - 1) else size.width
            series.forEach { s ->
                if (s.points.isEmpty()) return@forEach
                val path = Path()
                s.points.forEachIndexed { i, p ->
                    val x = if (count > 1) i * stepX else size.width / 2f
                    val y = size.height - (p.toFloat() / maxValue.toFloat()) * (size.height - 8f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, s.color, style = Stroke(width = 4f, cap = StrokeCap.Round))
                // точки
                s.points.forEachIndexed { i, p ->
                    val x = if (count > 1) i * stepX else size.width / 2f
                    val y = size.height - (p.toFloat() / maxValue.toFloat()) * (size.height - 8f)
                    drawCircle(s.color, radius = 4.5f, center = Offset(x, y))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            xLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color = color, size = 9)
        HGap(6)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
