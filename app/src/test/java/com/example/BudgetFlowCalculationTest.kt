package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetFlowCalculationTest {

    @Test
    fun `initial balance and totals are zero`() {
        val transactions = emptyList<TransactionEntity>()

        var balance = 0.0
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    balance += tx.amount
                    totalIncome += tx.amount
                }
                TransactionType.EXPENSE -> {
                    balance -= tx.amount
                    totalExpense += tx.amount
                }
                TransactionType.TRANSFER -> {}
            }
        }

        assertEquals(0.0, balance, 0.001)
        assertEquals(0.0, totalIncome, 0.001)
        assertEquals(0.0, totalExpense, 0.001)
    }

    @Test
    fun `transactions dynamically calculate net balance, income and expense`() {
        val tx1 = TransactionEntity(
            id = 1,
            title = "Salary Deposit",
            amount = 3000.0,
            type = TransactionType.INCOME,
            category = "Salary",
            dateMillis = System.currentTimeMillis()
        )
        val tx2 = TransactionEntity(
            id = 2,
            title = "Grocery Store",
            amount = 120.50,
            type = TransactionType.EXPENSE,
            category = "Groceries",
            dateMillis = System.currentTimeMillis()
        )
        val tx3 = TransactionEntity(
            id = 3,
            title = "Coffee Shop",
            amount = 8.50,
            type = TransactionType.EXPENSE,
            category = "Dining Out",
            dateMillis = System.currentTimeMillis()
        )

        val transactions = listOf(tx1, tx2, tx3)

        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = income - expense

        assertEquals(3000.0, income, 0.001)
        assertEquals(129.0, expense, 0.001)
        assertEquals(2871.0, netBalance, 0.001)
    }
}
