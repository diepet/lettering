package io.github.diepet.lettering.styles

class RandomCaps internal constructor(
    name: String,
    priority: Int,
    enabled: Boolean
) : AppTextStyle(id, name, enabled, priority, false, true) {

    init {
        encodedName = encode(name)!!
    }

    companion object {
        const val id = "spongemock"
    }


    override fun encode(text: String?, sequence: CharSequence?): String? {
        if (!isEmpty(text)) {
            var lowercase = false
            if (sequence != null && sequence.lastIndex >= 0) {
                val trimmedSeq = sequence.trim()
                if (trimmedSeq.isNotEmpty()) {
                    val lastChar = trimmedSeq[trimmedSeq.lastIndex].code
                    lowercase = lastChar in 'A'.code..'Z'.code
                }
            }
            val cs = text!!.toCharArray()
            val temp = StringBuilder()
            for (c in cs) {
                temp.append(if (lowercase) c.lowercaseChar() else c.uppercaseChar())
                lowercase = !lowercase
            }
            return temp.toString()
        }
        return text
    }
}
