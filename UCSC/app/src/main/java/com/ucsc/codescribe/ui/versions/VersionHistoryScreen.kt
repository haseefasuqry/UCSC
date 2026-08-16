package com.ucsc.codescribe.ui.versions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucsc.codescribe.domain.model.VersionInfo
import com.ucsc.codescribe.ui.AppViewModelFactory
import com.ucsc.codescribe.ui.components.ConfirmDialog
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    fileId: Long,
    viewModelFactory: AppViewModelFactory,
    onBack: () -> Unit,
    onViewDiff: (Long, Int, Int) -> Unit,
    onRollbackComplete: () -> Unit = {}
) {
    val viewModel: VersionsViewModel = viewModel(factory = viewModelFactory)
    LaunchedEffect(fileId) { viewModel.loadFile(fileId) }

    val trackedFile by viewModel.trackedFile.collectAsState()
    val versions by viewModel.versions.collectAsState()
    val rollbackMessage by viewModel.rollbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var versionToRestore by remember { mutableStateOf<VersionInfo?>(null) }

    LaunchedEffect(rollbackMessage) {
        rollbackMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearRollbackMessage()
            onRollbackComplete()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Version history", style = MaterialTheme.typography.titleMedium)
                        trackedFile?.let {
                            Text(
                                it.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (versions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                Text("No versions yet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(versions, key = { it.id }) { version ->
                    VersionRow(
                        version = version,
                        isLatest = version.versionNumber == versions.maxOf { it.versionNumber },
                        onViewDiff = {
                            val previous = (version.versionNumber - 1).coerceAtLeast(0)
                            onViewDiff(fileId, previous, version.versionNumber)
                        },
                        onRestore = { versionToRestore = version }
                    )
                }
            }
        }
    }

    versionToRestore?.let { version ->
        ConfirmDialog(
            title = "Restore this version?",
            message = "Restoring v${version.versionNumber}${version.label?.let { " (\"$it\")" } ?: ""} will create a new version with that content. Nothing already saved is deleted.",
            confirmLabel = "Restore",
            onConfirm = {
                viewModel.rollback(fileId, version.versionNumber)
                versionToRestore = null
            },
            onDismiss = { versionToRestore = null }
        )
    }
}

@Composable
private fun VersionRow(
    version: VersionInfo,
    isLatest: Boolean,
    onViewDiff: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (version.versionNumber == 0) "Base version" else "Version ${version.versionNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (isLatest) {
                    Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!version.label.isNullOrBlank()) {
                Text(version.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(version.timestampMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onViewDiff) {
                    Icon(Icons.Filled.Difference, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("View changes")
                }
                if (!isLatest) {
                    TextButton(onClick = onRestore) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Restore")
                    }
                }
            }
        }
    }
}
