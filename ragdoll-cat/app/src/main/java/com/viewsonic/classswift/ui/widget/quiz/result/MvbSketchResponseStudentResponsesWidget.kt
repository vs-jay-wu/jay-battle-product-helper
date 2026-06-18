package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.SketchStudentCardItem
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.WidgetMvbSketchResponseStudentResponsesBinding
import com.viewsonic.classswift.ui.window.adapter.MvbSketchStudentCardAdapter

/**
 * VSFT-8454 Sketch Response 結果頁 Student responses tab content widget。
 *
 * **Sprint 20 範圍**：只負責 cards mode（toggle + cards grid）。
 * 點 card 後切到 [SketchReviewWidget] 由 Window 層處理 — widget 蓋在整個 fl_shell_root 上，
 * 不再以 sub-overlay 形式嵌在此 widget 內（PushRespond 同樣做法）。
 *
 * Caller (Window) 透過：
 *  - [setRecords] 傳入學生資料
 *  - [setEventListener] 接 card click 事件 → 由 Window 決定要不要 show SketchReviewWidget
 *
 * @see <a href="https://viewsonic-vsi.atlassian.net/browse/VSFT-8454">VSFT-8454</a>
 */
class MvbSketchResponseStudentResponsesWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: WidgetMvbSketchResponseStudentResponsesBinding =
        WidgetMvbSketchResponseStudentResponsesBinding.inflate(
            LayoutInflater.from(context),
            this,
            true,
        )

    private val cardAdapter = MvbSketchStudentCardAdapter { item -> handleCardClick(item) }

    private var eventListener: EventListener? = null

    interface EventListener {
        /** 老師點擊可點 (Submitted / Click to view / Handed back) 學生卡 → Window 接手開 SketchReviewWidget。 */
        fun onCardClicked(record: TaskResultInfo)
    }

    init {
        initRecyclerView()
        initSwitch()
    }

    // ─── public API ─────────────────────────────────────────────────────────

    fun setRecords(records: List<TaskResultInfo>) {
        val items = records.map { SketchStudentCardItem.fromTaskResult(it) }
        cardAdapter.submitList(items)
    }

    fun setShowStudentsName(show: Boolean) {
        if (binding.swShowStudentsName.isChecked == show) {
            // Switch 狀態已正確；listener 不會觸發，需直接更新 adapter。
            cardAdapter.setShowStudentsName(show)
        } else {
            // 改變 switch 會觸發 listener → adapter.setShowStudentsName，避免雙次 notifyItemRangeChanged。
            binding.swShowStudentsName.isChecked = show
        }
    }

    fun setEventListener(listener: EventListener?) {
        eventListener = listener
    }

    // ─── private setup ─────────────────────────────────────────────────────

    private fun initRecyclerView() {
        // 對稱左右間距：left = spacing/2, right = spacing/2。
        // 卡片之間的視覺 gap = left(右卡) + right(左卡) = spacing (10.67dp)，
        // 首末欄距 RecyclerView 邊緣 = spacing/2 (5.33dp)。
        // 不用 StudentAnswerResultItemDecoration（只加右邊），改用 inline decoration
        // 以得到更對稱的外觀。
        val spacing = context.resources.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        val halfSpacing = spacing / 2
        binding.rvStudentCards.apply {
            layoutManager = GridLayoutManager(context, COLUMN_COUNT)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    outRect.left = halfSpacing
                    outRect.right = halfSpacing
                    outRect.bottom = spacing
                    if (position < COLUMN_COUNT) outRect.top = halfSpacing
                }
            })
            adapter = cardAdapter
        }
    }

    private fun initSwitch() {
        binding.swShowStudentsName.setOnCheckedChangeListener { _, isChecked ->
            cardAdapter.setShowStudentsName(isChecked)
        }
    }

    private fun handleCardClick(item: SketchStudentCardItem) {
        eventListener?.onCardClicked(item.record)
    }

    companion object {
        private const val COLUMN_COUNT = 4
    }
}
