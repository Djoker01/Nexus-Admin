package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.NexusAdminDatabase
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NavigationSection(val title: String) {
    DASHBOARD("Dashboard"),
    INVENTARIO("Inventario"),
    VENTAS("Ventas"),
    CAJA("Caja"),
    GASTOS("Gastos"),
    CUENTAS_POR_COBRAR("Cuentas por Cobrar"),
    MERMAS("Mermas y Consumo"),
    REABASTECIMIENTO("Reabastecimiento"),
    PROVEEDORES("Análisis de Proveedores"),
    REPORTES("Reportes"),
    RESPALDOS("Respaldos"),
    NOTIFICACIONES("Notificaciones")
}

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexusAdminDatabase.getInstance(application)
    val repository = NexusRepository(db.nexusDao())

    // Active Screen Section
    private val _currentSection = MutableStateFlow(NavigationSection.DASHBOARD)
    val currentSection: StateFlow<NavigationSection> = _currentSection.asStateFlow()

    // Toast message trigger
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Reactive StateFlows from DB
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashMovements: StateFlow<List<CashMovementEntity>> = repository.allCashMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<ClientEntity>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountsReceivable: StateFlow<List<AccountsReceivableEntity>> = repository.allAccountsReceivable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shrinkageList: StateFlow<List<ShrinkageEntity>> = repository.allShrinkage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val restockList: StateFlow<List<RestockEntity>> = repository.allRestock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierQuotes: StateFlow<List<SupplierQuoteEntity>> = repository.allSupplierQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseComparisons: StateFlow<List<PurchaseComparisonEntity>> = repository.allPurchaseComparisons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unread Notifications Count
    val unreadNotificationsCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current Cash Balance
    val currentCashBalance: StateFlow<Double> = cashMovements
        .map { list -> list.firstOrNull()?.balanceAfter ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Sales POS Cart State
    private val _cart = MutableStateFlow<List<NexusRepository.CartItem>>(emptyList())
    val cart: StateFlow<List<NexusRepository.CartItem>> = _cart.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun navigateTo(section: NavigationSection) {
        _currentSection.value = section
    }

    private fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }

    // --- POS CART ACTIONS ---
    fun addToCart(product: ProductEntity) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val existing = currentList[index]
            if (existing.quantity + 1 > product.stock) {
                showToast("Stock máximo alcanzado (${product.stock})")
                return
            }
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            if (product.stock < 1) {
                showToast("Producto sin stock disponible")
                return
            }
            currentList.add(NexusRepository.CartItem(product, 1))
        }
        _cart.value = currentList
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentList[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else if (newQty > item.product.stock) {
                showToast("Stock disponible: ${item.product.stock}")
            } else {
                currentList[index] = item.copy(quantity = newQty)
            }
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun checkoutSale(customerName: String, paymentMethod: String, onSuccess: (Long) -> Unit) {
        if (_cart.value.isEmpty()) {
            showToast("El carrito está vacío")
            return
        }
        viewModelScope.launch {
            try {
                val saleId = repository.registerSale(
                    customerName = customerName,
                    paymentMethod = paymentMethod,
                    cartItems = _cart.value
                )
                clearCart()
                showToast("¡Venta #$saleId registrada con éxito!")
                onSuccess(saleId)
            } catch (e: Exception) {
                showToast("Error al registrar venta: ${e.message}")
            }
        }
    }

    // --- PRODUCT CRUD ---
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
                showToast("Producto creado correctamente")
            } else {
                repository.updateProduct(product)
                showToast("Producto actualizado")
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showToast("Producto eliminado")
        }
    }

    // --- CATEGORY CRUD ---
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(name.trim())
            showToast("Categoría agregada")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showToast("Categoría eliminada")
        }
    }

    // --- CASH MOVEMENT ---
    fun addManualCashMovement(type: String, amount: Double, concept: String, description: String) {
        if (amount <= 0) {
            showToast("Ingrese un monto válido")
            return
        }
        viewModelScope.launch {
            repository.addCashMovement(type, amount, concept, description)
            showToast("Movimiento de caja registrado")
        }
    }

    // --- EXPENSES ---
    fun registerExpense(
        category: String,
        amount: Double,
        description: String,
        receipt: String,
        notes: String,
        isFixed: Boolean,
        deductFromCash: Boolean
    ) {
        if (amount <= 0) {
            showToast("Ingrese un monto válido")
            return
        }
        viewModelScope.launch {
            repository.registerExpense(category, amount, description, receipt, notes, isFixed, deductFromCash)
            showToast("Gasto registrado")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            showToast("Gasto eliminado")
        }
    }

    // --- CLIENTS & ACCOUNTS RECEIVABLE ---
    fun saveClient(client: ClientEntity) {
        viewModelScope.launch {
            if (client.id == 0L) {
                repository.insertClient(client)
                showToast("Cliente agregado")
            } else {
                repository.updateClient(client)
                showToast("Cliente actualizado")
            }
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client)
            showToast("Cliente eliminado")
        }
    }

    fun registerAccountReceivable(clientId: Long, clientName: String, concept: String, total: Double, notes: String) {
        if (total <= 0) {
            showToast("Monto debe ser mayor a 0")
            return
        }
        viewModelScope.launch {
            repository.registerAccountReceivable(clientId, clientName, concept, total, notes)
            showToast("Cuenta por cobrar registrada")
        }
    }

    fun registerPayment(accountId: Long, clientId: Long, amount: Double, method: String, notes: String) {
        if (amount <= 0) {
            showToast("Monto debe ser mayor a 0")
            return
        }
        viewModelScope.launch {
            repository.registerPayment(accountId, clientId, amount, method, notes)
            showToast("Abono/Pago registrado con éxito")
        }
    }

    // --- MERMAS / SHRINKAGE ---
    fun registerShrinkage(product: ProductEntity, quantity: Int, type: String, reason: String, notes: String) {
        if (quantity <= 0) {
            showToast("Cantidad debe ser mayor a 0")
            return
        }
        if (quantity > product.stock) {
            showToast("No puede descontar más del stock actual (${product.stock})")
            return
        }
        viewModelScope.launch {
            repository.registerShrinkage(product, quantity, type, reason, notes)
            showToast("Merma/Consumo registrado")
        }
    }

    // --- REABASTECIMIENTO / RESTOCK ---
    fun registerRestock(
        supplierId: Long,
        supplierName: String,
        invoice: String,
        notes: String,
        items: List<NexusRepository.RestockItemInput>,
        deductFromCash: Boolean
    ) {
        if (items.isEmpty()) {
            showToast("Seleccione al menos un producto para reabastecer")
            return
        }
        val totalCost = items.sumOf { it.newUnitCost * it.quantity }
        if (deductFromCash && totalCost > currentCashBalance.value) {
            showToast("¡Advertencia! El costo ($totalCost) excede el saldo en caja (${currentCashBalance.value})")
        }
        viewModelScope.launch {
            repository.registerRestock(supplierId, supplierName, invoice, notes, items, deductFromCash)
            showToast("Reabastecimiento registrado y stock actualizado")
        }
    }

    // --- SUPPLIERS & QUOTES ---
    fun saveSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            if (supplier.id == 0L) {
                repository.insertSupplier(supplier)
                showToast("Proveedor guardado")
            } else {
                repository.updateSupplier(supplier)
                showToast("Proveedor actualizado")
            }
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            showToast("Proveedor eliminado")
        }
    }

    fun addSupplierQuote(quote: SupplierQuoteEntity) {
        viewModelScope.launch {
            repository.insertSupplierQuote(quote)
            showToast("Cotización agregada")
        }
    }

    fun deleteSupplierQuote(quote: SupplierQuoteEntity) {
        viewModelScope.launch {
            repository.deleteSupplierQuote(quote)
            showToast("Cotización eliminada")
        }
    }

    fun savePurchaseComparison(name: String, summaryJson: String) {
        viewModelScope.launch {
            repository.insertPurchaseComparison(
                PurchaseComparisonEntity(
                    name = name,
                    date = System.currentTimeMillis(),
                    summaryJson = summaryJson
                )
            )
            showToast("Análisis guardado")
        }
    }

    fun deletePurchaseComparison(comparison: PurchaseComparisonEntity) {
        viewModelScope.launch {
            repository.deletePurchaseComparison(comparison)
            showToast("Análisis eliminado")
        }
    }

    // --- NOTIFICATIONS ---
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            showToast("Notificaciones limpiadas")
        }
    }

    // --- BACKUP & RESET ---
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showToast("Se han eliminado todos los datos de la aplicación")
        }
    }
}
