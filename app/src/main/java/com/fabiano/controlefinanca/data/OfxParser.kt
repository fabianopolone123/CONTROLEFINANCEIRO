package com.fabiano.controlefinanca.data

import java.util.Calendar

object OfxParser {
    fun parse(rawContent: String): List<OfxParsedTransaction> {
        val normalized = rawContent.replace("><", ">\n<")
        val lines = normalized
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val transactions = mutableListOf<OfxParsedTransaction>()
        var inTransaction = false
        val values = mutableMapOf<String, String>()

        lines.forEach { line ->
            if (line.startsWith("<STMTTRN>", ignoreCase = true)) {
                inTransaction = true
                values.clear()
                return@forEach
            }
            if (line.startsWith("</STMTTRN>", ignoreCase = true)) {
                parseTransaction(values)?.let { transactions += it }
                inTransaction = false
                return@forEach
            }
            if (!inTransaction) return@forEach

            parseTagLine(line)?.let { (tag, value) ->
                values[tag] = value
            }
        }

        if (inTransaction && values.isNotEmpty()) {
            parseTransaction(values)?.let { transactions += it }
        }

        return transactions.sortedBy { it.transactionDateMillis }
    }

    private fun parseTransaction(values: Map<String, String>): OfxParsedTransaction? {
        val amountSigned = values["TRNAMT"]?.replace(",", ".")?.toDoubleOrNull() ?: return null
        val dateMillis = parseOfxDate(values["DTPOSTED"] ?: values["DTUSER"] ?: return null) ?: return null
        val name = values["NAME"]
            .orEmpty()
            .ifBlank { values["MEMO"].orEmpty() }
            .ifBlank { "Sem nome" }
            .take(120)

        return OfxParsedTransaction(
            transactionDateMillis = dateMillis,
            amountSigned = amountSigned,
            counterpartyLabel = name
        )
    }

    private fun parseTagLine(line: String): Pair<String, String>? {
        val match = Regex("<([A-Za-z0-9_]+)>(.*)").find(line) ?: return null
        val tag = match.groupValues[1].uppercase()
        val value = match.groupValues[2].substringBefore("<").trim()
        if (value.isBlank()) return null
        return tag to value
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
