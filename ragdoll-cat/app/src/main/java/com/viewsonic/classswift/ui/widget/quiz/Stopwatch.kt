package com.viewsonic.classswift.ui.widget.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.databinding.ViewStopwatchBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.TimeUtils
import com.viewsonic.classswift.utils.extension.startTimerInMilliSec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class Stopwatch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val coroutineScope = CoroutineManager.getScope(this)
    private val quizManager by inject<QuizManager>(QuizManager::class.java)
    private var binding: ViewStopwatchBinding =
        ViewStopwatchBinding.inflate(LayoutInflater.from(context), this)

    private var timerJob: Job? = null
    private var startTimeInMillis: Long = 0

    init {
        startStopwatch()
    }

    fun startStopwatch() {
        if (timerJob?.isActive != true) {
            startTimeInMillis = if (quizManager.quizStartTimeInMillis > 0) {
                quizManager.quizStartTimeInMillis
            } else {
                System.currentTimeMillis()
            }
            val timeDiffInMillis = TimeUtils.getTimeDiffFromCurrentTimeInMillis(startTimeInMillis)
            timerJob?.cancel()
            timerJob = coroutineScope.startTimerInMilliSec(
                startTimeInMillis,
                timeDiffInMillis,
                onTick = { tickSecond ->
                    withContext(Dispatchers.Main) {
                        binding.tvStopwatch.text = formatTime(tickSecond)
                    }
                }
            )
        }
    }

    fun stopStopwatch() {
        timerJob?.cancel()
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val (minutes, seconds) = DateTimeUtils.formatToMinuteSecondPair(milliseconds)
        return String.format("%02d:%02d", minutes, seconds)
    }

    private companion object {
        const val RESET_TIME = "00:00"
    }
}