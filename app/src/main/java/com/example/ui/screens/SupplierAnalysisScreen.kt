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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SupplierEntity
import com.example.data.model.SupplierQuoteEntity
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierAnalysisScreen(viewModel: NexusViewModel) {
    val context = LocalContext.current
    val suppliers by viewModel.suppliers.collectAsState()
    val quotes by viewModel.supplierQuotes.collectAsState()
    val products by viewModel.products.collectAsState()
    val comparisons by viewModel.purchaseComparisons.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Cotizaciones, 1: Proveedores, 2: Análisis, 3: Guardados

    var showSupplierModal by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    var showQuoteModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- TABS ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Cotizaciones", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Proveedores", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Análisis ROI 🏆", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Guardados", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
        }

        if (selectedTab == 0) {
            // --- QUOTES LIST & BEST OPTION HIGHLIGHT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${quotes.size} Cotizaciones Registradas", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Button(onClick = { showQuoteModal = true }, colors = ButtonDefaults.buttonColors(containerColor = NexusDark)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Cotización")
                }
            }

            if (quotes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay cotizaciones registradas", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(quotes, key = { it.id }) { q ->
                        val product = products.firstOrNull { it.id == q.productId }
                        val curCost = product?.cost ?: q.quotedPrice
                        val savingsPerUnit = curCost - q.quotedPrice
                        val isBestOption = savingsPerUnit > 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBestOption) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(q.productName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        if (isBestOption) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🏆 MEJOR OPCIÓN", style = MaterialTheme.typography.labelSmall.copy(color = NexusGreen, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                    Text("Proveedor: ${q.supplierName}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Precio Cotizado: ${Utils.formatCurrency(q.quotedPrice)} (Costo actual: ${Utils.formatCurrency(curCost)})", style = MaterialTheme.typography.labelSmall)
                                    if (savingsPerUnit > 0) {
                                        Text("¡Ahorro por unidad: ${Utils.formatCurrency(savingsPerUnit)}!", style = MaterialTheme.typography.labelSmall.copy(color = NexusGreen, fontWeight = FontWeight.Bold))
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteSupplierQuote(q) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexusRed)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // --- SUPPLIERS MANAGEMENT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${suppliers.size} Proveedores Registrados", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Button(onClick = {
                    editingSupplier = null
                    showSupplierModal = true
                }, colors = ButtonDefaults.buttonColors(containerColor = NexusDark)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Proveedor")
                }
            }

            if (suppliers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay proveedores registrados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(suppliers, key = { it.id }) { sup ->
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
                                    Text(sup.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    if (sup.company.isNotBlank()) Text("Empresa: ${sup.company}", style = MaterialTheme.typography.bodySmall)
                                    if (sup.phone.isNotBlank()) Text("Tel: ${sup.phone}", style = MaterialTheme.typography.bodySmall)
                                    Text("Entrega: ${sup.deliveryDays} días | Términos: ${sup.paymentTerms}", style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                                }

                                Row {
                                    IconButton(onClick = {
                                        editingSupplier = sup
                                        showSupplierModal = true
                                    }) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                                    IconButton(onClick = { viewModel.deleteSupplier(sup) }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexusRed) }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 2) {
            // --- DETAILED ROI & SAVINGS ANALYSIS ---
            if (quotes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Agregue cotizaciones para ver el Análisis de Inversión y ROI", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                val totalInvestment = quotes.sumOf { it.quotedPrice * it.minQuantity }
                val estimatedPotentialRevenue = quotes.sumOf { q ->
                    val prod = products.firstOrNull { it.id == q.productId }
                    val price = prod?.price ?: (q.quotedPrice * 1.5)
                    price * q.minQuantity
                }
                val potentialProfit = estimatedPotentialRevenue - totalInvestment
                val roiPercentage = if (totalInvestment > 0) (potentialProfit / totalInvestment) * 100 else 0.0

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NexusDark),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Resumen de Análisis Financiero", style = MaterialTheme.typography.titleSmall.copy(color = NexusWhite, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Inversión Total Requerida:", color = Color.White.copy(alpha = 0.8f))
                                    Text(Utils.formatCurrency(totalInvestment), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ganancia Potencial Estimada:", color = Color.White.copy(alpha = 0.8f))
                                    Text(Utils.formatCurrency(potentialProfit), color = NexusGreen, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ROI Promedio Estimado:", color = Color.White.copy(alpha = 0.8f))
                                    Text("%.1f%%".format(roiPercentage), color = NexusYellow, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val csv = StringBuilder("Producto,Proveedor,Precio Cotizado,Cant Minima,Inversion,Ganancia Est,ROI %\n")
                                quotes.forEach { q ->
                                    val prod = products.firstOrNull { it.id == q.productId }
                                    val price = prod?.price ?: (q.quotedPrice * 1.5)
                                    val inv = q.quotedPrice * q.minQuantity
                                    val prof = (price - q.quotedPrice) * q.minQuantity
                                    val roi = if (inv > 0) (prof / inv) * 100 else 0.0
                                    csv.append("${q.productName},${q.supplierName},${q.quotedPrice},${q.minQuantity},$inv,$prof,%.1f\n".format(roi))
                                }
                                Utils.shareTextFile(context, "analisis_proveedores.csv", csv.toString(), "text/csv")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NexusBlue)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar Análisis a CSV")
                        }
                    }
                }
            }
        } else {
            // --- SAVED COMPARISONS ---
            if (comparisons.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay análisis guardados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(comparisons, key = { it.id }) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(Utils.formatDateTime(c.date), style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                                }
                                IconButton(onClick = { viewModel.deletePurchaseComparison(c) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = NexusRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SUPPLIER MODAL ---
    if (showSupplierModal) {
        var name by remember { mutableStateOf(editingSupplier?.name ?: "") }
        var company by remember { mutableStateOf(editingSupplier?.company ?: "") }
        var phone by remember { mutableStateOf(editingSupplier?.phone ?: "") }
        var email by remember { mutableStateOf(editingSupplier?.email ?: "") }
        var deliveryDaysStr by remember { mutableStateOf(editingSupplier?.deliveryDays?.toString() ?: "1") }

        AlertDialog(
            onDismissRequest = { showSupplierModal = false },
            title = { Text(if (editingSupplier == null) "Nuevo Proveedor" else "Editar Proveedor", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del Proveedor *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Empresa") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = deliveryDaysStr, onValueChange = { deliveryDaysStr = it }, label = { Text("Tiempo de Entrega (Días)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dDays = deliveryDaysStr.toIntOrNull() ?: 1
                        if (name.isNotBlank()) {
                            viewModel.saveSupplier(
                                SupplierEntity(
                                    id = editingSupplier?.id ?: 0L,
                                    name = name.trim(),
                                    company = company.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    deliveryDays = dDays
                                )
                            )
                            showSupplierModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showSupplierModal = false }) { Text("Cancelar") } }
        )
    }

    // --- QUOTE MODAL ---
    if (showQuoteModal) {
        var selectedProductId by remember { mutableStateOf(if (products.isNotEmpty()) products.first().id else 0L) }
        var selectedSupplierId by remember { mutableStateOf(if (suppliers.isNotEmpty()) suppliers.first().id else 0L) }
        var quotedPriceStr by remember { mutableStateOf("") }
        var minQtyStr by remember { mutableStateOf("12") }

        val prod = products.firstOrNull { it.id == selectedProductId }
        val sup = suppliers.firstOrNull { it.id == selectedSupplierId }

        AlertDialog(
            onDismissRequest = { showQuoteModal = false },
            title = { Text("Registrar Cotización", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Producto:", style = MaterialTheme.typography.bodySmall)
                    var prodExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { prodExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(prod?.name ?: "Seleccionar...") }
                        DropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                    selectedProductId = p.id
                                    prodExpanded = false
                                })
                            }
                        }
                    }

                    Text("Proveedor:", style = MaterialTheme.typography.bodySmall)
                    var supExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { supExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(sup?.name ?: "Seleccionar...") }
                        DropdownMenu(expanded = supExpanded, onDismissRequest = { supExpanded = false }) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = {
                                    selectedSupplierId = s.id
                                    supExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = quotedPriceStr, onValueChange = { quotedPriceStr = it }, label = { Text("Precio Cotizado ($) *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = minQtyStr, onValueChange = { minQtyStr = it }, label = { Text("Cantidad Mínima Requerida") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = quotedPriceStr.toDoubleOrNull() ?: 0.0
                        val minQty = minQtyStr.toIntOrNull() ?: 1
                        if (prod != null && sup != null && price > 0) {
                            viewModel.addSupplierQuote(
                                SupplierQuoteEntity(
                                    productId = prod.id,
                                    productName = prod.name,
                                    supplierId = sup.id,
                                    supplierName = sup.name,
                                    quotedPrice = price,
                                    minQuantity = minQty
                                )
                            )
                            showQuoteModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) { Text("Guardar Cotización") }
            },
            dismissButton = { TextButton(onClick = { showQuoteModal = false }) { Text("Cancelar") } }
        )
    }
}
