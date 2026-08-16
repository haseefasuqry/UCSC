package com.ucsc.codescribe.editor.undo

import androidx.compose.ui.text.input.TextFieldValue

/**
 * Bounded, in-memory undo/redo history for the active editing session only.
 * This is deliberately separate from the delta version-control system: it is
 * never persisted and is cleared whenever a different file is opened.
 */
class UndoRedoStack(private val capacity: Int = 200) {

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun reset(initial: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        undoStack.addLast(initial)
    }

    /** Call after a settled edit (e.g. debounced) to record a new checkpoint. */
    fun push(value: TextFieldValue) {
        if (undoStack.lastOrNull()?.text == value.text) return
        undoStack.addLast(value)
        if (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.size <= 1) return null
        redoStack.addLast(undoStack.removeLast())
        return undoStack.last()
    }

    fun redo(): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        val value = redoStack.removeLast()
        undoStack.addLast(value)
        return value
    }
}
