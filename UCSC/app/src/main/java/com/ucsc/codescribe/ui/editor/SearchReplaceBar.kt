package com.ucsc.codescribe.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchReplaceBar(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onReplacementChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Find") }
                )
                val matchLabel = if (state.matches.isEmpty()) "0/0" else "${state.currentIndex + 1}/${state.matches.size}"
                Text(matchLabel, style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onPrevious, enabled = state.matches.isNotEmpty()) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
                }
                IconButton(onClick = onNext, enabled = state.matches.isNotEmpty()) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = state.replacement,
                    onValueChange = onReplacementChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Replace with") }
                )
                TextButton(onClick = onReplaceCurrent, enabled = state.matches.isNotEmpty()) { Text("Replace") }
                TextButton(onClick = onReplaceAll, enabled = state.matches.isNotEmpty()) { Text("Replace all") }
            }
        }
    }
}
