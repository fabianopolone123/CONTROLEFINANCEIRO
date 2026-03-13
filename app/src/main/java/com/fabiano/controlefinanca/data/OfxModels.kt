package com.fabiano.controlefinanca.data

data class OfxParsedTransaction(
    val transactionDateMillis: Long,
    val amountSigned: Double,
    val counterpartyLabel: String,
    val fitId: String?
)

data class OfxImportPreview(
    val transactions: List<OfxParsedTransaction>,
    val previousBalance: Double?
)
