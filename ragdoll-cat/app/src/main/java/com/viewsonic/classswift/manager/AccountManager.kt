package com.viewsonic.classswift.manager

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.AccountApiService
import com.viewsonic.classswift.api.body.GuestLoginPostBody
import com.viewsonic.classswift.api.body.LoginPostNoTokenBody
import com.viewsonic.classswift.api.body.LoginWithMvbTokenPostBody
import com.viewsonic.classswift.api.body.LoginWithRefreshTokenPostBody
import com.viewsonic.classswift.api.body.UserInfoBody
import com.viewsonic.classswift.api.response.ArticleResponse
import com.viewsonic.classswift.api.response.Organization
import com.viewsonic.classswift.api.response.toFillUserInfo
import com.viewsonic.classswift.api.response.toGuestOrgInfo
import com.viewsonic.classswift.api.response.toOrganizationInfo
import com.viewsonic.classswift.api.response.toUserInfo
import com.viewsonic.classswift.api.response.toUserOrgInfo
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.constant.AppConstants.FIVE_SEC_DELAY
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardLaunchState
import com.viewsonic.classswift.data.datastore.LoginDataStore
import com.viewsonic.classswift.data.info.FillUserInfo
import com.viewsonic.classswift.data.info.GuestOrganizationInfo
import com.viewsonic.classswift.data.info.LoginInfo
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.data.info.SignInUrlInfo
import com.viewsonic.classswift.data.info.UserInfo
import com.viewsonic.classswift.data.info.UserOrganizationInfo
import com.viewsonic.classswift.data.socket.LogoutSocketMessage
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.service.ClassSwiftService
import com.viewsonic.classswift.ui.activity.LoginActivity
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.extension.localizedContext
import com.viewsonic.classswift.utils.extension.withMyViewBoardConsentPromptsSuppressed
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.system.exitProcess

