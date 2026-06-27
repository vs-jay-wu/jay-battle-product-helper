package com.viewsonic.classswift.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.constant.AppConstants.FIVE_SEC_DELAY
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.data.datastore.SettingsDataStore
import com.viewsonic.classswift.data.info.SignInUrlInfo
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.AccountManager.GetOAuthCodeStatus
import com.viewsonic.classswift.manager.FirebaseInstallationManager
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.LanguageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

class LoginViewModel(
    private val accountManager: AccountManager,
    private val firebaseInstallationManager: FirebaseInstallationManager,
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager,
    private val settingsDataStore: SettingsDataStore,
    private val myViewBoardEventNotifier: MyViewBoardEventNotifier,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider,
) : ViewModel() {
    private var serverLatestAppVersionName: String = ""

    private val _activityEventFlow = MutableSharedFlow<LoginModelEvent>()
    val activityEventFlow = _activityEventFlow.asSharedFlow()

    private val _loginFlowState = MutableStateFlow(LoginFlowState())
    val loginFlowState: StateFlow<LoginFlowState> = _loginFlowState.asStateFlow()

    private val _getSignInUrlFlow = MutableSharedFlow<GetSignInUrlEvent>()
    val getSignInUrlFlow = _getSignInUrlFlow.asSharedFlow()

    private val _regionDetectFlow = MutableSharedFlow<DetectRegionEvent>()
    val regionDetectFlow = _regionDetectFlow.asSharedFlow()

    private var getOAuthsCodeInJob: Job? = null
    private var crossDayJob: Job? = null

    var isLogout = false
    var logoutIDToken = ""
    var goSelectOrgWindow = false

    private var lastDate = LocalDate.now()

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    fun getInstallationId() = firebaseInstallationManager.getInstallationId()

    fun setLoginMethod(loginMethod: AmplitudeConstant.LoginMethod) = accountManager.setLoginMethod(loginMethod)

    suspend fun startMaintenanceAnnouncementsCheck(): MaintenanceFetchResult {
        maintenanceAnnouncementsUiManager.resetHasViewedFiveMinutesBeforeMaintenanceAnnouncement()
        return when (maintenanceAnnouncementsUiManager.fetchMaintenanceAnnouncements()) {
            is ApiResponse.Success -> {
                if (maintenanceAnnouncementsUiManager.isInMaintenancePhase(MaintenanceAnnouncementsUiManager.MaintenancePhase.DURING_DOWNTIME)) {
                    MaintenanceFetchResult.UNDER_MAINTENANCE
                } else {
                    maintenanceAnnouncementsUiManager.startCheckCronJob()
                    MaintenanceFetchResult.SUCCESS
                }
            }
            is ApiResponse.NetworkDisconnected -> {
                MaintenanceFetchResult.NETWORK_ERROR
            }
            else -> {
                MaintenanceFetchResult.FAILED
            }
        }
    }

    fun showSelectOrgWindow() {
        viewModelScope.launch {
            _activityEventFlow.emit(LoginModelEvent.SelectOrg)
        }
    }

    fun quitApp() {
        viewModelScope.launch {
            _activityEventFlow.emit(LoginModelEvent.QuitApp)
        }
    }

    fun getChallengeCode(refreshChallengeCode: Boolean = false) {
        Timber.tag("loginFlow").d("getChallengeCode")
        viewModelScope.launch {
            // has refresh token, didn't need challenge code.
            if (accountManager.isRememberLogin) {
                setLoginState(LoginState.LOGIN)
            } else {
                if (accountManager.getChallengeCode()) {
                    // formLoginPage is false, call by LoginFragment should nav to sign in page.
                    if (refreshChallengeCode) {
                        getSignInUrlList()
                    } else {
                        setLoginState(LoginState.TO_SIGN_IN_PAGE)
                    }
                } else {
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE, AmplitudeConstant.EventProperties.Value.GET_PKCE_FAILED)
                        .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                        .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                        .send()
                    setErrorState(LoginState.GET_CHALLENGE_CODE)
                }
            }
        }
    }

    fun isLogOutAndQuit(): Boolean {
        return accountManager.logoutAndQuit
    }

    fun loginWithMvbToken() {
        Timber.tag("loginFlow").d("loginWithMvbToken")
        viewModelScope.launch {
            val result = accountManager.loginWithMvbToken()
            if (accountManager.isGuestMode) {
                setLoginState(LoginState.GUEST_GET_CLASSROOM_LIST)
            } else {
                if (result) {
                    if (accountManager.fillUserInfo.needFillAccountPage()) {
                        setLoginState(LoginState.GET_ARTICLE_ID)
                    } else {
                        setLoginState(LoginState.GET_ACCOUNT_INFO)
                    }
                } else {
                    Timber.d("login api response is null")
                    setLoginState(LoginState.LOGIN_WITH_MVB_TOKEN_FAIL)
                    myViewBoardEventNotifier.notifyLoginFailed()
                }
            }
        }
    }

    fun login() {
        Timber.tag("loginFlow").d("login")
        viewModelScope.launch {
            val result = accountManager.login()
            if (result) {
                if (accountManager.fillUserInfo.needFillAccountPage()) {
                    setLoginState(LoginState.GET_ARTICLE_ID)
                } else {
                    setLoginState(LoginState.GET_ACCOUNT_INFO)
                }
            } else {
                Timber.d("login api response is null")
                setErrorState(LoginState.TO_SIGN_IN_PAGE)
            }
        }
    }

    fun getArticleID() {
        Timber.tag("loginFlow").d("getArticleID")
        viewModelScope.launch {
            val loginResponse = accountManager.getArticleID()
            if (loginResponse) {
                setLoginState(LoginState.GET_ACCOUNT_INFO)
            } else {
                Timber.d("getArticleID api response is failed")
                setErrorState(LoginState.GET_ARTICLE_ID)
            }
        }
    }

    fun getRegion() {
        Timber.tag("detectRegion").d("getRegion")
        viewModelScope.launch {
            if (accountManager.getRegion()) {
                _regionDetectFlow.emit(DetectRegionEvent.IsAvailableRegion(accountManager.singInUrlInfo.isAvailableRegion()))
            } else {
                _regionDetectFlow.emit(DetectRegionEvent.GetRegionApiError)
            }
        }
    }

    fun getSignInUrlList() {
        Timber.tag("signInFlow").d("getSignInUrlList")
        viewModelScope.launch {
            try {
                val (getsSsoUrlInfoResult, getQrCodeUrlInfoResult) = awaitAll(
                    async { accountManager.getSsoSignInUrlInfo() },
                    async { accountManager.getQrCodeSignInUrl() }
                )
                if (getsSsoUrlInfoResult && getQrCodeUrlInfoResult) {
                    _getSignInUrlFlow.emit(GetSignInUrlEvent.Complete(accountManager.singInUrlInfo))
                } else {
                    if (!getsSsoUrlInfoResult) {
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                            .appendEventProperty(AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE, AmplitudeConstant.EventProperties.Value.GET_LOGIN_URLS_FAILED)
                            .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                            .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                            .send()
                    }

                    if (!getQrCodeUrlInfoResult) {
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                            .appendEventProperty(AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE, AmplitudeConstant.EventProperties.Value.GET_QRCODE_URL_FAILED)
                            .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                            .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                            .send()
                    }

                    _getSignInUrlFlow.emit(GetSignInUrlEvent.GetSignUrlError)
                }
            } catch (_: Exception) {
                _getSignInUrlFlow.emit(GetSignInUrlEvent.GetSignUrlError)
            }
        }
    }

    fun getAccountInfo() {
        Timber.tag("loginFlow").d("getAccountInfo")
        viewModelScope.launch {
            val loginResponse = accountManager.getAccountInfo()
            if (loginResponse) {
                Timber.d("getAccountInfo api response is success")
                val resolution = resolveLoginAccountInfoSuccess(
                    isMyViewBoardBound = myViewBoardConnectionStateProvider.isBound(),
                    isRememberLogin = accountManager.isRememberLogin,
                )
                if (resolution.shouldClearRememberLoginInStore) {
                    accountManager.setRememberLogin(false)
                }
                setLoginState(resolution.nextState)
            } else {
                Timber.d("getAccountInfo api response is failed")
                setErrorState(LoginState.GET_ACCOUNT_INFO)
            }
        }
    }

    private fun setErrorState(retryState: LoginState) {
        viewModelScope.launch(Dispatchers.IO) {
            setLoginState(LoginState.ERROR)
            //error toast show 3 sec, will do retryState
            delay(THREE_SEC_DELAY)
            setLoginState(retryState)
        }
    }

    fun setLoginState(state: LoginState) {
        Timber.tag("loginFlow").d("setLoginState: $state")
        viewModelScope.launch {
            _loginFlowState.emit(LoginFlowState(state))
        }
    }

    fun getOAuthCode() {
        if (getOAuthsCodeInJob?.isActive == true) return // avoid execute multiple time
        getOAuthsCodeInJob = viewModelScope.launch {
            while (isActive) {
                //after 5 sec, call getOAuthsCode API.
                delay(FIVE_SEC_DELAY)
                when (accountManager.getOAuthsCode()) {
                    GetOAuthCodeStatus.SUCCESS -> {
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_METHOD_SELECTED)
                            .appendUserProperty(AmplitudeConstant.UserProperties.Key.LOGIN_METHOD, AmplitudeConstant.UserProperties.Value.QR_CODE)
                            .send()
                        setLoginMethod(AmplitudeConstant.LoginMethod.QrCode)
                        Timber.tag("SignIn").d("get OAuths code")
                        setLoginState(LoginState.GET_AUTHS_CODE)
                    }
                    GetOAuthCodeStatus.FAILED -> {
                        Timber.tag("SignIn").d("OAuths code invalided")
                        getChallengeCode(true)
                        cancelGetOAuthCode()
                    }
                    GetOAuthCodeStatus.PULLING -> {
                        Timber.tag("SignIn").d("keep call getOAuthsCode api")
                    }
                }
            }
        }
    }

    fun cancelGetOAuthCode() {
        if (getOAuthsCodeInJob?.isActive == true)  {
            getOAuthsCodeInJob?.cancel() }
    }

    fun startDateMonitor() {
        if (crossDayJob?.isActive == true) return
        val oneMinDelayTime = 60000L
        crossDayJob = viewModelScope.launch {
            while (isActive) {
                val now = LocalDate.now()
                if (now != lastDate) {
                    lastDate = now
                    cancelGetOAuthCode()
                    getChallengeCode(true)
                }
                delay(oneMinDelayTime) // check every min
            }
        }
    }

    fun hasOAuthCode(): Boolean {
        return !accountManager.oAuthCode.isBlank()
    }

    fun cancelDateMonitor() {
        if (crossDayJob?.isActive == true)  {
            getOAuthsCodeInJob?.cancel() }
    }

    fun getUnderMaintenanceTitle() = maintenanceAnnouncementsUiManager.getMaintenanceTitle(MaintenanceAnnouncementsUiManager.MaintenancePhase.DURING_DOWNTIME)

    fun getUnderMaintenanceDescription() = maintenanceAnnouncementsUiManager.getMaintenanceDescription(MaintenanceAnnouncementsUiManager.MaintenancePhase.DURING_DOWNTIME)

    fun setAppLanguage(context: Context) {
        viewModelScope.launch {
            settingsDataStore.setLanguageIsChinese(LanguageUtils.isAppLanguageChinese(context = context))
            LanguageUtils.setLanguageCode(LanguageUtils.isAppLanguageChinese(context = context))
        }
    }

    data class LoginFlowState(
        var state: LoginState = LoginState.SET_LANGUAGE,
    )

    enum class LoginState {
        ERROR,
        SET_LANGUAGE,
        CHECK_REGION,
        CHECK_OVERLAY_PERMISSION,
        CHECK_MAINTENANCE_ANNOUNCEMENTS,
        OTA,
        GET_CHALLENGE_CODE,
        TO_SIGN_IN_PAGE,
        LOGIN,
        GET_ARTICLE_ID,
        GET_ACCOUNT_INFO,
        SHOW_REMEMBER_DIALOG,
        CHECK_FILL_USER_INFO,
        GET_AUTHS_CODE,
        // myViewBoard flow
        LOGIN_WITH_MVB_TOKEN,
        LOGIN_WITH_MVB_TOKEN_FAIL,
        GUEST_GET_CLASSROOM_LIST,
        GUEST_GET_CLASSROOM_LIST_FAIL,
    }

    fun setServerLatestAppVersionName(versionName: String) {
        serverLatestAppVersionName = versionName
    }

    fun getServerLatestAppVersionName(): String = serverLatestAppVersionName

    sealed class GetSignInUrlEvent {
        data object GetSignUrlError : GetSignInUrlEvent()
        data class Complete(val info: SignInUrlInfo = SignInUrlInfo()) : GetSignInUrlEvent()
    }

    sealed class DetectRegionEvent {
        data object GetRegionApiError : DetectRegionEvent()
        data class IsAvailableRegion(val isAvailable: Boolean) : DetectRegionEvent()
    }

    sealed class LoginModelEvent {
        data object QuitApp: LoginModelEvent()
        data object SelectOrg: LoginModelEvent()
        data class ChangeLanguage(val language: String) : LoginModelEvent()
    }

    enum class MaintenanceFetchResult {
        SUCCESS,
        UNDER_MAINTENANCE,
        NETWORK_ERROR,
        FAILED
    }

    data class LoginAccountInfoSuccessResolution(
        val shouldClearRememberLoginInStore: Boolean,
        val nextState: LoginState,
    )

    companion object {
        /**
         * Pure resolver for post-[AccountManager.getAccountInfo] login flow
         * (CLSWAN-1233: MVB must not show save-login dialog and must clear remember-login flag).
         */
        @androidx.annotation.VisibleForTesting
        internal fun resolveLoginAccountInfoSuccess(
            isMyViewBoardBound: Boolean,
            isRememberLogin: Boolean,
        ): LoginAccountInfoSuccessResolution {
            if (isMyViewBoardBound) {
                return LoginAccountInfoSuccessResolution(
                    shouldClearRememberLoginInStore = true,
                    nextState = LoginState.CHECK_FILL_USER_INFO,
                )
            }
            if (isRememberLogin) {
                return LoginAccountInfoSuccessResolution(
                    shouldClearRememberLoginInStore = false,
                    nextState = LoginState.CHECK_FILL_USER_INFO,
                )
            }
            return LoginAccountInfoSuccessResolution(
                shouldClearRememberLoginInStore = false,
                nextState = LoginState.SHOW_REMEMBER_DIALOG,
            )
        }
    }
}
