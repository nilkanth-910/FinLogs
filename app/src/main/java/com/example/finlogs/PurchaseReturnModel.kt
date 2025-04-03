package com.example.finlogs

data class PurchaseReturnModel(
    val purchaseReturnId: String = "",
    val date: String = "",
    val supplierName: String = "",
    val returninvoiceNo: String = "",
    val totalAmount: Double = 0.0,
    val purchaseReturnItems: List<PurchaseItemModel> = emptyList() // Use corrected model
)
