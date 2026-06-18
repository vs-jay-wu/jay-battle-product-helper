package com.viewsonic.classswift.ui.window.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class StudentAnswerResultItemDecoration(
    private val spanCount: Int,
    private val rightMarginInPixels: Int,
    private val bottomMarginInPixels: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        if (position < spanCount) { // Top edge
            outRect.top = bottomMarginInPixels
        }
        outRect.right = rightMarginInPixels
        outRect.bottom = bottomMarginInPixels

    }
}