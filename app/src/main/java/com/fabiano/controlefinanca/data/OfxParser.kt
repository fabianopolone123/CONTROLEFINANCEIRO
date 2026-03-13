package com.fabiano.controlefinanca.data

import java.util.Calendar
import kotlin.math.abs

object OfxParser {
    fun parse(rawContent: String): OfxImportPreview {
        val normalized = rawContent.replace("><", ">\n<")
        val lines = normalized
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val transactions = mutableListOf<OfxParsedTransaction>()
        var inTransaction = false
        val buffer = mutableMapOf<String, String>()
        var ledgerBalance: Double? = null

        lines.forEach { line ->
            if (line.startsWith("<STMTTRN>", ignoreCase = true)) {
                inTransaction = true
                buffer.clear()
                return@forEach
            }
            if (line.startsWith("</STMTTRN>", ignoreCase = true)) {
                parseTransaction(buffer)?.let { transactions += it }
                inTransaction = false
                return@forEach
            }

            val tag = parseTagLine(line) ?: return@forEach
            if (inTransaction) {
                buffer[tag.first] = tag.second
            } else if (tag.first.equals("BALAMT", ignoreCase = true)) {
                ledgerBalance = parseAmount(tag.second)
            }
        }

        if (inTransaction && buffer.isNotEmpty()) {
            parseTransaction(buffer)?.let { transactions += it }
        }

        val normalizedTransactions = transactions.sortedBy { it.transactionDateMillis }
        val netImported = normalizedTransactions.sumOf { it.amountSigned }
        val previousBalance = ledgerBalance?.let { it - netImported }?.takeIf { abs(it) >= 0.01 }

        return OfxImportPreview(
            transactions = normalizedTransactions,
            previousBalance = previousBalance
        )
    }

    private fun parseTransaction(values: Map<String, String>): OfxParsedTransaction? {
        val amountSigned = parseAmount(values["TRNAMT"] ?: return null) ?: return null
        val rawDate = values["DTPOSTED"] ?: values["DTUSER"] ?: return null
        val dateMillis = parseOfxDate(rawDate) ?: return null
        val name = values["NAME"].orEmpty().ifBlank { values["MEMO"].orEmpty() }.ifBlank { "Sem nome" }

        return OfxParsedTransaction(
            transactionDateMillis = dateMillis,
            amountSigned = amountSigned,
            counterpartyLabel = name.take(120)
        )
    }

    private fun parseTagLine(line: String): Pair<String, String>? {
        val match = Regex("<([A-Za-z0-9_]+)>(.*)").find(line) ?: return null
        val tag = match.groupValues[1].uppercase()
        val rawValue = match.groupValues[2]
        val value = rawValue.substringBefore("<").trim()
        if (value.isBlank()) return null
        return tag to value
    }

    private fun parseAmount(value: String): Double? {
        return value.replace(",", ".").toDoubleOrNull()
    }

    private fun parseOfxDate(raw: String): Long? {
        val digits = Regex("(\\d{8,14})").find(raw)?.groupValues?.get(1) ?: return null
        if (digits.length < 8) return null

        val year = digits.substring(0, 4).toIntOrNull() ?: return null
        val month = digits.substring(4, 6).toIntOrNull() ?: return null
        val day = digits.substring(6, 8).toIntOrNull() ?: return null
        val hour = digits.substring(8, 10).toIntOrNull() ?: 12
        val minute = digits.substring(10, 12).toIntOrNull() ?: 0
        val second = digits.substring(12, 14).toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
