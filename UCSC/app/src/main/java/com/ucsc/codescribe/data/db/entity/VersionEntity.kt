package com.ucsc.codescribe.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * versionNumber 0 is always the base (full snapshot stored on disk, patchText null).
 * Every later version stores only the unified-diff patch needed to go from the
 * previous reconstructed version to this one - no full-file duplication.
 */
@Entity(
    tableName = "versions",
    foreignKeys = [
        ForeignKey(
            entity = TrackedFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fileId")]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val versionNumber: Int,
    val label: String?,
    val timestampMillis: Long,
    val patchText: String?,
    val isBase: Boolean
)
