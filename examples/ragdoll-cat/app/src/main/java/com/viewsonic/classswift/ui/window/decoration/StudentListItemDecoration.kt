package com.viewsonic.classswift.ui.window.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

class StudentListItemDecoration(
    private val spanCount: Int,
    private val outerMarginInPixels: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        if (position < spanCount) { // Top edge
            outRect.top = outerMarginInPixels
        }

        parent.adapter?.let { adapter ->
            var remainder = adapter.itemCount.mod(spanCount)
            if (remainder == 0) {
                remainder = spanCount
            }
            if (position >= adapter.itemCount - remainder) { // Bottom edge
                outRect.bottom = outerMarginInPixels
            }
        }
    }


}