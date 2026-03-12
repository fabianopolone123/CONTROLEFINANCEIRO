package com.fabiano.controlefinanca.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiano.controlefinanca.data.CategoryTotalRow
import com.fabiano.controlefinanca.data.MonthlyNetRow
import kotlin.math.abs
import kotlin.math.max

private val chartPalette = listOf(
    Color(0xFF00D1FF),
    Color(0xFFFF8A00),
    Color(0xFF6CFF63),
    Color(0xFFFF4FB3),
    Color(0xFF9B8CFF),
    Color(0xFFFFE03E)
)

@Composable
fun ExpensePieChart(
    values: List<CategoryTotalRow>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        Text(
            text = "Sem despesas para exibir no gráfico de categorias.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val total = values.sumOf { it.total ?: 0.0 }.coerceAtLeast(0.01)
    val filtered = values.filter { (it.total ?: 0.0) > 0 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val stroke = 36.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            var start = -90f
            filtered.forEachIndexed { index, item ->
                val value = (item.total ?: 0.0)
                val sweep = (value / total * 360f).toFloat()
                drawArc(
                    color = chartPalette[index % chartPalette.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filtered.forEachIndexed { index, item ->
                val value = item.total ?: 0.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(chartPalette[index % chartPalette.size], CircleShape)
                    )
                    Text(
                        text = "${item.category}: ${"%.0f".format((value / total) * 100)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    values: List<MonthlyNetRow>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        Text(
            text = "Sem histórico mensal para exibir no gráfico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxValue = max(
        1.0,
        values.maxOf { abs(it.net ?: 0.0) }
    )
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val barWidth = size.width / (values.size * 2f)
            val centerY = size.height * 0.52f
            drawLine(
                color = axisColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 2.dp.toPx()
            )

            values.forEachIndexed { index, item ->
                val value = item.net ?: 0.0
                val ratio = (abs(value) / maxValue).toFloat()
                val barHeight = ratio * (size.height * 0.42f)
                val x = (index * 2f + 0.8f) * barWidth
                val y = if (value >= 0) centerY - barHeight else centerY
                drawRect(
                    color = if (value >= 0) Color(0xFF43E97B) else Color(0xFFFF5C8A),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            values.forEach { item ->
                Text(
                    text = item.month.substring(5),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
