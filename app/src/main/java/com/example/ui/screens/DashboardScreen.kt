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
import com.example.data.model.SaleEntity
import com.example.ui.components.BarChart7Days
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationSection
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: NexusViewModel,
    onNavigate: (NavigationSection) -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val cashBalance by viewModel.currentCashBalance.collectAsState()

    val todayStart = Utils.getTodayStart()
    val todayEnd = Utils.getTodayEnd()

    val salesTodayList = sales.filter { it.date in todayStart..todayEnd }
    val totalSalesToday = salesTodayList.sumOf { it.total }
    val netProfitToday = salesTodayList.sumOf { it.profit }
    val transactionsTodayCount = salesTodayList.size

    val lowStockCount = products.count { it.stock <= it.minStock }
    val activeProductsCount = products.count { it.status == "activo" }

    val avgTicketToday = if (transactionsTodayCount > 0) totalSalesToday / transactionsTodayCount else 0.0

    // Profit Margin %
    val totalRevenueAll = sales.sumOf { it.total }
    val totalProfitAll = sales.sumOf { it.profit }
    val marginPercentage = if (totalRevenueAll > 0) (totalProfitAll / totalRevenueAll) * 100 else 0.0

    // Top 5 Most Sold Products
    val top5Products = remember(sales, products) {
        products.sortedByDescending { p ->
            sales.sumOf { s -> if (s.customer.contains(p.name, ignoreCase = true)) s.itemsCount else 0 }
        }.take(5)
    }

    // 7 Days Sales & Profit Chart Data
    val (chartSales, chartProfit) = remember(sales) {
        val sdf = SimpleDateFormat("dd/MM", Locale("es", "MX"))
        val cal = Calendar.getInstance()
        val sList = mutableListOf<Pair<String, Double>>()
        val pList = mutableListOf<Pair<String, Double>>()

        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            dateCal.set(Calendar.HOUR_OF_DAY, 0)
            dateCal.set(Calendar.MINUTE, 0)
            val dayStart = dateCal.timeInMillis
            dateCal.set(Calendar.HOUR_OF_DAY, 23)
            dateCal.set(Calendar.MINUTE, 59)
            val dayEnd = dateCal.timeInMillis

            val daySales = sales.filter { it.date in dayStart..dayEnd }
            val dayLabel = sdf.format(dateCal.time)
            sList.add(dayLabel to daySales.sumOf { it.total })
            pList.add(dayLabel to daySales.sumOf { it.profit })
        }
        sList to pList
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- REAL-TIME KPIs ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Ventas Hoy",
                        value = Utils.formatCurrency(totalSalesToday),
                        icon = Icons.Default.TrendingUp,
                        accentColor = NexusBlue,
                        subtitle = "$transactionsTodayCount transacciones",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Ganancias Hoy",
                        value = Utils.formatCurrency(netProfitToday),
                        icon = Icons.Default.AttachMoney,
                        accentColor = NexusGreen,
                        subtitle = "Neto acumulado",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Estado de Caja",
                        value = Utils.formatCurrency(cashBalance),
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (cashBalance >= 0) NexusDark else NexusRed,
                        subtitle = "Saldo actual",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Stock Bajo",
                        value = "$lowStockCount prod.",
                        icon = Icons.Default.Warning,
                        accentColor = if (lowStockCount > 0) NexusYellow else NexusGreen,
                        subtitle = "Requieren reabastecimiento",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- QUICK INDICATORS ROW ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IndicatorItem(title = "Ticket Promedio", value = Utils.formatCurrency(avgTicketToday))
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = NexusBorder)
                    IndicatorItem(title = "Margen Ganancia", value = "%.1f%%".format(marginPercentage))
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = NexusBorder)
                    IndicatorItem(title = "Productos Activos", value = activeProductsCount.toString())
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = NexusBorder)
                    IndicatorItem(title = "Clientes", value = clients.size.toString())
                }
            }
        }

        // --- CHART: 7 DAYS SALES & PROFIT ---
        item {
            BarChart7Days(salesData = chartSales, profitData = chartProfit)
        }

        // --- TOP 5 MOST SOLD PRODUCTS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top 5 Productos Más Vendidos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { onNavigate(NavigationSection.INVENTARIO) }) {
                            Text("Ver inventario")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (top5Products.isEmpty()) {
                        Text("No hay productos registrados", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                    } else {
                        top5Products.forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(NexusDark, shape = RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${p.category} | Stock: ${p.stock}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                                    )
                                }
                                Text(
                                    text = Utils.formatCurrency(p.price),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = NexusGreen)
                                )
                            }
                            if (idx < top5Products.size - 1) Divider(color = NexusBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // --- 5 MOST RECENT SALES ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Últimas 5 Ventas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { onNavigate(NavigationSection.VENTAS) }) {
                            Text("Ir a Ventas")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val recent5Sales = sales.take(5)
                    if (recent5Sales.isEmpty()) {
                        Text("No se han registrado ventas", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted))
                    } else {
                        recent5Sales.forEach { sale ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Venta #${sale.id} - ${sale.customer}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${Utils.formatDateTime(sale.date)} | ${sale.itemsCount} arts.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                                    )
                                }
                                StatusBadge(text = sale.paymentMethod.uppercase(), type = "info")
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = Utils.formatCurrency(sale.total),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Divider(color = NexusBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}
