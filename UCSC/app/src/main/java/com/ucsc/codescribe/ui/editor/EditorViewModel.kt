package com.ucsc.codescribe.ui.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucsc.codescribe.data.repository.FileRepository
import com.ucsc.codescribe.data.repository.VersionRepository
import com.ucsc.codescribe.domain.model.TrackedFile
import com.ucsc.codescribe.editor.autosave.AutosaveManager
import com.ucsc.codescribe.editor.undo.UndoRedoStack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val fileRepository: FileRepository,
    private val versionRepository: VersionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val undoRedoStack = UndoRedoStack()
    private val autosaveManager = AutosaveManager(fileRepository)

    private var currentFileId: Long = -1
    private var lastSavedContent: String = ""
    private var pendingVersionLabel: String? = null
    private var autosaveJob: Job? = null
    private var pushDebounceJob: Job? = null

    fun loadFile(fileId: Long) {
        if (currentFileId == fileId) return
        currentFileId = fileId
        autosaveJob?.cancel()
        _state.value = EditorUiState(isLoading = true)

        viewModelScope.launch {
            val tracked = fileRepository.getFile(fileId) ?: return@launch
            val content = versionRepository.reconstructLatest(fileId)
            val autosave = fileRepository.readAutosave(fileId)
            lastSavedContent = content

            val initialBuffer = TextFieldValue(content)
            undoRedoStack.reset(initialBuffer)

            _state.value = EditorUiState(
                isLoading = false,
                trackedFile = tracked,
                buffer = initialBuffer,
                pendingRecoveryContent = autosave?.takeIf { it != content },
                canUndo = undoRedoStack.canUndo,
                canRedo = undoRedoStack.canRedo
            )

            autosaveJob = viewModelScope.launch {
                autosaveManager.run(fileId) { _state.value.buffer.text }
            }
        }
    }

    /**
     * Forces a reload of tracked-file metadata and the reconstructed latest content,
     * bypassing the [loadFile] same-id guard. Used after a rollback happens on the
     * Version History screen for this same file, since Navigation-Compose keeps
     * reusing this same ViewModel instance when the user navigates back to it.
     */
    fun reloadFromHistory() {
        val fileId = currentFileId
        if (fileId < 0) return
        viewModelScope.launch {
            val tracked = fileRepository.getFile(fileId) ?: return@launch
            val content = versionRepository.reconstructLatest(fileId)
            lastSavedContent = content
            val newBuffer = TextFieldValue(content)
            undoRedoStack.reset(newBuffer)
            _state.update {
                it.copy(
                    trackedFile = tracked,
                    buffer = newBuffer,
                    isDirty = false,
                    canUndo = false,
                    canRedo = false
                )
            }
        }
    }

    fun refreshTrackedFile() {
        val id = currentFileId
        if (id < 0) return
        viewModelScope.launch {
            fileRepository.getFile(id)?.let { refreshed ->
                _state.update { it.copy(trackedFile = refreshed) }
            }
        }
    }

    // --- Editing ---

    fun onTextChange(new: TextFieldValue) {
        if (_state.value.trackedFile?.isReadOnly == true) return
        _state.update { it.copy(buffer = new, isDirty = new.text != lastSavedContent) }

        pushDebounceJob?.cancel()
        pushDebounceJob = viewModelScope.launch {
            delay(500)
            undoRedoStack.push(new)
            _state.update { it.copy(canUndo = undoRedoStack.canUndo, canRedo = undoRedoStack.canRedo) }
        }
    }

    fun undo() {
        val prev = undoRedoStack.undo(_state.value.buffer) ?: return
        _state.update {
            it.copy(buffer = prev, isDirty = prev.text != lastSavedContent, canUndo = undoRedoStack.canUndo, canRedo = undoRedoStack.canRedo)
        }
    }

    fun redo() {
        val next = undoRedoStack.redo() ?: return
        _state.update {
            it.copy(buffer = next, isDirty = next.text != lastSavedContent, canUndo = undoRedoStack.canUndo, canRedo = undoRedoStack.canRedo)
        }
    }

    fun toggleWordWrap() = _state.update { it.copy(wordWrap = !it.wordWrap) }

    fun toggleMarkdownPreview() = _state.update { it.copy(showMarkdownPreview = !it.showMarkdownPreview) }

    fun toggleReadOnly() {
        val tf = _state.value.trackedFile ?: return
        viewModelScope.launch {
            fileRepository.setReadOnly(tf.id, !tf.isReadOnly)
            _state.update { it.copy(trackedFile = tf.copy(isReadOnly = !tf.isReadOnly)) }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    // --- Save / Save As / explicit versions ---

    fun requestSave() = beginSave(label = null)

    fun openSaveVersionDialog() = _state.update { it.copy(saveVersionDialogOpen = true) }
    fun dismissSaveVersionDialog() = _state.update { it.copy(saveVersionDialogOpen = false) }

    fun confirmSaveVersion(label: String) {
        _state.update { it.copy(saveVersionDialogOpen = false) }
        beginSave(label = label.ifBlank { null })
    }

    private fun beginSave(label: String?) {
        val tf = _state.value.trackedFile ?: return
        if (tf.isReadOnly) {
            _state.update { it.copy(message = "File is read-only") }
            return
        }
        if (fileRepository.isUnsaved(tf)) {
            pendingVersionLabel = label
            _state.update { it.copy(awaitingSaveAsUri = true) }
        } else {
            viewModelScope.launch { performSave(tf, label) }
        }
    }

    fun dismissSaveAsRequest() {
        pendingVersionLabel = null
        _state.update { it.copy(awaitingSaveAsUri = false) }
    }

    fun onSaveAsUriPicked(uri: Uri) {
        viewModelScope.launch {
            val tf = _state.value.trackedFile ?: return@launch
            val content = _state.value.buffer.text
            val updated = fileRepository.saveAs(tf.id, uri, content)
            versionRepository.saveNewVersion(tf.id, content, pendingVersionLabel)
            pendingVersionLabel = null
            fileRepository.clearAutosave(tf.id)
            lastSavedContent = content
            _state.update { it.copy(trackedFile = updated, isDirty = false, awaitingSaveAsUri = false, message = "Saved") }
        }
    }

    private suspend fun performSave(tf: TrackedFile, label: String?) {
        val content = _state.value.buffer.text
        fileRepository.writeContent(Uri.parse(tf.uri), content)
        versionRepository.saveNewVersion(tf.id, content, label)
        fileRepository.clearAutosave(tf.id)
        lastSavedContent = content
        val refreshed = fileRepository.getFile(tf.id) ?: tf
        _state.update { it.copy(trackedFile = refreshed, isDirty = false, message = "Saved") }
    }

    // --- Crash recovery ---

    fun restoreFromAutosave() {
        val recovered = _state.value.pendingRecoveryContent ?: return
        val newBuffer = TextFieldValue(recovered)
        undoRedoStack.reset(newBuffer)
        _state.update {
            it.copy(buffer = newBuffer, pendingRecoveryContent = null, isDirty = true, canUndo = false, canRedo = false)
        }
    }

    fun discardAutosave() {
        val tf = _state.value.trackedFile ?: return
        viewModelScope.launch {
            fileRepository.clearAutosave(tf.id)
            _state.update { it.copy(pendingRecoveryContent = null) }
        }
    }

    // --- Search & replace ---

    fun toggleSearch() {
        _state.update { it.copy(search = it.search.copy(active = !it.search.active)) }
        if (_state.value.search.active) updateSearchQuery(_state.value.search.query)
    }

    fun closeSearch() = _state.update { it.copy(search = SearchState()) }

    fun updateSearchQuery(query: String) {
        val matches = findMatches(_state.value.buffer.text, query)
        _state.update { it.copy(search = it.search.copy(query = query, matches = matches, currentIndex = if (matches.isNotEmpty()) 0 else -1)) }
        focusCurrentMatch()
    }

    fun updateReplacement(text: String) = _state.update { it.copy(search = it.search.copy(replacement = text)) }

    fun nextMatch() {
        val s = _state.value.search
        if (s.matches.isEmpty()) return
        _state.update { it.copy(search = it.search.copy(currentIndex = (s.currentIndex + 1) % s.matches.size)) }
        focusCurrentMatch()
    }

    fun previousMatch() {
        val s = _state.value.search
        if (s.matches.isEmpty()) return
        val prev = if (s.currentIndex - 1 < 0) s.matches.size - 1 else s.currentIndex - 1
        _state.update { it.copy(search = it.search.copy(currentIndex = prev)) }
        focusCurrentMatch()
    }

    fun replaceCurrent() {
        val s = _state.value.search
        val range = s.matches.getOrNull(s.currentIndex) ?: return
        val newText = _state.value.buffer.text.replaceRange(range.first, range.last + 1, s.replacement)
        applyProgrammaticEdit(newText)
        updateSearchQuery(s.query)
    }

    fun replaceAll() {
        val s = _state.value.search
        if (s.query.isEmpty()) return
        val newText = _state.value.buffer.text.replace(s.query, s.replacement, ignoreCase = true)
        applyProgrammaticEdit(newText)
        updateSearchQuery(s.query)
    }

    private fun applyProgrammaticEdit(newText: String) {
        val newBuffer = TextFieldValue(newText)
        undoRedoStack.push(newBuffer)
        _state.update { it.copy(buffer = newBuffer, isDirty = newText != lastSavedContent, canUndo = undoRedoStack.canUndo, canRedo = undoRedoStack.canRedo) }
    }

    private fun findMatches(text: String, query: String): List<IntRange> {
        if (query.isEmpty()) return emptyList()
        val results = mutableListOf<IntRange>()
        var index = text.indexOf(query, 0, ignoreCase = true)
        while (index >= 0) {
            results.add(index until index + query.length)
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
        return results
    }

    private fun focusCurrentMatch() {
        val s = _state.value.search
        val range = s.matches.getOrNull(s.currentIndex) ?: return
        _state.update { it.copy(buffer = it.buffer.copy(selection = TextRange(range.first, range.last + 1))) }
    }

    override fun onCleared() {
        autosaveJob?.cancel()
        super.onCleared()
    }
}
