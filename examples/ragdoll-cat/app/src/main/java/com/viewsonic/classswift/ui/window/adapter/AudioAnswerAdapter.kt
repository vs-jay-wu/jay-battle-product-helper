package com.viewsonic.classswift.ui.window.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.databinding.ItemQuizAudioAnsweringBinding
import com.viewsonic.classswift.databinding.ItemQuizAudioResultBinding
import com.viewsonic.classswift.ui.window.viewholder.QuizAudioAnsweringViewHolder
import com.viewsonic.classswift.ui.window.viewholder.QuizAudioResultViewHolder
import timber.log.Timber

class AudioAnswerAdapter(private val listener: OnAudioAnswerItemEventListener) :
    ListAdapter<AudioAnswerInfo, RecyclerView.ViewHolder>(diffCallback) {
    private val inAnswer: Int = 1
    private val result: Int = 2
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == result) {
            QuizAudioResultViewHolder(
                ItemQuizAudioResultBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                listener
            )
        } else {
            QuizAudioAnsweringViewHolder(
                ItemQuizAudioAnsweringBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ,listener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        if (viewType == result) {
            (holder as QuizAudioResultViewHolder).onBind(item)
        } else {
            (holder as QuizAudioAnsweringViewHolder).onBind(item)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        if (payloads.isEmpty()) {
            if (viewType == result) {
                (holder as QuizAudioResultViewHolder).onBind(item)
            } else {
                (holder as QuizAudioAnsweringViewHolder).onBind(item)
            }
        } else {
            val bundle = payloads[0] as Bundle
            if (viewType == result) {
                val quizAudioResultHolder = holder as? QuizAudioResultViewHolder ?: return
                if (bundle.containsKey(CHANGE_AUDIO_STATE)) {
                    Timber.tag("audioState").d("[onBindViewHolder]: result item CHANGE_AUDIO_STATE")
                    quizAudioResultHolder.setAudioState(item)
                }
                if (bundle.containsKey(UPDATE_AUDIO_TIME)) {
                    Timber.tag("audioState").d("[onBindViewHolder]: result item UPDATE_AUDIO_TIME")
                    quizAudioResultHolder.updateRemainTime(item)
                }
                if (bundle.containsKey(PARTIAL_VISIBLE)) {
                    quizAudioResultHolder.setIsPartiallyView(item.isPartiallyVisible)
                }
            } else {
                val quizAudioAnsweringHolder = holder as? QuizAudioAnsweringViewHolder ?: return
                if (bundle.containsKey(CHANGE_SHOW_ANSWER_UI)) {
                    quizAudioAnsweringHolder.setAnswerUI(item)
                }
                if (bundle.containsKey(CHANGE_AUDIO_STATE)) {
                    quizAudioAnsweringHolder.setAudioState(item)
                }
                if (bundle.containsKey(UPDATE_AUDIO_TIME)) {
                    quizAudioAnsweringHolder.updateRemainTime(item)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isInResultState) result else inAnswer
    }

    interface OnAudioAnswerItemEventListener {
        fun onAudioAnswerItemEvent(event: AudioAnswerItemEventListener)
    }

    companion object {
        const val PARTIAL_VISIBLE = "PARTIAL_VISIBLE"
        const val CHANGE_SHOW_ANSWER_UI = "CHANGE_SHOW_ANSWER_UI"
        const val CHANGE_AUDIO_STATE = "CHANGE_AUDIO_STATE"
        const val UPDATE_AUDIO_TIME = "UPDATE_AUDIO_TIME"
        private val diffCallback = object : DiffUtil.ItemCallback<AudioAnswerInfo>() {
            override fun areItemsTheSame(oldItem: AudioAnswerInfo, newItem: AudioAnswerInfo): Boolean {
                return oldItem.serialNumber == newItem.serialNumber
            }

            override fun areContentsTheSame(oldItem: AudioAnswerInfo, newItem: AudioAnswerInfo): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: AudioAnswerInfo, newItem: AudioAnswerInfo): Any? {
                val diff = Bundle()
                if (oldItem.isPartiallyVisible != newItem.isPartiallyVisible) {
                    Timber.d("[diffCallback]: getChangePayload PARTIAL_VISIBLE")
                    diff.putBoolean(PARTIAL_VISIBLE, true)
                }
                if (oldItem.audioState != newItem.audioState) {
                    Timber.tag("audioState").d("[diffCallback]: getChangePayload CHANGE_AUDIO_STATE, ${newItem.serialNumber} new state: ${newItem.audioState}")
                    diff.putBoolean(CHANGE_AUDIO_STATE, true)
                }
                if (oldItem.audioRemainTime != newItem.audioRemainTime) {
                    Timber.tag("audioState").d("[diffCallback]: getChangePayload UPDATE_AUDIO_TIME: ${newItem.serialNumber} time: ${newItem.audioRemainTime}")
                    diff.putBoolean(UPDATE_AUDIO_TIME, true)
                }
                if (oldItem.canShowAnswer != newItem.canShowAnswer) {
                    Timber.d("[diffCallback]: getChangePayload CHANGE_SHOW_ANSWER_UI")
                    diff.putBoolean(CHANGE_SHOW_ANSWER_UI, true)
                }
                return if (diff.isEmpty) null else diff
            }
        }
    }

    sealed class AudioAnswerItemEventListener(open val info: AudioAnswerInfo) {
        data class ItemClick(override val info: AudioAnswerInfo): AudioAnswerItemEventListener(info)
        data class PlayAudio(override val info: AudioAnswerInfo) : AudioAnswerItemEventListener(info)
        data class PauseAudio(override val info: AudioAnswerInfo) : AudioAnswerItemEventListener(info)
        data class UpdateDuration(override val info: AudioAnswerInfo): AudioAnswerItemEventListener(info)
    }
}