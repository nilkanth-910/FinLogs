package com.example.finlogs

data class ExpenseModel(
    val expenseId: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val payee: String = ""
)