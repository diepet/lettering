package io.github.diepet.lettering.keyboard

import android.text.InputType
import io.github.diepet.lettering.styles.AppTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInputPolicyTest {
    @Test
    fun recognizesTextAndNumericPasswordVariationsByClassAndVariation() {
        val passwordTypes = listOf(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )

        passwordTypes.forEach { assertTrue(isPasswordInputType(it)) }
        assertFalse(isPasswordInputType(InputType.TYPE_CLASS_TEXT))
        assertFalse(isPasswordInputType(InputType.TYPE_CLASS_NUMBER))
        assertFalse(
            isPasswordInputType(
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )
        )
    }

    @Test
    fun passwordTypingKeepsShiftedIntendedTextAndDoesNotReadOrTransform() {
        val connection = RecordingConnection()
        val sequenceStyle = TestStyle(sequenceAware = true, reverse = true)

        commitStyledCharacter(connection, "A", sequenceStyle, isPasswordField = true)

        assertEquals(listOf("A"), connection.committedText)
        assertEquals(0, connection.beforeCursorRequests)
        assertEquals(emptyList<Int>(), connection.cursorMoves)
    }

    @Test
    fun reverseStyleMovesCursorOnlyForRegularTyping() {
        val connection = RecordingConnection()

        commitStyledCharacter(
            connection,
            "x",
            TestStyle(sequenceAware = false, reverse = true),
            isPasswordField = false
        )

        assertEquals(listOf("styled:x", ""), connection.committedText)
        assertEquals(listOf(-"styled:x".length), connection.cursorMoves)
    }

    @Test
    fun switchingFromPasswordToRegularFieldAppliesSelectedStyleAgain() {
        val session = InputSessionState()
        val style = TestStyle(sequenceAware = true)
        val connection = RecordingConnection()
        val passwordType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        session.start(passwordType, 0, 0)
        commitStyledCharacter(connection, "A", style, session.isPasswordField)
        session.start(InputType.TYPE_CLASS_TEXT, 0, 0)
        commitStyledCharacter(connection, "b", style, session.isPasswordField)

        assertFalse(session.isPasswordField)
        assertEquals(listOf("A", "styled:b"), connection.committedText)
        assertEquals(1, connection.beforeCursorRequests)
    }

    @Test
    fun popupTextUsesShiftAndStylePipelineForAccentedCharacters() {
        val connection = RecordingConnection()
        val intendedText = applyKeyboardShift("àèìòù", shifted = true)

        commitStyledCharacter(connection, intendedText, TestStyle(sequenceAware = false), false)

        assertEquals(listOf("styled:ÀÈÌÒÙ"), connection.committedText)
    }

    @Test
    fun backspaceUsesEditorSelectionAndBothCollapsedDirectionsWithoutReadingText() {
        val connection = RecordingConnection()

        deleteBackspace(
            connection,
            SelectionSnapshot.from(8, 3),
            reverseCursorDirection = false,
            isPasswordField = false
        )
        deleteBackspace(
            connection,
            SelectionSnapshot.from(4, 4),
            reverseCursorDirection = false,
            isPasswordField = false
        )
        deleteBackspace(
            connection,
            SelectionSnapshot.from(4, 4),
            reverseCursorDirection = true,
            isPasswordField = false
        )

        assertEquals(listOf(""), connection.committedText)
        assertEquals(listOf(1 to 0, 0 to 1), connection.deletions)
        assertEquals(0, connection.beforeCursorRequests)
    }

    @Test
    fun unknownSelectionUsesSafeEditorFallbackAndPasswordAlwaysDeletesNormally() {
        val connection = RecordingConnection()

        assertTrue(
            deleteBackspace(
                connection,
                SelectionSnapshot.unknown(),
                reverseCursorDirection = true,
                isPasswordField = false
            )
        )
        deleteBackspace(
            connection,
            SelectionSnapshot.from(2, 2),
            reverseCursorDirection = true,
            isPasswordField = true
        )

        assertEquals(listOf(""), connection.committedText)
        assertEquals(listOf(1 to 0), connection.deletions)
        assertEquals(0, connection.beforeCursorRequests)
    }

    @Test
    fun repeatedBackspaceAfterEditorOperationUsesCollapsedSelection() {
        val connection = RecordingConnection()
        var selection = SelectionSnapshot.from(5, 5)

        repeat(3) {
            assertTrue(deleteBackspace(connection, selection, false, false))
            selection = SelectionSnapshot.collapsed()
        }

        assertEquals(listOf(1 to 0, 1 to 0, 1 to 0), connection.deletions)
        assertEquals(0, connection.beforeCursorRequests)
    }

    private class TestStyle(
        sequenceAware: Boolean,
        reverse: Boolean = false
    ) : AppTextStyle("test", "Test", true, 0, reverse, sequenceAware) {
        init {
            encodedName = "Test"
        }

        override fun encode(text: String?, sequence: CharSequence?): String? =
            text?.let { "styled:$it" }
    }

    private class RecordingConnection : EditorInputConnection {
        val committedText = mutableListOf<String>()
        val cursorMoves = mutableListOf<Int>()
        val deletions = mutableListOf<Pair<Int, Int>>()
        var beforeCursorRequests = 0

        override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence? {
            beforeCursorRequests++
            return "previous"
        }

        override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
            committedText.add(text.toString())
            if (text.isEmpty()) cursorMoves.add(newCursorPosition)
            return true
        }

        override fun deleteCodePoints(beforeLength: Int, afterLength: Int): Boolean {
            deletions.add(beforeLength to afterLength)
            return true
        }
    }
}
