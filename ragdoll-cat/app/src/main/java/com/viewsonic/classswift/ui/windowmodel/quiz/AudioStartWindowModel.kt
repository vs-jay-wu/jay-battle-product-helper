package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.manager.AudioPlayerHelper
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.AudioState
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class AudioStartWindowModel(private val quizManager: QuizManager, private val audioPlayerHelper: AudioPlayerHelper) : IWindowModel {
    private var audioAnswerInfoMap: Map<Int, AudioAnswerInfo> = emptyMap()
    private var audioAnswerInfos: List<AudioAnswerInfo> = emptyList()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var playAudioJob: Job? = null
    private var errorJob: Job? = null

    private val _audioInfosSharedFlow = MutableSharedFlow<List<AudioAnswerInfo>>()
    val audioInfosSharedFlow: SharedFlow<List<AudioAnswerInfo>> = _audioInfosSharedFlow.asSharedFlow()

    init {
        initCollection()
    }

    fun getCurrentStudentQuizAudioAnsweringInfoList(): List<AudioAnswerInfo> {
        audioAnswerInfos = quizManager.quizzingUiState.value.studentQuizzingInfoList.map {
            var answeringState = AnsweringState.ABSENT
            var answerResultState = AnswerResultState.ABSENT
            if (it.status == StudentInfo.Status.ACTIVE) {
                answeringState = AnsweringState.NOT_ANSWER
                answerResultState = AnswerResultState.NO_ANSWER
            }
            if (it.answerStringData.isNotEmpty()) {
                answeringState = AnsweringState.ANSWERED
                answerResultState = AnswerResultState.ANSWERED
            }
            val info = QuizAnsweringInfo.fromStudentAudioQuizzingInfo(
                it,
                answeringState,
                quizManager.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS,
                answerResultState
            )
            audioAnswerInfoMap[info.serialNumber]?.let {
                info.canShowAnswer = it.canShowAnswer
                info.isPartiallyVisible = it.isPartiallyVisible
                info.audioDuration = it.audioDuration
                info.audioRemainTime = it.audioRemainTime
                info.audioState = it.audioState
            }
            info
        }
        audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
        return audioAnswerInfos
    }

    fun setPartiallyVisibleState(isPartiallyVisible: Boolean): List<AudioAnswerInfo> {
        audioAnswerInfos = audioAnswerInfos.map {
            it.copy(isPartiallyVisible = isPartiallyVisible)
        }
        audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
        return audioAnswerInfos
    }

    fun setCanShowAnswer(info: AudioAnswerInfo) {
        if (info.audioUrl == audioPlayerHelper.currentUrl) {
            audioPlayerHelper.stop()
        }
        audioAnswerInfos = audioAnswerInfos.map {
            if (it.serialNumber == info.serialNumber) {
                it.copy(canShowAnswer = !it.canShowAnswer, audioState = AudioState.INIT,  audioRemainTime = 0)
            } else {
                it
            }
        }
        audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
        coroutineScope.launch(Dispatchers.IO) {
            _audioInfosSharedFlow.emit(audioAnswerInfos)
        }
    }

    fun setQuizResult(): List<AudioAnswerInfo> {
        // Result state reveals every submitted recording immediately —
        // no per-card click required to enter playable state.
        audioAnswerInfos.forEach {
            it.audioState = AudioState.INIT
            it.canShowAnswer = true
        }
        quizManager.setAudioAnswerResultInfos(audioAnswerInfos)
        audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
        return audioAnswerInfos
    }

    fun setStop() {
        Timber.d("setPause function")
        cancelJob()
        audioPlayerHelper.stop()
    }

    fun setPause() {
        Timber.d("setPause function")
        audioPlayerHelper.togglePlayPause()
    }

    fun setDuration(info: AudioAnswerInfo) {
        audioAnswerInfos = audioAnswerInfos.map {
            if(it.serialNumber == info.serialNumber) it.copy(audioDuration = info.audioDuration)
            else it
        }
        audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
    }

    fun setPlay(info: AudioAnswerInfo) {
        Timber.d("setPlay function")
        if (playAudioJob == null) {
            initCollection()
        }
        if (info.audioState == AudioState.INIT) {
            audioPlayerHelper.play(info.audioUrl)
        } else {
            audioPlayerHelper.togglePlayPause()
        }
    }

    private fun initCollection() {
        playAudioJob = coroutineScope.launch(Dispatchers.IO) {
            audioPlayerHelper.playerState.collect { uiState ->
                withContext(Dispatchers.Main) {
                    when (uiState) {
                        is AudioPlayerHelper.PlayerState.Complete -> {
                            Timber.tag("playerState").d("AudioPlayerHelper call back Complete")
                            audioAnswerInfos = audioAnswerInfos.map {
                                 it.copy(audioState = AudioState.INIT)
                            }
                            audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
                            _audioInfosSharedFlow.emit(audioAnswerInfos)
                        }

                        is AudioPlayerHelper.PlayerState.GetDuration -> {
                            audioAnswerInfos = audioAnswerInfos.map {
                                if (it.audioUrl == uiState.audioUrl && it.audioDuration == null) {
                                    Timber.tag("playerState").d("${it.serialNumber} will update Duration")
                                    it.copy(audioDuration = uiState.duration)
                                }
                                else it
                            }
                            audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
                            _audioInfosSharedFlow.emit(audioAnswerInfos)
                        }

                        is AudioPlayerHelper.PlayerState.Pause -> {
                            audioAnswerInfos = audioAnswerInfos.map {
                                // switch to play other audio, last playing audio have Pause call back
                                if (it.audioUrl == uiState.audioUrl) {
                                    Timber.tag("playerState").d("${it.serialNumber} will be pause UI")
                                    it.copy(audioState = AudioState.PAUSE)
                                } else it.copy(audioState = AudioState.INIT)
                            }
                            audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
                            _audioInfosSharedFlow.emit(audioAnswerInfos)
                        }

                        is AudioPlayerHelper.PlayerState.Playing -> {
                            audioAnswerInfos = audioAnswerInfos.map {
                                if (it.audioUrl == uiState.audioUrl) {
                                    Timber.tag("playerState").d("${it.serialNumber} will be play UI")
                                    it.copy(audioState = AudioState.PLAY)
                                }
                                else it.copy(audioState = AudioState.INIT)
                            }
                            audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
                            _audioInfosSharedFlow.emit(audioAnswerInfos)
                        }

                        is AudioPlayerHelper.PlayerState.UpdateRemainTime -> {
                            if (uiState.remainTime > 0) {
                                audioAnswerInfos = audioAnswerInfos.map {
                                    if (it.audioUrl == uiState.audioUrl)
                                        it.copy(audioState = AudioState.PLAY, audioRemainTime = uiState.remainTime)
                                    else it
                                }
                                audioAnswerInfoMap = audioAnswerInfos.associateBy { it.serialNumber }
                                _audioInfosSharedFlow.emit(audioAnswerInfos)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
        errorJob = coroutineScope.launch(Dispatchers.IO) {
            audioPlayerHelper.errorEvent.collect { errorEvent ->
            }
        }
    }

    private fun cancelJob() {
        playAudioJob?.cancel()
        errorJob?.cancel()
        playAudioJob = null
        errorJob = null
    }

    override fun onCleared() {
        Timber.d("[AudioWindowModel] onCleared")
        audioAnswerInfos = emptyList()
        audioAnswerInfoMap = emptyMap()
        audioPlayerHelper.release()
        coroutineScope.cancel()
    }
}