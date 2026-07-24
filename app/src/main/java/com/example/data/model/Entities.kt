package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val category: String,
    val supplier: String,
    val cost: Double,
    val price: Double,
    val stock: Int,
    val minStock: Int,
    val description: String = "",
    val status: String = "activo", // "activo", "inactivo"
    val profit: Double = price - cost,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val customer: String = "Público General",
    val paymentMethod: String = "efectivo", // "efectivo", "tarjeta", "transferencia"
    val itemsCount: Int,
    val total: Double,
    val cost: Double,
    val profit: Double,
    val status: String = "completada"
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double
)

@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val type: String, // "ingreso", "egreso"
    val amount: Double,
    val concept: String,
    val description: String = "",
    val balanceAfter: Double
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // servicios, alquiler, salarios, insumos, marketing, transporte, impuestos, mantenimiento, seguros, otros
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val receipt: String = "",
    val notes: String = "",
    val isFixed: Boolean = false,
    val status: String = "registrado"
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val status: String = "activo",
    val totalDebt: Double = 0.0
)

@Entity(tableName = "accounts_receivable")
data class AccountsReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val clientName: String,
    val date: Long = System.currentTimeMillis(),
    val concept: String,
    val total: Double,
    val notes: String = "",
    val status: String = "pending" // "pending", "partial", "paid"
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val clientId: Long,
    val date: Long = System.currentTimeMillis(),
    val amount: Double,
    val method: String = "efectivo",
    val notes: String = ""
)

@Entity(tableName = "shrinkage")
data class ShrinkageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val cost: Double,
    val totalLoss: Double = quantity * cost,
    val type: String, // "merma", "consumo", "caducidad", "robo", "error", "otro"
    val reason: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "restock")
data class RestockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val date: Long = System.currentTimeMillis(),
    val invoice: String = "",
    val totalItems: Int,
    val totalCost: Double,
    val notes: String = ""
)

@Entity(tableName = "restock_items")
data class RestockItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restockId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val subtotal: Double
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val contactPerson: String = "",
    val company: String = "",
    val deliveryDays: Int = 1,
    val paymentTerms: String = "Contado",
    val notes: String = "",
    val status: String = "activo"
)

@Entity(tableName = "supplier_quotes")
data class SupplierQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val supplierId: Long,
    val supplierName: String,
    val quotedPrice: Double,
    val minQuantity: Int = 1,
    val deliveryDays: Int = 1,
    val paymentTerms: String = "Contado",
    val includesShipping: Boolean = true,
    val date: Long = System.currentTimeMillis(),
    val status: String = "activa"
)

@Entity(tableName = "purchase_comparisons")
data class PurchaseComparisonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: Long = System.currentTimeMillis(),
    val summaryJson: String = "",
    val status: String = "guardado"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "warning", "danger", "info"
    val title: String,
    val message: String,
    val link: String = "",
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
