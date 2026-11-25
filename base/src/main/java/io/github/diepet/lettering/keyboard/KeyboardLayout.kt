package io.github.diepet.lettering.keyboard

import io.github.diepet.lettering.R

/** Widths are percentages of the keyboard width. Short rows are centered. */
internal data class Key(
    val code: Int,
    val label: String = "",
    val width: Int = 10,
    val icon: Int = 0,
    val description: Int = 0,
    val alternates: Int = 0,
    val cycle: String = ""
)

internal enum class ShiftState { OFF, ONCE, LOCKED }

internal class KeyboardLayout(val rows: List<List<Key>>) {
    companion object {
        private val shift = Key(Constants.SHIFT, width = 15, icon = R.drawable.kbd_ic_arrow_up_bold_outline,
            description = R.string.key_shift)
        private val delete = Key(Constants.DELETE, width = 15, icon = R.drawable.kbd_ic_backspace_outline,
            description = R.string.key_delete)
        private val enter = Key(Constants.ENTER, width = 15, icon = R.drawable.kbd_ic_keyboard_return,
            description = R.string.key_enter)
        private val space = Key(Constants.KEYCODE_SPACE, width = 50, icon = R.drawable.kbd_ic_keyboard_space,
            description = R.string.key_space)
        private val emoji = Key(Constants.EMOJI_PICKER_KEYCODE, width = 10, icon = R.drawable.kbd_ic_emoji,
            description = R.string.key_emoji)
        private val symbols = Key(Constants.SECONDARY_KBD_KEYCODE, width = 15, icon = R.drawable.kbd_ic_numeric,
            description = R.string.key_symbols)
        private val letters = Key(Constants.ALPHA_KEYBOARD_KEYCODE, width = 15, icon = R.drawable.kbd_ic_alphabetical_variant,
            description = R.string.key_letters)

        private fun keys(chars: String) = chars.map { char ->
            val alternates = when (char) {
                'a' -> R.string.alternates_for_a
                'c' -> R.string.alternates_for_c
                'e' -> R.string.alternates_for_e
                'i' -> R.string.alternates_for_i
                'l' -> R.string.alternates_for_l
                'n' -> R.string.alternates_for_n
                'o' -> R.string.alternates_for_o
                's' -> R.string.alternates_for_s
                'u' -> R.string.alternates_for_u
                'y' -> R.string.alternates_for_y
                'z' -> R.string.alternates_for_z
                else -> 0
            }
            Key(char.code, char.toString(), alternates = alternates)
        }

        private fun bottom(switch: Key, left: String, right: String, includeEmoji: Boolean = true): List<Key> {
            val emojiKeys = if (includeEmoji) listOf(emoji) else emptyList()
            val spaceWidth = 100 - switch.width - enter.width - 10 * (left.length + right.length) - emojiKeys.sumOf { it.width }
            return listOf(switch) + keys(left) + emojiKeys + space.copy(width = spaceWidth) + keys(right) + enter
        }

        private fun alphabet(top: String, middle: String, lower: String, left: String = ",", right: String = ".") =
            KeyboardLayout(listOf(keys(top), keys(middle), listOf(shift) + keys(lower) + delete,
                bottom(symbols, left, right)))

        private val qwerty by lazy { alphabet("qwertyuiop", "asdfghjkl", "zxcvbnm") }
        private val qwertz by lazy { alphabet("qwertzuiop", "asdfghjkl", "yxcvbnm") }
        private val azerty by lazy { alphabet("azertyuiop", "qsdfghjklm", "wxcvbn'", "/,") }
        private val dvorak by lazy { alphabet("',.pyfgcrl", "aoeuidhtns", "jkxbmwv", "q", "z") }

        fun forAlphabet(preference: Int) = when (preference) {
            Constants.AZERTY -> azerty
            Constants.QWERTZ -> qwertz
            Constants.DVORAK -> dvorak
            else -> qwerty
        }

        val numbers by lazy {
            val mathSwitch = Key(Constants.SECONDARY_KBD_KEYCODE, "=\\<", 15)
            KeyboardLayout(listOf(keys("1234567890"), keys("@#\$_&-+()/"),
                listOf(mathSwitch) + keys("*\"':;!?") + delete, bottom(letters, ",", ".")))
        }

        val math by lazy {
            KeyboardLayout(listOf(keys("~`|•√π÷×¶∆"), keys("£¢€¥^°={}\\"),
                listOf(symbols) + keys("%©®™✓[]") + delete, bottom(letters, "<", ">", includeEmoji = false)))
        }

        val phone by lazy {
            val minus = Key('-'.code, icon = R.drawable.kbd_ic_minus, description = R.string.key_minus)
            val rows = listOf(
                keys("123") + minus, keys("456") + space, keys("789") + delete,
                listOf(Key('*'.code, "*#", cycle = "*#"), Key('0'.code, "0+", cycle = "0+")) + keys(".") + enter
            )
            KeyboardLayout(rows.map { row -> row.map { it.copy(width = 25) } })
        }
    }
}
