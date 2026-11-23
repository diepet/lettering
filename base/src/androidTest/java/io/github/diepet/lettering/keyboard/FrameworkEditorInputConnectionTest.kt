package io.github.diepet.lettering.keyboard

import android.text.Selection
import android.view.View
import android.view.inputmethod.BaseInputConnection
import androidx.test.platform.app.InstrumentationRegistry
import io.github.diepet.lettering.R
import io.github.diepet.lettering.styles.CharsetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameworkEditorInputConnectionTest {
    @Test
    fun filledBoxesRequestTextPresentationAndBackspaceRemovesTheWholeBox() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val charset = context.resources.getStringArray(R.array.filled_box)
        val style = CharsetStyle("boxes", "Boxes", 0, true, charset, false)
        val withSelectors = "🅰\uFE0E🅱\uFE0E🅾\uFE0E🅿\uFE0E"
        assertEquals(withSelectors + withSelectors + "🅲", style.encode("ABOPabopC"))
        assertEquals("🅱\uFE0E🅾\uFE0E🆇🅴🆂", style.styledName)

        val editor = BaseInputConnection(View(context), true)
        val connection = FrameworkEditorInputConnection(editor)
        val text = editor.editable!!
        text.append("xy")
        Selection.setSelection(text, 1)
        for (letter in "ABOP") {
            assertTrue(commitStyledCharacter(connection, letter.toString(), style, false))
        }
        assertEquals("x${withSelectors}y", text.toString())
        repeat(4) { assertTrue(deleteBackspace(connection, SelectionSnapshot.collapsed(), false, false)) }
        assertEquals("xy", text.toString())
    }

    @Test
    fun backspaceRemovesACompleteStyledCodePointInBothTypingDirections() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val editor = BaseInputConnection(View(context), true)
        val connection = FrameworkEditorInputConnection(editor)
        val text = editor.editable!!
        text.append("x𝕒𝕓y")
        Selection.setSelection(text, 3)
        assertTrue(deleteBackspace(connection, SelectionSnapshot.collapsed(), false, false))
        assertEquals("x𝕓y", text.toString())
        assertTrue(deleteBackspace(connection, SelectionSnapshot.collapsed(), true, false))
        assertEquals("xy", text.toString())
    }
}
