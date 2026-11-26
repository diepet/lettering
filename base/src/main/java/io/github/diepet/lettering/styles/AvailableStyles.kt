package io.github.diepet.lettering.styles

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.content.res.TypedArray
import io.github.diepet.lettering.letteringPreferences
import io.github.diepet.lettering.R
import java.util.*
import kotlin.collections.HashSet

object AvailableStyles {

    private const val disabled_key = "pref_disabled_fonts"
    private const val order_key = "pref_font_order"

    // Resource IDs keep the default order independent of localized names and saved style IDs.
    private val defaultStyleOrder = intArrayOf(
        R.array.charset_cutout,
        R.array.charset_olde,
        R.array.charset_script_bold,
        R.array.charset_organic,
        R.array.charset_upside_down,
        R.array.charset_underlined,
        R.string.name_spongemock,
        R.array.charset_mirrored,
        R.array.charset_supertiny,
        R.array.charset_script,
        R.array.charset_strikethrough,
        R.array.charset_filled_boxes,
        R.array.charset_clear_boxes,
        R.array.charset_maritime,
        R.array.charset_filled_circles,
        R.array.charset_clear_circles,
        R.array.charset_mono,
        R.array.charset_olde_bold,
        R.string.name_zalgo,
        R.array.charset_extra_wide,
        R.array.charset_clapclap,
        R.array.sans_bold,
        R.array.charset_bold_serif,
        R.array.sans_italic,
        R.array.charset_serif_italic,
        R.array.sans_bolditalic,
        R.array.charset_bolditalic,
        R.array.accent_frowny_face,
        R.array.accent_happy_face,
        R.array.accent_thunder,
        R.array.accents_slashed,
        R.array.accents_mustache,
        R.array.starry_night,
        R.array.accents_lite_strikes,
        R.array.charset_small_caps,
    )

    private val styles = LinkedList<AppTextStyle>()
    private val disabledList = HashSet<String>()
    private val styleOrder = LinkedList<String>()
    private lateinit var prefs: SharedPreferences

    class SortComparator : Comparator<AppTextStyle> {
        override fun compare(style1: AppTextStyle, style2: AppTextStyle): Int {
            val a = style1.priority
            val b = style2.priority
            return if (a < b) -1 else if (a > b) 1 else 0
        }
    }

    /**
     * This method returns a list of all known styles
     */
    fun getStyles(): List<AppTextStyle> {
        return styles.sortedWith(SortComparator())
    }

    /**
     * This method returns a list of enabled text styles only.
     * All styles are enabled by default but user may
     * choose to disable some
     */
    fun getEnabledStyles(): List<AppTextStyle> {
        val enableCondition: ((AppTextStyle)) -> Boolean = { it.isEnabled }
        return styles.filter(enableCondition).sortedWith(SortComparator())
    }

    /**
     * Call this method once on application start to initialize application text styles
     *
     * @param ctx - Context reference
     */
    fun init(ctx: Context) {
        styles.clear()
        disabledList.clear()
        styleOrder.clear()
        val res = ctx.resources
        prefs = ctx.letteringPreferences
        restoreDisabledList()
        restoreOrder()
        initCharsetFonts(res, res.obtainTypedArray(R.array.charset_styles))
        styles.add(
            RandomCaps(
                res.getString(R.string.name_spongemock),
                getOrder(RandomCaps.id, R.string.name_spongemock),
                !disabledList.contains(RandomCaps.id)
            )
        )
        styles.add(
            Zalgo(
                res.getString(R.string.name_zalgo),
                getOrder(Zalgo.id, R.string.name_zalgo),
                !disabledList.contains(Zalgo.id)
            )
        )
        initAccentStyles(res, res.obtainTypedArray(R.array.accent_styles))
    }

    private fun persistDisabledList() {
        val arr = disabledList.toSet()
        val editor = prefs.edit()
        editor.putStringSet(disabled_key, arr)
        editor.apply()
    }

