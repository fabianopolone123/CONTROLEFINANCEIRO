package com.fabiano.controlefinanca.data

data class SummaryRow(
    val income: Double?,
    val expense: Double?
)

data class CategoryTotalRow(
    val category: String,
    val total: Double?
)

data class MonthlyNetRow(
    val month: String,
    val net: Double?
)
