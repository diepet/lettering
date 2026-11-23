package io.github.diepet.lettering.keyboard

object Constants {

    // Alpha keyboard layouts
    const val QWERTY = 1
    const val AZERTY = 2
    const val QWERTZ = 3
    const val DVORAK = 4
    const val DEFAULT_LAYOUT = QWERTY

    // themes
    const val LIGHT = 1
    const val DARK = 2
    const val AUTO = 3
    const val DEFAULT_THEME = AUTO

    // Key actions use negative values, outside the Unicode range.
    const val SHIFT = -1
    const val ENTER = -4
    const val DELETE = -5
    const val ALPHA_KEYBOARD_KEYCODE = -10
    const val SECONDARY_KBD_KEYCODE = -11
    const val KEYCODE_SPACE = 32

    // other
    const val DOUBLETAP_MAX_DELAY_MS = 500L
    const val VIBRATION_DURATION_MS = 25L
    const val LONG_PRESS = 400L
    const val DEFAULT_HEIGHT = 8
    const val DEFAULT_VIBRATIONS = false
    const val REGULAR_STYLE_INDEX = -1 // Font with "no style"
}