package com.ucsc.codescribe.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ucsc.codescribe.data.db.dao.TrackedFileDao
import com.ucsc.codescribe.data.db.entity.TrackedFileEntity
import com.ucsc.codescribe.domain.model.FileType
import com.ucsc.codescribe.domain.model.OpenedFile
import com.ucsc.codescribe.domain.model.TrackedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val UNSAVED_SCHEME = "codescribe-unsaved"

/**
 * Handles real file I/O (Storage Access Framework) for user-visible files plus
 * the app-internal crash-recovery cache. Recent files live in Room, keyed by the
 * SAF content:// uri (or a placeholder codescribe-unsaved:// uri for new, not-yet-saved files).
 */
class FileRepository(
    private val context: Context,
    private val trackedFileDao: TrackedFileDao
) {

    private fun TrackedFileEntity.toDomain() = TrackedFile(
        id = id,
        uri = uri,
        displayName = displayName,
        fileType = FileType.valueOf(fileType),
        lastOpenedMillis = lastOpenedMillis,
        isReadOnly = isReadOnly,
        latestVersionNumber = latestVersionNumber
    )

    fun observeRecent(): Flow<List<TrackedFile>> =
        trackedFileDao.observeRecent().map { list -> list.map { it.toDomain() } }

    suspend fun getFile(id: Long): TrackedFile? = withContext(Dispatchers.IO) {
        trackedFileDao.findById(id)?.toDomain()
    }

    fun isUnsaved(trackedFile: TrackedFile): Boolean = trackedFile.uri.startsWith("$UNSAVED_SCHEME://")

    suspend fun createNewFile(fileType: FileType, suggestedName: String): OpenedFile =
        withContext(Dispatchers.IO) {
            val placeholderUri = "$UNSAVED_SCHEME://${UUID.randomUUID()}"
            val id = trackedFileDao.insert(
                TrackedFileEntity(
                    uri = placeholderUri,
                    displayName = suggestedName,
                    fileType = fileType.name,
                    lastOpenedMillis = System.currentTimeMillis(),
                    isReadOnly = false,
                    latestVersionNumber = 0
                )
            )
            OpenedFile(
                trackedFile = TrackedFile(id, placeholderUri, suggestedName, fileType, System.currentTimeMillis(), false, 0),
                content = ""
            )
        }

    suspend fun openFile(uri: Uri): OpenedFile = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val content = readContent(uri)
        val name = displayNameForUri(uri)
        val fileType = FileType.fromFileName(name)
        val uriString = uri.toString()
        val existing = trackedFileDao.findByUri(uriString)
        val id = if (existing != null) {
            trackedFileDao.touchLastOpened(existing.id, System.currentTimeMillis())
            existing.id
        } else {
            trackedFileDao.insert(
                TrackedFileEntity(
                    uri = uriString,
                    displayName = name,
                    fileType = fileType.name,
                    lastOpenedMillis = System.currentTimeMillis(),
                    isReadOnly = false,
                    latestVersionNumber = 0
                )
            )
        }
        val record = trackedFileDao.findById(id)!!
        OpenedFile(record.toDomain(), content)
    }

    suspend fun readContent(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Unable to open $uri for reading")
    }

    suspend fun writeContent(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open $uri for writing")
    }

    suspend fun saveAs(fileId: Long, uri: Uri, content: String): TrackedFile = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        writeContent(uri, content)
        val name = displayNameForUri(uri)
        trackedFileDao.updateLocation(fileId, uri.toString(), name)
        trackedFileDao.findById(fileId)!!.toDomain()
    }

    suspend fun setReadOnly(fileId: Long, readOnly: Boolean) = withContext(Dispatchers.IO) {
        trackedFileDao.setReadOnly(fileId, readOnly)
    }

    suspend fun removeRecent(fileId: Long) = withContext(Dispatchers.IO) {
        trackedFileDao.delete(fileId)
    }

    private fun displayNameForUri(uri: Uri): String =
        DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment ?: "untitled"

    // --- Crash-recovery autosave cache (app-internal storage, not the version history) ---

    private fun autosaveFile(fileId: Long): File {
        val dir = File(context.filesDir, "crash_recovery").apply { mkdirs() }
        return File(dir, "$fileId.tmp")
    }

    suspend fun writeAutosave(fileId: Long, content: String) = withContext(Dispatchers.IO) {
        autosaveFile(fileId).writeText(content)
    }

    suspend fun readAutosave(fileId: Long): String? = withContext(Dispatchers.IO) {
        val file = autosaveFile(fileId)
        if (file.exists()) file.readText() else null
    }

    suspend fun clearAutosave(fileId: Long) = withContext(Dispatchers.IO) {
        autosaveFile(fileId).delete()
    }
}
