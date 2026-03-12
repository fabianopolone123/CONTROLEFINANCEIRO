package com.fabiano.controlefinanca.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabiano.controlefinanca.data.TransactionEntity
import com.fabiano.controlefinanca.data.TransactionType
import com.fabiano.controlefinanca.data.RecurrenceType
import com.fabiano.controlefinanca.ui.components.ExpensePieChart
import com.fabiano.controlefinanca.ui.components.MonthlyBarChart
import java.util.Calendar

private enum class AppTab {
    DASHBOARD,
    LIST,
    ADD
}

@Composable
fun FinanceApp(viewModel: FinanceViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    var isQuickAddExpanded by rememberSaveable { mutableStateOf(false) }
    var preselectedTypeName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTab) {
        if (currentTab != AppTab.DASHBOARD) {
            isQuickAddExpanded = false
        }
    }

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
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                if (currentTab == AppTab.DASHBOARD) {
                    DashboardQuickAddFab(
                        expanded = isQuickAddExpanded,
                        onToggle = { isQuickAddExpanded = !isQuickAddExpanded },
                        onAddExpense = {
                            preselectedTypeName = TransactionType.EXPENSE.name
                            isQuickAddExpanded = false
                            currentTab = AppTab.ADD
                        },
                        onAddIncome = {
                            preselectedTypeName = TransactionType.INCOME.name
                            isQuickAddExpanded = false
                            currentTab = AppTab.ADD
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF131B2B)) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.DASHBOARD,
                        onClick = { currentTab = AppTab.DASHBOARD },
                        icon = { Icon(Icons.Rounded.PieChart, contentDescription = null) },
                        label = { Text("Resumo") }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.LIST,
                        onClick = { currentTab = AppTab.LIST },
                        icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null) },
                        label = { Text("Lancamentos") }
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
                    preselectedType = preselectedTypeName?.let { TransactionType.valueOf(it) },
                    onPreselectedTypeConsumed = { preselectedTypeName = null },
                    onAddCategory = viewModel::addCategory,
                    onSubmit = { type, amount, category, note, transactionDateMillis, recurrenceType, installmentCurrent, installmentTotal ->
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            category = category,
                            note = note,
                            transactionDateMillis = transactionDateMillis,
                            recurrenceType = recurrenceType,
                            installmentCurrent = installmentCurrent,
                            installmentTotal = installmentTotal
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
private fun DashboardQuickAddFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(220)) +
                slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 2 }) +
                scaleIn(animationSpec = tween(220), initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(160)) +
                slideOutVertically(animationSpec = tween(160), targetOffsetY = { it / 2 }) +
                scaleOut(animationSpec = tween(160), targetScale = 0.85f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddIncome,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33D17A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Nova receita", color = Color(0xFF001A0C), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAddExpense,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C8A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Nova despesa", color = Color(0xFF2B0010), fontWeight = FontWeight.Bold)
                }
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = Color(0xFF00D1FF),
            contentColor = Color(0xFF02131A)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Adicionar",
                modifier = Modifier.graphicsLayer(rotationZ = rotation)
            )
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
    preselectedType: TransactionType?,
    onPreselectedTypeConsumed: () -> Unit,
    onAddCategory: (TransactionType, String) -> Unit,
    onSubmit: (
        TransactionType,
        Double,
        String,
        String,
        Long,
        RecurrenceType,
        Int,
        Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var transactionDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var recurrenceType by rememberSaveable { mutableStateOf(RecurrenceType.ONE_TIME) }
    var installmentCurrentText by rememberSaveable { mutableStateOf("1") }
    var installmentTotalText by rememberSaveable { mutableStateOf("2") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var isCategoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isCreatingNewCategory by rememberSaveable { mutableStateOf(false) }
    var newCategoryText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(preselectedType) {
        if (preselectedType != null) {
            type = preselectedType
            onPreselectedTypeConsumed()
        }
    }

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
    val initialDate = remember(transactionDateMillis) {
        Calendar.getInstance().apply { timeInMillis = transactionDateMillis }
    }

    val openDatePicker = {
        DatePickerDialog(
            context,
            { _, year, monthOfYear, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthOfYear)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                transactionDateMillis = picked.timeInMillis
            },
            initialDate.get(Calendar.YEAR),
            initialDate.get(Calendar.MONTH),
            initialDate.get(Calendar.DAY_OF_MONTH)
        ).show()
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

        OutlinedTextField(
            value = transactionDateMillis.toDateLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Data da transacao") },
            trailingIcon = {
                Text(
                    text = "Hoje",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openDatePicker() },
            colors = OutlinedTextFieldDefaults.colors()
        )

        Text(
            text = "Recorrencia",
            style = MaterialTheme.typography.titleSmall
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = recurrenceType == RecurrenceType.ONE_TIME,
                    onClick = { recurrenceType = RecurrenceType.ONE_TIME },
                    label = { Text("Unica") }
                )
            }
            item {
                FilterChip(
                    selected = recurrenceType == RecurrenceType.FIXED,
                    onClick = { recurrenceType = RecurrenceType.FIXED },
                    label = { Text("Fixa") }
                )
            }
            item {
                FilterChip(
                    selected = recurrenceType == RecurrenceType.INSTALLMENT,
                    onClick = { recurrenceType = RecurrenceType.INSTALLMENT },
                    label = { Text("Parcelada") }
                )
            }
        }

        if (recurrenceType == RecurrenceType.INSTALLMENT) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = installmentCurrentText,
                    onValueChange = { installmentCurrentText = it },
                    label = { Text("Parcela atual") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = installmentTotalText,
                    onValueChange = { installmentTotalText = it },
                    label = { Text("Total parcelas") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

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

                val installmentCurrent = if (recurrenceType == RecurrenceType.INSTALLMENT) {
                    installmentCurrentText.toIntOrNull()
                } else {
                    1
                }
                val installmentTotal = if (recurrenceType == RecurrenceType.INSTALLMENT) {
                    installmentTotalText.toIntOrNull()
                } else {
                    1
                }
                if (recurrenceType == RecurrenceType.INSTALLMENT) {
                    if (installmentCurrent == null || installmentTotal == null) {
                        Toast.makeText(
                            context,
                            "Informe parcelas validas.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    if (installmentTotal < 2 || installmentCurrent !in 1..installmentTotal) {
                        Toast.makeText(
                            context,
                            "Parcela atual deve estar entre 1 e total.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                }

                onSubmit(
                    type,
                    parsed,
                    finalCategory,
                    note,
                    transactionDateMillis,
                    recurrenceType,
                    installmentCurrent ?: 1,
                    installmentTotal ?: 1
                )
                amountText = ""
                note = ""
                if (recurrenceType == RecurrenceType.INSTALLMENT) {
                    installmentCurrentText = "1"
                    installmentTotalText = "2"
                }
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
            Text(
                text = "${item.transactionDateMillis.toDateLabel()} • ${item.recurrenceLabel()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
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
                    text = item.transactionDateMillis.toDateLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = item.recurrenceLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
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

private fun TransactionEntity.recurrenceLabel(): String {
    return when (recurrenceType) {
        RecurrenceType.ONE_TIME -> "Unica"
        RecurrenceType.FIXED -> "Fixa"
        RecurrenceType.INSTALLMENT -> "${installmentCurrent}/${installmentTotal}"
    }
}
