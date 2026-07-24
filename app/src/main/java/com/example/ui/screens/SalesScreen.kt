package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: NexusViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val cart by viewModel.cart.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Nueva Venta (POS), 1: Historial de Ventas

    // POS State
    var customerName by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Efectivo") } // Efectivo, Tarjeta, Transferencia
    var productSearchQuery by remember { mutableStateOf("") }

    // Completed Sale Ticket Modal State
    var showTicketModal by remember { mutableStateOf(false) }
    var lastCompletedSale by remember { mutableStateOf<SaleEntity?>(null) }

    // Statistics Calculation
    val todayStart = Utils.getTodayStart()
    val todayEnd = Utils.getTodayEnd()
    val monthStart = Utils.getMonthStart()

    val salesToday = sales.filter { it.date in todayStart..todayEnd }
    val salesMonth = sales.filter { it.date >= monthStart }

    val totalSalesToday = salesToday.sumOf { it.total }
    val totalSalesMonth = salesMonth.sumOf { it.total }
    val transCountToday = salesToday.size
    val avgTicketToday = if (transCountToday > 0) totalSalesToday / transCountToday else 0.0

    val cartTotal = cart.sumOf { it.product.price * it.quantity }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- STATS SUMMARY CARDS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Ventas Hoy",
                value = Utils.formatCurrency(totalSalesToday),
                icon = Icons.Default.Today,
                accentColor = NexusBlue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Ventas Mes",
                value = Utils.formatCurrency(totalSalesMonth),
                icon = Icons.Default.CalendarMonth,
                accentColor = NexusGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Ticket Promedio",
                value = Utils.formatCurrency(avgTicketToday),
                icon = Icons.Default.ConfirmationNumber,
                accentColor = NexusDark,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- TAB SELECTOR ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Nueva Venta (POS)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Historial (${sales.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // --- POS NEW SALE VIEW ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT: PRODUCT SELECTOR
                Column(modifier = Modifier.weight(1.2f)) {
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        placeholder = { Text("Buscar producto...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredProds = products.filter {
                        it.status == "activo" && (it.name.contains(productSearchQuery, ignoreCase = true) || it.code.contains(productSearchQuery, ignoreCase = true))
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredProds) { prod ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.addToCart(prod) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Stock: ${prod.stock} | ${Utils.formatCurrency(prod.price)}", style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                                    }
                                    IconButton(onClick = { viewModel.addToCart(prod) }) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Agregar", tint = NexusBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT: CART & CHECKOUT
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                    ) {
                        Text("Carrito (${cart.sumOf { it.quantity }})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                        Spacer(modifier = Modifier.height(8.dp))

                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Carrito vacío", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(cart) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.product.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                            Text(Utils.formatCurrency(item.product.price), style = MaterialTheme.typography.labelSmall)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.updateCartQuantity(item.product.id, -1) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Remove, contentDescription = null)
                                            }
                                            Text("${item.quantity}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 4.dp))
                                            IconButton(onClick = { viewModel.updateCartQuantity(item.product.id, 1) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = null)
                                            }
                                        }
                                    }
                                    Divider(color = NexusBorder.copy(alpha = 0.5f))
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            placeholder = { Text("Nombre Cliente (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Payment Method Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total:", style = MaterialTheme.typography.titleMedium)
                            Text(
                                Utils.formatCurrency(cartTotal),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = NexusGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.checkoutSale(customerName, paymentMethod) { saleId ->
                                    val completed = sales.firstOrNull { it.id == saleId }
                                    lastCompletedSale = completed
                                    showTicketModal = true
                                    customerName = ""
                                }
                            },
                            enabled = cart.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NexusGreen)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cobrar Venta")
                        }
                    }
                }
            }
        } else {
            // --- SALES HISTORY VIEW ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sales, key = { it.id }) { sale ->
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
                                Text("Venta #${sale.id} - ${sale.customer}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("${Utils.formatDateTime(sale.date)} | ${sale.itemsCount} productos", style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                                Text("Ganancia: ${Utils.formatCurrency(sale.profit)}", style = MaterialTheme.typography.labelSmall.copy(color = NexusGreen, fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusBadge(text = sale.paymentMethod.uppercase(), type = "info")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(Utils.formatCurrency(sale.total), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                IconButton(
                                    onClick = {
                                        lastCompletedSale = sale
                                        showTicketModal = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = "Ticket", tint = NexusDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- TICKET PRINT/SHARE MODAL ---
    if (showTicketModal && lastCompletedSale != null) {
        val sale = lastCompletedSale!!
        AlertDialog(
            onDismissRequest = { showTicketModal = false },
            title = { Text("Ticket de Venta #${sale.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NEXUS ADMIN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    Text("Comprobante de Venta", style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fecha:", style = MaterialTheme.typography.bodySmall)
                        Text(Utils.formatDateTime(sale.date), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cliente:", style = MaterialTheme.typography.bodySmall)
                        Text(sale.customer, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Método Pago:", style = MaterialTheme.typography.bodySmall)
                        Text(sale.paymentMethod, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(Utils.formatCurrency(sale.total), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NexusGreen))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("¡Gracias por su compra!", style = MaterialTheme.typography.labelMedium.copy(color = NexusTextSecondary))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ticketText = """
                            ==============================
                                     NEXUS ADMIN          
                                Comprobante de Venta      
                            ==============================
                            Venta #: ${sale.id}
                            Fecha: ${Utils.formatDateTime(sale.date)}
                            Cliente: ${sale.customer}
                            Método: ${sale.paymentMethod}
                            Artículos: ${sale.itemsCount}
                            ------------------------------
                            TOTAL: ${Utils.formatCurrency(sale.total)}
                            ==============================
                            ¡Gracias por su preferencia!
                        """.trimIndent()
                        Utils.shareTextFile(context, "ticket_venta_${sale.id}.txt", ticketText, "text/plain")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTicketModal = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
