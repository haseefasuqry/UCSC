package com.ucsc.codescribe.editor.highlight

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.ucsc.codescribe.domain.model.FileType

/**
 * Renders keyword/string/comment/heading styling over the raw text buffer without
 * ever mutating it - the underlying TextFieldValue stays plain text, so undo/redo,
 * search, save, and diffing all keep operating on unstyled content.
 */
class SyntaxVisualTransformation(
    private val fileType: FileType,
    private val kotlinHighlighter: KotlinHighlighter,
    private val markdownHighlighter: MarkdownHighlighter,
    private val colors: SyntaxColors
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = when (fileType) {
            FileType.KOTLIN -> kotlinHighlighter.highlight(text.text, colors)
            FileType.MARKDOWN -> markdownHighlighter.highlight(text.text, colors)
            FileType.PLAIN -> text
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
