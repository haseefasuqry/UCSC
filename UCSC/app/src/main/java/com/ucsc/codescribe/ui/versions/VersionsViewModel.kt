package com.ucsc.codescribe.ui.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucsc.codescribe.data.repository.FileRepository
import com.ucsc.codescribe.data.repository.VersionRepository
import com.ucsc.codescribe.domain.model.DiffLine
import com.ucsc.codescribe.domain.model.TrackedFile
import com.ucsc.codescribe.domain.model.VersionInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val versionRepository: VersionRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _trackedFile = MutableStateFlow<TrackedFile?>(null)
    val trackedFile: StateFlow<TrackedFile?> = _trackedFile.asStateFlow()

    private val _versions = MutableStateFlow<List<VersionInfo>>(emptyList())
    val versions: StateFlow<List<VersionInfo>> = _versions.asStateFlow()

    private val _rollbackMessage = MutableStateFlow<String?>(null)
    val rollbackMessage: StateFlow<String?> = _rollbackMessage.asStateFlow()

    private var currentFileId = -1L
    private var collectJob: Job? = null

    fun loadFile(fileId: Long) {
        if (currentFileId == fileId) return
        currentFileId = fileId
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            _trackedFile.value = fileRepository.getFile(fileId)
            versionRepository.observeVersions(fileId).collect { list ->
                _versions.value = list.sortedByDescending { it.versionNumber }
            }
        }
    }

    fun rollback(fileId: Long, targetVersion: Int) {
        viewModelScope.launch {
            val restored = versionRepository.rollback(fileId, targetVersion)
            fileRepository.getFile(fileId)?.let { tf ->
                if (!fileRepository.isUnsaved(tf)) {
                    fileRepository.writeContent(android.net.Uri.parse(tf.uri), restored)
                }
                _trackedFile.value = fileRepository.getFile(fileId)
            }
            _rollbackMessage.value = "Restored to v$targetVersion"
        }
    }

    fun clearRollbackMessage() {
        _rollbackMessage.value = null
    }

    suspend fun loadDiff(fileId: Long, oldVersion: Int, newVersion: Int): List<DiffLine> =
        versionRepository.diffBetweenVersions(fileId, oldVersion, newVersion)
}
