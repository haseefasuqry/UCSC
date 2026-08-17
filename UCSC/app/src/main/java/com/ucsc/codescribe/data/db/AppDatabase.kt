package com.ucsc.codescribe.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ucsc.codescribe.data.db.dao.TrackedFileDao
import com.ucsc.codescribe.data.db.dao.VersionDao
import com.ucsc.codescribe.data.db.entity.TrackedFileEntity
import com.ucsc.codescribe.data.db.entity.VersionEntity

@Database(
    entities = [TrackedFileEntity::class, VersionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackedFileDao(): TrackedFileDao
    abstract fun versionDao(): VersionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "codescribe.db"
                ).build().also { instance = it }
            }
    }
}
