package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.api.AccountApiService
import com.viewsonic.classswift.api.SettingsApiService
import com.viewsonic.classswift.api.body.InAppTutorialUserPreferenceBody
import com.viewsonic.classswift.api.body.IsFeedbackHelpedBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.constant.ApiConstant
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class InAppTutorialWindowModel(
    private val settingsApiService: SettingsApiService,
    private val accountApiService: AccountApiService,
    private val accountManager: AccountManager,
    private val socketManager: SocketManager
) : IWindowModel {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var orgId = ""
    private val _uiEventSharedFlow = MutableSharedFlow<InAppTutorialUiEvent>()
    val uiEventSharedFlow: SharedFlow<InAppTutorialUiEvent> = _uiEventSharedFlow.asSharedFlow()
    var isNeedToShowFeedbackUI: Boolean = false
        private set

    init {
        accountManager.selectedOrg?.let {
            orgId = it.orgId
        }
        coroutineScope.launch {
            fetchHasSendIsFeedbackHelped()
        }
    }

    override fun onCleared() {}

    fun connectSocket() {
        socketManager.connect()
    }

    fun checkMultipleLogin() {
        accountManager.checkMultipleLogin()
    }

    fun forceQuitApp() {
        accountManager.quitApp(false)
    }

    fun sendFeedback(isHelped: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            when (accountApiService.sendIsFeedbackHelped(
                accountManager.getBearerToken(),
                IsFeedbackHelpedBody(
                    isHelped
                )
            )) {
                is ApiResponse.Success -> {
                    _uiEventSharedFlow.emit(InAppTutorialUiEvent.SendFeedbackSuccessful)
                }
                else -> {
                    _uiEventSharedFlow.emit(InAppTutorialUiEvent.SendFeedbackFailed)
                }
            }
        }
    }

    fun sendIsInAppTutorialShown(isShown: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            when (settingsApiService.setUserPreference(
                accountManager.getBearerToken(),
                orgId,
                ApiConstant.UserPreferenceType.TUTORIAL.typeInServer,
                InAppTutorialUserPreferenceBody(isShown).toRequestBody()
            )) {
                is ApiResponse.Success -> {
                    _uiEventSharedFlow.emit(InAppTutorialUiEvent.SendIsInAppTutorialShownSuccessful)
                }
                else -> {
                    _uiEventSharedFlow.emit(InAppTutorialUiEvent.SendIsInAppTutorialShownFailed)
                }
            }
        }
    }

    private suspend fun fetchHasSendIsFeedbackHelped() = withContext(Dispatchers.IO) {
        isNeedToShowFeedbackUI = when (val response = accountApiService.getFeedbackContent(
            accountManager.getBearerToken()
        )) {
            is ApiResponse.Success -> {
                response.data.feedbackDataList.find {
                    it.userId == accountManager.userInfo.userId
                } == null
            }
            else -> {
                false
            }
        }
    }

    sealed class InAppTutorialUiEvent {
        data object SendIsInAppTutorialShownSuccessful : InAppTutorialUiEvent()
        data object SendIsInAppTutorialShownFailed : InAppTutorialUiEvent()
        data object SendFeedbackSuccessful : InAppTutorialUiEvent()
        data object SendFeedbackFailed : InAppTutorialUiEvent()
    }
}