package io.github.diepet.lettering.settings

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import io.github.diepet.lettering.R
import io.github.diepet.lettering.keyboard.Constants
import io.github.diepet.lettering.letteringPreferences

class SettingsActivity : SettingsPage() {
    private lateinit var rows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setPageContent(ScrollView(this).apply { addView(rows) })
        heading(R.string.label_settings)
        row(R.string.configure_styles, R.drawable.pref_ic_sort, getString(R.string.config_styles_summary))
            .setOnClickListener { startActivity(Intent(this, StyleConfigActivity::class.java)) }

        val vibration = Switch(this).apply {
            text = getString(R.string.key_vibrations)
            isChecked = letteringPreferences.getBoolean("key_vibrations", Constants.DEFAULT_VIBRATIONS)
            setOnCheckedChangeListener { _, checked ->
                letteringPreferences.edit().putBoolean("key_vibrations", checked).apply()
            }
        }
        rows.addView(vibration)
        decorate(vibration, R.drawable.pref_ic_vibrate)

        choice(R.string.setting_keyboard_layout, R.drawable.pref_ic_keyboard_outline,
            R.array.settings_layout_names_array, R.array.settings_layout_values_array,
            "kbd_layout", Constants.DEFAULT_LAYOUT)
        val height = letteringPreferences.getInt("kdb_key_height", Constants.DEFAULT_HEIGHT)
        val label = row(R.string.key_height, R.drawable.pref_ic_arrow_up_down, height.toString())
        val slider = SeekBar(this).apply {
            min = 5
            max = 12
            progress = height
            contentDescription = getString(R.string.key_height)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    label.text = summary(R.string.key_height, progress.toString())
                    if (fromUser) letteringPreferences.edit().putInt("kdb_key_height", progress).apply()
                }
                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}
            })
        }
        rows.addView(slider)
        choice(R.string.appearance, R.drawable.pref_ic_theme_light_dark,
            R.array.settings_appearance_names_array, R.array.settings_appearance_values_array,
            "kbd_appearance", Constants.DEFAULT_THEME)

        heading(R.string.label_about)
        link(R.string.label_feedback, R.drawable.pref_ic_chat_processing_outline,
            R.string.feedback_summary, getString(R.string.feedbackUri))
        link(R.string.source_code, R.drawable.pref_ic_code, R.string.source_summary,
            "https://github.com/diepet/lettering")
        row(R.string.label_version, R.drawable.pref_ic_information_outline, getString(R.string.versionName))
        link(R.string.label_developer, R.drawable.pref_ic_information_outline, R.string.developer,
            "https://github.com/diepet")
    }

    private fun heading(title: Int) {
        rows.addView(TextView(this).apply {
            setText(title)
            isAccessibilityHeading = true
            setTextAppearance(android.R.style.TextAppearance_Material_Subhead)
            val padding = resources.getDimensionPixelSize(R.dimen.style_config_padding)
            setPadding(padding, padding, padding, 0)
        })
    }

    private fun summary(title: Int, detail: String) = "${getString(title)}\n$detail"

    private fun decorate(view: TextView, icon: Int) {
        val padding = resources.getDimensionPixelSize(R.dimen.style_config_padding)
        view.setPadding(padding, padding / 2, padding, padding / 2)
        view.compoundDrawablePadding = padding
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        view.compoundDrawableTintList = getColorStateList(R.color.icon_color)
        view.minHeight = resources.getDimensionPixelSize(R.dimen.style_config_row_minHeight)
        view.gravity = Gravity.CENTER_VERTICAL
        val background = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        view.setBackgroundResource(background.resourceId)
    }

    private fun row(title: Int, icon: Int, detail: String) = TextView(this).also {
        it.text = summary(title, detail)
        decorate(it, icon)
        rows.addView(it)
    }

    private fun choice(title: Int, icon: Int, namesRes: Int, valuesRes: Int, key: String, default: Int) {
        val names = resources.getStringArray(namesRes)
        val values = resources.getStringArray(valuesRes)
        fun selected() = values.indexOf(letteringPreferences.getString(key, default.toString()))
            .takeIf { it >= 0 } ?: values.indexOf(default.toString())
        val view = row(title, icon, names[selected()])
        view.setOnClickListener {
            AlertDialog.Builder(this).setTitle(title)
                .setSingleChoiceItems(names, selected()) { dialog, index ->
                    letteringPreferences.edit().putString(key, values[index]).apply()
                    view.text = summary(title, names[index])
                    dialog.dismiss()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    private fun link(title: Int, icon: Int, detail: Int, url: String) {
        row(title, icon, getString(detail)).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                AlertDialog.Builder(this).setMessage(url).setPositiveButton(android.R.string.ok, null).show()
            }
        }
    }
}
