package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val dateMillis: Long,
    val note: String? = null,
    val paymentMethod: String = "Cash",
    val receiptUri: String? = null,
    val receiptNote: String? = null,
    val receiptItemsJson: String? = null
)
