package com.fabiano.controlefinanca.data

import android.content.Context
import androidx.room.withTransaction
import java.util.Locale
import kotlin.math.abs

class FinanceRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()

    fun observeTransactions() = transactionDao.observeAll()

    fun observeSummary() = transactionDao.observeSummary()

    fun observeExpenseByCategory() = transactionDao.observeExpenseByCategory()

    fun observeMonthlyNet() = transactionDao.observeMonthlyNet()

    fun observeCategoryNames(type: TransactionType) = categoryDao.observeNamesByType(type)

    suspend fun addCategory(type: TransactionType, name: String) {
        val normalizedName = normalizeCategory(name) ?: return
        categoryDao.insert(
            CategoryEntity(
                type = type,
                name = normalizedName
            )
        )
    }

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        note: String,
        transactionDateMillis: Long,
        recurrenceType: RecurrenceType,
        installmentTotal: Int
    ) {
        val normalizedCategory = normalizeCategory(category) ?: defaultCategory(type)
        val normalizedInstallmentTotal = if (recurrenceType == RecurrenceType.INSTALLMENT) {
            installmentTotal.coerceAtLeast(2)
        } else {
            1
        }

        categoryDao.insert(
            CategoryEntity(
                type = type,
                name = normalizedCategory
            )
        )

        transactionDao.insert(
            TransactionEntity(
                type = type,
                amount = amount,
                category = normalizedCategory,
                note = note.trim(),
                dateMillis = System.currentTimeMillis(),
                transactionDateMillis = transactionDateMillis,
                recurrenceType = recurrenceType,
                installmentCurrent = 1,
                installmentTotal = normalizedInstallmentTotal
            )
        )
    }

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    suspend fun updateTransactionCategory(id: Long, category: String) {
        val normalized = normalizeCategory(category) ?: return
        transactionDao.updateCategory(id, normalized)
    }

    suspend fun importOfxTransactions(ofxTransactions: List<OfxParsedTransaction>): Int {
        if (ofxTransactions.isEmpty()) return 0

        val minDate = ofxTransactions.minOf { it.transactionDateMillis }
        val maxDate = ofxTransactions.maxOf { it.transactionDateMillis }

        return database.withTransaction {
            categoryDao.insert(CategoryEntity(type = TransactionType.INCOME, name = "Nao categorizado"))
            categoryDao.insert(CategoryEntity(type = TransactionType.EXPENSE, name = "Nao categorizado"))

            val existing = transactionDao
                .listImportSignaturesInRange(minDate, maxDate)
                .toHashSet()

            val candidates = ofxTransactions.map { source ->
                val type = if (source.amountSigned >= 0) TransactionType.INCOME else TransactionType.EXPENSE
                val amount = abs(source.amountSigned)
                val prefix = if (type == TransactionType.EXPENSE) "Destinatario" else "Remetente"
                val note = "$prefix: ${source.counterpartyLabel}"
                val signature = buildImportSignature(type, amount, source.transactionDateMillis, note)
                signature to TransactionEntity(
                    type = type,
                    amount = amount,
                    category = "Nao categorizado",
                    note = note,
                    dateMillis = System.currentTimeMillis(),
                    transactionDateMillis = source.transactionDateMillis,
                    recurrenceType = RecurrenceType.ONE_TIME,
                    installmentCurrent = 1,
                    installmentTotal = 1
                )
            }.distinctBy { it.first }

            val toInsert = candidates
                .filterNot { existing.contains(it.first) }
                .map { it.second }

            if (toInsert.isEmpty()) return@withTransaction 0
            transactionDao.insertAll(toInsert)
            toInsert.size
        }
    }

    private fun normalizeCategory(value: String): String? {
        val cleaned = value.trim().replace(Regex("\\s+"), " ")
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun defaultCategory(type: TransactionType): String {
        return if (type == TransactionType.INCOME) "Receitas" else "Outros"
    }

    private fun buildImportSignature(
        type: TransactionType,
        amount: Double,
        transactionDateMillis: Long,
        note: String
    ): String {
        val normalizedNote = note.trim().lowercase()
        val normalizedAmount = String.format(Locale.US, "%.2f", amount)
        return "${type.name}|$normalizedAmount|$transactionDateMillis|$normalizedNote"
    }
}
