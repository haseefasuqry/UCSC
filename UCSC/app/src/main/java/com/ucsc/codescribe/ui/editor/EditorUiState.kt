package com.ucsc.codescribe.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.ucsc.codescribe.domain.model.TrackedFile

data class SearchState(
    val active: Boolean = false,
    val query: String = "",
    val replacement: String = "",
    val matches: List<IntRange> = emptyList(),
    val currentIndex: Int = -1
)

data class EditorUiState(
    val isLoading: Boolean = true,
    val trackedFile: TrackedFile? = null,
    val buffer: TextFieldValue = TextFieldValue(""),
    val wordWrap: Boolean = true,
    val showMarkdownPreview: Boolean = false,
    val isDirty: Boolean = false,
    val search: SearchState = SearchState(),
    val pendingRecoveryContent: String? = null,
    val saveVersionDialogOpen: Boolean = false,
    val awaitingSaveAsUri: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val message: String? = null
)
