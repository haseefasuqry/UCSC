package com.ucsc.codescribe.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_files")
data class TrackedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val displayName: String,
    val fileType: String,
    val lastOpenedMillis: Long,
    val isReadOnly: Boolean = false,
    val latestVersionNumber: Int = 0
)
