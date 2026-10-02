package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@Composable
fun HeroBalanceCard(
    totalBalance: Double,
    monthIncome: Double,
    monthExpense: Double,
    todayIncome: Double,
    todayExpense: Double,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onOpenCalculatorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F766E),
                            Color(0xFF115E59),
                            Color(0xFF042F2E)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                // Header & Savings Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NET TOTAL BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF99F6E4)
                        )
                    }

                    val netSaved = monthIncome - monthExpense
                    val savingsRate = if (monthIncome > 0) ((netSaved / monthIncome) * 100).toInt() else 0
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33000000)
                    ) {
                        Text(
                            text = if (savingsRate >= 0) "Saved $savingsRate% this month" else "Deficit ${-savingsRate}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (savingsRate >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Balance Amount
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFCCFBF1),
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(bottom = 6.dp, end = 4.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%,.2f", totalBalance),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Today's Flow Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today Income Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x24FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0x3310B981), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Today's Income",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Today In",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCCFBF1)
                                )
                                Text(
                                    text = "+\$${String.format(Locale.US, "%.2f", todayIncome)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Today Expense Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x24FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0x33EF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Today's Expense",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Today Out",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCCFBF1)
                                )
                                Text(
                                    text = "-\$${String.format(Locale.US, "%.2f", todayExpense)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Action Bar inside Hero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Remove,
                        label = "Expense",
                        color = Color(0xFFF87171),
                        onClick = onAddExpenseClick,
                        modifier = Modifier.testTag("hero_add_expense_btn")
                    )
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        label = "Income",
                        color = Color(0xFF34D399),
                        onClick = onAddIncomeClick,
                        modifier = Modifier.testTag("hero_add_income_btn")
                    )
                    QuickActionButton(
                        icon = Icons.Default.DocumentScanner,
                        label = "Scan",
                        color = Color(0xFF38BDF8),
                        onClick = onScanReceiptClick,
                        modifier = Modifier.testTag("hero_scan_receipt_btn")
                    )
                    QuickActionButton(
                        icon = Icons.Default.Calculate,
                        label = "Calc",
                        color = Color(0xFFFBBF24),
                        onClick = onOpenCalculatorClick,
                        modifier = Modifier.testTag("hero_calculator_btn")
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0x33000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0)
        )
    }
}
