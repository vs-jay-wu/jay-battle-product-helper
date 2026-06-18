package com.viewsonic.classswift.ui.window.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.checkerframework.common.subtyping.qual.Bottom

class StudentGroupItemDecoration(
    private val itemTopMarginInPixels: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount
        with(outRect) {
            // first group top margin is different
            top = when (position) {
                0 -> 0
                else -> itemTopMarginInPixels
            }
            // last group has bottom margin
            if (position == itemCount - 1) {
                bottom = 0
            }
        }
    }
}