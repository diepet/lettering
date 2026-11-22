package io.github.diepet.lettering.keyboard

import io.github.diepet.lettering.styles.AppTextStyle

/** Keeps the selected style stable when the enabled list is reordered or filtered. */
internal class StylePickerSelection {
    private var activeStyleId: String? = null
    private var lastSelectedStyleId: String? = null

    fun reconcile(enabledStyles: List<AppTextStyle>, allStyles: List<AppTextStyle>) {
        if (activeStyleId != null && enabledStyles.none { it.styleId == activeStyleId }) {
            activeStyleId = null
        }
        if (lastSelectedStyleId != null && allStyles.none { it.styleId == lastSelectedStyleId }) {
            lastSelectedStyleId = null
        }
    }

    fun selectedIndex(enabledStyles: List<AppTextStyle>): Int {
        val id = activeStyleId ?: return -1
        val index = enabledStyles.indexOfFirst { it.styleId == id }
        if (index < 0) activeStyleId = null
        return index
    }

    fun selectedStyle(enabledStyles: List<AppTextStyle>): AppTextStyle? {
        val index = selectedIndex(enabledStyles)
        return enabledStyles.getOrNull(index)
    }

    fun select(styleId: String, enabledStyles: List<AppTextStyle>): Boolean {
        if (enabledStyles.none { it.styleId == styleId }) return false
        activeStyleId = styleId
        lastSelectedStyleId = styleId
        return true
    }

    fun toggle(enabledStyles: List<AppTextStyle>) {
        if (selectedIndex(enabledStyles) >= 0) {
            activeStyleId = null
            return
        }
        activeStyleId = lastSelectedStyleId?.takeIf { id ->
            enabledStyles.any { it.styleId == id }
        }
    }
}
