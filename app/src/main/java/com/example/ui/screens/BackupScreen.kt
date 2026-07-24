package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(viewModel: NexusViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val cashMovements by viewModel.cashMovements.collectAsState()
    val clients by viewModel.clients.collectAsState()

    var autoBackupEnabled by remember { mutableStateOf(true) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val jsonStr = inputStream?.bufferedReader().use { it?.readText() }
                    if (!jsonStr.isNullOrBlank()) {
                        val success = viewModel.repository.importFromJson(jsonStr)
                        if (success) {
                            Toast.makeText(context, "¡Base de datos restaurada correctamente!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Error al procesar archivo de respaldo", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- STORAGE USAGE BAR ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Almacenamiento Local (Room / IndexedDB)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Salud: Óptima", style = MaterialTheme.typography.labelSmall.copy(color = NexusGreen, fontWeight = FontWeight.Bold))
                }

                val totalRecords = products.size + sales.size + cashMovements.size + clients.size
                LinearProgressIndicator(
                    progress = (totalRecords / 1000f).coerceIn(0.05f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = NexusBlue,
                    trackColor = Color(0xFFE2E8F0)
                )

                Text(
                    text = "$totalRecords registros guardados en la base de datos local",
                    style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                )
            }
        }

        // --- EXPORT & IMPORT ACTIONS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Copia de Seguridad en JSON", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Exporte todos sus productos, ventas, movimientos de caja, clientes y gastos a un archivo de respaldo seguro para compartir o almacenar externamente.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NexusTextSecondary)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoBackupEnabled, onCheckedChange = { autoBackupEnabled = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Respaldo automático local activo", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val json = viewModel.repository.exportFullBackupJson()
                            val filename = "nexus_admin_backup_${System.currentTimeMillis()}.json"
                            Utils.shareTextFile(context, filename, json, "application/json")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Copia de Seguridad (JSON)")
                }

                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar desde Archivo JSON")
                }
            }
        }

        // --- RESET DATA DANGER ZONE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Zona de Peligro", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NexusRed))
                Text(
                    "Esta acción eliminará permanentemente todas las ventas, productos, clientes y movimientos de caja de la base de datos local.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NexusRed.copy(alpha = 0.8f))
                )

                Button(
                    onClick = { showClearDataConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusRed)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reiniciar / Borrar Todos los Datos")
                }
            }
        }
    }

    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            title = { Text("¿Eliminar todos los datos?", fontWeight = FontWeight.Bold, color = NexusRed) },
            text = {
                Text("¿Está completamente seguro? Se borrarán todos los productos, ventas, registros de caja e historial sin posibilidad de recuperación a menos que tenga un respaldo previo.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusRed)
                ) {
                    Text("Sí, Borrar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
