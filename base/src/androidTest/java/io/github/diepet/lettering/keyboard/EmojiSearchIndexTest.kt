package io.github.diepet.lettering.keyboard

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiSearchIndexTest {
    private fun entries(languageTag: String): List<EmojiSearchEntry> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return EmojiSearchIndex.load(context.createConfigurationContext(configuration))
    }

    @Test
    fun localizedNamesAndKeywordsStayPairedWithTheEmojiVariant() {
        val variant = "👩🏽‍💻"
        val french = entries("fr-FR").single { it.emoji == variant }
        assertEquals("informaticienne : peau légèrement mate", french.description)
        assertTrue(french.normalizedSearchTerms.startsWith("informaticienne : peau legerement mate codeuse"))
        assertTrue(french.normalizedSearchTerms.contains("developpeuse"))
        val norwegian = entries("nb-NO").single { it.emoji == variant }
        assertEquals("kvinnelig IT-ekspert: hudtype 4", norwegian.description)
    }

    @Test
    fun esperantoUsesEnglishEmojiAnnotations() {
        val heart = entries("eo").single { it.emoji == "❤️" }
        assertEquals("red heart", heart.description)
        assertEquals("red heart emotion heart love red", heart.searchTerms)
    }
}
