package com.example.data.model

data class ReceiptLineItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val quantity: Int = 1
) {
    val total: Double get() = price * quantity
}
