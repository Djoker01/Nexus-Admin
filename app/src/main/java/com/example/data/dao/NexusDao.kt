package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NexusDao {

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = stock + :delta, updatedAt = :now WHERE id = :id")
    suspend fun updateStock(id: Long, delta: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = stock + :delta, cost = :newCost, updatedAt = :now WHERE id = :id")
    suspend fun updateStockAndCost(id: Long, delta: Int, newCost: Double, now: Long = System.currentTimeMillis())

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // --- SALES ---
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItemsBySaleId(saleId: Long): Flow<List<SaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(item: SaleItemEntity): Long

    // --- CASH MOVEMENTS ---
    @Query("SELECT * FROM cash_movements ORDER BY date DESC")
    fun getAllCashMovements(): Flow<List<CashMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovement(movement: CashMovementEntity): Long

    @Query("SELECT balanceAfter FROM cash_movements ORDER BY id DESC LIMIT 1")
    suspend fun getLastCashBalance(): Double?

    // --- EXPENSES ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // --- CLIENTS ---
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("UPDATE clients SET totalDebt = totalDebt + :delta WHERE id = :clientId")
    suspend fun updateClientDebt(clientId: Long, delta: Double)

    // --- ACCOUNTS RECEIVABLE ---
    @Query("SELECT * FROM accounts_receivable ORDER BY date DESC")
    fun getAllAccountsReceivable(): Flow<List<AccountsReceivableEntity>>

    @Query("SELECT * FROM accounts_receivable WHERE id = :id LIMIT 1")
    suspend fun getAccountReceivableById(id: Long): AccountsReceivableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountReceivable(account: AccountsReceivableEntity): Long

    @Update
    suspend fun updateAccountReceivable(account: AccountsReceivableEntity)

    // --- PAYMENTS ---
    @Query("SELECT * FROM payments WHERE accountId = :accountId ORDER BY date DESC")
    fun getPaymentsByAccount(accountId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    // --- SHRINKAGE ---
    @Query("SELECT * FROM shrinkage ORDER BY date DESC")
    fun getAllShrinkage(): Flow<List<ShrinkageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShrinkage(shrinkage: ShrinkageEntity): Long

    // --- RESTOCK ---
    @Query("SELECT * FROM restock ORDER BY date DESC")
    fun getAllRestock(): Flow<List<RestockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestock(restock: RestockEntity): Long

    @Query("SELECT * FROM restock_items WHERE restockId = :restockId")
    fun getRestockItems(restockId: Long): Flow<List<RestockItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestockItem(item: RestockItemEntity): Long

    // --- SUPPLIERS ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    // --- SUPPLIER QUOTES ---
    @Query("SELECT * FROM supplier_quotes ORDER BY date DESC")
    fun getAllSupplierQuotes(): Flow<List<SupplierQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierQuote(quote: SupplierQuoteEntity): Long

    @Delete
    suspend fun deleteSupplierQuote(quote: SupplierQuoteEntity)

    // --- PURCHASE COMPARISONS ---
    @Query("SELECT * FROM purchase_comparisons ORDER BY date DESC")
    fun getAllPurchaseComparisons(): Flow<List<PurchaseComparisonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseComparison(comparison: PurchaseComparisonEntity): Long

    @Delete
    suspend fun deletePurchaseComparison(comparison: PurchaseComparisonEntity)

    // --- NOTIFICATIONS ---
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    // --- SETTINGS ---
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingEntity)

    // CLEAR ALL DATA FOR RESTORE/RESET
    @Query("DELETE FROM products") suspend fun clearProducts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM sales") suspend fun clearSales()
    @Query("DELETE FROM sale_items") suspend fun clearSaleItems()
    @Query("DELETE FROM cash_movements") suspend fun clearCashMovements()
    @Query("DELETE FROM expenses") suspend fun clearExpenses()
    @Query("DELETE FROM clients") suspend fun clearClients()
    @Query("DELETE FROM accounts_receivable") suspend fun clearAccountsReceivable()
    @Query("DELETE FROM payments") suspend fun clearPayments()
    @Query("DELETE FROM shrinkage") suspend fun clearShrinkage()
    @Query("DELETE FROM restock") suspend fun clearRestock()
    @Query("DELETE FROM restock_items") suspend fun clearRestockItems()
    @Query("DELETE FROM suppliers") suspend fun clearSuppliers()
    @Query("DELETE FROM supplier_quotes") suspend fun clearSupplierQuotes()
    @Query("DELETE FROM purchase_comparisons") suspend fun clearPurchaseComparisons()
    @Query("DELETE FROM notifications") suspend fun clearNotifications()
    @Query("DELETE FROM settings") suspend fun clearSettings()
}
