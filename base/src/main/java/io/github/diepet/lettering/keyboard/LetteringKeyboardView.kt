package io.github.diepet.lettering.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.emoji2.emojipicker.EmojiPickerView
import io.github.diepet.lettering.R
import kotlin.math.roundToInt

/** Each key is a focusable framework View. One parent coordinates sliding and overlapping fingers. */
class LetteringKeyboardView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    internal var onKey: (Int) -> Unit = {}
    internal var onPress: () -> Unit = {}
    internal var onChooseInputMethod: () -> Unit = {}
    internal var spaceHoldMillis = Constants.LONG_PRESS

    private var layout: KeyboardLayout? = null
    private var rowHeight = 1
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, resources.displayMetrics)
        val colorValue = TypedValue()
        context.theme.resolveAttribute(R.attr.kbd_text_color, colorValue, true)
        color = colorValue.data
    }
    private val baselineOffset = -(textPaint.ascent() + textPaint.descent()) / 2
    private val iconRatio = resources.getFloat(R.dimen.key_icon_ratio)
    private val icons = SparseArray<Drawable>()
    private val keyBackground = requireNotNull(context.getDrawable(R.drawable.key_normal))
    private val pressedBackground = requireNotNull(context.getDrawable(R.drawable.key_pressed))
    private var pointer = MotionEvent.INVALID_POINTER_ID
    private var pressedKey: KeyFace? = null
    private var consumed = false
    private var popup: PopupWindow? = null
    private var emojiPickerWindow: PopupWindow? = null
    private var emojiSearchActive = false
    private var emojiSearchField: EditText? = null
    private var emojiSearchToggle: ImageButton? = null
    private var emojiPickerView: EmojiPickerView? = null
    private var emojiSearchGrid: GridView? = null
    private var emojiSearchResultsContainer: FrameLayout? = null
    private var emojiSearchEmptyView: TextView? = null
    private var emojiSearchAdapter: EmojiSearchAdapter? = null
    private var emojiSearchEntries: List<EmojiSearchEntry> = emptyList()
    private var lastCycleKey: Key? = null
    private var cycleIndex = 0
    private var lastCycleTime = 0L

    internal var shiftState = ShiftState.OFF
        set(value) {
            if (field == value) return
            field = value
            for (i in 0 until childCount) (getChildAt(i) as KeyFace).updateLabel()
        }

    private inner class KeyFace(val key: Key) : View(context) {
        val frame = Rect()
        // A centered row's outer keys also receive touches in its margins.
        val face = Rect()
        private val upperLabel = applyKeyboardShift(key.label, true)
        private val label get() = if (shiftState == ShiftState.OFF) key.label else upperLabel

        init {
            isFocusable = true
            isSoundEffectsEnabled = false
            isHapticFeedbackEnabled = false
            setOnClickListener { activate(key) }
            if (key.alternates != 0 || key.code == Constants.KEYCODE_SPACE) {
                setOnLongClickListener { longClick(this) }
            }
            updateLabel()
        }

        fun updateLabel() {
            contentDescription = when {
                key.code == Constants.SHIFT && shiftState == ShiftState.LOCKED -> context.getString(R.string.key_caps_lock)
                key.description != 0 -> context.getString(key.description)
                else -> label
            }
            invalidate()
        }

        override fun getAccessibilityClassName(): CharSequence = Button::class.java.name

        override fun onDraw(canvas: Canvas) {
            val background = if (isPressed || isFocused) pressedBackground else keyBackground
            background.bounds = face
            background.draw(canvas)
            if (key.icon == 0) {
                canvas.drawText(label, face.exactCenterX(), face.exactCenterY() + baselineOffset, textPaint)
            } else {
                val id = when {
                    key.code != Constants.SHIFT -> key.icon
                    shiftState == ShiftState.ONCE -> R.drawable.kbd_ic_arrow_up_bold
                    shiftState == ShiftState.LOCKED -> R.drawable.kbd_ic_keyboard_caps_filled
                    else -> key.icon
                }
                val icon = icons[id] ?: requireNotNull(context.getDrawable(id)).also { icons.put(id, it) }
                val size = (minOf(face.width(), face.height()) * iconRatio).roundToInt()
                val left = face.centerX() - size / 2
                val top = face.centerY() - size / 2
                icon.setBounds(left, top, left + size, top + size)
                icon.draw(canvas)
            }
        }

        override fun drawableStateChanged() {
            super.drawableStateChanged()
            invalidate()
        }
    }

    internal fun showLayout(next: KeyboardLayout, keyHeight: Int) {
        clearPressedKey()
        pointer = MotionEvent.INVALID_POINTER_ID
        lastCycleKey = null
        if (!emojiSearchActive) dismissPopup()
        if (layout !== next) {
            layout = next
            removeAllViews()
            for (row in next.rows) for (key in row) addView(KeyFace(key))
        }
        if (rowHeight != keyHeight) {
            rowHeight = keyHeight
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rows = layout?.rows.orEmpty()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(rowHeight * rows.size, heightMeasureSpec)
        setMeasuredDimension(width, height)
        var index = 0
        rows.forEachIndexed { rowIndex, row ->
            var start = (100 - row.sumOf { it.width }) / 2
            val top = height * rowIndex / rows.size
            val bottom = height * (rowIndex + 1) / rows.size
            row.forEachIndexed { column, key ->
                val left = width * start / 100
                start += key.width
                val right = width * start / 100
                val child = getChildAt(index++) as KeyFace
                val frame = child.frame
                frame.set(if (column == 0) 0 else left, top, if (column == row.lastIndex) width else right, bottom)
                child.face.set(left - frame.left, 0, right - frame.left, frame.height())
                child.measure(MeasureSpec.makeMeasureSpec(frame.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(frame.height(), MeasureSpec.EXACTLY))
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed) {
            clearPressedKey()
            pointer = MotionEvent.INVALID_POINTER_ID
            lastCycleKey = null
            if (!emojiSearchActive) dismissPopup()
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i) as KeyFace
            with(child.frame) { child.layout(left, top, right, bottom) }
        }
    }

    private fun hit(x: Float, y: Float): KeyFace? {
        if (x < 0 || y < 0) return null
        for (i in 0 until childCount) {
            val child = getChildAt(i) as KeyFace
            if (child.frame.contains(x.toInt(), y.toInt())) return child
        }
        return null
    }

    private val hold = object : Runnable {
        override fun run() {
            val child = pressedKey ?: return
            consumed = true
            if (child.key.code == Constants.DELETE) {
                onKey(Constants.DELETE)
                postDelayed(this, ViewConfiguration.getKeyRepeatDelay().toLong())
            } else {
                child.performLongClick()
            }
        }
    }

    private fun press(child: KeyFace?) {
        clearPressedKey()
        pressedKey = child
        consumed = false
        if (child == null) return
        child.isPressed = true
        onPress()
        when {
            child.key.code == Constants.DELETE -> {
                consumed = true
                activate(child.key)
                postDelayed(hold, ViewConfiguration.getKeyRepeatTimeout().toLong())
            }
            child.key.code == Constants.KEYCODE_SPACE -> postDelayed(hold, spaceHoldMillis)
            child.isLongClickable -> postDelayed(hold, ViewConfiguration.getLongPressTimeout().toLong())
        }
    }

    private fun release() {
        val child = pressedKey
        clearPressedKey()
        pointer = MotionEvent.INVALID_POINTER_ID
        if (!consumed) child?.performClick()
    }

    private fun clearPressedKey() {
        removeCallbacks(hold)
        pressedKey?.isPressed = false
        pressedKey = null
    }

    override fun onInterceptTouchEvent(event: MotionEvent) = true

    // The parent tracks gestures and calls performClick on the actual key.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val keyboardEnabled = popup == null || emojiSearchActive
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointer != MotionEvent.INVALID_POINTER_ID) release()
                if (keyboardEnabled) {
                    pointer = event.getPointerId(index)
                    press(hit(event.getX(index), event.getY(index)))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val i = event.findPointerIndex(pointer)
                val choseInputMethod = consumed && pressedKey?.key?.code == Constants.KEYCODE_SPACE
                if (i >= 0 && (popup == null || emojiSearchActive) && !choseInputMethod) {
                    val next = hit(event.getX(i), event.getY(i))
                    if (next !== pressedKey) press(next)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
                if (event.getPointerId(index) == pointer) release()
            MotionEvent.ACTION_CANCEL -> {
                clearPressedKey()
                pointer = MotionEvent.INVALID_POINTER_ID
            }
        }
        return true
    }

    private fun activate(key: Key) {
        if (key.cycle.isEmpty()) {
            lastCycleKey = null
            onKey(key.code)
            return
        }
        val now = SystemClock.uptimeMillis()
        val replace = key === lastCycleKey && now - lastCycleTime < 800
        cycleIndex = if (replace) (cycleIndex + 1) % key.cycle.length else 0
        lastCycleKey = key
        lastCycleTime = now
        if (replace) onKey(Constants.DELETE)
        onKey(key.cycle[cycleIndex].code)
    }

    private fun longClick(child: KeyFace): Boolean {
        if (child.key.code == Constants.KEYCODE_SPACE) {
            onChooseInputMethod()
            return true
        }
        val choices = resources.getString(child.key.alternates)
        if (choices.isEmpty()) return false
        dismissPopup()
        val buttonWidth = minOf(child.face.width(), width / choices.length)
        val strip = LinearLayout(context).apply {
            val color = TypedValue()
            context.theme.resolveAttribute(R.attr.kbd_bg, color, true)
            setBackgroundColor(color.data)
            layoutDirection = LAYOUT_DIRECTION_LTR
        }
        for (character in choices) {
            strip.addView(Button(context).apply {
                isAllCaps = false
                text = applyKeyboardShift(character.toString(), shiftState != ShiftState.OFF)
                setTextColor(textPaint.color)
                textSize = 22f
                setPadding(0, 0, 0, 0)
                setBackgroundResource(R.drawable.key_background)
                setOnClickListener {
                    dismissPopup()
                    onKey(character.code)
                }
            }, buttonWidth, child.height)
        }
        val popupWidth = buttonWidth * choices.length
        val left = (child.left + child.face.centerX() - popupWidth / 2).coerceIn(0, width - popupWidth)
        popup = PopupWindow(strip, popupWidth, child.height, true).apply {
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setBackgroundDrawable(requireNotNull(context.getDrawable(R.drawable.key_normal)))
            setOnDismissListener { popup = null }
            val rtl = child.layoutDirection == LAYOUT_DIRECTION_RTL
            val offset = if (rtl) left + popupWidth - child.right else left - child.left
            showAsDropDown(child, offset, -2 * child.height, Gravity.START)
        }
        return true
    }

    internal val isEmojiSearchActive: Boolean
        get() = emojiSearchActive

    internal fun handleEmojiSearchKey(code: Int, uppercase: Boolean): Boolean {
        if (!emojiSearchActive) return false
        when (code) {
            Constants.SHIFT, Constants.SECONDARY_KBD_KEYCODE, Constants.ALPHA_KEYBOARD_KEYCODE -> return false
            Constants.DELETE -> {
                deleteEmojiSearchCharacter()
                return true
            }
            Constants.ENTER, Constants.EMOJI_PICKER_KEYCODE -> return true
        }
        if (!Character.isValidCodePoint(code)) return true
        val character = String(Character.toChars(code))
        insertEmojiSearchText(applyKeyboardShift(character, uppercase))
        return true
    }

    internal fun showEmojiPicker(onEmojiPicked: (String) -> Unit) {
        dismissPopup()
        val textColor = keyboardTextColor()
        val picker = EmojiPickerView(context).apply {
            emojiGridColumns = 9
            setOnEmojiPickedListener { item ->
                dismissPopup()
                onEmojiPicked(item.emoji)
            }
        }
        val adapter = EmojiSearchAdapter()
        val searchToggle = ImageButton(context).apply {
            setImageResource(R.drawable.kbd_ic_search)
            imageTintList = ColorStateList.valueOf(textColor)
            background = null
            contentDescription = context.getString(R.string.search_emoji)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val searchField = EditText(context).apply {
            hint = context.getString(R.string.search_emoji)
            setTextColor(textColor)
            setHintTextColor(textColor.withAlpha(0.65f))
            textSize = 16f
            isSingleLine = true
            showSoftInputOnFocus = false
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(8), 0, dp(8), 0)
            background = null
            visibility = GONE
        }
        val searchGrid = GridView(context).apply {
            numColumns = 9
            horizontalSpacing = dp(2)
            verticalSpacing = dp(2)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            this.adapter = adapter
            setOnItemClickListener { _, _, position, _ ->
                val selected = adapter.getItem(position)
                dismissPopup()
                onEmojiPicked(selected.emoji)
            }
        }
        val emptyView = TextView(context).apply {
            text = context.getString(R.string.no_emoji_search_results)
            setTextColor(textColor)
            textSize = 16f
            gravity = Gravity.CENTER
            visibility = GONE
        }
        val searchResults = FrameLayout(context).apply {
            addView(searchGrid, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(emptyView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            visibility = GONE
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = LAYOUT_DIRECTION_LOCALE
            addView(searchToggle, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(searchField, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundDrawable(requireNotNull(context.getDrawable(R.drawable.emoji_picker_background)))
            addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
            addView(picker, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(searchResults, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        searchToggle.setOnClickListener {
            if (emojiSearchActive) {
                setEmojiSearchActive(false)
            } else {
                setEmojiSearchActive(true)
            }
        }
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateEmojiSearchResults(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        emojiSearchEntries = EmojiSearchIndex.load(context)
        emojiPickerView = picker
        emojiSearchField = searchField
        emojiSearchToggle = searchToggle
        emojiSearchGrid = searchGrid
        emojiSearchResultsContainer = searchResults
        emojiSearchEmptyView = emptyView
        emojiSearchAdapter = adapter
        emojiSearchActive = false
        val popupHeight = resources.getDimensionPixelSize(R.dimen.emoji_picker_height)
        val window = PopupWindow(content, width, popupHeight, true).apply {
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            elevation = resources.displayMetrics.density * 8
            setBackgroundDrawable(requireNotNull(context.getDrawable(R.drawable.emoji_picker_background)))
            setOnDismissListener {
                if (popup === this) popup = null
                if (emojiPickerWindow === this) clearEmojiSearchState()
            }
        }
        emojiPickerWindow = window
        popup = window
        window.showAsDropDown(this, 0, -popupHeight - height, Gravity.START)
    }

    private fun setEmojiSearchActive(active: Boolean) {
        emojiSearchActive = active
        val searchField = emojiSearchField ?: return
        val searchToggle = emojiSearchToggle ?: return
        searchField.visibility = if (active) VISIBLE else GONE
        searchToggle.contentDescription = context.getString(
            if (active) R.string.close_emoji_search else R.string.search_emoji
        )
        searchToggle.setImageResource(if (active) R.drawable.kbd_ic_clear else R.drawable.kbd_ic_search)
        emojiPickerWindow?.let { window ->
            window.setTouchModal(!active)
            window.isOutsideTouchable = !active
            window.isFocusable = !active
            window.update()
        }
        if (active) {
            searchField.requestFocus()
        } else {
            searchField.setText("")
            searchField.clearFocus()
        }
    }

    private fun updateEmojiSearchResults(query: String) {
        val picker = emojiPickerView ?: return
        val grid = emojiSearchGrid ?: return
        val resultsContainer = emojiSearchResultsContainer ?: return
        val normalizedQuery = normalizeEmojiSearchText(query)
        if (normalizedQuery.isEmpty()) {
            picker.visibility = VISIBLE
            resultsContainer.visibility = GONE
            return
        }
        picker.visibility = GONE
        resultsContainer.visibility = VISIBLE
        val results = emojiSearchEntries.filter { entry ->
            entry.normalizedSearchTerms.contains(normalizedQuery) || entry.emoji.contains(query)
        }.sortedBy { entry ->
            if (entry.normalizedSearchTerms.startsWith(normalizedQuery)) 0 else 1
        }
        grid.visibility = if (results.isEmpty()) GONE else VISIBLE
        emojiSearchEmptyView?.visibility = if (results.isEmpty()) VISIBLE else GONE
        emojiSearchAdapter?.replace(results)
    }

    private fun insertEmojiSearchText(text: String) {
        val field = emojiSearchField ?: return
        val editable = field.text ?: return
        val start = field.selectionStart.coerceAtLeast(0)
        val end = field.selectionEnd.coerceAtLeast(0)
        editable.replace(minOf(start, end), maxOf(start, end), text)
        field.setSelection(minOf(start, end) + text.length)
    }

    private fun deleteEmojiSearchCharacter() {
        val field = emojiSearchField ?: return
        val editable = field.text ?: return
        val start = field.selectionStart.coerceAtLeast(0)
        val end = field.selectionEnd.coerceAtLeast(0)
        if (start != end) {
            editable.delete(minOf(start, end), maxOf(start, end))
        } else if (start > 0) {
            val previous = Character.offsetByCodePoints(editable, start, -1)
            editable.delete(previous, start)
        }
        field.setSelection(minOf(start, end).coerceAtMost(editable.length))
    }

    private fun clearEmojiSearchState() {
        emojiPickerWindow = null
        emojiSearchActive = false
        emojiSearchField = null
        emojiSearchToggle = null
        emojiPickerView = null
        emojiSearchGrid = null
        emojiSearchResultsContainer = null
        emojiSearchEmptyView = null
        emojiSearchAdapter = null
        emojiSearchEntries = emptyList()
    }

    private fun keyboardTextColor(): Int {
        val color = TypedValue()
        context.theme.resolveAttribute(R.attr.kbd_text_color, color, true)
        return color.data
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun Int.withAlpha(alpha: Float): Int =
        (this and 0x00ffffff) or ((alpha.coerceIn(0f, 1f) * 255).roundToInt() shl 24)

    private inner class EmojiSearchAdapter : BaseAdapter() {
        private var items: List<EmojiSearchEntry> = emptyList()

        fun replace(next: List<EmojiSearchEntry>) {
            items = next
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): EmojiSearchEntry = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (convertView as? TextView ?: TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 26f
                setPadding(dp(2), dp(4), dp(2), dp(4))
                layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
                )
            }).apply {
                val entry = items[position]
                text = entry.emoji
                contentDescription = entry.description
            }
    }

    internal fun dismissPopup(): Boolean {
        val window = popup ?: return false
        window.dismiss()
        return true
    }

    internal fun cancelInput() {
        clearPressedKey()
        pointer = MotionEvent.INVALID_POINTER_ID
        dismissPopup()
        lastCycleKey = null
    }

    override fun onDetachedFromWindow() {
        cancelInput()
        super.onDetachedFromWindow()
    }
}
