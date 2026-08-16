package com.ucsc.codescribe.editor.autosave

import com.ucsc.codescribe.data.repository.FileRepository
import kotlinx.coroutines.delay

/**
 * Crash-recovery cache: periodically snapshots the live buffer into app-internal
 * storage so a crash or killed process doesn't lose unsaved edits. Independent of
 * both the undo/redo stack and the delta version-control history.
 */
class AutosaveManager(
    private val fileRepository: FileRepository,
    private val intervalMillis: Long = 10_000
) {
    /** Runs until the enclosing coroutine is cancelled (e.g. ViewModel cleared or file switched). */
    suspend fun run(fileId: Long, currentContent: () -> String) {
        while (true) {
            delay(intervalMillis)
            fileRepository.writeAutosave(fileId, currentContent())
        }
    }
}
