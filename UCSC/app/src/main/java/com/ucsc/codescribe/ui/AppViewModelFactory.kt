package com.ucsc.codescribe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ucsc.codescribe.AppContainer
import com.ucsc.codescribe.ui.editor.EditorViewModel
import com.ucsc.codescribe.ui.home.HomeViewModel
import com.ucsc.codescribe.ui.versions.VersionsViewModel

/** Hand-rolled ViewModelProvider.Factory backed by the app's manual DI container. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.fileRepository, container.openFileCoordinator) as T

            modelClass.isAssignableFrom(EditorViewModel::class.java) ->
                EditorViewModel(container.fileRepository, container.versionRepository) as T

            modelClass.isAssignableFrom(VersionsViewModel::class.java) ->
                VersionsViewModel(container.versionRepository, container.fileRepository) as T

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
