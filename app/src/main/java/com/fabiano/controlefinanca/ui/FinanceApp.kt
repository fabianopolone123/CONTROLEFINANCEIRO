package com.fabiano.controlefinanca.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabiano.controlefinanca.data.TransactionEntity
import com.fabiano.controlefinanca.data.TransactionType
import com.fabiano.controlefinanca.ui.components.ExpensePieChart
import com.fabiano.controlefinanca.ui.components.MonthlyBarChart

private enum class AppTab(val label: String) {
    DASHBOARD("Resumo"),
    LIST("Lancamentos"),
    ADD("Adicionar")
}

@Composable
fun FinanceApp(viewModel: FinanceViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }

    val appBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0A0A),
            Color(0xFF101728),
            Color(0xFF161616)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF131B2B)) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.DASHBOARD,
                        onClick = { currentTab = AppTab.DASHBOARD },
                        icon = { Icon(Icons.Rounded.PieChart, contentDescription = null) },
                        label = { Text(AppTab.DASHBOARD.label) }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.LIST,
                        onClick = { currentTab = AppTab.LIST },
                        icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null) },
                        label = { Text(AppTab.LIST.label) }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.ADD,
                        onClick = { currentTab = AppTab.ADD },
                        icon = { Icon(Icons.Rounded.AddCircle, contentDescription = null) },
                        label = { Text(AppTab.ADD.label) }
                    )
                }
            }
        ) { padding ->
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    modifier = Modifier.padding(padding)
                )

                AppTab.LIST -> TransactionsScreen(
                    transactions = state.transactions,
                    onDelete = viewModel::deleteTransaction,
                    modifier = Modifier.padding(padding)
                )

                AppTab.ADD -> AddTransactionScreen(
                    expenseCategories = state.expenseCategories,
                    incomeCategories = state.incomeCategories,
                    onAddCategory = viewModel::addCategory,
                    onSubmit = { type, amount, category, note ->
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            category = category,
                            note = note
                        )
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    state: FinanceUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Meu Controle Financeiro",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                title = "Receitas",
                value = state.income.toBrl(),
                color = Color(0xFF33D17A),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Despesas",
                value = state.expense.toBrl(),
                color = Color(0xFFFF5C8A),
                modifier = Modifier.weight(1f)
            )
        }

        SummaryCard(
            title = "Saldo Atual",
            value = state.balance.toBrl(),
            color = if (state.balance >= 0) Color(0xFF00D1FF) else Color(0xFFFFA500),
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A243A)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Despesas por categoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ExpensePieChart(values = state.expenseByCategory)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B34)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Saldo liquido mensal (6 meses)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                MonthlyBarChart(values = state.monthlyNet)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2420)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ultimos lancamentos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.transactions.isEmpty()) {
                    Text(
                        text = "Ainda nao ha lancamentos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.transactions.take(5).forEach { item ->
                        CompactTransactionRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf("TODOS") }

    val filtered = remember(transactions, filter) {
        when (filter) {
            "RECEITAS" -> transactions.filter { it.type == TransactionType.INCOME }
            "DESPESAS" -> transactions.filter { it.type == TransactionType.EXPENSE }
            else -> transactions
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Lancamentos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == "TODOS",
                onClick = { filter = "TODOS" },
                label = { Text("Todos") }
            )
            FilterChip(
                selected = filter == "RECEITAS",
                onClick = { filter = "RECEITAS" },
                label = { Text("Receitas") }
            )
            FilterChip(
                selected = filter == "DESPESAS",
                onClick = { filter = "DESPESAS" },
                label = { Text("Despesas") }
            )
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum lancamento encontrado.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    TransactionItem(
                        item = item,
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddTransactionScreen(
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onAddCategory: (TransactionType, String) -> Unit,
    onSubmit: (TransactionType, Double, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var isCategoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isCreatingNewCategory by rememberSaveable { mutableStateOf(false) }
    var newCategoryText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    val selectedTypeCategories = if (type == TransactionType.EXPENSE) {
        expenseCategories
    } else {
        incomeCategories
    }

    LaunchedEffect(type, selectedTypeCategories) {
        val selectedStillExists = selectedTypeCategories.any {
            it.equals(selectedCategory.trim(), ignoreCase = true)
        }
        if (!isCreatingNewCategory && (selectedCategory.isBlank() || !selectedStillExists)) {
            selectedCategory = selectedTypeCategories.firstOrNull().orEmpty()
        }
        if (!isCreatingNewCategory) newCategoryText = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Novo lancamento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == TransactionType.EXPENSE,
                onClick = { type = TransactionType.EXPENSE },
                label = { Text("Despesa") }
            )
            FilterChip(
                selected = type == TransactionType.INCOME,
                onClick = { type = TransactionType.INCOME },
                label = { Text("Receita") }
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Valor (ex.: 130,50)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Categoria",
            style = MaterialTheme.typography.titleSmall
        )

        ExposedDropdownMenuBox(
            expanded = isCategoryMenuExpanded,
            onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (isCreatingNewCategory) "Nova categoria..." else selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Escolha a categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isCategoryMenuExpanded,
                onDismissRequest = { isCategoryMenuExpanded = false }
            ) {
                selectedTypeCategories.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            selectedCategory = item
                            isCreatingNewCategory = false
                            isCategoryMenuExpanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Nova categoria...") },
                    onClick = {
                        isCreatingNewCategory = true
                        isCategoryMenuExpanded = false
                    }
                )
            }
        }

        if (isCreatingNewCategory) {
            OutlinedTextField(
                value = newCategoryText,
                onValueChange = { newCategoryText = it },
                label = { Text("Nome da nova categoria") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val normalized = newCategoryText.trim().replace(Regex("\\s+"), " ")
                    if (normalized.isBlank()) {
                        Toast.makeText(
                            context,
                            "Digite o nome da categoria.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val alreadyExists = selectedTypeCategories.any {
                        it.equals(normalized, ignoreCase = true)
                    }
                    if (!alreadyExists) {
                        onAddCategory(type, normalized)
                    }

                    selectedCategory = selectedTypeCategories
                        .firstOrNull { it.equals(normalized, ignoreCase = true) }
                        ?: normalized
                    isCreatingNewCategory = false
                    newCategoryText = ""

                    Toast.makeText(
                        context,
                        "Categoria pronta para uso.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Criar categoria")
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Descricao (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val parsed = amountText.replace(',', '.').toDoubleOrNull()
                if (parsed == null || parsed <= 0.0) {
                    Toast.makeText(
                        context,
                        "Informe um valor valido.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                val finalCategory = if (isCreatingNewCategory) {
                    newCategoryText.trim().replace(Regex("\\s+"), " ")
                } else {
                    selectedCategory.trim()
                }
                if (finalCategory.isBlank()) {
                    Toast.makeText(
                        context,
                        "Escolha ou cadastre uma categoria.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (isCreatingNewCategory) {
                    onAddCategory(type, finalCategory)
                    selectedCategory = finalCategory
                    isCreatingNewCategory = false
                    newCategoryText = ""
                }

                onSubmit(type, parsed, finalCategory, note)
                amountText = ""
                note = ""
                Toast.makeText(
                    context,
                    "Lancamento salvo no celular.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Salvar lancamento")
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompactTransactionRow(item: TransactionEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.category,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.note.ifBlank { "Sem descricao" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (item.type == TransactionType.INCOME) {
                "+ ${item.amount.toBrl()}"
            } else {
                "- ${item.amount.toBrl()}"
            },
            color = if (item.type == TransactionType.INCOME) Color(0xFF43E97B) else Color(0xFFFF5C8A),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TransactionItem(item: TransactionEntity, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2C)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(46.dp)
                    .background(
                        color = if (item.type == TransactionType.INCOME) {
                            Color(0xFF4CFFB2)
                        } else {
                            Color(0xFFFF6D95)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = item.category, fontWeight = FontWeight.SemiBold)
                Text(
                    text = item.note.ifBlank { "Sem descricao" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.dateMillis.toDateLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (item.type == TransactionType.INCOME) {
                        "+ ${item.amount.toBrl()}"
                    } else {
                        "- ${item.amount.toBrl()}"
                    },
                    color = if (item.type == TransactionType.INCOME) {
                        Color(0xFF53F38C)
                    } else {
                        Color(0xFFFF6D95)
                    },
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint = Color(0xFFFFB5C7)
                    )
                }
            }
        }
    }
}
