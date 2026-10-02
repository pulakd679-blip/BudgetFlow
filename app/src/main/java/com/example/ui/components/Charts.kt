package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryRegistry
import com.example.data.model.DailyBalanceSummary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategorySpendData
import com.example.ui.viewmodel.MonthlyBarData
import java.util.Locale
import kotlin.math.max

/**
 * Monthly Income vs Expense Comparison Bar Chart
 */
@Composable
fun MonthlyTrendBarChart(
    bars: List<MonthlyBarData>,
    currentMonthIndex: Int,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    val maxAmount = max(
        bars.maxOfOrNull { max(it.income, it.expense) } ?: 1000.0,
        500.0
    )

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700))
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(currentMonthIndex) {
        // Scroll near current month
        val scrollTarget = (currentMonthIndex * 140).coerceAtLeast(0)
        scrollState.animateScrollTo(scrollTarget)
    }

    Column(modifier = modifier) {
        // Chart Header Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(IncomeGreen, CircleShape)
            )
            Text(
                text = " Income",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ExpenseRed, CircleShape)
            )
            Text(
                text = " Expense",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Horizontal scrollable bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .horizontalScroll(scrollState)
        ) {
            val barGroupWidth = 56.dp
            val singleBarWidth = 14.dp
            val chartHeight = 140.dp

            Row(
                modifier = Modifier
                    .height(180.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { item ->
                    val isCurrent = item.monthIndex == currentMonthIndex
                    val incomeRatio = ((item.income / maxAmount) * animProgress.value).toFloat().coerceIn(0f, 1f)
                    val expenseRatio = ((item.expense / maxAmount) * animProgress.value).toFloat().coerceIn(0f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(barGroupWidth)
                    ) {
                        // Bars Container
                        Box(
                            modifier = Modifier
                                .height(chartHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // Income Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(chartHeight * incomeRatio.coerceAtLeast(0.04f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(IncomeGreen, Color(0xFF059669))
                                            ),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                // Expense Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(chartHeight * expenseRatio.coerceAtLeast(0.04f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(ExpenseRed, Color(0xFFDC2626))
                                            ),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month label badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Text(
                                text = item.monthName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Daily Net Balance Progression Line Chart
 */
@Composable
fun DailyBalanceLineChart(
    dailySummaries: List<DailyBalanceSummary>,
    modifier: Modifier = Modifier
) {
    if (dailySummaries.isEmpty()) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dailySummaries) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    val balances = dailySummaries.map { it.cumulativeBalance }
    val minVal = balances.minOrNull() ?: 0.0
    val maxVal = balances.maxOrNull() ?: 100.0
    val range = if (maxVal - minVal > 0.01) (maxVal - minVal) else 100.0

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(modifier = modifier) {
        // High & Low markers
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Peak: \$${String.format(Locale.US, "%,.0f", maxVal)}",
                style = MaterialTheme.typography.labelSmall,
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Floor: \$${String.format(Locale.US, "%,.0f", minVal)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 16f
            val paddingTop = 16f
            val usableHeight = height - paddingTop - paddingBottom

            // Draw horizontal dashed grid lines
            for (i in 0..3) {
                val y = paddingTop + (usableHeight / 3f) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (balances.size < 2) return@Canvas

            val stepX = width / (balances.size - 1)
            val points = balances.mapIndexed { index, balance ->
                val normY = ((balance - minVal) / range).toFloat() * animProgress.value
                val y = height - paddingBottom - (normY * usableHeight)
                Offset(index * stepX, y)
            }

            // Path for gradient fill
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f),
                        primaryColor.copy(alpha = 0.01f)
                    )
                )
            )

            // Path for smooth line
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw current/latest day pulse point
            val lastPoint = points.last()
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = lastPoint
            )
            drawCircle(
                color = primaryColor,
                radius = 4.5.dp.toPx(),
                center = lastPoint
            )
        }
    }
}

/**
 * Category Breakdown Donut / Segment Chart
 */
@Composable
fun CategoryDonutChart(
    categories: List<CategorySpendData>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier.height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded this month yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalSpend = categories.sumOf { it.amount }
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f
                categories.forEach { item ->
                    val sweep = (item.percentage * 360f) * animProgress.value
                    val catDef = CategoryRegistry.getCategory(item.category)

                    drawArc(
                        color = catDef.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\$${String.format(Locale.US, "%,.0f", totalSpend)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Top Category List Legend
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(5).forEach { item ->
                val catDef = CategoryRegistry.getCategory(item.category)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(catDef.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.0f", item.percentage * 100)}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
