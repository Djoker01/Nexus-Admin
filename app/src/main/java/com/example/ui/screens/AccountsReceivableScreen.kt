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
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountsReceivableEntity
import com.example.data.model.ClientEntity
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsReceivableScreen(viewModel: NexusViewModel) {
    val clients by viewModel.clients.collectAsState()
    val accounts by viewModel.accountsReceivable.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Cuentas por Cobrar, 1: Clientes

    var showAccountModal by remember { mutableStateOf(false) }
    var showClientModal by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }

    var showPaymentModal by remember { mutableStateOf(false) }
    var selectedAccountForPayment by remember { mutableStateOf<AccountsReceivableEntity?>(null) }

    val pendingAccounts = accounts.filter { it.status != "paid" }
    val totalPending = pendingAccounts.sumOf { it.total }
    val paidAccounts = accounts.filter { it.status == "paid" }
    val totalCollected = paidAccounts.sumOf { it.total }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- SUMMARY METRICS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Total Pendiente",
                value = Utils.formatCurrency(totalPending),
                icon = Icons.Default.PendingActions,
                accentColor = NexusRed,
                subtitle = "${pendingAccounts.size} cuentas por cobrar",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Cobrado",
                value = Utils.formatCurrency(totalCollected),
                icon = Icons.Default.CheckCircle,
                accentColor = NexusGreen,
                subtitle = "Cuentas saldadas",
                modifier = Modifier.weight(1f)
            )
        }

        // --- TAB NAVIGATION ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Cuentas (${accounts.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Clientes (${clients.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            // --- ACCOUNTS RECEIVABLE LIST ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAccountModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva Cuenta Fiada")
                }
            }

            if (accounts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay cuentas registrados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
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
                                        Text(acc.clientName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(
                                            text = when (acc.status) {
                                                "paid" -> "PAGADO"
                                                "partial" -> "PARCIAL"
                                                else -> "PENDIENTE"
                                            },
                                            type = acc.status
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Concepto: ${acc.concept}", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${Utils.formatDateTime(acc.date)}${if (acc.notes.isNotBlank()) " | Notes: ${acc.notes}" else ""}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Utils.formatCurrency(acc.total), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                    if (acc.status != "paid") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                selectedAccountForPayment = acc
                                                showPaymentModal = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NexusGreen),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Abonar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- CLIENTS LIST ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        editingClient = null
                        showClientModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo Cliente")
                }
            }

            if (clients.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay clientes registrados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
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
                                    Text(client.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    if (client.phone.isNotBlank()) Text("Tel: ${client.phone}", style = MaterialTheme.typography.bodySmall)
                                    if (client.address.isNotBlank()) Text("Dir: ${client.address}", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextSecondary))
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Deuda: ${Utils.formatCurrency(client.totalDebt)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (client.totalDebt > 0) NexusRed else NexusGreen
                                        )
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            editingClient = client
                                            showClientModal = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                                        }
                                        IconButton(onClick = { viewModel.deleteClient(client) }) {
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
    }

    // --- ACCOUNT REGISTRATION DIALOG ---
    if (showAccountModal) {
        var selectedClientId by remember { mutableStateOf(if (clients.isNotEmpty()) clients.first().id else 0L) }
        var concept by remember { mutableStateOf("") }
        var totalStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAccountModal = false },
            title = { Text("Nueva Cuenta por Cobrar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Seleccione Cliente:", style = MaterialTheme.typography.bodySmall)
                    var clientDropdownExpanded by remember { mutableStateOf(false) }
                    val selectedClient = clients.firstOrNull { it.id == selectedClientId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { clientDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedClient?.name ?: "Seleccionar...")
                        }
                        DropdownMenu(expanded = clientDropdownExpanded, onDismissRequest = { clientDropdownExpanded = false }) {
                            clients.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedClientId = c.id
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = { Text("Concepto *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = totalStr,
                        onValueChange = { totalStr = it },
                        label = { Text("Monto Total ($) *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas / Promesa de Pago") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val total = totalStr.toDoubleOrNull() ?: 0.0
                        val client = clients.firstOrNull { it.id == selectedClientId }
                        if (client != null && total > 0 && concept.isNotBlank()) {
                            viewModel.registerAccountReceivable(client.id, client.name, concept.trim(), total, notes.trim())
                            showAccountModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Text("Guardar Cuenta")
                }
            },
            dismissButton = { TextButton(onClick = { showAccountModal = false }) { Text("Cancelar") } }
        )
    }

    // --- PAYMENT MODAL ---
    if (showPaymentModal && selectedAccountForPayment != null) {
        val acc = selectedAccountForPayment!!
        var amountStr by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("Efectivo") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPaymentModal = false },
            title = { Text("Abonar a Cuenta de ${acc.clientName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Cuenta: ${Utils.formatCurrency(acc.total)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Monto de Abono ($) *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Efectivo", "Tarjeta", "Transferencia").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.registerPayment(acc.id, acc.clientId, amount, method, notes.trim())
                            showPaymentModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusGreen)
                ) {
                    Text("Registrar Abono")
                }
            },
            dismissButton = { TextButton(onClick = { showPaymentModal = false }) { Text("Cancelar") } }
        )
    }

    // --- CLIENT MODAL ---
    if (showClientModal) {
        var name by remember { mutableStateOf(editingClient?.name ?: "") }
        var phone by remember { mutableStateOf(editingClient?.phone ?: "") }
        var email by remember { mutableStateOf(editingClient?.email ?: "") }
        var address by remember { mutableStateOf(editingClient?.address ?: "") }
        var notes by remember { mutableStateOf(editingClient?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showClientModal = false },
            title = { Text(if (editingClient == null) "Nuevo Cliente" else "Editar Cliente", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val c = ClientEntity(
                                id = editingClient?.id ?: 0L,
                                name = name.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                notes = notes.trim(),
                                totalDebt = editingClient?.totalDebt ?: 0.0
                            )
                            viewModel.saveClient(c)
                            showClientModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showClientModal = false }) { Text("Cancelar") } }
        )
    }
}
