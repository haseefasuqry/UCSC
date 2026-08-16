package com.ucsc.codescribe.domain.model

enum class DiffLineKind {
    UNCHANGED,
    ADDED,
    REMOVED,
    CHANGED
}

data class DiffLine(
    val kind: DiffLineKind,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val oldText: String?,
    val newText: String?
)

data class VersionInfo(
    val id: Long,
    val fileId: Long,
    val versionNumber: Int,
    val label: String?,
    val timestampMillis: Long,
    val isBase: Boolean
)

data class TrackedFile(
    val id: Long,
    val uri: String,
    val displayName: String,
    val fileType: FileType,
    val lastOpenedMillis: Long,
    val isReadOnly: Boolean,
    val latestVersionNumber: Int
)
