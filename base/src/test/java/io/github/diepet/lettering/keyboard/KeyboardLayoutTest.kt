package io.github.diepet.lettering.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun alphabetPreferencesKeepTheirLetterOrderAndBottomRowPunctuation() {
        fun characters(preference: Int) = KeyboardLayout.forAlphabet(preference).rows.map { row ->
            row.filter { it.code > 0 }.joinToString("") { it.code.toChar().toString() }
        }
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm", ", ."), characters(Constants.QWERTY))
        assertEquals(listOf("qwertzuiop", "asdfghjkl", "yxcvbnm", ", ."), characters(Constants.QWERTZ))
        assertEquals(listOf("azertyuiop", "qsdfghjklm", "wxcvbn'", "/, ."), characters(Constants.AZERTY))
        assertEquals(listOf("',.pyfgcrl", "aoeuidhtns", "jkxbmwv", "q z"), characters(Constants.DVORAK))
        assertEquals(characters(Constants.QWERTY), characters(0))
    }

    @Test
    fun rowWidthsFitIncludingIndentedRowsAndTheNarrowerAzertySpacebar() {
        val layouts = listOf(Constants.QWERTY, Constants.AZERTY, Constants.QWERTZ, Constants.DVORAK)
            .map { KeyboardLayout.forAlphabet(it) } + listOf(KeyboardLayout.phone, KeyboardLayout.numbers, KeyboardLayout.math)
        for (layout in layouts) {
            val widths = layout.rows.map { row -> row.sumOf { it.width } }
            assertEquals(listOf(100, if (layout.rows[1].size == 9) 90 else 100, 100, 100), widths)
        }
    }
}
