package com.viewsonic.classswift.ui.widget.task.content

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class TaskItemDecoration(
    private val spanCount: Int,
    private val spacingStart: Int,
    private val spacingEnd: Int,
    private val spacingTop: Int,
    private val spacingBottom: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            val totalSpacing = spacingStart + spacingEnd
            val spacingPerItem = totalSpacing / spanCount

            with(outRect) {
                left = spacingStart - column * spacingPerItem
                right = (column + 1) * spacingPerItem - spacingEnd
                top = spacingTop
                bottom = spacingBottom
            }
        } else {
            val spacingPerItem = (spacingStart + spacingEnd) / spanCount
            with(outRect) {
                left = column * spacingPerItem
                right = spacingEnd - (column + 1) * spacingPerItem

                if (position >= spanCount) top = spacingTop
                bottom = spacingBottom
            }
        }
    }
}