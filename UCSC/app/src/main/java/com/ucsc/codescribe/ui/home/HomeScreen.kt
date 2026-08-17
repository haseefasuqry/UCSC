package com.ucsc.codescribe.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucsc.codescribe.domain.model.FileType
import com.ucsc.codescribe.domain.model.TrackedFile
import com.ucsc.codescribe.ui.AppViewModelFactory
import com.ucsc.codescribe.ui.components.ConfirmDialog
import com.ucsc.codescribe.ui.components.RecentFileCard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModelFactory: AppViewModelFactory,
    onOpenEditor: (Long) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val recentFiles by viewModel.recentFiles.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    var fileToRemove by remember { mutableStateOf<TrackedFile?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::openExisting) }

    LaunchedEffect(Unit) {
        viewModel.openedFile.collectLatest { fileId -> onOpenEditor(fileId) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("CodeScribe", style = MaterialTheme.typography.titleLarge)
                }
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { menuExpanded = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New") }
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("New Kotlin file") },
                        leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            viewModel.createNew(FileType.KOTLIN, HomeViewModel.suggestedFileName(FileType.KOTLIN))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Markdown file") },
                        leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            viewModel.createNew(FileType.MARKDOWN, HomeViewModel.suggestedFileName(FileType.MARKDOWN))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New plain text file") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            viewModel.createNew(FileType.PLAIN, HomeViewModel.suggestedFileName(FileType.PLAIN))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open file…") },
                        leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (recentFiles.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recentFiles, key = { it.id }) { file ->
                    RecentFileCard(
                        file = file,
                        onClick = { viewModel.openRecent(file) },
                        onLongClick = { fileToRemove = file }
                    )
                }
            }
        }
    }

    fileToRemove?.let { file ->
        ConfirmDialog(
            title = "Remove from recents?",
            message = "\"${file.displayName}\" will be removed from this list. Its version history stays until you delete the file itself.",
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.removeRecent(file)
                fileToRemove = null
            },
            onDismiss = { fileToRemove = null }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "No files yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Tap New to create a Kotlin or Markdown file, or open one from storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