    private fun restoreDisabledList() {
        val prefList = prefs.getStringSet(disabled_key, null)
        if (prefList != null) disabledList.addAll(prefList)
    }

    private fun persistOrder() {
        val arr = styleOrder.joinToString(",")
        val editor = prefs.edit()
        editor.putString(order_key, arr)
        editor.apply()
    }

    private fun restoreOrder() {
        val prefList = prefs.getString(order_key, null)
        if (prefList != null) {
            val temp = prefList.split(",")
            styleOrder.addAll(temp)
        }
    }

    private fun getOrder(styleId: String, styleResourceId: Int): Int {
        val position = if (styleOrder.isEmpty()) {
            defaultStyleOrder.indexOf(styleResourceId)
        } else {
            styleOrder.indexOf(styleId)
        }
        // Stable sorting keeps unlisted styles at the end in registration order.
        return if (position >= 0) position else Int.MAX_VALUE
    }

    fun setNewPosition(id: String, position: Int) {
        styleOrder.clear()
        val sorted = getStyles()
        for (i in sorted.indices) {
            val fId = sorted[i].styleId
            if (id != fId) styleOrder.add(fId)
        }
        styleOrder.add(position, id)
        for (i in styles.indices) {
            styles[i].priority = styleOrder.indexOf(styles[i].styleId)
        }
        persistOrder()
    }

    fun toggleEnabled(id: String) {
        styles.forEach {
            if (it.styleId == id) {
                val nextState = !it.isEnabled
                it.isEnabled = nextState
                if (!nextState) disabledList.add(id)
                else disabledList.remove(id)
                persistDisabledList()
                return
            }
        }
    }

    /**
     * Initialize character set styles
     *
     * @param res - Android resources reference
     * @param ta  - TypedArray of all styles with custom character set
     */
    private fun initCharsetFonts(res: Resources, ta: TypedArray) {

        val nameIndex = res.getInteger(R.integer.CharsetNameIndex)
        val reverseIndex = res.getInteger(R.integer.CharsetReverseIndex)
        val charsetIndex = res.getInteger(R.integer.CharsetIndex)

        for (i in 0 until ta.length()) {
            val id = ta.getResourceId(i, 0)
            if (id > 0) {
                val styleDef = res.obtainTypedArray(id)
                val styleId = styleDef.getString(nameIndex) + i.toShort()
                val name = styleDef.getString(nameIndex)
                val charset = res.getStringArray(styleDef.getResourceId(charsetIndex, 0))
                val reversed = styleDef.getBoolean(reverseIndex, false)
                val enabled = !disabledList.contains(styleId)
                val order = getOrder(styleId, id)
                if (name != null)
                    styles.add(CharsetStyle(styleId, name, order, enabled, charset, reversed))
                styleDef.recycle()
            }
        }
        ta.recycle()
    }

    /**
     * Initialize accented styles
     *
     * @param res - Android resources reference
     * @param ta  - TypedArray of all accented styles
     */
    private fun initAccentStyles(res: Resources, ta: TypedArray) {

        // Initialize accented styles
        val accentIdIndex = res.getInteger(R.integer.AccentIdIndex)
        val accentNameIndex = res.getInteger(R.integer.AccentNameIndex)
        val accentCharIndex = res.getInteger(R.integer.AccentValue)

        for (i in 0 until ta.length()) {
            val id = ta.getResourceId(i, 0)
            if (id > 0) {
                val styleDef = res.obtainTypedArray(id)
                val styleId = styleDef.getString(accentIdIndex)
                val name = styleDef.getString(accentNameIndex)
                val accentChar = styleDef.getString(accentCharIndex)
                val enabled = !disabledList.contains(styleId)
                if (styleId != null && name != null && accentChar != null) {
                    val order = getOrder(styleId, id)
                    styles.add(AccentStyle(styleId, name, order, enabled, accentChar))
                }
                styleDef.recycle()
            }
        }
        ta.recycle()
    }
}
