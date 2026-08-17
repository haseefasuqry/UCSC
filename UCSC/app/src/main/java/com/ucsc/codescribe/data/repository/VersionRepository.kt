package com.ucsc.codescribe.data.repository

import android.content.Context
import com.ucsc.codescribe.data.db.dao.TrackedFileDao
import com.ucsc.codescribe.data.db.dao.VersionDao
import com.ucsc.codescribe.data.db.entity.VersionEntity
import com.ucsc.codescribe.data.diff.DiffEngine
import com.ucsc.codescribe.domain.model.DiffLine
import com.ucsc.codescribe.domain.model.VersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Delta-based, non-duplicating version control. Version 0 (the base) is the only
 * full copy of the file, written once to internal storage. Every later version is
 * a unified-diff patch (via [DiffEngine]) stored as text in Room. Reconstructing
 * any version replays base -> patch(1) -> ... -> patch(N).
 */
class VersionRepository(
    private val context: Context,
    private val versionDao: VersionDao,
    private val trackedFileDao: TrackedFileDao
) {

    private fun baseFile(fileId: Long): File {
        val dir = File(context.filesDir, "versions/$fileId").apply { mkdirs() }
        return File(dir, "base.txt")
    }

    suspend fun createInitialVersion(fileId: Long, content: String, label: String = "Initial version") =
        withContext(Dispatchers.IO) {
            baseFile(fileId).writeText(content)
            versionDao.insert(
                VersionEntity(
                    fileId = fileId,
                    versionNumber = 0,
                    label = label,
                    timestampMillis = System.currentTimeMillis(),
                    patchText = null,
                    isBase = true
                )
            )
            trackedFileDao.setLatestVersionNumber(fileId, 0)
        }

    suspend fun saveNewVersion(fileId: Long, newContent: String, label: String?): VersionInfo =
        withContext(Dispatchers.IO) {
            val versions = versionDao.getVersions(fileId)
            val latest = versions.maxByOrNull { it.versionNumber }
                ?: error("No base version exists for file $fileId; call createInitialVersion first")
            val previousContent = reconstructFrom(versions, latest.versionNumber)
            val patch = DiffEngine.unifiedDiff(previousContent, newContent)
            val newVersionNumber = latest.versionNumber + 1
            val id = versionDao.insert(
                VersionEntity(
                    fileId = fileId,
                    versionNumber = newVersionNumber,
                    label = label,
                    timestampMillis = System.currentTimeMillis(),
                    patchText = patch,
                    isBase = false
                )
            )
            trackedFileDao.setLatestVersionNumber(fileId, newVersionNumber)
            VersionInfo(id, fileId, newVersionNumber, label, System.currentTimeMillis(), false)
        }

    /** Reconstructs the exact text content of [versionNumber] for [fileId]. */
    suspend fun reconstruct(fileId: Long, versionNumber: Int): String = withContext(Dispatchers.IO) {
        val versions = versionDao.getVersionsUpTo(fileId, versionNumber)
        reconstructFrom(versions, versionNumber)
    }

    suspend fun hasHistory(fileId: Long): Boolean = withContext(Dispatchers.IO) {
        versionDao.getVersions(fileId).isNotEmpty()
    }

    suspend fun reconstructLatest(fileId: Long): String = withContext(Dispatchers.IO) {
        val file = trackedFileDao.findById(fileId) ?: error("Unknown file $fileId")
        reconstruct(fileId, file.latestVersionNumber)
    }

    private fun reconstructFrom(orderedVersions: List<VersionEntity>, upToVersion: Int): String {
        val relevant = orderedVersions.filter { it.versionNumber <= upToVersion }.sortedBy { it.versionNumber }
        val base = relevant.firstOrNull { it.isBase }
            ?: error("Missing base version in history")
        var content = baseFile(base.fileId).readText()
        for (version in relevant) {
            if (version.isBase) continue
            val patch = version.patchText ?: continue
            content = DiffEngine.applyPatch(content, patch)
        }
        return content
    }

    /**
     * Restores [fileId] to [targetVersionNumber] by reconstructing its content and
     * recording that restoration as a brand-new version, so history is preserved
     * rather than destroyed.
     */
    suspend fun rollback(fileId: Long, targetVersionNumber: Int): String = withContext(Dispatchers.IO) {
        val restoredContent = reconstruct(fileId, targetVersionNumber)
        saveNewVersion(fileId, restoredContent, "Restored to v$targetVersionNumber")
        restoredContent
    }

    suspend fun diffAgainstVersion(fileId: Long, versionNumber: Int, otherContent: String): List<DiffLine> =
        withContext(Dispatchers.IO) {
            val base = reconstruct(fileId, versionNumber)
            DiffEngine.diffRows(base, otherContent)
        }

    suspend fun diffBetweenVersions(fileId: Long, oldVersion: Int, newVersion: Int): List<DiffLine> =
        withContext(Dispatchers.IO) {
            val old = reconstruct(fileId, oldVersion)
            val new = reconstruct(fileId, newVersion)
            DiffEngine.diffRows(old, new)
        }

    fun observeVersions(fileId: Long): Flow<List<VersionInfo>> =
        versionDao.observeVersions(fileId).map { list ->
            list.map { VersionInfo(it.id, it.fileId, it.versionNumber, it.label, it.timestampMillis, it.isBase) }
        }

    suspend fun deleteHistory(fileId: Long) = withContext(Dispatchers.IO) {
        versionDao.deleteAllForFile(fileId)
        baseFile(fileId).parentFile?.deleteRecursively()
    }
}
