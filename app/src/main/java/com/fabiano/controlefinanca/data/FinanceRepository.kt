package com.fabiano.controlefinanca.data

import android.content.Context
import java.util.Calendar
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
                installmentTotal = normalizedInstallmentTotal,
                ofxFingerprint = null
            )
        )
    }

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    suspend fun updateTransactionCategory(id: Long, category: String) {
        val normalized = normalizeCategory(category) ?: return
        transactionDao.updateCategory(id, normalized)
    }

    suspend fun importOfxTransactions(
        ofxTransactions: List<OfxParsedTransaction>,
        includePreviousBalance: Boolean,
        previousBalance: Double?
    ): Int {
        if (ofxTransactions.isEmpty()) return 0

        val importedCandidates = ofxTransactions.map { item ->
            val fingerprint = buildOfxFingerprint(item)
            val type = if (item.amountSigned >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            val amount = abs(item.amountSigned)
            val counterpartyPrefix = if (type == TransactionType.EXPENSE) "Destinatario" else "Remetente"
            ImportCandidate(
                fingerprint = fingerprint,
                entity = TransactionEntity(
                    type = type,
                    amount = amount,
                    category = "Nao categorizado",
                    note = "$counterpartyPrefix: ${item.counterpartyLabel}",
                    dateMillis = System.currentTimeMillis(),
                    transactionDateMillis = item.transactionDateMillis,
                    recurrenceType = RecurrenceType.ONE_TIME,
                    installmentCurrent = 1,
                    installmentTotal = 1,
                    ofxFingerprint = fingerprint
                )
            )
        }

        val fingerprintPool = importedCandidates.map { it.fingerprint }.toMutableSet()
        val includePrevious = includePreviousBalance && previousBalance != null && abs(previousBalance) >= 0.01
        val previousBalanceValue = previousBalance ?: 0.0
        val previousDate = if (includePrevious) {
            Calendar.getInstance().apply {
                timeInMillis = ofxTransactions.minOf { it.transactionDateMillis }
                add(Calendar.DAY_OF_MONTH, -1)
            }.timeInMillis
        } else {
            null
        }
        val previousFingerprint = if (includePrevious && previousDate != null) {
            buildPreviousBalanceFingerprint(previousBalanceValue, previousDate)
        } else {
            null
        }
        previousFingerprint?.let { fingerprintPool.add(it) }

        val existingFingerprints = if (fingerprintPool.isNotEmpty()) {
            transactionDao.findExistingOfxFingerprints(fingerprintPool.toList()).filterNotNull().toSet()
        } else {
            emptySet()
        }

        categoryDao.insert(CategoryEntity(type = TransactionType.INCOME, name = "Nao categorizado"))
        categoryDao.insert(CategoryEntity(type = TransactionType.EXPENSE, name = "Nao categorizado"))

        val imported = importedCandidates
            .filterNot { existingFingerprints.contains(it.fingerprint) }
            .map { it.entity }
            .toMutableList()

        if (includePrevious && previousDate != null && previousFingerprint != null) {
            val balanceType = if (previousBalanceValue >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            categoryDao.insert(
                CategoryEntity(
                    type = balanceType,
                    name = "Saldo anterior"
                )
            )
            if (!existingFingerprints.contains(previousFingerprint)) {
                imported.add(
                    0,
                    TransactionEntity(
                        type = balanceType,
                        amount = abs(previousBalanceValue),
                        category = "Saldo anterior",
                        note = "Saldo anterior importado via OFX",
                        dateMillis = System.currentTimeMillis(),
                        transactionDateMillis = previousDate,
                        recurrenceType = RecurrenceType.ONE_TIME,
                        installmentCurrent = 1,
                        installmentTotal = 1,
                        ofxFingerprint = previousFingerprint
                    )
                )
            }
        }

        if (imported.isEmpty()) return 0
        transactionDao.insertAll(imported)
        return imported.size
    }

    private fun normalizeCategory(value: String): String? {
        val cleaned = value.trim().replace(Regex("\\s+"), " ")
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun defaultCategory(type: TransactionType): String {
        return if (type == TransactionType.INCOME) "Receitas" else "Outros"
    }

    private fun buildOfxFingerprint(item: OfxParsedTransaction): String {
        val fitId = item.fitId?.trim()?.lowercase()
        if (!fitId.isNullOrBlank()) {
            return "fitid|$fitId"
        }
        val baseName = item.counterpartyLabel.trim().lowercase()
        return "fallback|${item.transactionDateMillis}|${String.format(Locale.US, "%.2f", item.amountSigned)}|$baseName"
    }

    private fun buildPreviousBalanceFingerprint(previousBalance: Double, previousDate: Long): String {
        return "saldo|${String.format(Locale.US, "%.2f", previousBalance)}|$previousDate"
    }

    private data class ImportCandidate(
        val fingerprint: String,
        val entity: TransactionEntity
    )
}
