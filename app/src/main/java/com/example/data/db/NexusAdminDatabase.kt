package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.NexusDao
import com.example.data.model.*

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        CashMovementEntity::class,
        ExpenseEntity::class,
        ClientEntity::class,
        AccountsReceivableEntity::class,
        PaymentEntity::class,
        ShrinkageEntity::class,
        RestockEntity::class,
        RestockItemEntity::class,
        SupplierEntity::class,
        SupplierQuoteEntity::class,
        PurchaseComparisonEntity::class,
        NotificationEntity::class,
        SettingEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class NexusAdminDatabase : RoomDatabase() {

    abstract fun nexusDao(): NexusDao

    companion object {
        @Volatile
        private var INSTANCE: NexusAdminDatabase? = null

        fun getInstance(context: Context): NexusAdminDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusAdminDatabase::class.java,
                    "NexusAdminDB"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
