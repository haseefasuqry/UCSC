package com.ucsc.codescribe.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucsc.codescribe.data.repository.FileRepository
import com.ucsc.codescribe.data.repository.OpenFileCoordinator
import com.ucsc.codescribe.domain.model.FileType
import com.ucsc.codescribe.domain.model.TrackedFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val fileRepository: FileRepository,
    private val openFileCoordinator: OpenFileCoordinator
) : ViewModel() {

    val recentFiles: StateFlow<List<TrackedFile>> = fileRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _openedFile = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val openedFile = _openedFile

    fun openExisting(uri: Uri) {
        viewModelScope.launch {
            val opened = openFileCoordinator.openFromUri(uri)
            _openedFile.tryEmit(opened.trackedFile.id)
        }
    }

    fun createNew(fileType: FileType, name: String) {
        viewModelScope.launch {
            val opened = openFileCoordinator.createNew(fileType, name)
            _openedFile.tryEmit(opened.trackedFile.id)
        }
    }

    fun openRecent(file: TrackedFile) {
        viewModelScope.launch {
            _openedFile.tryEmit(file.id)
        }
    }

    fun removeRecent(file: TrackedFile) {
        viewModelScope.launch {
            fileRepository.removeRecent(file.id)
        }
    }

    companion object {
        fun suggestedFileName(fileType: FileType): String = when (fileType) {
            FileType.KOTLIN -> "Untitled.kt"
            FileType.MARKDOWN -> "Untitled.md"
            FileType.PLAIN -> "Untitled.txt"
        }
    }
}
