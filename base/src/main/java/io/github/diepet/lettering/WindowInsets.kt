package io.github.diepet.lettering

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets

internal fun Activity.applyWindowInsets(root: View) {
    if (Build.VERSION.SDK_INT < 35) {
        @Suppress("DEPRECATION")
        window.setDecorFitsSystemWindows(false)
    }
    root.setOnApplyWindowInsetsListener { view, insets ->
        val safe = insets.getInsets(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime()
        )
        view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
        insets
    }
    root.requestApplyInsets()
}
