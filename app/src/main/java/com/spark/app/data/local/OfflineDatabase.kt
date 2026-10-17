package com.spark.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** spark-offline.db: cached feed/saved cards + pending interaction events. */
@Database(
    entities = [CachedItemEntity::class, PendingEventEntity::class],
    version = 1,
    exportSchema = true
)
abstract class OfflineDatabase : RoomDatabase() {

    abstract fun cachedItemDao(): CachedItemDao
    abstract fun pendingEventDao(): PendingEventDao

    companion object {
        fun build(context: Context): OfflineDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OfflineDatabase::class.java,
                "spark-offline.db"
            ).build()
    }
}
