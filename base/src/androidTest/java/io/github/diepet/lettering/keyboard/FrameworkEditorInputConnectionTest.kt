package io.github.diepet.lettering.keyboard

import android.text.Selection
import android.view.View
import android.view.inputmethod.BaseInputConnection
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameworkEditorInputConnectionTest {
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
