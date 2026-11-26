package io.github.diepet.lettering.styles

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.diepet.lettering.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvailableStylesTest {
    @Test
    fun defaultOrderAppliesToAllStylesAndTheEnabledStylePicker() = withFreshStylePreferences { context ->
        val expectedNames = listOf(
            R.string.name_cutout,
            R.string.name_olde,
            R.string.name_script_bold,
            R.string.name_organic,
            R.string.name_upsidedown,
            R.string.name_underlined,
            R.string.name_spongemock,
            R.string.name_mirror,
            R.string.name_supertiny,
            R.string.name_script,
            R.string.name_strikethrough,
            R.string.name_filled_boxes,
            R.string.name_clear_boxes,
            R.string.name_maritime,
            R.string.name_filled_circles,
            R.string.name_clear_circles,
            R.string.name_mono,
            R.string.name_olde_bold,
            R.string.name_zalgo,
            R.string.name_full_width,
            R.string.name_clapclap,
            R.string.name_ss_bold,
            R.string.name_serif_bold,
            R.string.name_ss_italic,
            R.string.name_serif_italic,
            R.string.name_ss_bold_italic,
            R.string.name_serif_bold_italic,
            R.string.name_frowny_faces,
            R.string.name_happy_faces,
            R.string.name_thunder,
            R.string.name_slashed,
            R.string.name_mustache,
            R.string.name_starry_night,
            R.string.name_lite_strikes,
            R.string.name_small_caps,
        ).map { context.getString(it) }

        val styles = AvailableStyles.getStyles()
        assertEquals(expectedNames, styles.map { it.displayName })
        // Filled and unfilled variants can have the same display name.
        assertEquals("🅰\uFE0E", styles[11].encode("A"))
        assertEquals("🄰", styles[12].encode("A"))
        assertEquals("🅐", styles[14].encode("A"))
        assertEquals("Ⓐ", styles[15].encode("A"))
        assertEquals(styles, AvailableStyles.getEnabledStyles())
    }

    @Test
    fun savedCustomOrderTakesPrecedenceAfterReinitializing() = withFreshStylePreferences { context ->
        val defaultOrder = AvailableStyles.getStyles().map { it.styleId }
        val movedStyle = defaultOrder.last()
        AvailableStyles.setNewPosition(movedStyle, 0)

        AvailableStyles.init(context)

        assertEquals(listOf(movedStyle) + defaultOrder.dropLast(1),
            AvailableStyles.getStyles().map { it.styleId })
    }

    private fun withFreshStylePreferences(test: (Context) -> Unit) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val preferencesName = "style_order_test"
        val preferences = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val testContext = object : ContextWrapper(appContext) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
        }

        preferences.edit().clear().commit()
        try {
            AvailableStyles.init(testContext)
            test(testContext)
        } finally {
            appContext.deleteSharedPreferences(preferencesName)
            AvailableStyles.init(appContext)
        }
    }
}
