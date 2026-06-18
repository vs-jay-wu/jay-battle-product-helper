package com.viewsonic.classswift.ui.windowmodel.tool

import android.content.Context
import com.viewsonic.classswift.api.SettingsApiService
import com.viewsonic.classswift.api.body.TranslationToolUserPreferenceBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.constant.ApiConstant
import com.viewsonic.classswift.data.datastore.DebugDataStore
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsWindowModel(
    private val accountManager: AccountManager,
    private val socketManager: SocketManager,
    private val apiService: SettingsApiService,
    private val debugDataStore: DebugDataStore,
) : IWindowModel {

    private val _updateUIFlow = MutableSharedFlow<SettingUiEvent>()
    val updateUIFlow = _updateUIFlow.asSharedFlow()

    val socketConnectionStateSharedFlow = socketManager.connectionStateSharedFlow

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var isPremiumUser: Boolean = false
    var isTranslationOn: Boolean = false
        private set

    init {
        isPremiumUser = accountManager.selectedOrg?.isPremiumUser == true
    }

    override fun onCleared() {}

    suspend fun isUseAndroidTestJsonForMaintenanceAnnouncements() = withContext(Dispatchers.IO) {
        return@withContext debugDataStore.isUseAndroidTestJsonForMaintenanceAnnouncements()
    }

    suspend fun setUseAndroidTestJsonForMaintenanceAnnouncements(value: Boolean) = withContext(Dispatchers.IO) {
        debugDataStore.setSsUseAndroidTestJsonForMaintenanceAnnouncements(value)
    }

    fun connectSocket() = socketManager.connect()

    fun disconnectSocket() = socketManager.disconnect()

    fun getSocketConnectionState() = socketManager.connectionState

    fun getUserPreference() {
        coroutineScope.launch(Dispatchers.IO) {
            when (val response = apiService.getUserPreference(accountManager.getBearerToken(), accountManager.selectedOrg?.orgId ?: "")) {
                is ApiResponse.Success -> {
                    isTranslationOn = response.data.userPreferenceData.translationTool.isOn
                    _updateUIFlow.emit(SettingUiEvent.UpdateTranslationSetting)
                }
                else -> {
                    _updateUIFlow.emit(SettingUiEvent.GetUserPreferenceFailed)
                }
            }
        }
    }

    fun getSetTranslationPreference() {
        coroutineScope.launch(Dispatchers.IO) {
            when (val response = apiService.setUserPreference(
                accountManager.getBearerToken(),
                accountManager.selectedOrg?.orgId ?: "",
                ApiConstant.UserPreferenceType.TRANSLATION_TOOL.typeInServer,
                TranslationToolUserPreferenceBody(!isTranslationOn).toRequestBody()
            )) {
                is ApiResponse.Success -> {
                    isTranslationOn = response.data.userPreferenceData.translationTool.isOn
                    _updateUIFlow.emit(SettingUiEvent.UpdateTranslationSetting)
                }
                else -> {
                    _updateUIFlow.emit(SettingUiEvent.UpdateTranslationPreferenceFailed)
                }
            }
        }
    }

    fun canUsingTranslationTool(): Boolean {
        return isPremiumUser
    }

    fun isChineseLanguage(context: Context): Boolean {
        return LanguageUtils.isAppLanguageChinese(context)
    }

    sealed class SettingUiEvent {
        data object UpdateTranslationSetting : SettingUiEvent()
        data object GetUserPreferenceFailed : SettingUiEvent()
        data object UpdateTranslationPreferenceFailed : SettingUiEvent()
    }
}