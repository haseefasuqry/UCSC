package com.ucsc.codescribe.editor.highlight

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Regex-based Kotlin tokenizer. Keywords are loaded from assets/kotlin_keywords.txt
 * (per the spec's "ideally with a keyword file" note) so the word list can be
 * edited without touching code. Strings/comments/annotations get their own token
 * classes so they highlight correctly even when they contain keyword-looking text.
 */
class KotlinHighlighter(context: Context) {

    private val keywords: Set<String> by lazy {
        context.assets.open("kotlin_keywords.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()
        }
    }

    fun highlight(text: String, colors: SyntaxColors): AnnotatedString = buildAnnotatedString {
        append(text)
        for (match in TOKEN_REGEX.findAll(text)) {
            val range = match.range
            val style = when {
                match.groups["comment"] != null -> SpanStyle(color = colors.comment)
                match.groups["string"] != null -> SpanStyle(color = colors.string)
                match.groups["annotation"] != null -> SpanStyle(color = colors.annotation)
                match.groups["word"] != null && match.value in keywords ->
                    SpanStyle(color = colors.keyword, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                else -> null
            }
            if (style != null) {
                addStyle(style, range.first, range.last + 1)
            }
        }
    }

    private companion object {
        val TOKEN_REGEX = Regex(
            """(?<comment>//[^\n]*|/\*[\s\S]*?\*/)""" +
                """|(?<string>"{3}[\s\S]*?"{3}|"(?:\\.|[^"\\\n])*")""" +
                """|(?<annotation>@[A-Za-z_][A-Za-z0-9_]*)""" +
                """|(?<word>\b[A-Za-z_][A-Za-z0-9_]*\b)"""
        )
    }
}
