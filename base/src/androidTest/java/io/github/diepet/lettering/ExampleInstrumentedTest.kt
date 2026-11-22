package io.github.diepet.lettering

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.diepet.lettering.styles.AvailableStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.diepet.lettering", appContext.packageName)
    }

    @Test
    fun disabledStylesAreRestoredWhenStyleCatalogIsInitializedAgain() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        AvailableStyles.init(appContext)
        val style = AvailableStyles.getStyles().firstOrNull { it.isEnabled }
        assertNotNull("At least one style should be enabled for this test", style)
        val styleId = style!!.styleId

        AvailableStyles.toggleEnabled(styleId)
        assertFalse(AvailableStyles.getStyles().first { it.styleId == styleId }.isEnabled)

        AvailableStyles.init(appContext)
        assertFalse(AvailableStyles.getStyles().first { it.styleId == styleId }.isEnabled)

        AvailableStyles.toggleEnabled(styleId)
        AvailableStyles.init(appContext)
        assertTrue(AvailableStyles.getStyles().first { it.styleId == styleId }.isEnabled)
    }
}
