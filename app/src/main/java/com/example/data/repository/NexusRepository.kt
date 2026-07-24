package com.example.data.repository

import com.example.data.dao.NexusDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class NexusRepository(private val dao: NexusDao) {

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allSales: Flow<List<SaleEntity>> = dao.getAllSales()
    val allCashMovements: Flow<List<CashMovementEntity>> = dao.getAllCashMovements()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val allClients: Flow<List<ClientEntity>> = dao.getAllClients()
    val allAccountsReceivable: Flow<List<AccountsReceivableEntity>> = dao.getAllAccountsReceivable()
    val allShrinkage: Flow<List<ShrinkageEntity>> = dao.getAllShrinkage()
    val allRestock: Flow<List<RestockEntity>> = dao.getAllRestock()
    val allSuppliers: Flow<List<SupplierEntity>> = dao.getAllSuppliers()
    val allSupplierQuotes: Flow<List<SupplierQuoteEntity>> = dao.getAllSupplierQuotes()
    val allPurchaseComparisons: Flow<List<PurchaseComparisonEntity>> = dao.getAllPurchaseComparisons()
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val allSettings: Flow<List<SettingEntity>> = dao.getAllSettings()

    // --- PRODUCT ACTIONS ---
    suspend fun insertProduct(product: ProductEntity): Long {
        val id = dao.insertProduct(product)
        checkAndNotifyLowStock(product.copy(id = id))
        return id
    }

    suspend fun updateProduct(product: ProductEntity) {
        dao.updateProduct(product)
        checkAndNotifyLowStock(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
    }

    // --- CATEGORY ACTIONS ---
    suspend fun insertCategory(name: String): Long {
        return dao.insertCategory(CategoryEntity(name = name))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        dao.deleteCategory(category)
    }

    // --- SALE REGISTRATION (CORE LOGIC) ---
    data class CartItem(
        val product: ProductEntity,
        val quantity: Int
    )

    suspend fun registerSale(
        customerName: String,
        paymentMethod: String,
        cartItems: List<CartItem>
    ): Long {
        var totalAmount = 0.0
        var totalCost = 0.0
        var totalItemsCount = 0

        cartItems.forEach { item ->
            totalAmount += item.product.price * item.quantity
            totalCost += item.product.cost * item.quantity
            totalItemsCount += item.quantity
        }

        val profit = totalAmount - totalCost

        val sale = SaleEntity(
            date = System.currentTimeMillis(),
            customer = customerName.ifBlank { "Público General" },
            paymentMethod = paymentMethod,
            itemsCount = totalItemsCount,
            total = totalAmount,
            cost = totalCost,
            profit = profit,
            status = "completada"
        )

        val saleId = dao.insertSale(sale)

        // Insert Sale Items & Deduct Stock
        cartItems.forEach { item ->
            val subtotal = item.product.price * item.quantity
            dao.insertSaleItem(
                SaleItemEntity(
                    saleId = saleId,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unitPrice = item.product.price,
                    unitCost = item.product.cost,
                    subtotal = subtotal
                )
            )

            // Subtract stock
            dao.updateStock(item.product.id, -item.quantity)

            // Re-fetch product to verify low stock
            val updated = dao.getProductById(item.product.id)
            if (updated != null) {
                checkAndNotifyLowStock(updated)
            }
        }

        // Cash Integration if cash
        if (paymentMethod.lowercase() == "efectivo") {
            val lastBalance = dao.getLastCashBalance() ?: 0.0
            val newBalance = lastBalance + totalAmount
            dao.insertCashMovement(
                CashMovementEntity(
                    date = System.currentTimeMillis(),
                    type = "ingreso",
                    amount = totalAmount,
                    concept = "Venta #$saleId",
                    description = "Cliente: ${sale.customer}",
                    balanceAfter = newBalance
                )
            )
        }

        return saleId
    }

    // --- CASH MOVEMENTS ---
    suspend fun addCashMovement(type: String, amount: Double, concept: String, description: String): Long {
        val lastBalance = dao.getLastCashBalance() ?: 0.0
        val newBalance = if (type == "ingreso") lastBalance + amount else lastBalance - amount
        return dao.insertCashMovement(
            CashMovementEntity(
                date = System.currentTimeMillis(),
                type = type,
                amount = amount,
                concept = concept,
                description = description,
                balanceAfter = newBalance
            )
        )
    }

    // --- EXPENSES ---
    suspend fun registerExpense(
        category: String,
        amount: Double,
        description: String,
        receipt: String,
        notes: String,
        isFixed: Boolean,
        deductFromCash: Boolean
    ): Long {
        val expenseId = dao.insertExpense(
            ExpenseEntity(
                category = category,
                amount = amount,
                date = System.currentTimeMillis(),
                description = description,
                receipt = receipt,
                notes = notes,
                isFixed = isFixed,
                status = "registrado"
            )
        )

        if (deductFromCash) {
            addCashMovement(
                type = "egreso",
                amount = amount,
                concept = "Gasto: $category",
                description = description
            )
        }

        return expenseId
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        dao.deleteExpense(expense)
    }

    // --- CLIENTS & ACCOUNTS RECEIVABLE ---
    suspend fun insertClient(client: ClientEntity): Long = dao.insertClient(client)
    suspend fun updateClient(client: ClientEntity) = dao.updateClient(client)
    suspend fun deleteClient(client: ClientEntity) = dao.deleteClient(client)

    suspend fun registerAccountReceivable(
        clientId: Long,
        clientName: String,
        concept: String,
        total: Double,
        notes: String
    ): Long {
        val id = dao.insertAccountReceivable(
            AccountsReceivableEntity(
                clientId = clientId,
                clientName = clientName,
                date = System.currentTimeMillis(),
                concept = concept,
                total = total,
                notes = notes,
                status = "pending"
            )
        )
        dao.updateClientDebt(clientId, total)
        return id
    }

    suspend fun registerPayment(
        accountId: Long,
        clientId: Long,
        amount: Double,
        method: String,
        notes: String
    ): Long {
        val paymentId = dao.insertPayment(
            PaymentEntity(
                accountId = accountId,
                clientId = clientId,
                date = System.currentTimeMillis(),
                amount = amount,
                method = method,
                notes = notes
            )
        )

        val account = dao.getAccountReceivableById(accountId)
        if (account != null) {
            // Check total paid for this account
            val payments = dao.getPaymentsByAccount(accountId).first()
            val totalPaid = payments.sumOf { it.amount }

            val newStatus = when {
                totalPaid >= account.total -> "paid"
                totalPaid > 0 -> "partial"
                else -> "pending"
            }

            dao.updateAccountReceivable(account.copy(status = newStatus))
            dao.updateClientDebt(clientId, -amount)
        }

        if (method.lowercase() == "efectivo") {
            addCashMovement(
                type = "ingreso",
                amount = amount,
                concept = "Abono Cuenta #$accountId",
                description = "Cliente ID: $clientId"
            )
        }

        return paymentId
    }

    // --- SHRINKAGE / MERMAS ---
    suspend fun registerShrinkage(
        product: ProductEntity,
        quantity: Int,
        type: String,
        reason: String,
        notes: String
    ): Long {
        val totalLoss = quantity * product.cost
        val id = dao.insertShrinkage(
            ShrinkageEntity(
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                cost = product.cost,
                totalLoss = totalLoss,
                type = type,
                reason = reason,
                date = System.currentTimeMillis(),
                notes = notes
            )
        )

        // Deduct stock
        dao.updateStock(product.id, -quantity)
        val updated = dao.getProductById(product.id)
        if (updated != null) {
            checkAndNotifyLowStock(updated)
        }

        return id
    }

    // --- RESTOCK / REABASTECIMIENTO ---
    data class RestockItemInput(
        val product: ProductEntity,
        val quantity: Int,
        val newUnitCost: Double
    )

    suspend fun registerRestock(
        supplierId: Long,
        supplierName: String,
        invoice: String,
        notes: String,
        items: List<RestockItemInput>,
        deductFromCash: Boolean
    ): Long {
        var totalCost = 0.0
        var totalItems = 0
        items.forEach { item ->
            totalCost += item.newUnitCost * item.quantity
            totalItems += item.quantity
        }

        val restockId = dao.insertRestock(
            RestockEntity(
                supplierId = supplierId,
                supplierName = supplierName,
                date = System.currentTimeMillis(),
                invoice = invoice,
                totalItems = totalItems,
                totalCost = totalCost,
                notes = notes
            )
        )

        items.forEach { item ->
            val subtotal = item.newUnitCost * item.quantity
            dao.insertRestockItem(
                RestockItemEntity(
                    restockId = restockId,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unitCost = item.newUnitCost,
                    subtotal = subtotal
                )
            )

            // Update product stock & cost
            dao.updateStockAndCost(item.product.id, item.quantity, item.newUnitCost)
        }

        if (deductFromCash) {
            addCashMovement(
                type = "egreso",
                amount = totalCost,
                concept = "Reabastecimiento #$restockId",
                description = "Proveedor: $supplierName"
            )
        }

        return restockId
    }

    // --- SUPPLIERS & QUOTES ---
    suspend fun insertSupplier(supplier: SupplierEntity) = dao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: SupplierEntity) = dao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: SupplierEntity) = dao.deleteSupplier(supplier)

    suspend fun insertSupplierQuote(quote: SupplierQuoteEntity) = dao.insertSupplierQuote(quote)
    suspend fun deleteSupplierQuote(quote: SupplierQuoteEntity) = dao.deleteSupplierQuote(quote)

    suspend fun insertPurchaseComparison(comparison: PurchaseComparisonEntity) = dao.insertPurchaseComparison(comparison)
    suspend fun deletePurchaseComparison(comparison: PurchaseComparisonEntity) = dao.deletePurchaseComparison(comparison)

    // --- NOTIFICATIONS ---
    suspend fun markNotificationAsRead(id: Long) = dao.markNotificationAsRead(id)
    suspend fun deleteNotification(id: Long) = dao.deleteNotification(id)
    suspend fun clearAllNotifications() = dao.clearAllNotifications()

    private suspend fun checkAndNotifyLowStock(product: ProductEntity) {
        if (product.stock <= product.minStock) {
            val isOutOfStock = product.stock <= 0
            val type = if (isOutOfStock) "danger" else "warning"
            val title = if (isOutOfStock) "Producto Agotado: ${product.name}" else "Stock Bajo: ${product.name}"
            val msg = if (isOutOfStock) "El producto '${product.name}' se ha quedado sin stock."
            else "El stock actual (${product.stock}) es menor o igual al mínimo (${product.minStock})."

            dao.insertNotification(
                NotificationEntity(
                    type = type,
                    title = title,
                    message = msg,
                    link = "inventario",
                    read = false
                )
            )
        }
    }

    // --- SEED INITIAL DATA IF EMPTY ---
    suspend fun seedInitialDataIfEmpty() {
        val existingProducts = dao.getAllProducts().first()
        if (existingProducts.isNotEmpty()) return

        // 1. Categories
        dao.insertCategory(CategoryEntity(name = "Bebidas"))
        dao.insertCategory(CategoryEntity(name = "Abarrotes"))
        dao.insertCategory(CategoryEntity(name = "Lácteos"))
        dao.insertCategory(CategoryEntity(name = "Limpieza"))
        dao.insertCategory(CategoryEntity(name = "Snacks"))

        // 2. Suppliers
        val sup1 = dao.insertSupplier(SupplierEntity(name = "Distribuidora del Norte", phone = "555-1234", company = "DisNorte S.A.", contactPerson = "Carlos Mendoza"))
        val sup2 = dao.insertSupplier(SupplierEntity(name = "Comercializadora Central", phone = "555-5678", company = "Central MKT", contactPerson = "Ana López"))
        val sup3 = dao.insertSupplier(SupplierEntity(name = "Proveedor Express", phone = "555-9012", company = "Express Cargo", contactPerson = "Jorge Ramos"))

        // 3. Products
        val p1 = dao.insertProduct(ProductEntity(name = "Refresco Cola 600ml", code = "75010001", category = "Bebidas", supplier = "Distribuidora del Norte", cost = 12.50, price = 18.00, stock = 45, minStock = 10, description = "Botella PET 600ml"))
        val p2 = dao.insertProduct(ProductEntity(name = "Agua Purificada 1L", code = "75010002", category = "Bebidas", supplier = "Distribuidora del Norte", cost = 6.00, price = 12.00, stock = 8, minStock = 15, description = "Agua de manantial 1 Litro")) // low stock!
        val p3 = dao.insertProduct(ProductEntity(name = "Leche Entera 1L", code = "75010003", category = "Lácteos", supplier = "Comercializadora Central", cost = 19.00, price = 26.50, stock = 22, minStock = 8, description = "Tetra Pak 1L"))
        val p4 = dao.insertProduct(ProductEntity(name = "Aceite Vegetal 900ml", code = "75010004", category = "Abarrotes", supplier = "Comercializadora Central", cost = 32.00, price = 45.00, stock = 14, minStock = 5, description = "100% puro de soya"))
        val p5 = dao.insertProduct(ProductEntity(name = "Detergente en Polvo 1kg", code = "75010005", category = "Limpieza", supplier = "Proveedor Express", cost = 28.00, price = 39.00, stock = 0, minStock = 6, description = "Multiusos aroma fresco")) // out of stock!
        val p6 = dao.insertProduct(ProductEntity(name = "Papas Fritas Saladas 150g", code = "75010006", category = "Snacks", supplier = "Proveedor Express", cost = 14.00, price = 22.00, stock = 30, minStock = 10, description = "Bolsa familiar"))

        // 4. Initial Cash Balance
        dao.insertCashMovement(CashMovementEntity(type = "ingreso", amount = 1500.0, concept = "Fondo Inicial de Caja", description = "Apertura de caja", balanceAfter = 1500.0))

        // 5. Initial Clients
        val c1 = dao.insertClient(ClientEntity(name = "Abarrotes Doña María", phone = "555-4321", address = "Calle Hidalgo #12", totalDebt = 450.0))
        val c2 = dao.insertClient(ClientEntity(name = "Restaurante El Sabor", phone = "555-8765", address = "Av. Juárez #45", totalDebt = 0.0))

        // 6. Account Receivable
        dao.insertAccountReceivable(AccountsReceivableEntity(clientId = c1, clientName = "Abarrotes Doña María", concept = "Mercancía Fiada de la Semana", total = 450.0, notes = "Promesa de pago el viernes", status = "pending"))

        // 7. Initial Expense
        dao.insertExpense(ExpenseEntity(category = "servicios", amount = 350.0, description = "Pago de servicio de internet", isFixed = true))

        // 8. Initial Supplier Quotes
        dao.insertSupplierQuote(SupplierQuoteEntity(productId = p1, productName = "Refresco Cola 600ml", supplierId = sup1, supplierName = "Distribuidora del Norte", quotedPrice = 12.50, minQuantity = 24, deliveryDays = 1, includesShipping = true))
        dao.insertSupplierQuote(SupplierQuoteEntity(productId = p1, productName = "Refresco Cola 600ml", supplierId = sup2, supplierName = "Comercializadora Central", quotedPrice = 11.80, minQuantity = 48, deliveryDays = 2, includesShipping = false))

        // 9. Low stock notifications trigger
        val prod2 = dao.getProductById(p2)
        if (prod2 != null) checkAndNotifyLowStock(prod2)
        val prod5 = dao.getProductById(p5)
        if (prod5 != null) checkAndNotifyLowStock(prod5)
    }

    // --- BACKUP EXPORT & IMPORT ---
    suspend fun exportFullBackupJson(): String {
        val root = JSONObject()

        val productsJson = JSONArray()
        dao.getAllProducts().first().forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("code", p.code)
            obj.put("category", p.category)
            obj.put("supplier", p.supplier)
            obj.put("cost", p.cost)
            obj.put("price", p.price)
            obj.put("stock", p.stock)
            obj.put("minStock", p.minStock)
            obj.put("description", p.description)
            obj.put("status", p.status)
            obj.put("profit", p.profit)
            productsJson.put(obj)
        }
        root.put("products", productsJson)

        val categoriesJson = JSONArray()
        dao.getAllCategories().first().forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            categoriesJson.put(obj)
        }
        root.put("categories", categoriesJson)

        val clientsJson = JSONArray()
        dao.getAllClients().first().forEach { cl ->
            val obj = JSONObject()
            obj.put("id", cl.id)
            obj.put("name", cl.name)
            obj.put("phone", cl.phone)
            obj.put("email", cl.email)
            obj.put("address", cl.address)
            obj.put("notes", cl.notes)
            obj.put("status", cl.status)
            obj.put("totalDebt", cl.totalDebt)
            clientsJson.put(obj)
        }
        root.put("clients", clientsJson)

        root.put("exportDate", System.currentTimeMillis())
        root.put("version", 6)
        return root.toString(2)
    }

    suspend fun importFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (root.has("products")) {
                val array = root.getJSONArray("products")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val prod = ProductEntity(
                        name = obj.optString("name", ""),
                        code = obj.optString("code", ""),
                        category = obj.optString("category", "General"),
                        supplier = obj.optString("supplier", "General"),
                        cost = obj.optDouble("cost", 0.0),
                        price = obj.optDouble("price", 0.0),
                        stock = obj.optInt("stock", 0),
                        minStock = obj.optInt("minStock", 5),
                        description = obj.optString("description", ""),
                        status = obj.optString("status", "activo"),
                        profit = obj.optDouble("profit", 0.0)
                    )
                    dao.insertProduct(prod)
                }
            }
            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dao.insertCategory(CategoryEntity(name = obj.optString("name", "")))
                }
            }
            if (root.has("clients")) {
                val array = root.getJSONArray("clients")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dao.insertClient(
                        ClientEntity(
                            name = obj.optString("name", ""),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            address = obj.optString("address", ""),
                            notes = obj.optString("notes", ""),
                            status = obj.optString("status", "activo"),
                            totalDebt = obj.optDouble("totalDebt", 0.0)
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllData() {
        dao.clearProducts()
        dao.clearCategories()
        dao.clearSales()
        dao.clearSaleItems()
        dao.clearCashMovements()
        dao.clearExpenses()
        dao.clearClients()
        dao.clearAccountsReceivable()
        dao.clearPayments()
        dao.clearShrinkage()
        dao.clearRestock()
        dao.clearRestockItems()
        dao.clearSuppliers()
        dao.clearSupplierQuotes()
        dao.clearPurchaseComparisons()
        dao.clearNotifications()
        dao.clearSettings()
    }
}
