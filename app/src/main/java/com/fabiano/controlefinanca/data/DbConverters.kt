package com.fabiano.controlefinanca.data

import androidx.room.TypeConverter

class DbConverters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}
