package com.example.finlogs

data class PurchaseModel(
    val purchaseId: String = "",
    val date: String = "",
    val supplierName: String = "",
    val invoiceNo: String = "",
    val totalAmount: Double = 0.0,
    val items: List<PurchaseItemModel> = emptyList() // Use corrected model
)
