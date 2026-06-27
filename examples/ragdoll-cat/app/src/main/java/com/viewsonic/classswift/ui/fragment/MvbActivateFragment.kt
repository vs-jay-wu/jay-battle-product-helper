package com.viewsonic.classswift.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentMvbActivateBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.AppUpdateManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.utils.LanguageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class MvbActivateFragment : Fragment() {

    private lateinit var binding: FragmentMvbActivateBinding
    private val accountManager: AccountManager by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val networkManager: NetworkManager by inject()
    private val loginViewModel: LoginViewModel by viewModel(ownerProducer = { requireActivity() })
    private val socketManager: SocketManager by inject(SocketManager::class.java)
    private val classroomManager: ClassroomManager by inject ()

    private var logoutIDToken = ""

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val isGranted = Settings.canDrawOverlays(this.activity)
            Timber.tag("loginFlow").d("canDrawOverlays isGranted: $isGranted")
            if (isGranted) {
                loginViewModel.setLoginState(LoginViewModel.LoginState.LOGIN_WITH_MVB_TOKEN)
            } else {
                lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                    }
                })
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMvbActivateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag("loginFlow").d("MvbActivateFragment -> onViewCreated")
        isLogOut()
        initClickAction()
        binding.apply {
            buttonEnterClass.setDisable()
            lottieLoading.playAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.lottieLoading.cancelAnimation()
    }

    private fun initCollection() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.loginFlowState.collect {
                    Timber.tag("loginFlow").d("initCollection() -> state: ${it.state} ")
                    withContext(Dispatchers.Main) {
                        binding.cstErrorToast.isVisible = it.state == LoginViewModel.LoginState.ERROR
                    }
                    when (it.state) {
                        LoginViewModel.LoginState.SET_LANGUAGE -> {
                            loginViewModel.setAppLanguage(this@MvbActivateFragment.requireContext())
                            loginViewModel.setLoginState(LoginViewModel.LoginState.CHECK_MAINTENANCE_ANNOUNCEMENTS)
                        }
                        LoginViewModel.LoginState.CHECK_MAINTENANCE_ANNOUNCEMENTS -> checkMaintenanceAnnouncements()
                        LoginViewModel.LoginState.OTA -> checkOta()
                        LoginViewModel.LoginState.CHECK_OVERLAY_PERMISSION -> checkOverlayPermission()
                        LoginViewModel.LoginState.LOGIN_WITH_MVB_TOKEN -> loginViewModel.loginWithMvbToken()
                        LoginViewModel.LoginState.LOGIN_WITH_MVB_TOKEN_FAIL -> {
                            lifecycleScope.launch {
                                findNavController().navigate(R.id.action_to_mvb_login_api_error_fragment)
                            }
                        }
                        LoginViewModel.LoginState.GET_ARTICLE_ID -> loginViewModel.getArticleID()
                        LoginViewModel.LoginState.GET_ACCOUNT_INFO -> loginViewModel.getAccountInfo()
                        LoginViewModel.LoginState.CHECK_FILL_USER_INFO -> checkNeedFillAccount()
                        LoginViewModel.LoginState.GUEST_GET_CLASSROOM_LIST -> getGuestClassroomList()
                        LoginViewModel.LoginState.GUEST_GET_CLASSROOM_LIST_FAIL -> {
                            lifecycleScope.launch {
                                findNavController().navigate(R.id.action_to_mvb_guest_login_api_error_fragment)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun initClickAction() {
        with(binding) {
            ibClose.setOnClickListener {
                loginViewModel.quitApp()
            }
            buttonExit.setOnClickListener {
                loginViewModel.quitApp()
            }
        }
    }

    private fun isLogOut() {
        val isLogout = loginViewModel.isLogout
        logoutIDToken = loginViewModel.logoutIDToken
        if (isLogout) {
            openCustomTabForLogout()
        } else {
            initCollection()
        }
    }

    private fun checkMaintenanceAnnouncements() {
        lifecycleScope.launch {
            val result = loginViewModel.startMaintenanceAnnouncementsCheck()
            when (result) {
                LoginViewModel.MaintenanceFetchResult.SUCCESS -> {
                    loginViewModel.setLoginState(LoginViewModel.LoginState.OTA)
                }
                LoginViewModel.MaintenanceFetchResult.UNDER_MAINTENANCE -> {
                    findNavController().navigate(R.id.action_to_mvb_under_maintenance_fragment)
                }
                LoginViewModel.MaintenanceFetchResult.NETWORK_ERROR -> {
                    findNavController().navigate(R.id.action_to_mvb_no_network_fragment)
                }
                LoginViewModel.MaintenanceFetchResult.FAILED -> {
                    findNavController().navigate(R.id.action_to_mvb_maintenance_api_error_fragment)
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this.context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${this@MvbActivateFragment.activity?.packageName}".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            loginViewModel.setLoginState(LoginViewModel.LoginState.LOGIN_WITH_MVB_TOKEN)
        }
    }

    private fun checkOta() {
        //check network status
        if (networkManager.isNetworkAvailable()) {
            lifecycleScope.launch {
                val appAvailabilityResult = appUpdateManager.checkAppAvailability()
                Timber.tag("loginFlow").d("checkConditionFlow -> checkAppAvailability done")
                Timber.tag("loginFlow").d("appAvailabilityResult -> $appAvailabilityResult")
                appUpdateManager.setIsCheck()
                when (appAvailabilityResult) {
                    AppUpdateManager.AppAvailabilityResult.Available -> {
                        loginViewModel.setLoginState(LoginViewModel.LoginState.CHECK_OVERLAY_PERMISSION)
                    }

                    is AppUpdateManager.AppAvailabilityResult.NeedToUpdate -> {
                        loginViewModel.setServerLatestAppVersionName(appAvailabilityResult.latestReleaseVersionName)
                        findNavController().navigate(R.id.action_to_mvb_download_apk_fragment)
                    }

                    AppUpdateManager.AppAvailabilityResult.NotAllowedDevice -> {
                        // removed
                    }
                    AppUpdateManager.AppAvailabilityResult.NetworkError -> {
                        findNavController().navigate(R.id.action_to_mvb_ota_api_error_fragment)
                    }
                }
            }
        } else {
            findNavController().navigate(R.id.action_to_mvb_no_network_fragment)
        }
    }

    private fun checkNeedFillAccount() {
        if (accountManager.fillUserInfo.needFillAccountPage()) {
            findNavController().navigate(R.id.action_fill_user_info_fragment)
        } else {
            loginViewModel.showSelectOrgWindow()
        }
    }

    private fun openCustomTabForLogout() {
        val logoutURL =
            "${BuildConfig.ACCOUNT_URL}/auth/v1/oidc/session/end?id_token_hint=$logoutIDToken" +
                    "&post_logout_redirect_uri=${BuildConfig.LOGOUT_REDIRECT_URL}&language=${LanguageUtils.vsAccountLanguageCode}"
        Timber.tag("Logout").d("logout url: $logoutURL")
        context?.let {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true) //show title area
                .setStartAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // fade in animation
                .setExitAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // fade out animation
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(it, logoutURL.toUri())
        }
    }

    private fun getGuestClassroomList() {
        lifecycleScope.launch {
            socketManager.connect()
            when (val responseData = classroomManager.getGuestClassroomList()) {
                is ClassroomManager.ClassroomResponseData.Success -> {
                    when (val responseData =
                        classroomManager.createLesson(responseData.resultData[0])) {
                        is ClassroomManager.ClassroomResponseData.Success -> {
                            // VSFT-8557: mark lesson as started so the student
                            // join-class webpage shows "Lesson in session" instead
                            // of "Class is about to start" while teacher is on JoinClass.
                            classroomManager.startLesson()
                            loginViewModel.showSelectOrgWindow()
                        }

                        else -> {
                            loginViewModel.setLoginState(LoginViewModel.LoginState.GUEST_GET_CLASSROOM_LIST_FAIL)
                        }
                    }
                }

                else -> {
                    loginViewModel.setLoginState(LoginViewModel.LoginState.GUEST_GET_CLASSROOM_LIST_FAIL)
                }
            }
        }
    }
}
