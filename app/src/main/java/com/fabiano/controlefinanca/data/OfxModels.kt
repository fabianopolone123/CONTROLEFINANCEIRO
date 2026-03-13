package com.fabiano.controlefinanca.data

data class OfxParsedTransaction(
    val transactionDateMillis: Long,
    val amountSigned: Double,
    val counterpartyLabel: String
)
