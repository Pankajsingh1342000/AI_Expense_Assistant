package com.epic.aiexpensevoice.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val localExpenseKey: String? = null,
    val createdAtEpochMillis: Long,
)
