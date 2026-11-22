package io.github.diepet.lettering.styles

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

open class CharsetStyle internal constructor(
    id: String,
    name: String,
    priority: Int,
    enabled: Boolean,
    private val charset: Array<String>?,
    reverse: Boolean
) : AppTextStyle(id, name, enabled, priority, reverse) {

    companion object {
        // all character sets start from first printing char
        private const val firstChar = 32
    }

    init {
        encodedName = encode(name)!!
    }

    private fun encodeAllChars(text: String): String {
        val clusters = ArrayList<String>()
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(text)

        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            clusters.add(encodeCluster(text.substring(start, end)))
            start = end
            end = iterator.next()
        }

        return (if (reverse) clusters.asReversed() else clusters).joinToString("")
    }

    private fun encodeCluster(cluster: String): String {
        val normalized = Normalizer.normalize(cluster, Normalizer.Form.NFD)
        val baseCodePoint = normalized.codePointAt(0)
        if (baseCodePoint < firstChar || baseCodePoint > '~'.code) return cluster

        val index = baseCodePoint - firstChar
        val encodedBase = if (index < charset!!.size) charset[index] else baseCodePoint.toChar().toString()
        val marks = normalized.substring(Character.charCount(baseCodePoint))
        return encodedBase + marks
    }

    final override fun encode(text: String?, sequence: CharSequence?): String? {
        if (!isEmpty(text)) {
            return if (charset != null && charset.isNotEmpty()) {
                encodeAllChars(text!!)
            } else {
                text
            }
        }
        return text
    }
}
