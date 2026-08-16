package com.ucsc.codescribe.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LineNumberGutter(lineCount: Int, textStyle: TextStyle, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        for (line in 1..lineCount) {
            Text(
                text = line.toString(),
                style = textStyle,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
