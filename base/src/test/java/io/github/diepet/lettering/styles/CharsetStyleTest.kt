package io.github.diepet.lettering.styles

import org.junit.Assert.assertEquals
import org.junit.Test

class CharsetStyleTest {
    @Test
    fun accentedLatinLettersUseStyledBaseAndKeepTheirAccent() {
        val style = createStyle()

        assertEquals(
            "[a]\u0300[e]\u0300[i]\u0300[o]\u0300[u]\u0300[e]\u0301",
            style.encode("àèìòùé")
        )
        assertEquals("[A]\u0300[E]\u0301", style.encode("ÀÉ"))
    }

    @Test
    fun decomposedAccentsStayAttachedWhenReverseStyleReversesText() {
        val style = createStyle(reverse = true)

        assertEquals("[a][e]\u0301", style.encode("e\u0301a"))
    }

    private fun createStyle(reverse: Boolean = false): CharsetStyle {
        val charset = Array(95) { index -> (index + 32).toChar().toString() }
        for (letter in 'a'..'z') {
            charset[letter.code - 32] = "[$letter]"
        }
        for (letter in 'A'..'Z') {
            charset[letter.code - 32] = "[$letter]"
        }
        return CharsetStyle("test", "test", 0, true, charset, reverse)
    }
}
