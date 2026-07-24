package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseEntity
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

val EXPENSE_CATEGORIES = listOf(
    "servicios", "alquiler", "salarios", "insumos",
    "marketing", "transporte", "impuestos", "mantenimiento", "seguros", "otros"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: NexusViewModel) {
    val expenses by viewModel.expenses.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("Todas") }
    var showExpenseModal by remember { mutableStateOf(false) }

    val monthStart = Utils.getMonthStart()
    val monthExpenses = expenses.filter { it.date >= monthStart }

    val totalMonthExpenses = monthExpenses.sumOf { it.amount }
    val fixedMonthExpenses = monthExpenses.filter { it.isFixed }.sumOf { it.amount }
    val variableMonthExpenses = monthExpenses.filter { !it.isFixed }.sumOf { it.amount }

    val filteredExpenses = expenses.filter {
        selectedCategoryFilter == "Todas" || it.category.lowercase() == selectedCategoryFilter.lowercase()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExpenseModal = true },
                containerColor = NexusDark,
                contentColor = NexusWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Gasto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- SUMMARY CARDS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Gastos del Mes",
                    value = Utils.formatCurrency(totalMonthExpenses),
                    icon = Icons.Default.ReceiptLong,
                    accentColor = NexusRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Gastos Fijos",
                    value = Utils.formatCurrency(fixedMonthExpenses),
                    icon = Icons.Default.Lock,
                    accentColor = NexusDark,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Gastos Variables",
                    value = Utils.formatCurrency(variableMonthExpenses),
                    icon = Icons.Default.TrendingDown,
                    accentColor = NexusYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            // --- CATEGORY FILTER CHIPS ---
            var categoryExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = selectedCategoryFilter != "Todas",
                    onClick = { categoryExpanded = true },
                    label = { Text("Categoría: $selectedCategoryFilter") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            selectedCategoryFilter = "Todas"
                            categoryExpanded = false
                        }
                    )
                    EXPENSE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedCategoryFilter = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron gastos registrados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(
                                            text = expense.category.uppercase(),
                                            type = "warning"
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (expense.isFixed) {
                                            StatusBadge(text = "FIJO", type = "info")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = expense.description.ifBlank { "Sin descripción" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${Utils.formatDateTime(expense.date)}${if (expense.receipt.isNotBlank()) " | Factura: ${expense.receipt}" else ""}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = Utils.formatCurrency(expense.amount),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NexusRed)
                                    )
                                    IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexusRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- EXPENSE REGISTRATION MODAL ---
    if (showExpenseModal) {
        var category by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
        var amountStr by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var receipt by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var isFixed by remember { mutableStateOf(false) }
        var deductFromCash by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showExpenseModal = false },
            title = { Text("Registrar Nuevo Gasto", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Categoría de Gasto") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Monto ($) *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción del Gasto") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = receipt,
                            onValueChange = { receipt = it },
                            label = { Text("N° Factura / Recibo (Opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isFixed, onCheckedChange = { isFixed = it })
                            Text("¿Es un gasto fijo mensual?")
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = deductFromCash, onCheckedChange = { deductFromCash = it })
                            Text("Descontar automáticamente de Caja")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.registerExpense(
                                category = category,
                                amount = amount,
                                description = description.trim(),
                                receipt = receipt.trim(),
                                notes = notes.trim(),
                                isFixed = isFixed,
                                deductFromCash = deductFromCash
                            )
                            showExpenseModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Text("Guardar Gasto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
