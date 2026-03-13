package com.fabiano.controlefinanca.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val note: String,
    val dateMillis: Long,
    val transactionDateMillis: Long,
    val recurrenceType: RecurrenceType,
    val installmentCurrent: Int,
    val installmentTotal: Int,
    val ofxFingerprint: String?
)
