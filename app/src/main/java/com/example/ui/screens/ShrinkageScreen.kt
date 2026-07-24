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
import com.example.data.model.ProductEntity
import com.example.data.model.ShrinkageEntity
import com.example.ui.components.DoughnutChart
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

val SHRINKAGE_TYPES = listOf("merma", "consumo", "caducidad", "robo", "error", "otro")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShrinkageScreen(viewModel: NexusViewModel) {
    val shrinkageList by viewModel.shrinkageList.collectAsState()
    val products by viewModel.products.collectAsState()

    var showShrinkageModal by remember { mutableStateOf(false) }

    val todayStart = Utils.getTodayStart()
    val todayEnd = Utils.getTodayEnd()
    val monthStart = Utils.getMonthStart()

    val lossToday = shrinkageList.filter { it.date in todayStart..todayEnd }.sumOf { it.totalLoss }
    val lossMonth = shrinkageList.filter { it.date >= monthStart }.sumOf { it.totalLoss }

    // Doughnut Chart Data by Type
    val typeDistribution = remember(shrinkageList) {
        SHRINKAGE_TYPES.map { type ->
            val sum = shrinkageList.filter { it.type.lowercase() == type.lowercase() }.sumOf { it.totalLoss }
            type.replaceFirstChar { it.uppercase() } to sum
        }.filter { it.second > 0 }
    }

    val colors = listOf(NexusRed, NexusYellow, NexusBlue, NexusDark, NexusGreen, NexusTextSecondary)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showShrinkageModal = true },
                containerColor = NexusDark,
                contentColor = NexusWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Merma")
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
            // --- SUMMARY METRICS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Pérdida Hoy",
                    value = Utils.formatCurrency(lossToday),
                    icon = Icons.Default.Delete,
                    accentColor = NexusRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Pérdida del Mes",
                    value = Utils.formatCurrency(lossMonth),
                    icon = Icons.Default.TrendingDown,
                    accentColor = NexusYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            // --- DISTRIBUTION DOUGHNUT CHART ---
            if (typeDistribution.isNotEmpty()) {
                DoughnutChart(items = typeDistribution, colors = colors)
            }

            Text("Historial de Mermas y Pérdidas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            if (shrinkageList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No se han registrado mermas o desperdicios", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shrinkageList, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(text = item.type.uppercase(), type = "danger")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(item.productName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Cant: ${item.quantity} unidades | Costo: ${Utils.formatCurrency(item.cost)}/u", style = MaterialTheme.typography.bodySmall)
                                    Text("${Utils.formatDateTime(item.date)}${if (item.reason.isNotBlank()) " | Motivo: ${item.reason}" else ""}", style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                                }

                                Text(
                                    text = "-" + Utils.formatCurrency(item.totalLoss),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NexusRed)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SHRINKAGE REGISTRATION DIALOG ---
    if (showShrinkageModal) {
        var selectedProductId by remember { mutableStateOf(if (products.isNotEmpty()) products.first().id else 0L) }
        var type by remember { mutableStateOf(SHRINKAGE_TYPES.first()) }
        var qtyStr by remember { mutableStateOf("1") }
        var reason by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        val selectedProduct = products.firstOrNull { it.id == selectedProductId }

        AlertDialog(
            onDismissRequest = { showShrinkageModal = false },
            title = { Text("Registrar Merma / Consumo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Seleccionar Producto:", style = MaterialTheme.typography.bodySmall)
                    var prodDropdownExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { prodDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedProduct?.let { "${it.name} (Stock: ${it.stock})" } ?: "Seleccionar...")
                        }
                        DropdownMenu(expanded = prodDropdownExpanded, onDismissRequest = { prodDropdownExpanded = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} - Stock: ${p.stock}") },
                                    onClick = {
                                        selectedProductId = p.id
                                        prodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Tipo de Pérdida:", style = MaterialTheme.typography.bodySmall)
                    var typeDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { typeDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(type.replaceFirstChar { it.uppercase() })
                        }
                        DropdownMenu(expanded = typeDropdownExpanded, onDismissRequest = { typeDropdownExpanded = false }) {
                            SHRINKAGE_TYPES.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        type = t
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Cantidad a descontar *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo / Causa") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedProduct != null) {
                        val qty = qtyStr.toIntOrNull() ?: 0
                        val estLoss = qty * selectedProduct.cost
                        Text(
                            text = "Pérdida Total estimada: ${Utils.formatCurrency(estLoss)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = NexusRed)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyStr.toIntOrNull() ?: 0
                        if (selectedProduct != null && qty > 0) {
                            viewModel.registerShrinkage(selectedProduct, qty, type, reason.trim(), notes.trim())
                            showShrinkageModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusRed)
                ) {
                    Text("Registrar Pérdida")
                }
            },
            dismissButton = { TextButton(onClick = { showShrinkageModal = false }) { Text("Cancelar") } }
        )
    }
}
