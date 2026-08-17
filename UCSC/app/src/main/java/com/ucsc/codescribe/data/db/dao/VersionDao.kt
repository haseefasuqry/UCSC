package com.ucsc.codescribe.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ucsc.codescribe.data.db.entity.VersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionDao {

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber ASC")
    fun observeVersions(fileId: Long): Flow<List<VersionEntity>>

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber ASC")
    suspend fun getVersions(fileId: Long): List<VersionEntity>

    @Query("SELECT * FROM versions WHERE fileId = :fileId AND versionNumber <= :upToVersion ORDER BY versionNumber ASC")
    suspend fun getVersionsUpTo(fileId: Long, upToVersion: Int): List<VersionEntity>

    @Query("SELECT * FROM versions WHERE fileId = :fileId AND versionNumber = :versionNumber LIMIT 1")
    suspend fun getVersion(fileId: Long, versionNumber: Int): VersionEntity?

    @Insert
    suspend fun insert(version: VersionEntity): Long

    @Query("DELETE FROM versions WHERE fileId = :fileId")
    suspend fun deleteAllForFile(fileId: Long)
}
