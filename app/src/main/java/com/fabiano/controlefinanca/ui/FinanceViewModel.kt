package com.fabiano.controlefinanca.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabiano.controlefinanca.data.CategoryTotalRow
import com.fabiano.controlefinanca.data.FinanceRepository
import com.fabiano.controlefinanca.data.MonthlyNetRow
import com.fabiano.controlefinanca.data.OfxParsedTransaction
import com.fabiano.controlefinanca.data.RecurrenceType
import com.fabiano.controlefinanca.data.TransactionEntity
import com.fabiano.controlefinanca.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FinanceUiState(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val transactions: List<TransactionEntity> = emptyList(),
    val expenseByCategory: List<CategoryTotalRow> = emptyList(),
    val monthlyNet: List<MonthlyNetRow> = emptyList(),
    val expenseCategories: List<String> = emptyList(),
    val incomeCategories: List<String> = emptyList()
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(application)
    private val defaultExpenseCategories = listOf(
        "Alimentacao",
        "Moradia",
        "Transporte",
        "Saude",
        "Lazer",
        "Educacao",
        "Outros"
    )
    private val defaultIncomeCategories = listOf(
        "Salario",
        "Freelance",
        "Rendimentos",
        "Vendas",
        "Outros"
    )
    private val expenseCategoriesFlow = repository.observeCategoryNames(TransactionType.EXPENSE)
    private val incomeCategoriesFlow = repository.observeCategoryNames(TransactionType.INCOME)

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.observeTransactions(),
        repository.observeSummary(),
        repository.observeExpenseByCategory(),
        repository.observeMonthlyNet(),
        expenseCategoriesFlow
    ) { transactions, summary, byCategory, monthly, expenseCategories ->
        UiStateBase(
            transactions = transactions,
            summaryIncome = summary.income ?: 0.0,
            summaryExpense = summary.expense ?: 0.0,
            byCategory = byCategory,
            monthly = monthly,
            expenseCategories = mergeCategories(defaultExpenseCategories, expenseCategories)
        )
    }.combine(incomeCategoriesFlow) { base, incomeCategories ->
        FinanceUiState(
            income = base.summaryIncome,
            expense = base.summaryExpense,
            balance = base.summaryIncome - base.summaryExpense,
            transactions = base.transactions,
            expenseByCategory = base.byCategory,
            monthlyNet = base.monthly,
            expenseCategories = base.expenseCategories,
            incomeCategories = mergeCategories(defaultIncomeCategories, incomeCategories)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState()
    )

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        note: String,
        transactionDateMillis: Long,
        recurrenceType: RecurrenceType,
        installmentTotal: Int
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                type = type,
                amount = amount,
                category = category,
                note = note,
                transactionDateMillis = transactionDateMillis,
                recurrenceType = recurrenceType,
                installmentTotal = installmentTotal
            )
        }
    }

    fun addCategory(type: TransactionType, name: String) {
        viewModelScope.launch {
            repository.addCategory(type = type, name = name)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateTransactionCategory(id: Long, category: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(id, category)
        }
    }

    suspend fun importOfx(transactions: List<OfxParsedTransaction>): Result<Int> {
        return runCatching {
            withContext(Dispatchers.IO) {
                repository.importOfxTransactions(transactions)
            }
        }
    }

    private fun mergeCategories(defaults: List<String>, saved: List<String>): List<String> {
        return (defaults + saved)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    private data class UiStateBase(
        val transactions: List<TransactionEntity>,
        val summaryIncome: Double,
        val summaryExpense: Double,
        val byCategory: List<CategoryTotalRow>,
        val monthly: List<MonthlyNetRow>,
        val expenseCategories: List<String>
    )
}
