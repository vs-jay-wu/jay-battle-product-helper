package com.viewsonic.classswift.ui.window.viewholder

import android.content.res.ColorStateList
import android.media.MediaMetadataRetriever
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.databinding.ItemQuizAudioAnsweringBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.AudioAnswerItemEventListener
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.OnAudioAnswerItemEventListener
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.AudioState
import com.viewsonic.classswift.utils.extension.milliSecondToTimerUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class QuizAudioAnsweringViewHolder(
    private val binding: ItemQuizAudioAnsweringBinding,
    private val onItemInteractionListener: OnAudioAnswerItemEventListener
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private lateinit var audioInfo: AudioAnswerInfo

    fun onBind(info: AudioAnswerInfo) {
        audioInfo = info
        with(binding) {
            val brandBlueColor = ContextCompat.getColor(context, R.color.brand_blue)
            val whiteColor = ContextCompat.getColor(context, R.color.neutral_0)
            val absentColor = ContextCompat.getColor(context, R.color.quiz_absent_text_color)
            val absentStrokeLineColor = ContextCompat.getColor(context, R.color.quiz_absent_text_color)
            when (audioInfo.answeringState) {
                AnsweringState.ANSWERED -> {
                    if (info.audioDuration == null) {
                        getAudioDurationFromUrl(info.audioUrl)
                    }
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(brandBlueColor))
                    viewDivider.setBackgroundColor(brandBlueColor)
                    tvNumberAndName.setTextColor(whiteColor)
                    tvNumberAndName.setBackgroundColor(brandBlueColor)
                    tvNotAnswer.isVisible = false
                    tvAbsent.isVisible = false
                    setAudioTime()
                    tvAnswered.isVisible = !audioInfo.canShowAnswer
                    if (audioInfo.audioState == AudioState.PLAY) {
                        ivAudioControl.setImageResource(R.drawable.ic_audio_pause)
                    } else {
                        ivAudioControl.setImageResource(R.drawable.ic_audio_play)
                    }
                    viewBottomBg.setBackgroundColor(ContextCompat.getColor(context, R.color.quiz_answered_second_color))
                }

                AnsweringState.NOT_ANSWER -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(brandBlueColor))
                    viewDivider.setBackgroundColor(brandBlueColor)
                    tvNumberAndName.setTextColor(ContextCompat.getColor(context, R.color.cs_main_black_text_color))
                    tvNumberAndName.setBackgroundColor(whiteColor)
                    tvNotAnswer.isVisible = true
                    tvAbsent.isVisible = false
                    llAudio.isVisible = false
                    tvAnswered.isVisible = false
                    viewBottomBg.setBackgroundColor(whiteColor)
                }

                AnsweringState.ABSENT -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(absentStrokeLineColor))
                    viewDivider.setBackgroundColor(absentColor)
                    tvNumberAndName.setTextColor(absentColor)
                    tvNumberAndName.setBackgroundColor(whiteColor)
                    llAudio.isVisible = false
                    tvNotAnswer.isVisible = false
                    tvAnswered.isVisible = false
                    tvAbsent.isVisible = true
                    viewBottomBg.setBackgroundColor(whiteColor)
                }
            }
            val title = "${audioInfo.displaySeatNumber}  ${audioInfo.displayName}"
            tvNumberAndName.text = title
        }
        initClick()
    }

    fun getAudioDurationFromUrl(url: String) {
        val retriever = MediaMetadataRetriever()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                retriever.setDataSource(url, HashMap()) // HTTP URL
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) // Duration in milliseconds
                audioInfo.audioDuration = durationStr?.toLongOrNull() ?: 0L
                onItemInteractionListener.onAudioAnswerItemEvent(AudioAnswerItemEventListener.UpdateDuration(audioInfo))
                withContext(Dispatchers.Main) {
                    setAudioTime()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    private fun initClick() {
        with(binding) {
            mcvRoot.setOnClickListener {
                if (audioInfo.answeringState == AnsweringState.ANSWERED) {
                    onItemInteractionListener.onAudioAnswerItemEvent(AudioAnswerItemEventListener.ItemClick(audioInfo))
                }
            }
            llAudio.setOnClickListener {
                if (audioInfo.audioState == AudioState.PLAY) {
                    onItemInteractionListener.onAudioAnswerItemEvent(AudioAnswerItemEventListener.PauseAudio(audioInfo))
                } else {
                    onItemInteractionListener.onAudioAnswerItemEvent(AudioAnswerItemEventListener.PlayAudio(audioInfo))
                }
            }
        }
    }

    private fun setAudioTime() {
        binding.apply {
            if (audioInfo.canShowAnswer) {
                audioInfo.audioDuration?.let { duration ->
                    cpiProgressIndicator.isVisible = false
                    llAudio.isVisible = true
                    when (audioInfo.audioState) {
                        AudioState.INIT ->
                            tvAudioTime.text = duration.milliSecondToTimerUnit()
                        else ->
                            tvAudioTime.text = audioInfo.audioRemainTime.milliSecondToTimerUnit()
                    }
                } ?: run {
                    cpiProgressIndicator.isVisible = true
                    llAudio.isVisible = false
                }
            } else {
                cpiProgressIndicator.isVisible = false
                llAudio.isVisible = false
            }
        }
    }

    fun setAnswerUI(info: AudioAnswerInfo) {
        audioInfo = info
        binding.apply {
            tvAnswered.isVisible = !info.canShowAnswer
            setAudioTime()
        }

    }

    fun setAudioState(info: AudioAnswerInfo) {
        audioInfo = info
        binding.apply {
            if (audioInfo.audioState == AudioState.PLAY) {
                ivAudioControl.setImageResource(R.drawable.ic_audio_pause)
            } else {
                ivAudioControl.setImageResource(R.drawable.ic_audio_play)
            }
            setAudioTime()
        }
    }

    fun updateRemainTime(info: AudioAnswerInfo) {
        audioInfo = info
        binding.tvAudioTime.text =
            if (audioInfo.audioRemainTime > 0) audioInfo.audioRemainTime.milliSecondToTimerUnit()
            else audioInfo.audioDuration?.milliSecondToTimerUnit()
    }

}