package com.ucsc.codescribe.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ucsc.codescribe.data.db.entity.TrackedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedFileDao {

    @Query("SELECT * FROM tracked_files ORDER BY lastOpenedMillis DESC")
    fun observeRecent(): Flow<List<TrackedFileEntity>>

    @Query("SELECT * FROM tracked_files WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): TrackedFileEntity?

    @Query("SELECT * FROM tracked_files WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TrackedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: TrackedFileEntity): Long

    @Update
    suspend fun update(file: TrackedFileEntity)

    @Query("UPDATE tracked_files SET isReadOnly = :readOnly WHERE id = :id")
    suspend fun setReadOnly(id: Long, readOnly: Boolean)

    @Query("UPDATE tracked_files SET latestVersionNumber = :versionNumber WHERE id = :id")
    suspend fun setLatestVersionNumber(id: Long, versionNumber: Int)

    @Query("UPDATE tracked_files SET lastOpenedMillis = :timestamp WHERE id = :id")
    suspend fun touchLastOpened(id: Long, timestamp: Long)

    @Query("UPDATE tracked_files SET uri = :uri, displayName = :displayName WHERE id = :id")
    suspend fun updateLocation(id: Long, uri: String, displayName: String)

    @Query("DELETE FROM tracked_files WHERE id = :id")
    suspend fun delete(id: Long)
}
