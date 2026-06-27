package com.viewsonic.classswift.ui.window.viewholder

import android.content.res.ColorStateList
import android.media.MediaMetadataRetriever
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.databinding.ItemMvbQuizAudioAnsweringBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.AudioAnswerItemEventListener
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.OnAudioAnswerItemEventListener
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.AudioState
import com.viewsonic.classswift.utils.extension.milliSecondToTimerUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MvbQuizAudioAnsweringViewHolder(
    private val binding: ItemMvbQuizAudioAnsweringBinding,
    private val onItemInteractionListener: OnAudioAnswerItemEventListener
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private lateinit var audioInfo: AudioAnswerInfo
    private var durationJob: Job? = null

    fun onBind(info: AudioAnswerInfo, showStudentsName: Boolean, isResult: Boolean) {
        // Cancel any in-flight duration fetch for the previously bound student;
        // otherwise its late completion could overwrite the new student's data.
        durationJob?.cancel()
        audioInfo = info
        with(binding) {
            val violetColor = ContextCompat.getColor(context, R.color.color_4848F0)
            val whiteColor = ContextCompat.getColor(context, R.color.neutral_0)
            val cardNeutralBg = ContextCompat.getColor(context, R.color.neutral_0)
            // Absent uses color_EBEBEB (not neutral_100) so the card stays
            // distinct from the surrounding panel background which is also neutral_100.
            val cardAbsentBg = ContextCompat.getColor(context, R.color.color_EBEBEB)
            val absentBorder = ContextCompat.getColor(context, R.color.neutral_300)

            // Submitted card palette: green in Result (final outcome), purple in Quizzing.
            val (answeredBg, answeredBanner) = if (isResult) {
                ContextCompat.getColor(context, R.color.color_E7F7D0) to
                    ContextCompat.getColor(context, R.color.color_48720F)
            } else {
                ContextCompat.getColor(context, R.color.color_EDEDFD) to violetColor
            }

            when (audioInfo.answeringState) {
                AnsweringState.ANSWERED -> bindAnsweredState(answeredBg, answeredBanner, whiteColor)
                AnsweringState.NOT_ANSWER -> bindNotAnsweredState(isResult, cardNeutralBg, violetColor)
                AnsweringState.ABSENT -> bindAbsentState(cardAbsentBg, absentBorder)
            }
            tvNumberAndName.text = audioInfo.displayName
            applyShowStudentsName(showStudentsName)
            applyPartiallyVisible(audioInfo.isPartiallyVisible)
        }
        initClick()
    }

    private fun bindAnsweredState(answeredBg: Int, answeredBanner: Int, whiteColor: Int) {
        with(binding) {
            if (audioInfo.audioDuration == null) {
                getAudioDurationFromUrl(audioInfo)
            }
            mcvRoot.setCardBackgroundColor(answeredBg)
            mcvRoot.strokeWidth = 0
            tvNumberAndName.setTextColor(whiteColor)
            tvNumberAndName.setBackgroundColor(answeredBanner)
            viewDivider.isVisible = false
            tvNotSubmitted.isVisible = false
            tvAbsent.isVisible = false
            setAudioTime()
            tvSubmitted.isVisible = !audioInfo.canShowAnswer
            if (audioInfo.audioState == AudioState.PLAY) {
                ivAudioControl.setImageResource(R.drawable.ic_audio_pause_outline)
            } else {
                ivAudioControl.setImageResource(R.drawable.ic_audio_play_outline)
            }
        }
    }

    private fun bindNotAnsweredState(isResult: Boolean, cardNeutralBg: Int, violetColor: Int) {
        with(binding) {
            mcvRoot.setCardBackgroundColor(cardNeutralBg)
            mcvRoot.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            tvSubmitted.isVisible = false
            llAudio.isVisible = false
            cpiProgressIndicator.isVisible = false
            tvAbsent.isVisible = false
            tvNotSubmitted.isVisible = true
            tvNumberAndName.setBackgroundColor(cardNeutralBg)
            tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_900),
            )
            if (isResult) {
                // Result: gray border + neutral_300 name banner + #333 name + #666 body.
                val borderColor = ContextCompat.getColor(context, R.color.neutral_300)
                mcvRoot.setStrokeColor(ColorStateList.valueOf(borderColor))
                tvNumberAndName.setBackgroundColor(borderColor)
                viewDivider.isVisible = false
                tvNotSubmitted.setTextColor(
                    ContextCompat.getColor(context, R.color.color_666666),
                )
            } else {
                // Quizzing: purple border + #333 name + purple divider (unchanged).
                mcvRoot.setStrokeColor(ColorStateList.valueOf(violetColor))
                viewDivider.setBackgroundColor(violetColor)
                viewDivider.isVisible = true
                tvNotSubmitted.setTextColor(
                    ContextCompat.getColor(context, R.color.neutral_500),
                )
            }
        }
    }

    private fun bindAbsentState(cardAbsentBg: Int, absentBorder: Int) {
        with(binding) {
            mcvRoot.setCardBackgroundColor(cardAbsentBg)
            mcvRoot.setStrokeColor(ColorStateList.valueOf(absentBorder))
            mcvRoot.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_500)
            )
            tvNumberAndName.setBackgroundColor(cardAbsentBg)
            viewDivider.setBackgroundColor(absentBorder)
            viewDivider.isVisible = true
            tvSubmitted.isVisible = false
            llAudio.isVisible = false
            cpiProgressIndicator.isVisible = false
            tvNotSubmitted.isVisible = false
            tvAbsent.isVisible = true
        }
    }

    private fun getAudioDurationFromUrl(targetInfo: AudioAnswerInfo) {
        val targetSerial = targetInfo.serialNumber
        val retriever = MediaMetadataRetriever()
        durationJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                retriever.setDataSource(targetInfo.audioUrl)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                targetInfo.audioDuration = duration
                onItemInteractionListener.onAudioAnswerItemEvent(
                    AudioAnswerItemEventListener.UpdateDuration(targetInfo)
                )
                withContext(Dispatchers.Main) {
                    // ViewHolder may have been rebound to a different student;
                    // only refresh UI if still bound to the one we fetched for.
                    // Mirror duration onto the live audioInfo — a payload rebind
                    // for the same student replaces it with a fresh copy that
                    // doesn't carry the duration yet.
                    if (audioInfo.serialNumber == targetSerial) {
                        audioInfo.audioDuration = duration
                        setAudioTime()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to retrieve audio duration for serial $targetSerial")
            } finally {
                retriever.release()
            }
        }
    }

    fun cancelPendingWork() {
        durationJob?.cancel()
    }

    private fun initClick() {
        with(binding) {
            mcvRoot.setOnClickListener {
                if (audioInfo.answeringState == AnsweringState.ANSWERED) {
                    onItemInteractionListener.onAudioAnswerItemEvent(
                        AudioAnswerItemEventListener.ItemClick(audioInfo)
                    )
                }
            }
            llAudio.setOnClickListener {
                if (audioInfo.audioState == AudioState.PLAY) {
                    onItemInteractionListener.onAudioAnswerItemEvent(
                        AudioAnswerItemEventListener.PauseAudio(audioInfo)
                    )
                } else {
                    onItemInteractionListener.onAudioAnswerItemEvent(
                        AudioAnswerItemEventListener.PlayAudio(audioInfo)
                    )
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
            tvSubmitted.isVisible = !info.canShowAnswer
            setAudioTime()
        }
    }

    fun setAudioState(info: AudioAnswerInfo) {
        audioInfo = info
        binding.apply {
            if (audioInfo.audioState == AudioState.PLAY) {
                ivAudioControl.setImageResource(R.drawable.ic_audio_pause_outline)
            } else {
                ivAudioControl.setImageResource(R.drawable.ic_audio_play_outline)
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

    fun setShowStudentsName(show: Boolean) {
        applyShowStudentsName(show)
    }

    fun setPartiallyVisible(info: AudioAnswerInfo) {
        audioInfo = info
        applyPartiallyVisible(info.isPartiallyVisible)
    }

    private fun applyShowStudentsName(show: Boolean) {
        // Toggle only renders in Result + Student responses tab; Quizzing keeps the
        // header visible because the default value stays true (no one can flip it).
        binding.tvNumberAndName.isVisible = show
    }

    private fun applyPartiallyVisible(isPartiallyVisible: Boolean) {
        // In MVB Result, isPartiallyVisible=true means the card is excluded from the
        // currently selected highlight bar — render dimmed. False = full color.
        binding.mcvRoot.alpha = if (isPartiallyVisible) DIMMED_ALPHA else FULL_ALPHA
    }

    companion object {
        private const val FULL_ALPHA = 1f
        private const val DIMMED_ALPHA = 0.3f
    }
}
