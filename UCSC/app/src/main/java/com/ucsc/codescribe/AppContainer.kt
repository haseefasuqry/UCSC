package com.ucsc.codescribe

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.ucsc.codescribe.data.db.AppDatabase
import com.ucsc.codescribe.data.repository.FileRepository
import com.ucsc.codescribe.data.repository.OpenFileCoordinator
import com.ucsc.codescribe.data.repository.VersionRepository

private val Context.settingsDataStore by preferencesDataStore(name = "codescribe_settings")

/** Simple hand-rolled dependency container; avoids annotation-processor DI risk for this project's size. */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    val settingsDataStore = context.settingsDataStore

    val fileRepository = FileRepository(context, database.trackedFileDao())
    val versionRepository = VersionRepository(context, database.versionDao(), database.trackedFileDao())
    val openFileCoordinator = OpenFileCoordinator(fileRepository, versionRepository)
}
