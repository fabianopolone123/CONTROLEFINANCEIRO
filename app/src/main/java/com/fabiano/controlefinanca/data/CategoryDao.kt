package com.fabiano.controlefinanca.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    @Query("SELECT name FROM categories WHERE type = :type ORDER BY name COLLATE NOCASE ASC")
    fun observeNamesByType(type: TransactionType): Flow<List<String>>
}
