package com.epic.aiexpensevoice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epic.aiexpensevoice.data.local.db.entity.DashboardCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardCacheDao {
    @Query("SELECT * FROM dashboard_cache WHERE id = 0")
    fun observeCache(): Flow<DashboardCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DashboardCacheEntity)

    @Query("DELETE FROM dashboard_cache")
    suspend fun clear()
}
