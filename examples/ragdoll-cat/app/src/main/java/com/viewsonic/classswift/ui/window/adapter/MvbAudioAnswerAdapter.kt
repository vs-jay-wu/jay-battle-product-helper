package com.viewsonic.classswift.ui.window.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.databinding.ItemMvbQuizAudioAnsweringBinding
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.OnAudioAnswerItemEventListener
import com.viewsonic.classswift.ui.window.viewholder.MvbQuizAudioAnsweringViewHolder

class MvbAudioAnswerAdapter(
    private val listener: OnAudioAnswerItemEventListener
) : ListAdapter<AudioAnswerInfo, MvbQuizAudioAnsweringViewHolder>(diffCallback) {

    private var showStudentsName: Boolean = true
    private var isResult: Boolean = false

    fun setShowStudentsName(show: Boolean) {
        if (showStudentsName == show) return
        showStudentsName = show
        notifyItemRangeChanged(0, itemCount, SHOW_STUDENTS_NAME)
    }

    fun setIsResult(isResult: Boolean) {
        if (this.isResult == isResult) return
        this.isResult = isResult
        notifyItemRangeChanged(0, itemCount, IS_RESULT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MvbQuizAudioAnsweringViewHolder {
        return MvbQuizAudioAnsweringViewHolder(
            ItemMvbQuizAudioAnsweringBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )
    }

    override fun onBindViewHolder(holder: MvbQuizAudioAnsweringViewHolder, position: Int) {
        holder.onBind(getItem(position), showStudentsName, isResult)
    }

    override fun onBindViewHolder(
        holder: MvbQuizAudioAnsweringViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val item = getItem(position)
        if (payloads.isEmpty()) {
            holder.onBind(item, showStudentsName, isResult)
            return
        }
        if (payloads.contains(SHOW_STUDENTS_NAME)) {
            holder.setShowStudentsName(showStudentsName)
        }
        if (payloads.contains(IS_RESULT)) {
            // Palette depends on Quizzing vs Result; rebind in full to reapply colors.
            holder.onBind(item, showStudentsName, isResult)
            return
        }
        val bundlePayload = payloads.firstOrNull { it is Bundle } as? Bundle ?: return
        if (bundlePayload.containsKey(CHANGE_SHOW_ANSWER_UI)) {
            holder.setAnswerUI(item)
        }
        if (bundlePayload.containsKey(CHANGE_AUDIO_STATE)) {
            holder.setAudioState(item)
        }
        if (bundlePayload.containsKey(UPDATE_AUDIO_TIME)) {
            holder.updateRemainTime(item)
        }
        if (bundlePayload.containsKey(PARTIAL_VISIBLE)) {
            holder.setPartiallyVisible(item)
        }
    }

    override fun onViewRecycled(holder: MvbQuizAudioAnsweringViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelPendingWork()
    }

    companion object {
        const val CHANGE_SHOW_ANSWER_UI = "CHANGE_SHOW_ANSWER_UI"
        const val CHANGE_AUDIO_STATE = "CHANGE_AUDIO_STATE"
        const val UPDATE_AUDIO_TIME = "UPDATE_AUDIO_TIME"
        const val PARTIAL_VISIBLE = "PARTIAL_VISIBLE"
        private const val SHOW_STUDENTS_NAME = "SHOW_STUDENTS_NAME"
        private const val IS_RESULT = "IS_RESULT"

        private val diffCallback = object : DiffUtil.ItemCallback<AudioAnswerInfo>() {
            override fun areItemsTheSame(oldItem: AudioAnswerInfo, newItem: AudioAnswerInfo): Boolean {
                return oldItem.serialNumber == newItem.serialNumber
            }

            override fun areContentsTheSame(oldItem: AudioAnswerInfo, newItem: AudioAnswerInfo): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: AudioAnswerInfo, newItem: AudioAnswerInfo): Any? {
                val diff = Bundle()
                if (oldItem.audioState != newItem.audioState) {
                    diff.putBoolean(CHANGE_AUDIO_STATE, true)
                }
                if (oldItem.audioRemainTime != newItem.audioRemainTime) {
                    diff.putBoolean(UPDATE_AUDIO_TIME, true)
                }
                if (oldItem.canShowAnswer != newItem.canShowAnswer) {
                    diff.putBoolean(CHANGE_SHOW_ANSWER_UI, true)
                }
                if (oldItem.isPartiallyVisible != newItem.isPartiallyVisible) {
                    diff.putBoolean(PARTIAL_VISIBLE, true)
                }
                return if (diff.isEmpty) null else diff
            }
        }
    }
}
