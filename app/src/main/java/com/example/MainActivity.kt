package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NexusDrawerContent
import com.example.ui.components.NexusHeader
import com.example.ui.screens.*
import com.example.ui.theme.NexusAdminTheme
import com.example.ui.viewmodel.NavigationSection
import com.example.ui.viewmodel.NexusViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusAdminTheme {
                NexusAdminApp()
            }
        }
    }
}

@Composable
fun NexusAdminApp(viewModel: NexusViewModel = viewModel()) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentSection by viewModel.currentSection.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()

    // Listen to Toast message events
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NexusDrawerContent(
                    currentSection = currentSection,
                    onSectionSelected = { section ->
                        viewModel.navigateTo(section)
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                NexusHeader(
                    currentSection = currentSection,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onNotificationClick = {
                        viewModel.navigateTo(NavigationSection.NOTIFICACIONES)
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentSection) {
                    NavigationSection.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { sec -> viewModel.navigateTo(sec) }
                    )
                    NavigationSection.INVENTARIO -> InventoryScreen(viewModel = viewModel)
                    NavigationSection.VENTAS -> SalesScreen(viewModel = viewModel)
                    NavigationSection.CAJA -> CashScreen(viewModel = viewModel)
                    NavigationSection.GASTOS -> ExpensesScreen(viewModel = viewModel)
                    NavigationSection.CUENTAS_POR_COBRAR -> AccountsReceivableScreen(viewModel = viewModel)
                    NavigationSection.MERMAS -> ShrinkageScreen(viewModel = viewModel)
                    NavigationSection.REABASTECIMIENTO -> RestockScreen(viewModel = viewModel)
                    NavigationSection.PROVEEDORES -> SupplierAnalysisScreen(viewModel = viewModel)
                    NavigationSection.REPORTES -> ReportsScreen(viewModel = viewModel)
                    NavigationSection.RESPALDOS -> BackupScreen(viewModel = viewModel)
                    NavigationSection.NOTIFICACIONES -> NotificationsScreen(
                        viewModel = viewModel,
                        onNavigate = { sec -> viewModel.navigateTo(sec) }
                    )
                }
            }
        }
    }
}
