package com.example.finlogs

data class SaleModel(
    val saleId: String = "",
    val date: String = "",
    val customerName: String = "",
    val invoiceNo: String = "",
    val totalAmount: Double = 0.0,
    val saleItems: List<SaleItemModel> = emptyList() // ✅ Fix: Provide default value
)
