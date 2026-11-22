package io.github.diepet.lettering.styles

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZalgoTest {
    @Test
    fun zalgoDoesNotNeedSurroundingTextAndStillAddsCombiningMarks() {
        val style = Zalgo("Zalgo", 0, true)

        assertFalse(style.isSequenceAware)
        val encoded = style.encode("a", null)!!

        assertTrue(encoded.startsWith("a"))
        assertTrue(encoded.drop(1).any { character ->
            when (Character.getType(character)) {
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt() -> true
                else -> false
            }
        })
    }

    @Test
    fun randomCapsRemainsSequenceAware() {
        assertTrue(RandomCaps("Random Caps", 0, true).isSequenceAware)
    }
}
