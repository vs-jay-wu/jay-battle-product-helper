package com.viewsonic.classswift.ui.widget.quiz.disclose

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.children

class CSDiscloseAnswerOptionGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var mode: SelectionMode = SelectionMode.SINGLE
    private val selectedIds: MutableSet<Int> = mutableSetOf()

    var onSelectionChanged: ((Set<Int>) -> Unit)? = null

    init {
        orientation = HORIZONTAL
    }

    fun setMode(newMode: SelectionMode) {
        if (mode == newMode) return
        mode = newMode
        selectedIds.clear()
        refreshChildren()
        onSelectionChanged?.invoke(emptySet())
    }

    fun setItems(items: List<DiscloseOptionItemData>) {
        removeAllViews()
        selectedIds.clear()
        val gapPx = (context.resources.displayMetrics.density * GAP_BETWEEN_ITEMS_DP).toInt()
        items.forEachIndexed { index, data ->
            val item = CSDiscloseAnswerOptionItem(context).apply {
                val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                if (index > 0) lp.marginStart = gapPx
                layoutParams = lp
                bind(data, mode, checked = false)
                setOnClickListener { handleItemClick(data.id) }
            }
            addView(item)
        }
        onSelectionChanged?.invoke(emptySet())
    }

    private fun handleItemClick(id: Int) {
        when (mode) {
            SelectionMode.SINGLE -> {
                if (selectedIds.size == 1 && selectedIds.first() == id) return
                selectedIds.clear()
                selectedIds.add(id)
            }
            SelectionMode.MULTIPLE -> {
                if (!selectedIds.add(id)) selectedIds.remove(id)
            }
        }
        refreshChildren()
        onSelectionChanged?.invoke(selectedIds.toSet())
    }

    private fun refreshChildren() {
        children.forEach { child ->
            if (child is CSDiscloseAnswerOptionItem) {
                child.setChecked(child.optionId in selectedIds)
            }
        }
    }

    companion object {
        private const val GAP_BETWEEN_ITEMS_DP = 10.67f
    }
}
