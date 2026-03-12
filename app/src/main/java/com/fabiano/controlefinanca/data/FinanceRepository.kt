package com.fabiano.controlefinanca.data

import android.content.Context

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

    private fun normalizeCategory(value: String): String? {
        val cleaned = value.trim().replace(Regex("\\s+"), " ")
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun defaultCategory(type: TransactionType): String {
        return if (type == TransactionType.INCOME) "Receitas" else "Outros"
    }
}
