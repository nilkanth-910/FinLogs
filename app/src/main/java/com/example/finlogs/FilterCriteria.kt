package com.example.finlogs

data class FilterCriteria(
    val name: String,
    val invoiceNo: String,
    val minPrice: Double?,
    val maxPrice: Double?,
    val startDate: String,
    val endDate: String
)