class AccountManager(
    private val applicationContext: Context,
    private val apiService: AccountApiService,
    private val tutorialManager: TutorialManager,
    private val networkManager: NetworkManager,
    private val okHttpClient: OkHttpClient,
    private val loginDataStore: LoginDataStore,
    private val firebaseManager: FirebaseManager,
    private val activityManager: ActivityManager,
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) {

    private val coroutineScope = CoroutineManager.getScope(this)
    private var checkMultipleLoginJob: Job? = null
    private var detectNetworkStateJob: Job? = null

    private var dialogWindow: CSSystemDialogWindow? = null

    var loginMethod: AmplitudeConstant.LoginMethod = AmplitudeConstant.LoginMethod.Automatic
        private set

    //todo check accessToken propose
    private var loginInfo = LoginInfo()
    val singInUrlInfo = SignInUrlInfo()
    var oAuthCode = ""
        private set
    var isRememberLogin = false
        private set
    var logoutAndQuit: Boolean = false
        private set
    var country = ""
        private set

    val canUseStandards: Boolean
        get() = country.uppercase() in STANDARDS_ENABLED_COUNTRIES

    // for set re-direct url.
    private var isAuthCodeFromApi = false

    private var _isGuestMode = false
    var isGuestMode: Boolean
        get() = _isGuestMode
        set(value) {
            _isGuestMode = value
        }
    private var _guestOrgInfo = GuestOrganizationInfo()
    var guestOrgInfo: GuestOrganizationInfo
        get() = _guestOrgInfo
        set(value) {
            _guestOrgInfo = value
        }

    private var _selectedOrg: OrganizationInfo? = null
    var selectedOrg: OrganizationInfo?
        get() = _selectedOrg
        set(value) {
            _selectedOrg = value
        }

    private var userOrgInfo = UserOrganizationInfo(null, "")
    var userInfo = UserInfo()
        private set
    var fillUserInfo = FillUserInfo()
        private set
    private var articleInfo = ArticleResponse.ArticleData()

    // For Amplitude Used
    var organizationList: MutableList<Organization> = mutableListOf()
        private set

    private val socketManager: SocketManager by inject(SocketManager::class.java)

    private val oAuthResponseErrorHandleCode = 422

    init {
        coroutineScope.launch(Dispatchers.IO) {
            loginInfo = LoginInfo(refreshToken = loginDataStore.getRefreshToken())
            isRememberLogin = loginDataStore.isRememberLoginInfo()
        }
    }

    fun setLoginMethod(loginMethod: AmplitudeConstant.LoginMethod) {
        this.loginMethod = loginMethod
    }

    private fun collectNetworkStatus() {
        detectNetworkStateJob?.cancel()
        detectNetworkStateJob = coroutineScope.launch(Dispatchers.IO) {
            networkManager.networkAvailabilityState.collect { hasNetwork ->
                dialogWindow?.setIsNetworkDisconnect(!hasNetwork)
            }
        }
    }

    suspend fun getChallengeCode(): Boolean = withContext(Dispatchers.IO) {
        when (val response = apiService.getChallengeCode()) {
            is ApiResponse.Success -> {
                singInUrlInfo.setChallengeCodeInfo(response.data)
                Timber.d("login api challenge code: ${response.data.codeChallenge}")
                return@withContext true
            }
            else -> {
                return@withContext false
            }
        }
    }

    suspend fun loginWithMvbToken(): Boolean = withContext(Dispatchers.IO) {
        val mvbToken = MyViewBoardLaunchState.mvbToken.value
        Timber.tag("loginFlow").d("mvbToken：$mvbToken")
        if (mvbToken.isBlank()) {
            // mvbToken 空的，嘗試走 Guest mode
            val country: String
            if (getRegion()) {
                Timber.tag("loginFlow").d("singInUrlInfo.countryCode：${singInUrlInfo.countryCode}")
                country = singInUrlInfo.countryCode
            } else {
                country = "TW"
            }
            Timber.tag("loginFlow").d("Guest login country：$country")
            when (val response = apiService.guestLogin(
                apiKey = BuildConfig.GUEST_MODE_LOGIN_API_KEY,
                body = GuestLoginPostBody(country = country)
            )) {
                is ApiResponse.Success -> {
                    loginInfo = LoginInfo(response.data.data.accessToken)
                    guestOrgInfo = response.data.data.organization.toGuestOrgInfo()
                    selectedOrg = response.data.data.organization.toOrganizationInfo()
                    userInfo = response.data.data.toUserInfo()
                    isGuestMode = true
                    Timber.tag("loginFlow").d("guest mode：$isGuestMode")
                    return@withContext true
                }

                else -> {
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                        .appendEventProperty(
                            AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE,
                            AmplitudeConstant.EventProperties.Value.GET_REFRESH_TOKEN_FAILED
                        )
                        .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                        .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                        .send()
                    clearLoginInfo()
                    return@withContext false
                }
            }
        }
        val response = apiService.loginWithMvbToken(LoginWithMvbTokenPostBody(token = MyViewBoardLaunchState.mvbToken.value))
        when (response) {
            is ApiResponse.Success -> {
                loginInfo = LoginInfo(
                    response.data.loginData.accessToken,
                    response.data.loginData.idToken,
                    response.data.loginData.refreshToken
                )
                Timber.tag("loginFlow").d("save refresh token：${loginInfo.refreshToken}")
                loginDataStore.setRefreshToken(loginInfo.refreshToken)
                country = response.data.loginData.country
                fillUserInfo = response.data.loginData.toFillUserInfo().withMyViewBoardConsentPromptsSuppressed()
                return@withContext true
            }
            else -> {
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                    .appendEventProperty(AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE, AmplitudeConstant.EventProperties.Value.GET_REFRESH_TOKEN_FAILED)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                    .send()
                clearLoginInfo()
                return@withContext false
            }
        }
    }

    suspend fun login(): Boolean = withContext(Dispatchers.IO) {
        Timber.tag("loginFlow").d("refreshToken：${loginInfo.refreshToken}")
        val response =
            if (isRememberLogin) apiService.login(
                LoginWithRefreshTokenPostBody(
                    BuildConfig.LOGIN_REDIRECT_URL,
                    loginInfo.refreshToken
                )
            )
            else apiService.login(
                LoginPostNoTokenBody(
                    oAuthCode,
                    singInUrlInfo.codeChallenge ,
                    if (isAuthCodeFromApi) BuildConfig.QR_CODE_REDIRECT_URL else BuildConfig.LOGIN_REDIRECT_URL
                )
            )
        when (response) {
            is ApiResponse.Success -> {
                loginInfo = LoginInfo(
                    response.data.loginData.accessToken,
                    response.data.loginData.idToken,
                    response.data.loginData.refreshToken
                )
                Timber.tag("loginFlow").d("save refresh token：${loginInfo.refreshToken}")
                loginDataStore.setRefreshToken(loginInfo.refreshToken)
                country = response.data.loginData.country
                fillUserInfo = response.data.loginData.toFillUserInfo()
                return@withContext true
            }
            else -> {
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                    .appendEventProperty(AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE, AmplitudeConstant.EventProperties.Value.GET_REFRESH_TOKEN_FAILED)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                    .send()
                clearLoginInfo()
                return@withContext false
            }
        }
    }

    suspend fun getAccountInfo(): Boolean = withContext(Dispatchers.IO) {
        Timber.d("Bearer token: ${getBearerToken()}")
        when (val response = apiService.getAccountInfo(getBearerToken())) {
            is ApiResponse.Success -> {
                organizationList = response.data.accountInfoData.organizations.toMutableList()
                userOrgInfo = response.data.toUserOrgInfo()
                userInfo = response.data.toUserInfo()
                firebaseManager.setUserInfo(userInfo.userId, userInfo.email)
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                    .send()
                tutorialManager.setUserId(userInfo.userId)
                return@withContext true
            }
            else -> {
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_FAILED)
                    .appendEventProperty(AmplitudeConstant.EventProperties.Key.FAILED_MESSAGE, AmplitudeConstant.EventProperties.Value.GET_USER_INFO_FAILED)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.LOGIN_DATA)
                    .appendUserProperty(AmplitudeFactory.UserPropertyType.USER_DATA)
                    .send()
                return@withContext false
            }
        }
    }

    fun checkMultipleLogin() {
        Timber.d("checkMultipleLogin")
        checkMultipleLoginJob?.cancel()
        collectNetworkStatus()
        checkMultipleLoginJob = coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                Timber.d("getTeacherState BearerToken: ${getBearerToken()}")
                when (val response = apiService.getTeacherState(getBearerToken(), selectedOrg?.orgId ?: "", userInfo.userId)) {
                    is ApiResponse.Success -> {
                        Timber.d("getTeacherState -> isMultiLogin: ${response.data.teacherStateData.isMultiLogin}")
                        if (response.data.teacherStateData.isMultiLogin) {
                            withContext(Dispatchers.Main) {
                                val context = applicationContext.localizedContext()
                                dialogWindow = CSSystemDialogWindow.Builder(context, WindowTag.CS_SYSTEM_DIALOG)
                                    .setTitle(context.getString(R.string.dialog_sign_out_notification_title))
                                    .setMessage(context.getString(R.string.dialog_sign_out_notification_message))
                                    .setPositiveButton(
                                        context.getString(R.string.common_confirm),
                                        context.getColor(R.color.neutral_900)
                                    ) {
                                        if (dialogWindow?.isNetworkDisconnect() == true || myViewBoardConnectionStateProvider.isBound()) {
                                            quitApp()
                                        } else {
                                            logout()
                                        }
                                    }
                                    .build()
                                dialogWindow?.show()
                            }
                            checkMultipleLoginJob?.cancel()
                        }
                    }
                    else -> {}
                }
                delay(FIVE_SEC_DELAY)
            }
        }
    }

    fun stopMultipleLoginCheck() {
        Timber.d("stopMultipleLoginCheck")
        checkMultipleLoginJob?.cancel()
        detectNetworkStateJob?.cancel()
    }

    fun logout() {
        coroutineScope.launch {
            firebaseManager.setUserInfo("", "")
            tutorialManager.clearUserData()
            maintenanceAnnouncementsUiManager.stopCheckCronJob()
        }
        disconnectSocket()
        stopMultipleLoginCheck()
        stopCSService()
        openLogoutWeb()
    }

    // checkRememberLogin is false, force quit app.
    fun quitApp(checkRememberLogin: Boolean = true) {
        disconnectSocket()
        stopMultipleLoginCheck()
        //stop service first, to avoid activity won't be actived
        stopCSService()
        // not remember login info, should logout first then quit
        logoutAndQuit = !isRememberLogin
        if (!myViewBoardConnectionStateProvider.isBound() && !isRememberLogin && checkRememberLogin) {
            openLogoutWeb()
            // do quit app in activity, so return
            return
        }
        activityManager.appTasks.forEach { task ->
            task.finishAndRemoveTask()
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    fun disconnectSocket() {
        //check socket connect status first
        if (socketManager.isConnected()) {
            //this event is for hub kick online user, not related to Oauth login mechanism.
            socketManager.emit(
                SocketManager.EmittedEvent.TEACHER_LOGOUT,
                LogoutSocketMessage(
                    userID = userOrgInfo.userId,
                    orgID = selectedOrg?.orgId ?: ""
                ).toJSONObject()
            )
            socketManager.disconnect()
        }
        socketManager.clearToken()
    }

    private fun openLogoutWeb() {
        val intent = Intent(applicationContext, LoginActivity::class.java)
        intent.putExtra("is_logout", true)
        intent.putExtra("logout_id_token", loginInfo.idToken)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(intent)
        coroutineScope.launch { clearLoginInfo() }
    }

    private fun stopCSService() {
        if (ClassSwiftService.isServiceStarted()) {
            val serviceIntent = ClassSwiftService.getStartIntent()
            applicationContext.stopService(serviceIntent)
        }
    }

    suspend fun sendUserInfo(body: UserInfoBody): Boolean = withContext(Dispatchers.IO) {
        Timber.d("Bearer token: ${getBearerToken()}")
        when (apiService.sendAccountInfo(getBearerToken(), body)) {
            is ApiResponse.Success -> {
                return@withContext true
            }
            else -> {
                return@withContext false
            }
        }
    }

    suspend fun getArticleID(): Boolean = withContext(Dispatchers.IO) {
        when (val apiResponse = apiService.getArticleID()) {
            is ApiResponse.Success -> {
                articleInfo = apiResponse.data.articleData
                Timber.tag("Account_Page").d("set articleInfo: $articleInfo")
                return@withContext true
            }
            else -> {
                return@withContext false
            }
        }
    }

    private suspend fun clearLoginInfo() {
        isRememberLogin = false
        oAuthCode = ""
        isAuthCodeFromApi = false
        setRememberLogin(false)
        loginInfo.clear()
        loginDataStore.setRefreshToken(loginInfo.refreshToken)
    }

    // Clear in-memory session synchronously so a follow-up login flow sees no
    // leftover account data even before the datastore write completes.
    fun clearSessionForMvbTakeover() {
        loginInfo.clear()
        _selectedOrg = null
        userInfo = UserInfo()
        fillUserInfo = FillUserInfo()
        userOrgInfo = UserOrganizationInfo(null, "")
        oAuthCode = ""
        isAuthCodeFromApi = false
        isRememberLogin = false
        coroutineScope.launch(Dispatchers.IO) {
            loginDataStore.setRememberLoginInfo(false)
            loginDataStore.setRefreshToken("")
        }
    }

    fun setOAuthsCode(code: String) {
        isAuthCodeFromApi = false
        oAuthCode = code
    }

    fun getAccessToken(): String {
        return loginInfo.accessToken
    }


    fun getBearerToken(): String {
        return "Bearer ${loginInfo.accessToken}"
    }

    fun getUserOrganizationInfo(): UserOrganizationInfo {
        return userOrgInfo
    }

    suspend fun setRememberLogin(flag: Boolean) {
        isRememberLogin = flag
        loginDataStore.setRememberLoginInfo(flag)
    }

    fun getArticleInfo(): ArticleResponse.ArticleData {
        return articleInfo
    }

    suspend fun getRegion(): Boolean = withContext(Dispatchers.IO) {
        val parseRegionKey = "cloudfront-viewer-country"
        try {
            val request = Request.Builder().url(BuildConfig.ARTICLE_URL).build()
            okHttpClient.newCall(request).execute().use { response ->
                Timber.tag("region").d("Region response: $response")
                response.header(parseRegionKey)?.let {
                    it.isBlank()
                    singInUrlInfo.countryCode = it.uppercase()
                    response.header(parseRegionKey)?.uppercase().toString()
                } ?: run {
                    // no region info, return false
                    Timber.tag("region").d("getRegion return false")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            // network error, return false
            Timber.tag("region").d("Exception: ${e.printStackTrace()}")
            return@withContext false
        }
        return@withContext true
    }

    suspend fun getSsoSignInUrlInfo(): Boolean = withContext(Dispatchers.IO) {
        when (val response = apiService.getSsoSignInUrls(
            ssoTypes = singInUrlInfo.getSsoList(),
            codeChallenge = singInUrlInfo.codeChallenge
        )) {
            is ApiResponse.Success -> {
                singInUrlInfo.setUrlList(response.data)
                Timber.tag("signInFlow").d("$singInUrlInfo")
                return@withContext true
            }
            else -> {
                return@withContext false
            }
        }
    }

    suspend fun getQrCodeSignInUrl(): Boolean = withContext(Dispatchers.IO) {
        when (val response = apiService.getQrCodeSignInUrl(
            codeChallenge = singInUrlInfo.codeChallenge,
            state = "codeChallenge=${singInUrlInfo.codeChallenge}"
        )) {
            is ApiResponse.Success -> {
                singInUrlInfo.setQrCodeSignInUrl(response.data)
                Timber.tag("signInFlow").d("$singInUrlInfo")
                return@withContext true
            }
            else -> {
                return@withContext false
            }
        }
    }

    suspend fun getOAuthsCode(): GetOAuthCodeStatus = withContext(Dispatchers.IO) {
        when (val response = apiService.getOAuthCode(singInUrlInfo.codeChallenge)) {
            is ApiResponse.Rfc7807Failure -> {
                if (response.responseCode == oAuthResponseErrorHandleCode) {
                    return@withContext GetOAuthCodeStatus.FAILED
                }
                return@withContext GetOAuthCodeStatus.PULLING
            }
            is ApiResponse.NetworkDisconnected -> {
                return@withContext GetOAuthCodeStatus.PULLING
            }
            is ApiResponse.Success -> {
                val code = response.data
                if (code.oAuthCode.isEmpty()) {
                   return@withContext GetOAuthCodeStatus.PULLING
                }
                oAuthCode = code.oAuthCode
                isAuthCodeFromApi = true
                return@withContext GetOAuthCodeStatus.SUCCESS
            }
            else -> {
                return@withContext GetOAuthCodeStatus.PULLING
            }
        }
    }

    enum class GetOAuthCodeStatus {
        SUCCESS, FAILED, PULLING
    }

    companion object {
        private val STANDARDS_ENABLED_COUNTRIES = setOf("US", "MY")
    }
}