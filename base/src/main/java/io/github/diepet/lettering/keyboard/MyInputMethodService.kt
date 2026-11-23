package io.github.diepet.lettering.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.text.method.MetaKeyKeyListener
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import io.github.diepet.lettering.letteringPreferences
import io.github.diepet.lettering.R
import io.github.diepet.lettering.keyboard.Constants.DEFAULT_HEIGHT
import io.github.diepet.lettering.keyboard.Constants.DEFAULT_VIBRATIONS
import io.github.diepet.lettering.keyboard.Constants.DOUBLETAP_MAX_DELAY_MS
import io.github.diepet.lettering.keyboard.Constants.LONG_PRESS
import io.github.diepet.lettering.keyboard.Constants.REGULAR_STYLE_INDEX
import io.github.diepet.lettering.keyboard.Constants.VIBRATION_DURATION_MS
import io.github.diepet.lettering.settings.SettingsActivity
import io.github.diepet.lettering.styles.AvailableStyles.getEnabledStyles
import io.github.diepet.lettering.styles.AvailableStyles.getStyles
import kotlin.math.roundToInt

/**
 * This class sets up and handles virtual keyboard events
 */
class MyInputMethodService : InputMethodService() {

    // All available text styles
    private var styles = getEnabledStyles()
    private val styleSelection = StylePickerSelection()
    private val inputSession = InputSessionState()

    // UI Elements
    private var keyboardView: LetteringKeyboardView? = null
    private var styleToggle: ImageButton? = null
    private var stylePicker: LinearLayout? = null
    private var keyboardExtras: View? = null

    private enum class Page { LETTERS, NUMBERS, MATH, PHONE }

    private var page = Page.LETTERS
    private var metaState = 0L
    private var shift = ShiftState.OFF
    private var lastShift = 0L

    // user preferences
    private var keyVibrations = DEFAULT_VIBRATIONS
    private var keyboardLayout = Constants.DEFAULT_LAYOUT
    private var keyHeight = DEFAULT_HEIGHT
    private var appearance = Constants.DEFAULT_THEME
    private var sysDarkMode: Boolean? = null
    private var longPressMillis: Long = Constants.LONG_PRESS

    override fun onCreate() {
        super.onCreate()
        initPreferences()
    }

    // called initially when inflating keyboard
    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {

        val theme = when (appearance) {
            Constants.DARK -> R.style.KeyboardThemeDark
            Constants.LIGHT -> R.style.KeyboardThemeLight
            else -> R.style.KeyboardTheme
        }
        val contextThemeWrapper = ContextThemeWrapper(this, theme)
        val layout = LayoutInflater.from(contextThemeWrapper)
            .inflate(R.layout.keyboard_view, null)
        layout.setOnApplyWindowInsetsListener { view, insets ->
            val bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            view.setPadding(0, 0, 0, bottom)
            insets
        }

        /* initialize keyboard */
        keyboardView?.cancelInput()
        keyboardView = layout.findViewById<LetteringKeyboardView>(R.id.keyboard_view).apply {
            onKey = ::handleKey
            onPress = ::vibrate
            onChooseInputMethod = ::showKeyboardPicker
        }

        stylePicker = layout.findViewById(R.id.stylePicker)
        keyboardExtras = layout.findViewById(R.id.keyboard_extras)
        styleToggle = layout.findViewById<ImageButton>(R.id.style_button).apply {
            setOnClickListener {
                styleSelection.toggle(styles)
                updateStyleSelection()
            }
        }
        layout.findViewById<View>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        populateStylePicker()
        showPage(page)
        return layout
    }

    private fun initialPage(info: EditorInfo?) = when (info?.inputType?.and(InputType.TYPE_MASK_CLASS)) {
        InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME -> Page.NUMBERS
        InputType.TYPE_CLASS_PHONE -> Page.PHONE
        else -> Page.LETTERS
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshStyles()
        page = initialPage(info)
        if (initPreferences()) setInputView(onCreateInputView()) else showPage(page)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardView?.cancelInput()
        if (!restarting) metaState = 0
        shift = ShiftState.OFF
        page = initialPage(attribute)
        inputSession.start(attribute.inputType, attribute.initialSelStart, attribute.initialSelEnd)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboardView?.cancelInput()
        super.onFinishInputView(finishingInput)
    }

    override fun onWindowHidden() {
        keyboardView?.cancelInput()
        super.onWindowHidden()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        inputSession.updateSelection(newSelStart, newSelEnd)
    }

    private fun handleKey(code: Int) {
        if (currentInputConnection == null) return
        when (code) {
            Constants.SECONDARY_KBD_KEYCODE -> showPage(if (page == Page.NUMBERS) Page.MATH else Page.NUMBERS)
            Constants.ALPHA_KEYBOARD_KEYCODE -> showPage(Page.LETTERS)
            Constants.DELETE -> handleDeleteKeyPress()
            Constants.SHIFT -> handleShiftKeyPress()
            Constants.ENTER -> handleDoneKeyPress()
            else -> commitTypedText(applyKeyboardShift(String(Character.toChars(code)), shift != ShiftState.OFF))
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.repeatCount == 0 && keyboardView?.dismissAlternates() == true) return true
        } else {
            metaState = MetaKeyKeyListener.handleKeyDown(metaState, keyCode, event)
            val code = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(metaState))
            metaState = MetaKeyKeyListener.adjustMetaAfterKeypress(metaState)
            if (code > 0 && Character.isValidCodePoint(code) && currentInputConnection != null) {
                handleKey(code)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        metaState = MetaKeyKeyListener.handleKeyUp(metaState, keyCode, event)
        return super.onKeyUp(keyCode, event)
    }

