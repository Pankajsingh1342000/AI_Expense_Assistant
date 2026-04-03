package com.epic.aiexpensevoice.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.epic.aiexpensevoice.data.local.db.dao.DashboardCacheDao
import com.epic.aiexpensevoice.data.local.db.dao.ExpenseCacheDao
import com.epic.aiexpensevoice.data.local.db.dao.PendingSyncDao
import com.epic.aiexpensevoice.data.local.db.entity.DashboardCacheEntity
import com.epic.aiexpensevoice.data.local.db.entity.ExpenseCacheEntity
import com.epic.aiexpensevoice.data.local.db.entity.PendingSyncEntity

@Database(
    entities = [DashboardCacheEntity::class, ExpenseCacheEntity::class, PendingSyncEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dashboardCacheDao(): DashboardCacheDao
    abstract fun expenseCacheDao(): ExpenseCacheDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_expense_voice.db",
        ).fallbackToDestructiveMigration().build()
    }
}
