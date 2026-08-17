package com.ucsc.codescribe.editor.highlight

import androidx.compose.ui.graphics.Color

/** Palette handed to the highlighters so they stay theme-aware (light/dark) without a Compose dependency of their own. */
data class SyntaxColors(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val annotation: Color
)
