package io.github.diepet.lettering.keyboard

import android.text.InputType
import android.view.inputmethod.InputConnection
import io.github.diepet.lettering.styles.AppTextStyle

/** The editor operations Lettering needs, kept small so their privacy behavior is testable. */
internal interface EditorInputConnection {
    fun getTextBeforeCursor(length: Int, flags: Int): CharSequence?
    fun commitText(text: CharSequence, newCursorPosition: Int): Boolean
    fun deleteCodePoints(beforeLength: Int, afterLength: Int): Boolean
    fun deleteTextBeforeCursor(length: Int): Boolean
}

internal class FrameworkEditorInputConnection(
    private val inputConnection: InputConnection
) : EditorInputConnection {
    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence? =
        inputConnection.getTextBeforeCursor(length, flags)

    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean =
        inputConnection.commitText(text, newCursorPosition)

    override fun deleteCodePoints(beforeLength: Int, afterLength: Int): Boolean =
        inputConnection.deleteSurroundingTextInCodePoints(beforeLength, afterLength) ||
            inputConnection.deleteSurroundingText(beforeLength, afterLength)

    override fun deleteTextBeforeCursor(length: Int): Boolean =
        inputConnection.deleteSurroundingText(length, 0)
}

internal data class SelectionSnapshot(
    val start: Int?,
    val end: Int?,
    val isKnown: Boolean
) {
    val hasSelection: Boolean
        get() = isKnown && start != null && end != null && start != end

    companion object {
        fun from(start: Int, end: Int): SelectionSnapshot =
            if (start >= 0 && end >= 0) SelectionSnapshot(start, end, true) else unknown()

        fun unknown() = SelectionSnapshot(null, null, false)

        /** A successful editor operation leaves the cursor collapsed, even if offsets are stale. */
        fun collapsed() = SelectionSnapshot(null, null, true)
    }
}

internal class InputSessionState {
    var isPasswordField: Boolean = false
        private set

    var selection: SelectionSnapshot = SelectionSnapshot.unknown()
        private set

    fun start(inputType: Int, selectionStart: Int, selectionEnd: Int) {
        isPasswordField = isPasswordInputType(inputType)
        selection = SelectionSnapshot.from(selectionStart, selectionEnd)
    }

    fun updateSelection(start: Int, end: Int) {
        selection = SelectionSnapshot.from(start, end)
    }

    fun markCollapsed() {
        selection = SelectionSnapshot.collapsed()
    }
}

internal fun isPasswordInputType(inputType: Int): Boolean {
    val inputClass = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    return when (inputClass) {
        InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
}

internal fun applyKeyboardShift(text: CharSequence, shifted: Boolean): String {
    if (!shifted) return text.toString()

    return buildString(text.length) {
        for (character in text) {
            append(Character.toUpperCase(character))
        }
    }
}

internal fun commitStyledCharacter(
    connection: EditorInputConnection,
    intendedText: String,
    style: AppTextStyle?,
    isPasswordField: Boolean
): Boolean {
    val activeStyle = if (isPasswordField) null else style
    val text = if (activeStyle == null) {
        intendedText
    } else {
        val sequence = if (activeStyle.isSequenceAware) {
            connection.getTextBeforeCursor(5, 0)
        } else {
            null
        }
        activeStyle.encode(intendedText, sequence) ?: intendedText
    }

    val committed = connection.commitText(text, 1)
    if (committed && activeStyle?.isReversed == true) {
        return connection.commitText("", -text.length)
    }
    return committed
}

/**
 * Deletes a live selection with commitText, which replaces the editor's selection without
 * reading its contents. Unknown selection state uses the same safe editor-handled fallback.
 */
internal fun deleteBackspace(
    connection: EditorInputConnection,
    selection: SelectionSnapshot,
    reverseCursorDirection: Boolean,
    isPasswordField: Boolean
): Boolean {
    if (!selection.isKnown || selection.hasSelection) {
        return connection.commitText("", 1)
    }

    if (isPasswordField) return connection.deleteCodePoints(1, 0)
    if (reverseCursorDirection) return connection.deleteCodePoints(0, 1)

    // The boxed letter occupies two UTF-16 units and its text presentation selector adds one.
    return when (val before = connection.getTextBeforeCursor(3, 0)?.toString()) {
        "🅰\uFE0E", "🅱\uFE0E", "🅾\uFE0E", "🅿\uFE0E" ->
            connection.deleteTextBeforeCursor(before.length)
        else -> connection.deleteCodePoints(1, 0)
    }
}
