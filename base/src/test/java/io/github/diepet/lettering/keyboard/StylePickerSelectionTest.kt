package io.github.diepet.lettering.keyboard

import io.github.diepet.lettering.styles.AppTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StylePickerSelectionTest {
    @Test
    fun activeStyleStaysSelectedByIdWhenOrderChanges() {
        val first = TestStyle("first")
        val second = TestStyle("second")
        val selection = StylePickerSelection()
        val all = listOf(first, second)

        assertTrue(selection.select("second", all))
        selection.reconcile(all.reversed(), all)

        assertEquals(0, selection.selectedIndex(all.reversed()))
        assertEquals("second", selection.selectedStyle(all.reversed())?.styleId)
    }

    @Test
    fun disabledStyleFallsBackToPlainAndCanBeReenabled() {
        val style = TestStyle("style")
        val selection = StylePickerSelection()

        selection.select("style", listOf(style))
        selection.reconcile(emptyList(), listOf(style))
        assertEquals(-1, selection.selectedIndex(emptyList()))
        assertNull(selection.selectedStyle(emptyList()))

        selection.reconcile(listOf(style), listOf(style))
        assertEquals(-1, selection.selectedIndex(listOf(style)))
        selection.toggle(listOf(style))
        assertEquals(0, selection.selectedIndex(listOf(style)))
    }

    @Test
    fun disablingEveryStyleLeavesPlainTypingAndEmptyPicker() {
        val style = TestStyle("style")
        val selection = StylePickerSelection()

        selection.select("style", listOf(style))
        selection.reconcile(emptyList(), listOf(style))
        selection.toggle(emptyList())

        assertEquals(-1, selection.selectedIndex(emptyList()))
        assertNull(selection.selectedStyle(emptyList()))
        assertFalse(selection.select("style", emptyList()))
    }

    private class TestStyle(id: String) : AppTextStyle(id, id, true, 0) {
        init {
            encodedName = id
        }

        override fun encode(text: String?, sequence: CharSequence?): String? = text
    }
}
