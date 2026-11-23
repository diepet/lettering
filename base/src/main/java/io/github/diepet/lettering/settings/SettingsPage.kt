package io.github.diepet.lettering.settings

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toolbar
import io.github.diepet.lettering.applyWindowInsets

abstract class SettingsPage : Activity() {
    private lateinit var page: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = Toolbar(this)
        page.addView(toolbar, -1, -2)
        setContentView(page)
        setActionBar(toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        applyWindowInsets(page)
    }

    protected fun setPageContent(content: View) {
        page.addView(content, -1, -1)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
