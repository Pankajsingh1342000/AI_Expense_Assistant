package com.epic.aiexpensevoice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epic.aiexpensevoice.data.local.db.entity.ExpenseCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCacheDao {
    @Query("SELECT * FROM expense_cache ORDER BY dateIso DESC")
    fun observeExpenses(): Flow<List<ExpenseCacheEntity>>

    @Query("SELECT * FROM expense_cache ORDER BY dateIso DESC")
    suspend fun getExpenses(): List<ExpenseCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExpenseCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExpenseCacheEntity>)

    @Query("DELETE FROM expense_cache WHERE isPendingSync = 0")
    suspend fun deleteSyncedExpenses()

    @Query("UPDATE expense_cache SET isPendingSync = 0 WHERE cacheKey = :cacheKey")
    suspend fun markPendingSynced(cacheKey: String)

    @Query("DELETE FROM expense_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM expense_cache WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: Int)

    @Query("DELETE FROM expense_cache")
    suspend fun clear()
}
