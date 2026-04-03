package com.epic.aiexpensevoice.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_cache")
data class DashboardCacheEntity(
    @PrimaryKey val id: Int = 0,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)
