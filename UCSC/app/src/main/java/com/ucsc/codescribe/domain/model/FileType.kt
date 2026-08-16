package com.ucsc.codescribe.domain.model

enum class FileType {
    KOTLIN,
    MARKDOWN,
    PLAIN;

    companion object {
        fun fromFileName(name: String): FileType = when {
            name.endsWith(".kt", ignoreCase = true) || name.endsWith(".kts", ignoreCase = true) -> KOTLIN
            name.endsWith(".md", ignoreCase = true) || name.endsWith(".markdown", ignoreCase = true) -> MARKDOWN
            else -> PLAIN
        }
    }
}
