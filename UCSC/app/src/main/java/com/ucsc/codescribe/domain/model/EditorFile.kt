package com.ucsc.codescribe.domain.model

/** An opened file's metadata paired with the text content read from storage. */
data class OpenedFile(
    val trackedFile: TrackedFile,
    val content: String
)
