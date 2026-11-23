package io.github.diepet.lettering.keyboard

import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.diepet.lettering.MainActivity
import io.github.diepet.lettering.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LetteringKeyboardViewTest {
    @Test
    fun keyboardAndStylePickerInflateInEveryTheme() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                for (theme in listOf(R.style.KeyboardTheme, R.style.KeyboardThemeLight, R.style.KeyboardThemeDark)) {
                    val inflater = LayoutInflater.from(ContextThemeWrapper(activity, theme))
                    val keyboard = inflater.inflate(R.layout.keyboard_view, null)
                    val picker = keyboard.findViewById<LinearLayout>(R.id.stylePicker)
                    val style = inflater.inflate(R.layout.item_style, picker, false) as TextView
                    picker.addView(style)
                    style.isSelected = true
                    assertEquals(activity.getColor(R.color.light_style_text_selected), style.currentTextColor)
                }
            }
        }
    }

    private fun withView(layout: KeyboardLayout, check: LetteringKeyboardView.(MutableList<Int>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val context = ContextThemeWrapper(activity, R.style.KeyboardTheme)
                val view = LetteringKeyboardView(context, null)
                activity.setContentView(view)
                val output = mutableListOf<Int>()
                view.onKey = { output.add(it) }
                view.showLayout(layout, 60)
                view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST))
                assertEquals(240, view.measuredHeight)
                view.layout(0, 0, 1000, view.measuredHeight)
                view.check(output)
            }
        }
    }

    private fun LetteringKeyboardView.touch(action: Int, x: Int, y: Int) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x.toFloat(), y.toFloat(), 0)
        dispatchTouchEvent(event)
        event.recycle()
    }

    private fun LetteringKeyboardView.tap(x: Int, y: Int) {
        touch(MotionEvent.ACTION_DOWN, x, y)
        touch(MotionEvent.ACTION_UP, x, y)
    }

    @Test
    fun touchUsesCenteredRowsAndCancelsPendingPressesWhenTheEditorChanges() =
        withView(KeyboardLayout.forAlphabet(Constants.QWERTY)) { output ->
            tap(1, 90)
            touch(MotionEvent.ACTION_DOWN, 50, 30)
            touch(MotionEvent.ACTION_MOVE, 150, 30)
            touch(MotionEvent.ACTION_UP, 150, 30)
            touch(MotionEvent.ACTION_DOWN, 500, 210)
            touch(MotionEvent.ACTION_CANCEL, 500, 210)
            touch(MotionEvent.ACTION_DOWN, 50, 30)
            showLayout(KeyboardLayout.numbers, 60)
            touch(MotionEvent.ACTION_UP, 50, 30)
            measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
            layout(0, 0, measuredWidth, measuredHeight)
            tap(50, 30)
            assertEquals(listOf('a'.code, 'w'.code, '1'.code), output)
        }

    @Test
    fun phoneKeysCycleAndAChangedEditorStartsANewSequence() = withView(KeyboardLayout.phone) { output ->
        tap(375, 210)
        tap(375, 210)
        cancelInput()
        tap(375, 210)
        assertEquals(listOf('0'.code, Constants.DELETE, '+'.code, '0'.code), output)
    }

    @Test
    fun accessibilityExposesShiftedLabelsClickAndLongClick() =
        withView(KeyboardLayout.forAlphabet(Constants.QWERTY)) { output ->
            shiftState = ShiftState.ONCE
            val key = getChildAt(2)
            val e = key.createAccessibilityNodeInfo()
            assertEquals("E", e.contentDescription)
            assertTrue(e.isLongClickable)
            assertTrue(key.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
            assertEquals(listOf('e'.code), output)
            var pickerOpened = false
            onChooseInputMethod = { pickerOpened = true }
            val spaceIndex = KeyboardLayout.forAlphabet(Constants.QWERTY).rows.flatten()
                .indexOfFirst { it.code == Constants.KEYCODE_SPACE }
            assertTrue(getChildAt(spaceIndex).performAccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, null))
            assertTrue(pickerOpened)
        }
}
