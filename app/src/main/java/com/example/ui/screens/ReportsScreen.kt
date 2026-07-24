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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@Composable
fun ReportsScreen(viewModel: NexusViewModel) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val cashBalance by viewModel.currentCashBalance.collectAsState()

    var selectedPeriod by remember { mutableStateOf("Mensual") } // "Diario", "Semanal", "Mensual", "Anual"

    val periodStart = remember(selectedPeriod) {
        val now = System.currentTimeMillis()
        when (selectedPeriod) {
            "Diario" -> Utils.getTodayStart()
            "Semanal" -> now - (7L * 24 * 60 * 60 * 1000)
            "Mensual" -> Utils.getMonthStart()
            else -> now - (365L * 24 * 60 * 60 * 1000)
        }
    }

    val periodSales = sales.filter { it.date >= periodStart }
    val periodExpenses = expenses.filter { it.date >= periodStart }

    val totalRevenue = periodSales.sumOf { it.total }
    val totalCost = periodSales.sumOf { it.cost }
    val grossProfit = periodSales.sumOf { it.profit }
    val totalExpenses = periodExpenses.sumOf { it.amount }
    val netBalance = grossProfit - totalExpenses

    val marginPct = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100 else 0.0

    // Inventory General Balance Sheet
    val inventoryValuationCost = products.sumOf { it.cost * it.stock }
    val inventoryPotentialRevenue = products.sumOf { it.price * it.stock }

    // Payment Methods Breakdown
    val efectivoTotal = periodSales.filter { it.paymentMethod.lowercase() == "efectivo" }.sumOf { it.total }
    val tarjetaTotal = periodSales.filter { it.paymentMethod.lowercase() == "tarjeta" }.sumOf { it.total }
    val transferenciaTotal = periodSales.filter { it.paymentMethod.lowercase() == "transferencia" }.sumOf { it.total }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- PERIOD SELECTOR ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Diario", "Semanal", "Mensual", "Anual").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- FINANCIAL SUMMARY CARDS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Ventas Período",
                        value = Utils.formatCurrency(totalRevenue),
                        icon = Icons.Default.TrendingUp,
                        accentColor = NexusBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Balance Neto",
                        value = Utils.formatCurrency(netBalance),
                        icon = Icons.Default.AccountBalance,
                        accentColor = if (netBalance >= 0) NexusGreen else NexusRed,
                        subtitle = "Ganancia Bruta - Gastos",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Costo de Ventas",
                        value = Utils.formatCurrency(totalCost),
                        icon = Icons.Default.Inventory2,
                        accentColor = NexusDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Gastos Totales",
                        value = Utils.formatCurrency(totalExpenses),
                        icon = Icons.Default.ReceiptLong,
                        accentColor = NexusRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- GENERAL BALANCE SHEET ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Balance General de la Empresa", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Divider(color = NexusBorder)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Efectivo en Caja:")
                        Text(Utils.formatCurrency(cashBalance), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Valor de Inventario (al costo):")
                        Text(Utils.formatCurrency(inventoryValuationCost), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ingresos Potenciales en Stock:")
                        Text(Utils.formatCurrency(inventoryPotentialRevenue), fontWeight = FontWeight.Bold, color = NexusGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Margen Bruto de Ganancia:")
                        Text("%.1f%%".format(marginPct), fontWeight = FontWeight.Bold, color = NexusYellow)
                    }
                }
            }
        }

        // --- PAYMENT METHODS BREAKDOWN ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ventas por Método de Pago", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Divider(color = NexusBorder)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Efectivo:")
                        Text(Utils.formatCurrency(efectivoTotal), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tarjeta:")
                        Text(Utils.formatCurrency(tarjetaTotal), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transferencia:")
                        Text(Utils.formatCurrency(transferenciaTotal), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- EXPORT REPORT BUTTON ---
        item {
            Button(
                onClick = {
                    val reportContent = """
                        ====================================
                        REPORTE FINANCIERO - NEXUS ADMIN
                        Período: $selectedPeriod
                        ====================================
                        Ventas Totales: ${Utils.formatCurrency(totalRevenue)}
                        Costo de Ventas: ${Utils.formatCurrency(totalCost)}
                        Ganancia Bruta: ${Utils.formatCurrency(grossProfit)}
                        Gastos Totales: ${Utils.formatCurrency(totalExpenses)}
                        ------------------------------------
                        BALANCE NETO: ${Utils.formatCurrency(netBalance)}
                        MARGEN %: %.1f%%
                        ====================================
                        Efectivo en Caja: ${Utils.formatCurrency(cashBalance)}
                        Valor Inventario: ${Utils.formatCurrency(inventoryValuationCost)}
                        Ingresos Potenciales Stock: ${Utils.formatCurrency(inventoryPotentialRevenue)}
                    """.trimIndent().format(marginPct)

                    Utils.shareTextFile(context, "reporte_financiero_$selectedPeriod.txt", reportContent, "text/plain")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir Reporte Financiero")
            }
        }
    }
}
