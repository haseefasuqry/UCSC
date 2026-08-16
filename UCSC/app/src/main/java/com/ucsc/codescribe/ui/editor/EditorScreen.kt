package com.ucsc.codescribe.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucsc.codescribe.domain.model.FileType
import com.ucsc.codescribe.editor.highlight.KotlinHighlighter
import com.ucsc.codescribe.editor.highlight.MarkdownHighlighter
import com.ucsc.codescribe.editor.highlight.SyntaxColors
import com.ucsc.codescribe.editor.highlight.SyntaxVisualTransformation
import com.ucsc.codescribe.ui.AppViewModelFactory
import com.ucsc.codescribe.ui.components.ConfirmDialog
import com.ucsc.codescribe.ui.components.InputDialog
import com.ucsc.codescribe.ui.components.LineNumberGutter
import com.ucsc.codescribe.ui.theme.EditorFontFamily
import com.ucsc.codescribe.ui.theme.SyntaxAnnotation
import com.ucsc.codescribe.ui.theme.SyntaxComment
import com.ucsc.codescribe.ui.theme.SyntaxKeyword
import com.ucsc.codescribe.ui.theme.SyntaxString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    fileId: Long,
    viewModelFactory: AppViewModelFactory,
    rolledBack: State<Boolean>? = null,
    onConsumeRollback: () -> Unit = {},
    onBack: () -> Unit,
    onOpenVersions: (Long) -> Unit
) {
    val viewModel: EditorViewModel = viewModel(factory = viewModelFactory)
    LaunchedEffect(fileId) { viewModel.loadFile(fileId) }
    val state by viewModel.state.collectAsState()

    val rolledBackValue = rolledBack?.value == true
    LaunchedEffect(rolledBackValue) {
        if (rolledBackValue) {
            viewModel.reloadFromHistory()
            onConsumeRollback()
        }
    }

    val context = LocalContext.current
    val kotlinHighlighter = remember { KotlinHighlighter(context) }
    val markdownHighlighter = remember { MarkdownHighlighter() }
    val syntaxColors = SyntaxColors(
        keyword = SyntaxKeyword,
        string = SyntaxString,
        comment = SyntaxComment,
        annotation = SyntaxAnnotation
    )
    val fileType = state.trackedFile?.fileType ?: FileType.PLAIN
    val visualTransformation = remember(fileType) {
        SyntaxVisualTransformation(fileType, kotlinHighlighter, markdownHighlighter, syntaxColors)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }

    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(viewModel::onSaveAsUriPicked) ?: viewModel.dismissSaveAsRequest() }

    LaunchedEffect(state.awaitingSaveAsUri) {
        if (state.awaitingSaveAsUri) {
            saveAsLauncher.launch(state.trackedFile?.displayName ?: "untitled.txt")
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessage()
        }
    }

    val isReadOnly = state.trackedFile?.isReadOnly == true

    BackHandler(enabled = state.search.active) { viewModel.closeSearch() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.trackedFile?.displayName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.isDirty) {
                            Text(
                                text = "Unsaved changes",
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
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Find & replace")
                    }
                    IconButton(onClick = viewModel::requestSave, enabled = !isReadOnly) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Undo") },
                                enabled = state.canUndo,
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    viewModel.undo()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Redo") },
                                enabled = state.canRedo,
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    viewModel.redo()
                                }
                            )
                            if (fileType == FileType.MARKDOWN) {
                                DropdownMenuItem(
                                    text = { Text(if (state.showMarkdownPreview) "Hide preview" else "Show preview") },
                                    leadingIcon = {
                                        Icon(
                                            if (state.showMarkdownPreview) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        viewModel.toggleMarkdownPreview()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (isReadOnly) "Unlock file" else "Lock file (read-only)") },
                                leadingIcon = {
                                    Icon(
                                        if (isReadOnly) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    viewModel.toggleReadOnly()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.wordWrap) "Word wrap: On" else "Word wrap: Off") },
                                onClick = {
                                    overflowExpanded = false
                                    viewModel.toggleWordWrap()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save As…") },
                                enabled = !isReadOnly,
                                onClick = {
                                    overflowExpanded = false
                                    saveAsLauncher.launch(state.trackedFile?.displayName ?: "untitled.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save version…") },
                                enabled = !isReadOnly,
                                onClick = {
                                    overflowExpanded = false
                                    viewModel.openSaveVersionDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Version history") },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenVersions(fileId)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.search.active) {
                SearchReplaceBar(
                    state = state.search,
                    onQueryChange = viewModel::updateSearchQuery,
                    onReplacementChange = viewModel::updateReplacement,
                    onNext = viewModel::nextMatch,
                    onPrevious = viewModel::previousMatch,
                    onReplaceCurrent = viewModel::replaceCurrent,
                    onReplaceAll = viewModel::replaceAll,
                    onClose = viewModel::closeSearch
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(if (state.showMarkdownPreview) 0.5f else 1f).fillMaxSize()) {
                    EditorTextArea(
                        state = state,
                        isReadOnly = isReadOnly,
                        visualTransformation = visualTransformation,
                        onValueChange = viewModel::onTextChange
                    )
                }
                if (state.showMarkdownPreview && fileType == FileType.MARKDOWN) {
                    Box(modifier = Modifier.weight(0.5f).fillMaxSize()) {
                        MarkdownPreviewPane(markdownText = state.buffer.text)
                    }
                }
            }
        }
    }

    state.pendingRecoveryContent?.let {
        ConfirmDialog(
            title = "Restore unsaved changes?",
            message = "We found autosaved changes from a previous session that were never saved. Restore them into the editor?",
            confirmLabel = "Restore",
            dismissLabel = "Discard",
            onConfirm = viewModel::restoreFromAutosave,
            onDismiss = viewModel::discardAutosave
        )
    }

    if (state.saveVersionDialogOpen) {
        InputDialog(
            title = "Save version",
            label = "Version name (optional)",
            confirmLabel = "Save",
            onConfirm = viewModel::confirmSaveVersion,
            onDismiss = viewModel::dismissSaveVersionDialog
        )
    }
}

@Composable
private fun EditorTextArea(
    state: EditorUiState,
    isReadOnly: Boolean,
    visualTransformation: SyntaxVisualTransformation,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit
) {
    val lineCount = remember(state.buffer.text) { state.buffer.text.count { it == '\n' } + 1 }
    val textStyle = TextStyle(
        fontFamily = EditorFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Normal
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        LineNumberGutter(lineCount = lineCount, textStyle = textStyle)
        BasicTextField(
            value = state.buffer,
            onValueChange = onValueChange,
            readOnly = isReadOnly,
            textStyle = textStyle,
            visualTransformation = visualTransformation,
            modifier = if (state.wordWrap) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.horizontalScroll(rememberScrollState())
            }
        )
    }
}
