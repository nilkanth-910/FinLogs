package com.example.finlogs

data class SaleItemModel(
    val productId: String = "",
    val productName: String = "",
    val salePrice: Double = 0.0,
    val qty: Int = 0,
    val amount: Double = 0.0
)