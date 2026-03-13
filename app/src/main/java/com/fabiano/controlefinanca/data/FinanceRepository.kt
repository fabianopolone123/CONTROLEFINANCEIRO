package com.fabiano.controlefinanca.data

import android.content.Context
import java.util.Calendar
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

    suspend fun importOfxTransactions(
        ofxTransactions: List<OfxParsedTransaction>,
        includePreviousBalance: Boolean,
        previousBalance: Double?
    ): Int {
        if (ofxTransactions.isEmpty()) return 0

        categoryDao.insert(CategoryEntity(type = TransactionType.INCOME, name = "Nao categorizado"))
        categoryDao.insert(CategoryEntity(type = TransactionType.EXPENSE, name = "Nao categorizado"))

        val imported = ofxTransactions.map { item ->
            val type = if (item.amountSigned >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            val amount = abs(item.amountSigned)
            val counterpartyPrefix = if (type == TransactionType.EXPENSE) "Destinatario" else "Remetente"
            TransactionEntity(
                type = type,
                amount = amount,
                category = "Nao categorizado",
                note = "$counterpartyPrefix: ${item.counterpartyLabel}",
                dateMillis = System.currentTimeMillis(),
                transactionDateMillis = item.transactionDateMillis,
                recurrenceType = RecurrenceType.ONE_TIME,
                installmentCurrent = 1,
                installmentTotal = 1
            )
        }.toMutableList()

        if (includePreviousBalance && previousBalance != null && abs(previousBalance) >= 0.01) {
            val earliestDate = ofxTransactions.minOf { it.transactionDateMillis }
            val previousDate = Calendar.getInstance().apply {
                timeInMillis = earliestDate
                add(Calendar.DAY_OF_MONTH, -1)
            }.timeInMillis
            val balanceType = if (previousBalance >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            categoryDao.insert(
                CategoryEntity(
                    type = balanceType,
                    name = "Saldo anterior"
                )
            )
            imported.add(
                0,
                TransactionEntity(
                    type = balanceType,
                    amount = abs(previousBalance),
                    category = "Saldo anterior",
                    note = "Saldo anterior importado via OFX",
                    dateMillis = System.currentTimeMillis(),
                    transactionDateMillis = previousDate,
                    recurrenceType = RecurrenceType.ONE_TIME,
                    installmentCurrent = 1,
                    installmentTotal = 1
                )
            )
        }

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
}
