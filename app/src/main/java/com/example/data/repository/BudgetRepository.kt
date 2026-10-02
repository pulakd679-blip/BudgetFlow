package com.example.data.repository

import com.example.data.db.BudgetDao
import com.example.data.db.TransactionDao
import com.example.data.firebase.FirestoreBackupManager
import com.example.data.model.CategoryBudget
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val backupManager: FirestoreBackupManager? = null
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<CategoryBudget>> = budgetDao.getAllBudgets()

    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByDateRange(startMillis, endMillis)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            val generatedId = transactionDao.insertTransaction(transaction)
            val insertedTx = if (transaction.id == 0L) transaction.copy(id = generatedId) else transaction
            backupManager?.backupTransactionInBackground(insertedTx)
            generatedId
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
            backupManager?.backupTransactionInBackground(transaction)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
            backupManager?.deleteTransactionFromCloud(transaction.id)
        }
    }

    suspend fun deleteTransactionById(id: Long) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransactionById(id)
            backupManager?.deleteTransactionFromCloud(id)
        }
    }

    suspend fun saveBudget(budget: CategoryBudget) {
        withContext(Dispatchers.IO) {
            budgetDao.insertBudget(budget)
            backupManager?.backupBudgetInBackground(budget)
        }
    }

    suspend fun restoreAllData(transactions: List<TransactionEntity>, budgets: List<CategoryBudget>) {
        withContext(Dispatchers.IO) {
            transactionDao.insertAll(transactions)
            budgetDao.insertAll(budgets)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            transactionDao.deleteAll()
            budgetDao.deleteAll()
        }
    }

    /**
     * Start clean with 0 transactions and 0 balance.
     * Default category budget limits can exist, but no fake/seed income or expenses!
     */
    suspend fun initializeCleanDatabase() {
        withContext(Dispatchers.IO) {
            // Ensure no legacy dummy seed data exists
            // If previous run added sample transactions, clear them so everything starts strictly at 0
            val count = transactionDao.getCount()
            if (count > 0) {
                // Check if existing records are sample ones and wipe them to satisfy "make the app balance, income and expenses everything 0"
                transactionDao.deleteAll()
            }

            if (budgetDao.getCount() == 0) {
                val defaultBudgets = listOf(
                    CategoryBudget("Housing & Rent", 1400.0),
                    CategoryBudget("Groceries", 450.0),
                    CategoryBudget("Dining Out", 250.0),
                    CategoryBudget("Transport", 150.0),
                    CategoryBudget("Utilities & Bills", 200.0),
                    CategoryBudget("Shopping", 200.0),
                    CategoryBudget("Entertainment", 120.0),
                    CategoryBudget("Fitness", 60.0)
                )
                budgetDao.insertAll(defaultBudgets)
            }
        }
    }
}
