package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.StudentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ToolbarManager(
    private val accountManager : AccountManager,
    private val studentManager: StudentManager,
    private val classroomManager: ClassroomManager,
    private val quizManager: QuizManager,
    private val networkManager: NetworkManager
) {

    private val _toolbarUiState = MutableStateFlow(ToolbarUiState())
    val toolbarUiState: StateFlow<ToolbarUiState> = _toolbarUiState.asStateFlow()

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var collectNetworkStateJob: Job? = null
    // last ParticipationState when network is connected
    private var hasNetworkState: ParticipationState = ParticipationState.NOT_JOINED


    fun initNetworkStateCollection() {
        Timber.tag("ToolBarNetworkState").d("[initCollection] collectNetworkStateJob cancel")
        collectNetworkStateJob?.cancel()
        Timber.tag("ToolBarNetworkState").d("[initCollection] collectNetworkStateJob launch")
        collectNetworkStateJob = coroutineScope.launch(Dispatchers.IO) {
            networkManager.delayInformNetworkAvailabilityState.collect { hasNetwork ->
                Timber.tag("ToolBarNetworkState").d("collect delayInformNetworkAvailabilityState hasNetwork :$hasNetwork")
                if (hasNetwork) {
                    // to recover has network ParticipationState
                    Timber.tag("ToolBarNetworkState").d("toolbar recovery state: $hasNetworkState")
                    setParticipationState(hasNetworkState)
                } else {
                    hasNetworkState = _toolbarUiState.value.participationState
                    Timber.tag("ToolBarNetworkState").d("set toolbar state is NETWORK_DISCONNECT")
                    setParticipationState(ParticipationState.NETWORK_DISCONNECT)
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT_PROMPT_SHOWN).send()
                }
            }
        }
    }

    fun cancelNetworkStateCollection() {
        Timber.tag("ToolBarNetworkState").d("[initCollection] collectNetworkStateJob cancel")
        collectNetworkStateJob?.cancel()
    }

    fun setParticipationState(participationState: ParticipationState) {
        _toolbarUiState.update {
            it.copy(
                participationState = participationState
            )
        }
    }

    fun setIsExpanded(isExpanded: Boolean) {
        _toolbarUiState.update {
            it.copy(
                isExpanded = isExpanded
            )
        }
    }

    fun setPlan() {
        val isPremiumUser = accountManager.selectedOrg?.isPremiumUser == true
        Timber.tag("packageCode").d("isPremiumUser: $isPremiumUser")
        _toolbarUiState.update {
            it.copy(
                isPremiumUser = isPremiumUser,
            )
        }
    }

    fun resetMultipleQuizSelectionInfos() {
        quizManager.resetMultipleOptionInfos()
    }

    fun logOut() {
        accountManager.logout()
    }

    fun quitApp() {
        accountManager.quitApp()
    }

    fun showLeaderBoard(): Boolean {
        return studentManager.showLeaderboard()
    }

    suspend fun startLesson(): Boolean = classroomManager.startLesson()

    suspend fun endLesson(): Boolean = classroomManager.endLesson()

    data class ToolbarUiState(
        val isLoading: Boolean = false,
        val isExpanded: Boolean = false,
        val participationState: ParticipationState = ParticipationState.LESSON_STARTED,
        val isPremiumUser: Boolean = false,
        val counter: Int = 0
    )

    enum class ParticipationState {
        NOT_JOINED,
        JOINED,
        LESSON_STARTED,
        NETWORK_DISCONNECT
    }
}