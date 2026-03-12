package com.fabiano.controlefinanca.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_categories_type_name`
                    ON `categories` (`type`, `name`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (type, name)
                    SELECT type, category
                    FROM transactions
                    WHERE TRIM(category) <> ''
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controle_financa.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
