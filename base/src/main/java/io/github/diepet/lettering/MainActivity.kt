package io.github.diepet.lettering

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton


class MainActivity : AppCompatActivity() {

    private val enableStep = 1
    private val selectStep = 2

    private lateinit var step1: MaterialButton
    private lateinit var step2: MaterialButton
    private var activeStep = enableStep

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in,
                R.anim.fade_out
            )
        }
        applySystemBarInsets()

        step1 = findViewById(R.id.step_1)
        step2 = findViewById(R.id.step_2)

        step1.setOnClickListener(enableKeyboard(this))
        step2.setOnClickListener(selectKeyboard())

        determineActiveStep()
    }

    override fun onResume() {
        super.onResume()
        determineActiveStep()
    }

    @Suppress("DEPRECATION")
    private fun applySystemBarInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        val isNightTheme = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNightTheme
            isAppearanceLightNavigationBars = !isNightTheme
        }

        val content = findViewById<View>(R.id.main_content)
        val initialLeft = content.paddingLeft
        val initialTop = content.paddingTop
        val initialRight = content.paddingRight
        val initialBottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                initialLeft + insets.left,
                initialTop + insets.top,
                initialRight + insets.right,
                initialBottom + insets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(content)
    }

    private fun determineActiveStep() {
        activeStep = if (!isCodeBoardEnabled())
            enableStep
        else
            selectStep

        step1.isEnabled = activeStep == enableStep
        step2.isEnabled = activeStep == selectStep
    }

    private fun isCodeBoardEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun enableKeyboard(a: Activity): View.OnClickListener {
        return View.OnClickListener {
            val enableIntent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            enableIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            a.startActivity(enableIntent)
        }
    }

    private fun selectKeyboard(): View.OnClickListener {
        return View.OnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }
}
