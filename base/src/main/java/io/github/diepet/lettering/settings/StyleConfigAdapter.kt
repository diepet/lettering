package io.github.diepet.lettering.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Switch
import android.widget.TextView
import io.github.diepet.lettering.R
import io.github.diepet.lettering.styles.AvailableStyles

internal class StyleConfigAdapter : BaseAdapter() {
    private var styles = AvailableStyles.getStyles()

    override fun getCount() = styles.size
    override fun getItem(position: Int) = styles[position]
    override fun getItemId(position: Int) = position.toLong()

    fun move(id: String, position: Int) {
        if (position !in styles.indices || styles[position].styleId == id) return
        AvailableStyles.setNewPosition(id, position)
        styles = AvailableStyles.getStyles()
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_config_style, parent, false)
        val style = getItem(position)
        val label = row.findViewById<TextView>(R.id.text_value)
        label.text = style.styledName
        label.alpha = if (style.isEnabled) 1f else 0.5f
        row.setOnLongClickListener {
            row.startDragAndDrop(null, View.DragShadowBuilder(row), style, 0)
        }
        val toggle = row.findViewById<Switch>(R.id.enable_box)
        toggle.contentDescription = style.displayName
        toggle.isChecked = style.isEnabled
        toggle.setOnClickListener {
            AvailableStyles.toggleEnabled(style.styleId)
            notifyDataSetChanged()
        }
        fun bindArrow(id: Int, description: Int, target: Int) {
            row.findViewById<View>(id).apply {
                visibility = if (style.isEnabled) View.VISIBLE else View.GONE
                contentDescription = context.getString(description, style.displayName)
                setOnClickListener { move(style.styleId, target) }
            }
        }
        bindArrow(R.id.order_btn_up, R.string.move_to_top, 0)
        bindArrow(R.id.order_btn_down, R.string.move_to_bottom, count - 1)
        return row
    }
}
