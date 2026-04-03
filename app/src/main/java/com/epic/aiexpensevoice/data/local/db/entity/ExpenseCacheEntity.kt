package com.epic.aiexpensevoice.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_cache")
data class ExpenseCacheEntity(
    @PrimaryKey val cacheKey: String,
    val remoteId: Int?,
    val title: String,
    val amount: Double,
    val category: String,
    val dateIso: String,
    val description: String? = null,
    val isPendingSync: Boolean = false,
)
