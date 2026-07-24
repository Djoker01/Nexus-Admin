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
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationSection
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@Composable
fun NotificationsScreen(
    viewModel: NexusViewModel,
    onNavigate: (NavigationSection) -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${notifications.size} Notificaciones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            if (notifications.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllNotifications() }) {
                    Text("Limpiar Todo", color = NexusRed)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(56.dp), tint = NexusTextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay notificaciones sin leer", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextSecondary))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.read) Color(0xFFFEF3C7).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
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
                                    StatusBadge(
                                        text = notif.type.uppercase(),
                                        type = when (notif.type) {
                                            "stock_bajo" -> "warning"
                                            "agotado" -> "danger"
                                            else -> "info"
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(notif.message, style = MaterialTheme.typography.bodySmall)
                                Text(Utils.formatDateTime(notif.createdAt), style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary))

                                if (notif.type == "stock_bajo" || notif.type == "agotado") {
                                    TextButton(
                                        onClick = { onNavigate(NavigationSection.REABASTECIMIENTO) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Ir a Reabastecimiento →", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = NexusBlue))
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (!notif.read) {
                                    IconButton(onClick = { viewModel.markNotificationAsRead(notif.id) }) {
                                        Icon(Icons.Default.Done, contentDescription = "Marcar como leída", tint = NexusGreen)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteNotification(notif.id) }) {
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