    private fun showPage(next: Page) {
        page = next
        shift = ShiftState.OFF
        lastShift = 0L
        val layout = when (next) {
            Page.LETTERS -> KeyboardLayout.forAlphabet(keyboardLayout)
            Page.NUMBERS -> KeyboardLayout.numbers
            Page.MATH -> KeyboardLayout.math
            Page.PHONE -> KeyboardLayout.phone
        }
        keyboardExtras?.visibility = if (next == Page.PHONE) View.GONE else View.VISIBLE
        keyboardView?.apply {
            shiftState = shift
            spaceHoldMillis = longPressMillis
            showLayout(layout, keyHeight)
        }
    }

    private fun handleShiftKeyPress() {
        val now = SystemClock.uptimeMillis()
        shift = when {
            shift == ShiftState.OFF -> ShiftState.ONCE
            shift == ShiftState.ONCE && now - lastShift < DOUBLETAP_MAX_DELAY_MS -> ShiftState.LOCKED
            else -> ShiftState.OFF
        }
        lastShift = now
        keyboardView?.shiftState = shift
    }

    /**
     * Handle keyboard delete key press
     */
    private fun handleDeleteKeyPress() {
        val inputConnection = currentInputConnection ?: return
        val deleted = deleteBackspace(
            FrameworkEditorInputConnection(inputConnection),
            inputSession.selection,
            styleSelection.selectedStyle(styles)?.isReversed == true,
            inputSession.isPasswordField
        )
        if (deleted) {
            inputSession.markCollapsed()
        }
    }

    /**
     * Clicking on done key inserts a line break
     */
    private fun handleDoneKeyPress() {
        val inputConnection = currentInputConnection
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun commitTypedText(text: String) {
        val inputConnection = currentInputConnection ?: return
        if (commitStyledCharacter(
                FrameworkEditorInputConnection(inputConnection),
                text,
                styleSelection.selectedStyle(styles),
                inputSession.isPasswordField
            )
        ) {
            inputSession.markCollapsed()
        }

        if (shift == ShiftState.ONCE) {
            shift = ShiftState.OFF
            keyboardView?.shiftState = shift
        }
    }

    private fun populateStylePicker() {
        val picker = stylePicker ?: return
        picker.removeAllViews()
        val inflater = LayoutInflater.from(picker.context)
        for (style in styles) {
            val option = inflater.inflate(R.layout.item_style, picker, false) as TextView
            option.text = style.styledName
            option.contentDescription = style.displayName
            option.setOnClickListener {
                styleSelection.select(style.styleId, styles)
                updateStyleSelection()
            }
            picker.addView(option)
        }
        updateStyleSelection()
    }

    private fun refreshStyles() {
        val enabled = getEnabledStyles()
        styleSelection.reconcile(enabled, getStyles())
        if (enabled != styles) {
            styles = enabled
            populateStylePicker()
        } else {
            updateStyleSelection()
        }
    }

    private fun updateStyleSelection() {
        val selected = styleSelection.selectedIndex(styles)
        val regular = selected == REGULAR_STYLE_INDEX
        styleToggle?.setImageResource(if (regular) R.drawable.kbd_ic_style_off else R.drawable.kbd_ic_style_on)
        stylePicker?.apply {
            alpha = if (regular) 0.65f else 1f
            for (i in 0 until childCount) getChildAt(i).isSelected = i == selected
        }
    }

    private fun vibrate() {
        if (!keyVibrations) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * Read and convert a string preference to an integer
     */
    private fun prefStringToInt(prefs: SharedPreferences, key: String, defaultValue: Int): Int {
        return prefs.getString(key, defaultValue.toString())?.toIntOrNull() ?: defaultValue
    }

    /**
     * Initialize user preference variables
     *
     * Return boolean to indicate if preference changes should result in recreating the
     * keyboard view
     */
    private fun initPreferences(): Boolean {
        val displayHeight = window?.window?.windowManager?.currentWindowMetrics?.bounds?.height()
            ?: resources.displayMetrics.heightPixels
        val orientation = resources.configuration.orientation
        val heightMultiplier = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 1.7f else 1f
        val prefs = letteringPreferences
        val previousSystemMode = sysDarkMode
        val previousAppearance = appearance

        keyVibrations = prefs.getBoolean("key_vibrations", DEFAULT_VIBRATIONS)
        longPressMillis = prefs.getInt("kdb_long_press_delay", LONG_PRESS.toInt()).toLong()
        keyboardLayout = prefStringToInt(prefs, "kbd_layout", Constants.DEFAULT_LAYOUT)
        appearance = prefStringToInt(prefs, "kbd_appearance", Constants.DEFAULT_THEME)
        keyHeight = (.15f.coerceAtMost(
            heightMultiplier * (prefs.getInt("kdb_key_height", DEFAULT_HEIGHT) / 100f)
        ) * displayHeight).roundToInt()

        if (appearance == Constants.DEFAULT_THEME) {
            sysDarkMode = when (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> null
            }
        }
        return previousAppearance != appearance || previousSystemMode != sysDarkMode
    }

    /**
     * Show keyboard switcher
     */
    private fun showKeyboardPicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}
