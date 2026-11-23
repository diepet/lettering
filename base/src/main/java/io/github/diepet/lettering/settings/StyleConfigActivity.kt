package io.github.diepet.lettering.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.DragEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import io.github.diepet.lettering.R
import io.github.diepet.lettering.styles.AppTextStyle

class StyleConfigActivity : SettingsPage() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val styles = StyleConfigAdapter()
        val list = ListView(this).apply {
            id = android.R.id.list
            adapter = styles
        }
        list.setOnDragListener { _, event ->
            val dragged = event.localState as? AppTextStyle ?: return@setOnDragListener false
            when (event.action) {
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val edge = list.getChildAt(0)?.height ?: 0
                    when {
                        event.y < edge -> list.smoothScrollBy(-list.height, 700)
                        event.y > list.height - edge -> list.smoothScrollBy(list.height, 700)
                        else -> list.smoothScrollBy(0, 0)
                    }
                }
                DragEvent.ACTION_DROP -> {
                    val target = list.pointToPosition(event.x.toInt(), event.y.toInt())
                    if (target >= 0) styles.move(dragged.styleId, target)
                }
                DragEvent.ACTION_DRAG_ENDED, DragEvent.ACTION_DRAG_EXITED -> list.smoothScrollBy(0, 0)
            }
            true
        }
        setPageContent(list)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            AlertDialog.Builder(this).setTitle(R.string.configure_styles_help)
                .setMessage(R.string.config_instructions).setPositiveButton(R.string.ok, null).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
