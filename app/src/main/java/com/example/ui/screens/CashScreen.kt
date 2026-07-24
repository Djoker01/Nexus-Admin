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
import com.example.data.model.CashMovementEntity
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(viewModel: NexusViewModel) {
    val cashMovements by viewModel.cashMovements.collectAsState()
    val currentBalance by viewModel.currentCashBalance.collectAsState()

    val todayStart = Utils.getTodayStart()
    val todayEnd = Utils.getTodayEnd()

    val movementsToday = cashMovements.filter { it.date in todayStart..todayEnd }
    val incomesToday = movementsToday.filter { it.type == "ingreso" }.sumOf { it.amount }
    val expensesToday = movementsToday.filter { it.type == "egreso" }.sumOf { it.amount }
    val balanceToday = incomesToday - expensesToday

    var showMovementModal by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMovementModal = true },
                containerColor = NexusDark,
                contentColor = NexusWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Movimiento")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- CASH SUMMARY CARDS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Saldo en Caja",
                    value = Utils.formatCurrency(currentBalance),
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = if (currentBalance >= 0) NexusDark else NexusRed,
                    subtitle = "Efectivo disponible",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Ingresos Hoy",
                    value = Utils.formatCurrency(incomesToday),
                    icon = Icons.Default.ArrowUpward,
                    accentColor = NexusGreen,
                    subtitle = "Entradas de caja",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Egresos Hoy",
                    value = Utils.formatCurrency(expensesToday),
                    icon = Icons.Default.ArrowDownward,
                    accentColor = NexusRed,
                    subtitle = "Salidas de caja",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Historial de Movimientos de Caja",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (cashMovements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay movimientos de caja registrados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cashMovements, key = { it.id }) { mov ->
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
                                            text = mov.type.uppercase(),
                                            type = if (mov.type == "ingreso") "ingreso" else "egreso"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = mov.concept,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (mov.description.isNotEmpty()) {
                                        Text(
                                            text = mov.description,
                                            style = MaterialTheme.typography.bodySmall.copy(color = NexusTextSecondary)
                                        )
                                    }
                                    Text(
                                        text = "${Utils.formatDateTime(mov.date)} | Saldo posterior: ${Utils.formatCurrency(mov.balanceAfter)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = NexusTextMuted)
                                    )
                                }

                                Text(
                                    text = (if (mov.type == "ingreso") "+" else "-") + Utils.formatCurrency(mov.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (mov.type == "ingreso") NexusGreen else NexusRed
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MANUAL CASH MOVEMENT MODAL ---
    if (showMovementModal) {
        var type by remember { mutableStateOf("ingreso") }
        var amountStr by remember { mutableStateOf("") }
        var concept by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showMovementModal = false },
            title = { Text("Registrar Movimiento Manual de Caja", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == "ingreso",
                            onClick = { type = "ingreso" },
                            label = { Text("Ingreso (+)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = type == "egreso",
                            onClick = { type = "egreso" },
                            label = { Text("Egreso (-)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Monto ($) *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = { Text("Concepto * (Ej: Apertura, Retiro)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción u Observaciones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && concept.isNotBlank()) {
                            viewModel.addManualCashMovement(type, amount, concept.trim(), description.trim())
                            showMovementModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMovementModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
