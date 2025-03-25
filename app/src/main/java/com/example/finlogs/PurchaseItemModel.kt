package com.example.finlogs

data class PurchaseItemModel(
    val productId: String = "",  // Store productId
    val itemName: String = "",   // Display name (UI only)
    val grossPrice: Double = 0.0,
    val qty: Int = 0,
    val amount: Double = 0.0
)

