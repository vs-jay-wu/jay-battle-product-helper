package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.uimanager.QuizUiManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

class QuizCommonWindowModel(
    private val quizUiManager: QuizUiManager,
    private val networkManager: NetworkManager,
    private val socketManager: SocketManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) : IWindowModel {
    private val coroutineScope = CoroutineManager.getScope(this)

    private val _networkAvailabilityState = MutableStateFlow(false)
    val networkAvailabilityState: StateFlow<Boolean> = _networkAvailabilityState.asStateFlow()

    init {
        initCollection()
    }

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            combine(
                networkManager.delayInformNetworkAvailabilityState,
                socketManager.connectionStateSharedFlow
                    .onStart { emit(socketManager.connectionState) }
                    .map { socketManager.isConnected() }
                    .distinctUntilChanged(),
            ) { net, sock -> net && sock }
                .collect { hasNetwork ->
                    Timber.d("[QuizCommonWindowModel] NetworkAvailabilityState: $hasNetwork")
                    _networkAvailabilityState.emit(hasNetwork)
                    if (!hasNetwork) {
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT_PROMPT_SHOWN).send()
                    }
                }
        }
    }

    fun addOpenedQuizWindowTag(tag: WindowTag) {
        quizUiManager.addOpenedQuizWindowTag(tag)
    }

    fun removeOpenedQuizWindowTag(tag: WindowTag) {
        quizUiManager.removeOpenedQuizWindowTag(tag)
    }

    fun getScreenImageUri(): String {
        return QuizSharedUiInfo.screenshotImageUri
    }

    override fun onCleared() {
        coroutineScope.cancel()
    }

}
