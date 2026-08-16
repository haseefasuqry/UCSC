package com.ucsc.codescribe

import com.ucsc.codescribe.data.diff.DiffEngine
import com.ucsc.codescribe.domain.model.DiffLineKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffEngineTest {

    @Test
    fun `identical text produces no patch`() {
        val text = "fun main() {\n    println(\"hi\")\n}"
        assertNull(DiffEngine.unifiedDiff(text, text))
    }

    @Test
    fun `patch round trip reconstructs new content exactly`() {
        val old = "line one\nline two\nline three"
        val new = "line one\nline TWO changed\nline three\nline four"

        val patch = DiffEngine.unifiedDiff(old, new)
        checkNotNull(patch)
        val reconstructed = DiffEngine.applyPatch(old, patch)

        assertEquals(new, reconstructed)
    }

    @Test
    fun `chained patches reconstruct every version from a single base - mirrors VersionRepository`() {
        // Simulates VersionRepository.reconstructFrom: base + chained unified-diff patches,
        // with no full-file duplication anywhere in the chain.
        val v0 = "class Foo {\n}"
        val v1 = "class Foo {\n    fun bar() {}\n}"
        val v2 = "class Foo {\n    fun bar() {}\n    fun baz() {}\n}"
        val v3 = "class Foo {\n    fun baz() {}\n}"

        val patch1 = checkNotNull(DiffEngine.unifiedDiff(v0, v1))
        val patch2 = checkNotNull(DiffEngine.unifiedDiff(v1, v2))
        val patch3 = checkNotNull(DiffEngine.unifiedDiff(v2, v3))

        var reconstructed = v0
        reconstructed = DiffEngine.applyPatch(reconstructed, patch1)
        assertEquals(v1, reconstructed)

        reconstructed = DiffEngine.applyPatch(reconstructed, patch2)
        assertEquals(v2, reconstructed)

        reconstructed = DiffEngine.applyPatch(reconstructed, patch3)
        assertEquals(v3, reconstructed)
    }

    @Test
    fun `rollback reconstructs an earlier version from the same patch chain`() {
        val v0 = "alpha\nbeta\ngamma"
        val v1 = "alpha\nBETA\ngamma"
        val v2 = "alpha\nBETA\ngamma\ndelta"

        val patch1 = checkNotNull(DiffEngine.unifiedDiff(v0, v1))
        val patch2 = checkNotNull(DiffEngine.unifiedDiff(v1, v2))

        // Reconstruct v1 (rollback target) by stopping the replay early - never touching v2's patch.
        val reconstructedV1 = DiffEngine.applyPatch(v0, patch1)
        assertEquals(v1, reconstructedV1)

        // Sanity: replaying through patch2 still yields v2 independently.
        val reconstructedV2 = DiffEngine.applyPatch(reconstructedV1, patch2)
        assertEquals(v2, reconstructedV2)
    }

    @Test
    fun `diff rows classify added, removed and unchanged lines`() {
        val old = "keep\nremoveMe\nkeep2"
        val new = "keep\nkeep2\naddMe"

        val rows = DiffEngine.diffRows(old, new)

        assertTrue(rows.any { it.kind == DiffLineKind.REMOVED && it.oldText == "removeMe" })
        assertTrue(rows.any { it.kind == DiffLineKind.ADDED && it.newText == "addMe" })
        assertTrue(rows.count { it.kind == DiffLineKind.UNCHANGED } >= 2)
    }
}
