package com.ucsc.codescribe.data.repository

import android.net.Uri
import com.ucsc.codescribe.domain.model.FileType
import com.ucsc.codescribe.domain.model.OpenedFile

/**
 * Coordinates FileRepository (raw SAF I/O + recent-files metadata) with
 * VersionRepository (delta history) so every screen that opens or creates a
 * file gets consistent behavior: existing history wins over stale disk content,
 * and brand-new files always get a version 0 base recorded.
 */
class OpenFileCoordinator(
    private val fileRepository: FileRepository,
    private val versionRepository: VersionRepository
) {

    suspend fun openFromUri(uri: Uri): OpenedFile {
        val opened = fileRepository.openFile(uri)
        return reconcileHistory(opened)
    }

    suspend fun createNew(fileType: FileType, suggestedName: String): OpenedFile {
        val opened = fileRepository.createNewFile(fileType, suggestedName)
        versionRepository.createInitialVersion(opened.trackedFile.id, "")
        return opened
    }

    private suspend fun reconcileHistory(opened: OpenedFile): OpenedFile {
        val fileId = opened.trackedFile.id
        return if (versionRepository.hasHistory(fileId)) {
            opened.copy(content = versionRepository.reconstructLatest(fileId))
        } else {
            versionRepository.createInitialVersion(fileId, opened.content)
            opened
        }
    }
}
