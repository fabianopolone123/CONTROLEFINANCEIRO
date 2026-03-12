package com.fabiano.controlefinanca.data

import androidx.room.TypeConverter

class DbConverters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromRecurrence(type: RecurrenceType): String = type.name

    @TypeConverter
    fun toRecurrence(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}
