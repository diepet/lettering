package io.github.diepet.lettering.keyboard

import android.content.Context
import java.text.Normalizer
import java.util.Locale
import java.util.zip.GZIPInputStream

internal data class EmojiSearchEntry(
    val emoji: String,
    val searchTerms: String,
    val description: String,
    val normalizedSearchTerms: String = normalizeEmojiSearchText(searchTerms)
)

internal object EmojiSearchIndex {
    private val entriesByLocale = mutableMapOf<String, List<EmojiSearchEntry>>()

    @Synchronized
    fun load(context: Context): List<EmojiSearchEntry> {
        val activeLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val locale = supportedLocale(activeLocale)
        return entriesByLocale.getOrPut(locale) {
            openIndex(context, "sequences").use { sequences ->
                openIndex(context, locale).useLines { annotations ->
                    annotations.map { line ->
                        val separator = line.indexOf('\t')
                        val description = line.substring(0, separator)
                        EmojiSearchEntry(
                            emoji = checkNotNull(sequences.readLine()),
                            searchTerms = description + line.substring(separator + 1),
                            description = description
                        )
                    }.toList()
                }
            }
        }
    }

    private fun openIndex(context: Context, name: String) =
        GZIPInputStream(context.assets.open("emoji-search/$name.idx")).bufferedReader(Charsets.UTF_8)

    private fun supportedLocale(locale: Locale): String = when (locale.language.lowercase(Locale.ROOT)) {
        "ar", "ca", "de", "es", "fi", "fr", "it", "ja", "pt", "ru", "ta", "uk", "zh" ->
            locale.language.lowercase(Locale.ROOT)
        "nb", "no" -> "no"
        // CLDR does not publish Esperanto emoji annotations.
        "eo" -> "en"
        else -> "en"
    }
}

private val whitespace = Regex("\\s+")

internal fun normalizeEmojiSearchText(text: String): String {
    val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    return buildString(decomposed.length) {
        for (character in decomposed) {
            val type = Character.getType(character)
            if (type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                type != Character.ENCLOSING_MARK.toInt()
            ) {
                append(character)
            }
        }
    }.trim().replace(whitespace, " ")
}
