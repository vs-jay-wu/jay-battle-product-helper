package com.viewsonic.classswift.ui.window.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.SketchStudentCardItem
import com.viewsonic.classswift.data.quiz.SketchStudentStatus
import com.viewsonic.classswift.databinding.ItemMvbSketchStudentCardBinding

/**
 * VSFT-8454 Sketch Response 結果頁 Student responses tab 用 RecyclerView adapter。
 *
 * 從 `MvbQuizAnsweringAdapter` 拷貝後簡化：
 * - 砍 `isResult` flag（Sketch 永遠在 result phase）
 * - 砍 `highlightedOptionId`（Sketch 無 bar highlight 互動）
 * - 砍 answer chips / answer text view
 *
 * 5 狀態 apply：
 *  - Submitted（Sprint 20 未啟用，保留 enum + applySubmitted delegate 給未來）
 *  - Handed back（trigger_type=GRADED）
 *  - Click to view（trigger_type=RESPONSE）
 *  - Not submitted（trigger_type=UNSUBMITTED/UNKNOWN）
 *  - Absent（TaskResultInfo.Guest）
 */
class MvbSketchStudentCardAdapter(
    private val onCardClick: (SketchStudentCardItem) -> Unit,
) : ListAdapter<SketchStudentCardItem, MvbSketchStudentCardAdapter.ViewHolder>(DIFF) {

    private var showStudentsName: Boolean = true

    fun setShowStudentsName(show: Boolean) {
        if (showStudentsName == show) return
        showStudentsName = show
        notifyItemRangeChanged(
            0,
            itemCount,
            Bundle().apply { putBoolean(PAYLOAD_SHOW_STUDENTS_NAME, show) },
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMvbSketchStudentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding, onCardClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), showStudentsName)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            holder.bind(getItem(position), showStudentsName)
            return
        }
        payloads.forEach { payload ->
            if (payload is Bundle && payload.containsKey(PAYLOAD_SHOW_STUDENTS_NAME)) {
                holder.applyShowStudentsName(payload.getBoolean(PAYLOAD_SHOW_STUDENTS_NAME))
            }
        }
    }

    class ViewHolder(
        private val binding: ItemMvbSketchStudentCardBinding,
        private val callback: (SketchStudentCardItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SketchStudentCardItem, showStudentsName: Boolean) {
            val context = binding.root.context
            binding.mcvRoot.setOnClickListener {
                if (item.status.isClickable) callback(item)
            }
            binding.tvNumberAndName.text = item.numberAndName
            applyShowStudentsName(showStudentsName)

            when (item.status) {
                SketchStudentStatus.SUBMITTED -> applySubmitted(context)
                SketchStudentStatus.HANDED_BACK -> applyHandedBack(context)
                SketchStudentStatus.CLICK_TO_VIEW -> applyClickToView(context)
                SketchStudentStatus.NOT_SUBMITTED -> applyNotSubmitted(context)
                SketchStudentStatus.ABSENT -> applyAbsent(context)
            }
        }

        fun applyShowStudentsName(show: Boolean) {
            binding.tvNumberAndName.isVisible = show
        }

        /**
         * Submitted（Sprint 20 行為調整）：視覺直接走 Click to view。
         *
         * Result page 中「Submitted」=「老師可點擊查看學生作品」，與 Click to view 語意一致；
         * 為避免使用者區分不出兩種綠色 chip 而困惑，Sprint 20 起兩狀態共用同一視覺。
         *
         * Sprint 21+ 若 PM 決定要把「Submitted」（未批改）與「Click to view」（批改中）
         * 視覺分回兩種樣式，再恢復獨立 applySubmitted 邏輯。
         */
        private fun applySubmitted(context: Context) {
            applyClickToView(context)
        }

        /**
         * Handed back：與 Submitted 卡片視覺一致（綠 outline chip + check icon），文字「Handed back」。
         *
         * trigger 時機：老師按 Save and hand back 成功後，後端 `trigger_type` = GRADED，
         * [SketchStudentStatus.fromTaskResult] map 為 HANDED_BACK。
         */
        private fun applyHandedBack(context: Context) {
            applySuccessCardSkeleton(context)
            binding.tvSubmitted.setText(R.string.mvb_sketch_response_status_handed_back)
            showOnlyStateView(StateView.SUBMITTED)
        }

        /**
         * Submitted / Handed back 共用骨架：success 綠卡片 + success-variant name bar + 1px outline。
         *
         * Figma 「正確.png」(2026-05) 所有 card state 都有 `1px #E5E5E5` outline，
         * 包含綠 card。原本 strokeWidth=0 是誤把綠 card 邊框拿掉，導致與 Not submitted / Absent
         * 視覺重量不一致。
         */
        private fun applySuccessCardSkeleton(context: Context) {
            binding.mcvRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_E7F7D0),
            )
            binding.mcvRoot.strokeWidth =
                itemView.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            binding.mcvRoot.setStrokeColor(
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.neutral_300)),
            )
            binding.tvNumberAndName.setBackgroundColor(
                ContextCompat.getColor(context, R.color.color_48720F),
            )
            binding.tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_0),
            )
            binding.viewDivider.visibility = View.GONE
        }

        /**
         * Click to view：bg success-light + name bar success-variant，可點 → 開 SketchReviewWidget。
         * Figma：純圖示 + 文字（無 chip 框）— 比 Handed back chip 更輕量視覺。
         * 卡 outline 同 [applySuccessCardSkeleton] 1px neutral_300。
         */
        private fun applyClickToView(context: Context) {
            applySuccessCardSkeleton(context)
            showOnlyStateView(StateView.CLICK_TO_VIEW)
        }

        /**
         * Not submitted (Figma 對照)：
         *  - 卡片 bg=surface-100 (white)
         *  - 卡片 stroke=outline neutral_300 (#E5E5E5)
         *  - name bar bg=**surface-300 #E5E5E5**（與卡片 body 對比區隔）
         *  - name text=neutral_900
         *  - body=純文字 "Not submitted"（text-200 灰）
         *  - 不可點
         */
        private fun applyNotSubmitted(context: Context) {
            binding.mcvRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.neutral_0),
            )
            binding.mcvRoot.strokeWidth =
                itemView.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            binding.mcvRoot.setStrokeColor(
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.neutral_300)),
            )
            binding.tvNumberAndName.setBackgroundColor(
                ContextCompat.getColor(context, R.color.neutral_300),
            )
            binding.tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_900),
            )
            binding.viewDivider.visibility = View.GONE
            showOnlyStateView(StateView.NOT_SUBMITTED)
        }

        /** Absent: 灰底 + 灰名字 + 灰 divider，不可點。 */
        private fun applyAbsent(context: Context) {
            binding.mcvRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_EBEBEB),
            )
            binding.mcvRoot.strokeWidth =
                itemView.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            binding.mcvRoot.setStrokeColor(
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_E5E5E5)),
            )
            binding.tvNumberAndName.setBackgroundColor(
                ContextCompat.getColor(context, R.color.color_EBEBEB),
            )
            binding.tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_500),
            )
            binding.viewDivider.visibility = View.VISIBLE
            binding.viewDivider.setBackgroundColor(
                ContextCompat.getColor(context, R.color.color_E5E5E5),
            )
            showOnlyStateView(StateView.ABSENT)
        }

        /**
         * 四個狀態 view 中只顯示 [which]，其餘全部 GONE。
         *
         * 注意：[StateView.SUBMITTED] slot 由 Submitted 與 Handed back 共用。
         * [applyHandedBack] 傳入 [StateView.SUBMITTED] 並事先把 `tv_submitted` 文字改為
         * `R.string.mvb_sketch_response_status_handed_back`，Sprint 21+ 後端補 `HANDED_BACK`
         * enum 後可拆出獨立 slot。
         */
        private fun showOnlyStateView(which: StateView) {
            binding.tvSubmitted.visibility =
                if (which == StateView.SUBMITTED) View.VISIBLE else View.GONE
            binding.tvClickToView.visibility =
                if (which == StateView.CLICK_TO_VIEW) View.VISIBLE else View.GONE
            binding.tvNotSubmitted.visibility =
                if (which == StateView.NOT_SUBMITTED) View.VISIBLE else View.GONE
            binding.tvAbsent.visibility =
                if (which == StateView.ABSENT) View.VISIBLE else View.GONE
        }

        private enum class StateView { SUBMITTED, CLICK_TO_VIEW, NOT_SUBMITTED, ABSENT }
    }

    companion object {
        private const val PAYLOAD_SHOW_STUDENTS_NAME = "show_students_name"

        private val DIFF = object : DiffUtil.ItemCallback<SketchStudentCardItem>() {
            override fun areItemsTheSame(
                old: SketchStudentCardItem,
                new: SketchStudentCardItem,
            ): Boolean = old.studentId == new.studentId && old.serialNumber == new.serialNumber

            override fun areContentsTheSame(
                old: SketchStudentCardItem,
                new: SketchStudentCardItem,
            ): Boolean = old == new
        }
    }
}
