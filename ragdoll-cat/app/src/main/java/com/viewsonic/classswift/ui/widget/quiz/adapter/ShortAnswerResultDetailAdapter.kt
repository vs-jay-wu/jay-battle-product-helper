package com.viewsonic.classswift.ui.widget.quiz.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.ItemQuizShortAnswerResultDetailBinding
import com.viewsonic.classswift.ui.widget.quiz.viewholder.ShortAnswerResultDetailViewHolder

class ShortAnswerResultDetailAdapter(
    private val onItemInteractionListener: OnItemInteractionListener
) : ListAdapter<QuizAnswerResultInfo, ShortAnswerResultDetailViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortAnswerResultDetailViewHolder {
        return ShortAnswerResultDetailViewHolder(
            ItemQuizShortAnswerResultDetailBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false),
            onItemInteractionListener
        )
    }

    override fun onBindViewHolder(holder: ShortAnswerResultDetailViewHolder, position: Int) {
        holder.onBind(getItem(position), itemCount)
    }

    override fun onBindViewHolder(holder: ShortAnswerResultDetailViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val payloadBundle = payloads[0] as Bundle
            if (payloadBundle.containsKey(BUNDLE_KEY_PARTIALLY_UPDATE)) {
                holder.updatePartiallyVisibleState(getItem(position))
            }
        }
    }

    interface OnItemInteractionListener {
        fun onLeftButtonClicked() {}
        fun onRightButtonClicked() {}
        fun onCloseButtonClicked() {}
    }

    companion object {
        private const val BUNDLE_KEY_PARTIALLY_UPDATE = "BUNDLE_KEY_PARTIALLY_UPDATE"

        private val diffCallback = object : DiffUtil.ItemCallback<QuizAnswerResultInfo>() {
            override fun areItemsTheSame(
                oldItem: QuizAnswerResultInfo,
                newItem: QuizAnswerResultInfo
            ): Boolean {
                return oldItem.studentId == newItem.studentId
            }

            override fun areContentsTheSame(
                oldItem: QuizAnswerResultInfo,
                newItem: QuizAnswerResultInfo
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: QuizAnswerResultInfo, newItem: QuizAnswerResultInfo): Any? {
                val diff = Bundle()
                if (oldItem.isPartiallyVisible != newItem.isPartiallyVisible) {
                    diff.putBoolean(BUNDLE_KEY_PARTIALLY_UPDATE, true)
                    return diff
                }
                return null
            }
        }
    }
}