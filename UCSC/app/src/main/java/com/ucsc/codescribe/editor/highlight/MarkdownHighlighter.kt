package com.ucsc.codescribe.editor.highlight

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** Regex-based Markdown tokenizer covering the common inline/block elements for editor-time highlighting. */
class MarkdownHighlighter {

    fun highlight(text: String, colors: SyntaxColors): AnnotatedString = buildAnnotatedString {
        append(text)
        for (match in TOKEN_REGEX.findAll(text)) {
            val range = match.range
            val style = when {
                match.groups["heading"] != null -> SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)
                match.groups["code"] != null -> SpanStyle(color = colors.string, background = colors.comment.copy(alpha = 0.15f))
                match.groups["bold"] != null -> SpanStyle(fontWeight = FontWeight.Bold)
                match.groups["italic"] != null -> SpanStyle(fontStyle = FontStyle.Italic)
                match.groups["link"] != null -> SpanStyle(color = colors.annotation, fontWeight = FontWeight.Medium)
                match.groups["bullet"] != null -> SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)
                match.groups["quote"] != null -> SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic)
                else -> null
            }
            if (style != null) {
                addStyle(style, range.first, range.last + 1)
            }
        }
    }

    private companion object {
        val TOKEN_REGEX = Regex(
            """(?<heading>(?m)^#{1,6}\s.*$)""" +
                """|(?<code>`[^`\n]+`)""" +
                """|(?<bold>\*\*[^*\n]+\*\*|__[^_\n]+__)""" +
                """|(?<italic>\*[^*\n]+\*|_[^_\n]+_)""" +
                """|(?<link>\[[^\]\n]*\]\([^)\n]*\))""" +
                """|(?<bullet>(?m)^\s*([-*+]|\d+\.)\s)""" +
                """|(?<quote>(?m)^>.*$)"""
        )
    }
}
