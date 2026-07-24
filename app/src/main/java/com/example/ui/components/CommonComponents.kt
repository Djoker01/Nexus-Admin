package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationSection
import com.example.util.Utils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusHeader(
    currentSection: NavigationSection,
    unreadNotificationsCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX")).format(Date())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentSection.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NexusTextSecondary
                        )
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menú de Navegación",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = if (unreadNotificationsCount > 0) NexusYellow else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(NexusRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
fun NexusDrawerContent(
    currentSection: NavigationSection,
    onSectionSelected: (NavigationSection) -> Unit,
    onClose: () -> Unit
) {
    val sectionsWithIcons = listOf(
        NavigationSection.DASHBOARD to Icons.Default.Dashboard,
        NavigationSection.INVENTARIO to Icons.Default.Inventory2,
        NavigationSection.VENTAS to Icons.Default.PointOfSale,
        NavigationSection.CAJA to Icons.Default.AccountBalanceWallet,
        NavigationSection.GASTOS to Icons.Default.ReceiptLong,
        NavigationSection.CUENTAS_POR_COBRAR to Icons.Default.RequestQuote,
        NavigationSection.MERMAS to Icons.Default.DeleteOutline,
        NavigationSection.REABASTECIMIENTO to Icons.Default.LocalShipping,
        NavigationSection.PROVEEDORES to Icons.Default.Analytics,
        NavigationSection.REPORTES to Icons.Default.Assessment,
        NavigationSection.RESPALDOS to Icons.Default.CloudSync,
        NavigationSection.NOTIFICACIONES to Icons.Default.Notifications
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Drawer Header
        Surface(
            color = NexusDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NexusDark
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "NEXUS ADMIN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Sistema Negocios v1.0",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }

        Divider(color = NexusBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            sectionsWithIcons.forEach { (section, icon) ->
                val isSelected = section == currentSection
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = section.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = section.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else NexusTextSecondary
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        onSectionSelected(section)
                        onClose()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = NexusTextSecondary,
                        unselectedTextColor = NexusTextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }

        Divider(color = NexusBorder)
        PaddingValues(16.dp)
        Text(
            text = "Base de Datos Local Offline (Indexed/Room)",
            style = MaterialTheme.typography.labelSmall.copy(color = NexusTextMuted),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NexusTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NexusTextMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    type: String, // "success", "danger", "warning", "info", "neutral"
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type.lowercase()) {
        "success", "paid", "ganancia", "ingreso", "disponible" -> NexusGreen.copy(alpha = 0.15f) to NexusGreen
        "danger", "agotado", "egreso", "merma", "pending" -> NexusRed.copy(alpha = 0.15f) to NexusRed
        "warning", "bajo", "partial" -> NexusYellow.copy(alpha = 0.15f) to Color(0xFFD97706)
        "info", "tarjeta", "transferencia" -> NexusBlue.copy(alpha = 0.15f) to NexusBlue
        else -> Color(0xFFE2E8F0) to NexusTextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun BarChart7Days(
    salesData: List<Pair<String, Double>>, // Day label to sales amount
    profitData: List<Pair<String, Double>>, // Day label to profit amount
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = "Ventas y Ganancias (Últimos 7 días)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NexusBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ventas", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NexusGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ganancias", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = (salesData.map { it.second } + profitData.map { it.second }).maxOrNull() ?: 100.0
            val maxAmount = if (maxVal <= 0.0) 100.0 else maxVal

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height - 30.dp.toPx()
                    val barGroupWidth = width / salesData.size.coerceAtLeast(1)
                    val barWidth = barGroupWidth * 0.3f

                    // Draw grid lines
                    for (i in 0..3) {
                        val y = height * (i / 3f)
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw bars
                    salesData.forEachIndexed { index, (dayLabel, saleAmt) ->
                        val groupX = index * barGroupWidth + barGroupWidth * 0.15f
                        val saleHeight = ((saleAmt / maxAmount) * height).toFloat()
                        val profitAmt = profitData.getOrNull(index)?.second ?: 0.0
                        val profitHeight = ((profitAmt / maxAmount) * height).toFloat()

                        // Sales Bar
                        drawRoundRect(
                            color = NexusBlue,
                            topLeft = Offset(groupX, height - saleHeight),
                            size = Size(barWidth, saleHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        // Profit Bar
                        drawRoundRect(
                            color = NexusGreen,
                            topLeft = Offset(groupX + barWidth + 4.dp.toPx(), height - profitHeight),
                            size = Size(barWidth, profitHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }

                // X-Axis Day Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    salesData.forEach { (label, _) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NexusTextSecondary,
                                fontSize = 10.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoughnutChart(
    items: List<Pair<String, Double>>, // Category/Type label to value
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.second }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución por Tipo/Categoría",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (total <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin datos suficientes para mostrar gráfico",
                        style = MaterialTheme.typography.bodySmall.copy(color = NexusTextMuted)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            items.forEachIndexed { index, (_, value) ->
                                val sweepAngle = ((value / total) * 360f).toFloat()
                                val color = colors.getOrElse(index) { NexusBlue }
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                            )
                            Text(
                                text = Utils.formatCurrency(total),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        items.forEachIndexed { index, (label, value) ->
                            val color = colors.getOrElse(index) { NexusBlue }
                            val pct = if (total > 0) (value / total) * 100 else 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "%.1f%%".format(pct),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
