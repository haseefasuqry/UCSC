package com.ucsc.codescribe.data.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import com.ucsc.codescribe.domain.model.DiffLine
import com.ucsc.codescribe.domain.model.DiffLineKind

/**
 * Thin wrapper around java-diff-utils. All version-control storage/reconstruction
 * and the rollback diff view go through here so there is a single place that
 * knows how patches are generated, applied, and rendered.
 */
object DiffEngine {

    private const val CONTEXT_LINES = 3

    private fun String.toLineList(): List<String> = split("\n")

    /**
     * Generates a unified-diff patch turning [oldText] into [newText].
     * Returns null when the two texts are identical (nothing to store).
     */
    fun unifiedDiff(oldText: String, newText: String): String? {
        val oldLines = oldText.toLineList()
        val newLines = newText.toLineList()
        val patch = DiffUtils.diff(oldLines, newLines)
        if (patch.deltas.isEmpty()) return null
        val diffLines = UnifiedDiffUtils.generateUnifiedDiff(
            "old", "new", oldLines, patch, CONTEXT_LINES
        )
        return diffLines.joinToString("\n")
    }

    /** Applies a unified-diff [patchText] (as produced by [unifiedDiff]) to [baseText]. */
    fun applyPatch(baseText: String, patchText: String): String {
        val baseLines = baseText.toLineList()
        val patch: Patch<String> = UnifiedDiffUtils.parseUnifiedDiff(patchText.split("\n"))
        val result = patch.applyTo(baseLines)
        return result.joinToString("\n")
    }

    /** Produces a line-by-line diff for display (Diff view). */
    fun diffRows(oldText: String, newText: String): List<DiffLine> {
        val generator: DiffRowGenerator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .build()
        val rows: List<DiffRow> = generator.generateDiffRows(oldText.toLineList(), newText.toLineList())

        var oldLineNum = 0
        var newLineNum = 0
        return rows.map { row ->
            val kind = when (row.tag) {
                DiffRow.Tag.INSERT -> DiffLineKind.ADDED
                DiffRow.Tag.DELETE -> DiffLineKind.REMOVED
                DiffRow.Tag.CHANGE -> DiffLineKind.CHANGED
                DiffRow.Tag.EQUAL -> DiffLineKind.UNCHANGED
            }
            val hasOld = row.tag != DiffRow.Tag.INSERT
            val hasNew = row.tag != DiffRow.Tag.DELETE
            if (hasOld) oldLineNum++
            if (hasNew) newLineNum++
            DiffLine(
                kind = kind,
                oldLineNumber = if (hasOld) oldLineNum else null,
                newLineNumber = if (hasNew) newLineNum else null,
                oldText = if (hasOld) row.oldLine else null,
                newText = if (hasNew) row.newLine else null
            )
        }
    }
}
