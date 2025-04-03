package com.example.finlogs

data class Product(
    val productId: String = "",
    val barcode: String = "",
    val productName: String = "",
    val hsn: String = "",
    val rate : Double = 0.0,
    val tax: Double = 0.0,
    val grossPrice: Double = rate*(1+tax/100),
    val salePrice: Double = 0.0,
    val mrp: Double = 0.0,
    var stock: Int = 0
)
