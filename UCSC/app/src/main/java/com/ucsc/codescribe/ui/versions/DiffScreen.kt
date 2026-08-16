package com.ucsc.codescribe.ui.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucsc.codescribe.domain.model.DiffLine
import com.ucsc.codescribe.domain.model.DiffLineKind
import com.ucsc.codescribe.ui.AppViewModelFactory
import com.ucsc.codescribe.ui.theme.DiffAddedDark
import com.ucsc.codescribe.ui.theme.DiffAddedLight
import com.ucsc.codescribe.ui.theme.DiffChangedDark
import com.ucsc.codescribe.ui.theme.DiffChangedLight
import com.ucsc.codescribe.ui.theme.DiffRemovedDark
import com.ucsc.codescribe.ui.theme.DiffRemovedLight
import com.ucsc.codescribe.ui.theme.EditorFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(
    fileId: Long,
    oldVersion: Int,
    newVersion: Int,
    viewModelFactory: AppViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: VersionsViewModel = viewModel(factory = viewModelFactory)
    var diffLines by remember { mutableStateOf<List<DiffLine>>(emptyList()) }

    LaunchedEffect(fileId, oldVersion, newVersion) {
        diffLines = viewModel.loadDiff(fileId, oldVersion, newVersion)
    }

    val isDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changes: v$oldVersion → v$newVersion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (diffLines.none { it.kind != DiffLineKind.UNCHANGED }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No differences between these versions.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(diffLines) { line -> DiffLineRow(line, isDark) }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine, isDark: Boolean) {
    when (line.kind) {
        DiffLineKind.UNCHANGED -> DiffTextRow(
            prefix = " ",
            lineNumber = line.newLineNumber,
            text = line.newText.orEmpty(),
            background = MaterialTheme.colorScheme.surface
        )
        DiffLineKind.ADDED -> DiffTextRow(
            prefix = "+",
            lineNumber = line.newLineNumber,
            text = line.newText.orEmpty(),
            background = if (isDark) DiffAddedDark else DiffAddedLight
        )
        DiffLineKind.REMOVED -> DiffTextRow(
            prefix = "-",
            lineNumber = line.oldLineNumber,
            text = line.oldText.orEmpty(),
            background = if (isDark) DiffRemovedDark else DiffRemovedLight
        )
        DiffLineKind.CHANGED -> Column(modifier = Modifier.fillMaxWidth()) {
            DiffTextRow(
                prefix = "-",
                lineNumber = line.oldLineNumber,
                text = line.oldText.orEmpty(),
                background = if (isDark) DiffRemovedDark else DiffRemovedLight
            )
            DiffTextRow(
                prefix = "+",
                lineNumber = line.newLineNumber,
                text = line.newText.orEmpty(),
                background = if (isDark) DiffChangedDark else DiffChangedLight
            )
        }
    }
}

@Composable
private fun DiffTextRow(prefix: String, lineNumber: Int?, text: String, background: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Text(
            text = lineNumber?.toString() ?: "",
            fontFamily = EditorFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = "$prefix $text",
            fontFamily = EditorFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
