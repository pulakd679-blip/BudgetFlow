package com.example.data.model

data class DailyBalanceSummary(
    val dateKey: String,
    val dayOfMonth: Int,
    val dayOfWeek: String,
    val fullDateString: String,
    val dateMillis: Long,
    val incomeTotal: Double,
    val expenseTotal: Double,
    val netDayChange: Double,
    val cumulativeBalance: Double,
    val transactions: List<TransactionEntity>
)
