package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.SupplierEntity
import com.example.data.repository.NexusRepository
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(viewModel: NexusViewModel) {
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val restockList by viewModel.restockList.collectAsState()
    val cashBalance by viewModel.currentCashBalance.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Orden de Reabastecimiento, 1: Historial

    // Restock Order Form State
    var selectedSupplierId by remember { mutableStateOf(if (suppliers.isNotEmpty()) suppliers.first().id else 0L) }
    var invoice by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var deductFromCash by remember { mutableStateOf(true) }

    val restockItems = remember { mutableStateListOf<NexusRepository.RestockItemInput>() }

    val lowStockProducts = products.filter { it.stock <= it.minStock }

    val monthStart = Utils.getMonthStart()
    val monthRestocks = restockList.filter { it.date >= monthStart }
    val monthRestockTotal = monthRestocks.sumOf { it.totalCost }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- METRICS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Compras del Mes",
                value = Utils.formatCurrency(monthRestockTotal),
                icon = Icons.Default.LocalShipping,
                accentColor = NexusBlue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Stock Bajo",
                value = "${lowStockProducts.size} prod.",
                icon = Icons.Default.Warning,
                accentColor = if (lowStockProducts.isNotEmpty()) NexusYellow else NexusGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // --- TAB NAVIGATION ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Nuevo Reabastecimiento", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Historial (${restockList.size})", fontWeight = FontWeight.Bold) })
        }

        if (selectedTab == 0) {
            // --- NEW RESTOCK FORM ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Selección de Proveedor", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    var supplierDropdownExpanded by remember { mutableStateOf(false) }
                    val selectedSup = suppliers.firstOrNull { it.id == selectedSupplierId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { supplierDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedSup?.name ?: "Seleccionar Proveedor...")
                        }
                        DropdownMenu(expanded = supplierDropdownExpanded, onDismissRequest = { supplierDropdownExpanded = false }) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = { Text(sup.name) },
                                    onClick = {
                                        selectedSupplierId = sup.id
                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Productos a Reabastecer", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                        Button(
                            onClick = {
                                lowStockProducts.forEach { p ->
                                    if (restockItems.none { it.product.id == p.id }) {
                                        val qtyToAdd = (p.minStock * 2 - p.stock).coerceAtLeast(5)
                                        restockItems.add(NexusRepository.RestockItemInput(p, qtyToAdd, p.cost))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexusYellow),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar Todos los Bajos (${lowStockProducts.size})", fontSize = 11.sp, color = NexusDark)
                        }
                    }

                    // ADD INDIVIDUAL PRODUCT DIALOG TRIGGER
                    var showAddProductDialog by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showAddProductDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Seleccionar Producto")
                    }

                    if (showAddProductDialog) {
                        AddRestockProductDialog(
                            products = products,
                            onAdd = { item ->
                                restockItems.add(item)
                                showAddProductDialog = false
                            },
                            onDismiss = { showAddProductDialog = false }
                        )
                    }

                    // LIST OF ITEMS IN RESTOCK ORDER
                    if (restockItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            restockItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Cant: ${item.quantity} | Costo unit: ${Utils.formatCurrency(item.newUnitCost)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(Utils.formatCurrency(item.quantity * item.newUnitCost), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    IconButton(onClick = { restockItems.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexusRed)
                                    }
                                }
                            }
                        }
                    }

                    val totalOrderCost = restockItems.sumOf { it.quantity * it.newUnitCost }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deductFromCash, onCheckedChange = { deductFromCash = it })
                        Text("Descontar automáticamente de Caja")
                    }

                    if (deductFromCash && totalOrderCost > cashBalance) {
                        Text(
                            "⚠️ ¡Atención! El total (${Utils.formatCurrency(totalOrderCost)}) excede el saldo disponible en caja (${Utils.formatCurrency(cashBalance)}).",
                            style = MaterialTheme.typography.bodySmall.copy(color = NexusRed, fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Reabastecimiento:", style = MaterialTheme.typography.titleMedium)
                        Text(Utils.formatCurrency(totalOrderCost), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = NexusGreen))
                    }

                    Button(
                        onClick = {
                            val sup = suppliers.firstOrNull { it.id == selectedSupplierId }
                            if (sup != null && restockItems.isNotEmpty()) {
                                viewModel.registerRestock(
                                    supplierId = sup.id,
                                    supplierName = sup.name,
                                    invoice = invoice.trim(),
                                    notes = notes.trim(),
                                    items = restockItems.toList(),
                                    deductFromCash = deductFromCash
                                )
                                restockItems.clear()
                            }
                        },
                        enabled = restockItems.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                    ) {
                        Text("Confirmar y Reabastecer Stock")
                    }
                }
            }
        } else {
            // --- RESTOCK HISTORY ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(restockList, key = { it.id }) { r ->
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
                                Text("Reabastecimiento #${r.id} - ${r.supplierName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("${Utils.formatDateTime(r.date)} | ${r.totalItems} ítems", style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                            }
                            Text(Utils.formatCurrency(r.totalCost), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRestockProductDialog(
    products: List<ProductEntity>,
    onAdd: (NexusRepository.RestockItemInput) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProductId by remember { mutableStateOf(if (products.isNotEmpty()) products.first().id else 0L) }
    var qtyStr by remember { mutableStateOf("10") }
    var costStr by remember { mutableStateOf("") }

    val prod = products.firstOrNull { it.id == selectedProductId }
    LaunchedEffect(selectedProductId) {
        if (prod != null) costStr = prod.cost.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Producto a Reabastecer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var dropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { dropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(prod?.let { "${it.name} (Stock: ${it.stock})" } ?: "Seleccionar...")
                    }
                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(text = { Text("${p.name} - Stock: ${p.stock}") }, onClick = {
                                selectedProductId = p.id
                                dropdownExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("Cantidad") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("Nuevo Costo Unitario ($)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toIntOrNull() ?: 0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    if (prod != null && qty > 0) {
                        onAdd(NexusRepository.RestockItemInput(prod, qty, cost))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
