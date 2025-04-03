package com.example.finlogs

data class SaleReturnModel(
    val saleReturnId: String = "",
    val date: String = "",
    val customerName: String = "",
    val returnInvoiceNo: String = "",
    val totalAmount: Double = 0.0,
    val saleReturnItems: List<SaleItemModel> = emptyList() // ✅ Fix: Provide default value
)
