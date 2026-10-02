package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CategoryBudget
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Syncing : BackupStatus()
    data class Success(val message: String, val timestampMillis: Long) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

class FirestoreBackupManager(private val context: Context) {

    private val auth = Firebase.auth
    private val db: FirebaseFirestore by lazy {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _lastBackupTimeMillis = MutableStateFlow<Long?>(null)
    val lastBackupTimeMillis: StateFlow<Long?> = _lastBackupTimeMillis.asStateFlow()

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Automatic background backup of a single transaction
     */
    suspend fun backupTransactionInBackground(transaction: TransactionEntity) {
        val userId = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            val path = "users/$userId/transactions/${transaction.id}"
            try {
                val payload = hashMapOf<String, Any?>(
                    "id" to transaction.id.toString(),
                    "userId" to userId,
                    "title" to transaction.title,
                    "amount" to transaction.amount,
                    "type" to transaction.type.name,
                    "category" to transaction.category,
                    "dateMillis" to transaction.dateMillis,
                    "note" to transaction.note,
                    "paymentMethod" to transaction.paymentMethod,
                    "receiptUri" to transaction.receiptUri,
                    "receiptNote" to transaction.receiptNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document(transaction.id.toString())
                    .set(payload, SetOptions.merge())
                    .await()

                updateBackupMeta(userId, increment = 0)
                Log.d("FirestoreBackup", "Auto-backed up transaction ${transaction.id} to cloud")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
            }
        }
    }

    /**
     * Automatic background delete of a single transaction
     */
    suspend fun deleteTransactionFromCloud(transactionId: Long) {
        val userId = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            val path = "users/$userId/transactions/$transactionId"
            try {
                db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document(transactionId.toString())
                    .delete()
                    .await()

                Log.d("FirestoreBackup", "Deleted transaction $transactionId from cloud")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, path)
            }
        }
    }

    /**
     * Automatic background backup of category budget
     */
    suspend fun backupBudgetInBackground(budget: CategoryBudget) {
        val userId = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            val path = "users/$userId/budgets/${budget.category}"
            try {
                val payload = hashMapOf<String, Any?>(
                    "category" to budget.category,
                    "monthlyLimit" to budget.monthlyLimit,
                    "userId" to userId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                db.collection("users")
                    .document(userId)
                    .collection("budgets")
                    .document(budget.category)
                    .set(payload, SetOptions.merge())
                    .await()

                Log.d("FirestoreBackup", "Backed up budget for ${budget.category}")
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
            }
        }
    }

    /**
     * Full manual backup of all local transactions and budgets
     */
    suspend fun performFullBackup(
        transactions: List<TransactionEntity>,
        budgets: List<CategoryBudget>
    ): Result<Int> {
        val userId = getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Please connect your Google Account first"))

        _backupStatus.value = BackupStatus.Syncing

        return withContext(Dispatchers.IO) {
            try {
                var backedUpCount = 0

                // Backup transactions in batches
                val userDoc = db.collection("users").document(userId)
                val txCol = userDoc.collection("transactions")
                val budgetCol = userDoc.collection("budgets")

                for (tx in transactions) {
                    val payload = hashMapOf<String, Any?>(
                        "id" to tx.id.toString(),
                        "userId" to userId,
                        "title" to tx.title,
                        "amount" to tx.amount,
                        "type" to tx.type.name,
                        "category" to tx.category,
                        "dateMillis" to tx.dateMillis,
                        "note" to tx.note,
                        "paymentMethod" to tx.paymentMethod,
                        "receiptUri" to tx.receiptUri,
                        "receiptNote" to tx.receiptNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    txCol.document(tx.id.toString()).set(payload, SetOptions.merge()).await()
                    backedUpCount++
                }

                for (b in budgets) {
                    val bPayload = hashMapOf<String, Any?>(
                        "category" to b.category,
                        "monthlyLimit" to b.monthlyLimit,
                        "userId" to userId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    budgetCol.document(b.category).set(bPayload, SetOptions.merge()).await()
                }

                // Update root user metadata document
                val nowMillis = System.currentTimeMillis()
                val metaPayload = hashMapOf<String, Any?>(
                    "userId" to userId,
                    "lastBackupMillis" to nowMillis,
                    "transactionCount" to transactions.size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.set(metaPayload, SetOptions.merge()).await()

                _lastBackupTimeMillis.value = nowMillis
                _backupStatus.value = BackupStatus.Success("Backed up $backedUpCount transactions successfully", nowMillis)
                Result.success(backedUpCount)
            } catch (e: Exception) {
                val path = "users/$userId"
                val errJson = handleFirestoreError(e, OperationType.WRITE, path)
                _backupStatus.value = BackupStatus.Error(e.localizedMessage ?: "Backup failed")
                Result.failure(e)
            }
        }
    }

    /**
     * Restore transactions and budgets from cloud Firestore
     */
    suspend fun restoreFromCloud(): Result<Pair<List<TransactionEntity>, List<CategoryBudget>>> {
        val userId = getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Please connect your Google Account first"))

        _backupStatus.value = BackupStatus.Syncing

        return withContext(Dispatchers.IO) {
            val path = "users/$userId"
            try {
                val userDoc = db.collection("users").document(userId)

                // Fetch transactions
                val txSnapshot = userDoc.collection("transactions").get().await()
                val restoredTxs = txSnapshot.documents.mapNotNull { doc ->
                    try {
                        val idLong = doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: 0L
                        val title = doc.getString("title") ?: "Transaction"
                        val amount = doc.getDouble("amount") ?: (doc.get("amount") as? Number)?.toDouble() ?: 0.0
                        val typeStr = doc.getString("type") ?: "EXPENSE"
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val category = doc.getString("category") ?: "Other Expense"
                        val dateMillis = (doc.get("dateMillis") as? Number)?.toLong() ?: System.currentTimeMillis()
                        val note = doc.getString("note")
                        val paymentMethod = doc.getString("paymentMethod") ?: "Cash"
                        val receiptUri = doc.getString("receiptUri")
                        val receiptNote = doc.getString("receiptNote")

                        TransactionEntity(
                            id = idLong,
                            title = title,
                            amount = amount,
                            type = type,
                            category = category,
                            dateMillis = dateMillis,
                            note = note,
                            paymentMethod = paymentMethod,
                            receiptUri = receiptUri,
                            receiptNote = receiptNote
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Fetch budgets
                val budgetSnapshot = userDoc.collection("budgets").get().await()
                val restoredBudgets = budgetSnapshot.documents.mapNotNull { doc ->
                    try {
                        val category = doc.getString("category") ?: doc.id
                        val limit = doc.getDouble("monthlyLimit") ?: (doc.get("monthlyLimit") as? Number)?.toDouble() ?: 0.0
                        CategoryBudget(category = category, monthlyLimit = limit)
                    } catch (e: Exception) {
                        null
                    }
                }

                val nowMillis = System.currentTimeMillis()
                _lastBackupTimeMillis.value = nowMillis
                _backupStatus.value = BackupStatus.Success("Restored ${restoredTxs.size} transactions from cloud", nowMillis)

                Result.success(Pair(restoredTxs, restoredBudgets))
            } catch (e: Exception) {
                val errJson = handleFirestoreError(e, OperationType.GET, path)
                _backupStatus.value = BackupStatus.Error(e.localizedMessage ?: "Restore failed")
                Result.failure(e)
            }
        }
    }

    private suspend fun updateBackupMeta(userId: String, increment: Int) {
        try {
            val userDoc = db.collection("users").document(userId)
            val nowMillis = System.currentTimeMillis()
            _lastBackupTimeMillis.value = nowMillis
            userDoc.set(
                hashMapOf(
                    "userId" to userId,
                    "lastBackupMillis" to nowMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            // Non-critical background meta update
        }
    }
}
