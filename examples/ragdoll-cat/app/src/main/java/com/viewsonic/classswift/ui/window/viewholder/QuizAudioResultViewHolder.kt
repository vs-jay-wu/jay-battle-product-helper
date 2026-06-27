package com.viewsonic.classswift.ui.window.viewholder

import android.content.res.ColorStateList
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.databinding.ItemQuizAudioResultBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.AudioAnswerItemEventListener
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.OnAudioAnswerItemEventListener
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.AudioState
import com.viewsonic.classswift.utils.extension.milliSecondToTimerUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class QuizAudioResultViewHolder (
    private val binding: ItemQuizAudioResultBinding,
    private val onItemInteractionListener: OnAudioAnswerItemEventListener
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context
    private val color78CB3D = ContextCompat.getColor(context, R.color.color_78CB3D)
    private val colorC3C7C7 = ContextCompat.getColor(context, R.color.color_C3C7C7)
    private val color2E3133 = ContextCompat.getColor(context, R.color.color_2E3133)
    private val colorF1FAEB = ContextCompat.getColor(context, R.color.color_F1FAEB)
    private val colorNeutral0 = ContextCompat.getColor(context, R.color.neutral_0)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private lateinit var audioInfo: AudioAnswerInfo

    fun onBind(info: AudioAnswerInfo) {
        audioInfo = info
        with(binding) {
            val seatNumberAndName = "${info.displaySeatNumber}  ${info.displayName}"
            tvNumberAndName.text = seatNumberAndName
            tvNumberAndName.isVisible = !info.isPartiallyVisible
            viewDivider.isVisible = !info.isPartiallyVisible
            when (info.answerResultState) {
                AnswerResultState.ANSWERED -> {
                    if (info.audioDuration == null) {
                        getAudioDurationFromUrl(info.audioUrl)
                    }
                    setAudioTime()
                    setAudioIcon()
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(color78CB3D))
                    mcvRoot.setCardBackgroundColor(colorF1FAEB)
                    viewDivider.setBackgroundColor(color78CB3D)
                    tvNumberAndName.setTextColor(colorNeutral0)
                    tvNumberAndName.setBackgroundColor(color78CB3D)
                    tvAbsent.visibility = View.GONE
                    tvNoAnswer.visibility = View.GONE
                }
                AnswerResultState.NO_ANSWER -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(colorC3C7C7))
                    mcvRoot.setCardBackgroundColor(colorNeutral0)
                    viewDivider.setBackgroundColor(colorC3C7C7)
                    tvNumberAndName.setTextColor(color2E3133)
                    tvNumberAndName.setBackgroundColor(colorC3C7C7)
                    cpiProgressIndicator.visibility = View.GONE
                    llAudio.visibility = View.GONE
                    tvAbsent.visibility = View.GONE
                    tvNoAnswer.visibility = View.VISIBLE
                }
                AnswerResultState.ABSENT -> {
                    mcvRoot.setStrokeColor(ColorStateList.valueOf(colorC3C7C7))
                    mcvRoot.setCardBackgroundColor(colorNeutral0)
                    viewDivider.setBackgroundColor(colorC3C7C7)
                    tvNumberAndName.setTextColor(colorC3C7C7)
                    tvNumberAndName.setBackgroundColor(colorNeutral0)
                    cpiProgressIndicator.visibility = View.GONE
                    llAudio.visibility = View.GONE
                    tvAbsent.visibility = View.VISIBLE
                    tvNoAnswer.visibility = View.GONE
                }
                else ->{}
            }
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
            llAudio.setOnClickListener {
                if (audioInfo.audioState == AudioState.PLAY) {
                    onItemInteractionListener.onAudioAnswerItemEvent(AudioAnswerItemEventListener.PauseAudio(audioInfo))
                } else {
                    onItemInteractionListener.onAudioAnswerItemEvent(AudioAnswerItemEventListener.PlayAudio(audioInfo))
                }
            }
        }
    }

    private fun setAudioIcon() {
        binding.apply {
            if (audioInfo.audioState == AudioState.PLAY) {
                ivAudioControl.setImageResource(R.drawable.ic_audio_pause)
            } else {
                ivAudioControl.setImageResource(R.drawable.ic_audio_play)
            }
        }
    }

    private fun setAudioTime() {
        binding.apply {
            audioInfo.audioDuration?.let { duration ->
                cpiProgressIndicator.isVisible = false
                llAudio.isVisible = true
                when (audioInfo.audioState) {
                    AudioState.INIT ->
                        binding.tvAudioTime.text = duration.milliSecondToTimerUnit()
                    else ->
                        binding.tvAudioTime.text = audioInfo.audioRemainTime.milliSecondToTimerUnit()
                }
            } ?: run {
                cpiProgressIndicator.isVisible = true
                llAudio.isVisible = false
            }
        }
    }

    fun setIsPartiallyView(isPartially: Boolean) {
        binding.apply {
            tvNumberAndName.isVisible = !isPartially
            viewDivider.isVisible = !isPartially
        }
    }

    fun setAudioState(info: AudioAnswerInfo) {
        Timber.tag("AudioResultViewHolder").d("info ${info.serialNumber} setAudioState audio state：${audioInfo.audioState}")
        audioInfo = info
        setAudioIcon()
        setAudioTime()
    }

    fun updateRemainTime(info: AudioAnswerInfo) {
        Timber.tag("AudioResultViewHolder").d("info ${info.serialNumber} updateRemainTime audio audioRemainTime：${audioInfo.audioRemainTime}")
        audioInfo = info
        binding.tvAudioTime.text =
            if (audioInfo.audioRemainTime > 0) audioInfo.audioRemainTime.milliSecondToTimerUnit()
            else audioInfo.audioDuration?.milliSecondToTimerUnit()
    }
}