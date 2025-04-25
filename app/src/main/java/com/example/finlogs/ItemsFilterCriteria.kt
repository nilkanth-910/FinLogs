package com.example.finlogs

data class ItemsFilterCriteria(
    val name: String,
    val minPrice: Double?,
    val maxPrice: Double?,
    val minStock: Int?,
    val maxStock: Int?,
    val minRate: Double?,
    val maxRate: Double?
)