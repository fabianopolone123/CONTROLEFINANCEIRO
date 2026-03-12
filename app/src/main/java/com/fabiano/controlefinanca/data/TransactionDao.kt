package com.fabiano.controlefinanca.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT 
            SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS income,
            SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense
        FROM transactions
        """
    )
    fun observeSummary(): Flow<SummaryRow>

    @Query(
        """
        SELECT category, SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
        GROUP BY category
        ORDER BY total DESC
        LIMIT 6
        """
    )
    fun observeExpenseByCategory(): Flow<List<CategoryTotalRow>>

    @Query(
        """
        SELECT month, net
        FROM (
            SELECT 
                strftime('%Y-%m', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) AS month,
                SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) AS net
            FROM transactions
            GROUP BY month
            ORDER BY month DESC
            LIMIT 6
        )
        ORDER BY month ASC
        """
    )
    fun observeMonthlyNet(): Flow<List<MonthlyNetRow>>
}
