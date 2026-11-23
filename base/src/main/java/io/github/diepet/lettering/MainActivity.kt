package io.github.diepet.lettering

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyWindowInsets(findViewById(R.id.main_content))
        findViewById<View>(R.id.step_1).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<View>(R.id.step_2).setOnClickListener {
            getSystemService(InputMethodManager::class.java).showInputMethodPicker()
        }

        val input = findViewById<EditText>(R.id.test_input)
        val clear = findViewById<View>(R.id.clear_input)
        clear.setOnClickListener { input.text.clear() }
        clear.visibility = if (input.length() > 0) View.VISIBLE else View.GONE
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        val enabled = getSystemService(InputMethodManager::class.java).enabledInputMethodList
            .any { it.packageName == packageName }
        findViewById<View>(R.id.step_1).isEnabled = !enabled
        findViewById<View>(R.id.step_2).isEnabled = enabled
    }
}
