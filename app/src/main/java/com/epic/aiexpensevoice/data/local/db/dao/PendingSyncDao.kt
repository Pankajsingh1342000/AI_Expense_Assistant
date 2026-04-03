package com.epic.aiexpensevoice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epic.aiexpensevoice.data.local.db.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAtEpochMillis ASC")
    suspend fun getPendingCommands(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: PendingSyncEntity): Long

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun clear()
}
