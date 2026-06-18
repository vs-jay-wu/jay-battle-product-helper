package com.viewsonic.classswift.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentLoginBinding
import com.viewsonic.classswift.databinding.WindowSystemDialogBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.AppUpdateManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.utils.LanguageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val accountManager: AccountManager by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val networkManager: NetworkManager by inject()
    private val loginViewModel: LoginViewModel by viewModel(ownerProducer = { requireActivity() })

    private var logoutIDToken = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag("loginFlow").d("LoginFragment -> onViewCreated")
        isLogOut()
        initClickAction()
    }

    private fun initCollection() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.loginFlowState.collect {
                    Timber.tag("loginFlow").d("initCollection() -> state: ${it.state} ")
                    withContext(Dispatchers.Main) {
                        binding.cstToast.isVisible = it.state == LoginViewModel.LoginState.ERROR
                    }
                    when (it.state) {
                        LoginViewModel.LoginState.SET_LANGUAGE -> {
                            loginViewModel.setAppLanguage(this@LoginFragment.requireContext())
                            loginViewModel.setLoginState(LoginViewModel.LoginState.CHECK_REGION)
                        }
                        LoginViewModel.LoginState.CHECK_REGION -> checkRegion()
                        LoginViewModel.LoginState.CHECK_OVERLAY_PERMISSION -> checkOverlayPermission()
                        LoginViewModel.LoginState.CHECK_MAINTENANCE_ANNOUNCEMENTS -> checkMaintenanceAnnouncements()
                        LoginViewModel.LoginState.OTA -> checkOta()
                        LoginViewModel.LoginState.GET_CHALLENGE_CODE -> loginViewModel.getChallengeCode()
                        LoginViewModel.LoginState.TO_SIGN_IN_PAGE -> findNavController().navigate(R.id.action_to_signIn_fragment)
                        LoginViewModel.LoginState.LOGIN -> loginViewModel.login()
                        LoginViewModel.LoginState.GET_ARTICLE_ID -> loginViewModel.getArticleID()
                        LoginViewModel.LoginState.GET_ACCOUNT_INFO -> loginViewModel.getAccountInfo()
                        LoginViewModel.LoginState.CHECK_FILL_USER_INFO -> checkNeedFillAccount()
                        LoginViewModel.LoginState.SHOW_REMEMBER_DIALOG -> {
                                binding.root.isVisible = false
                                showRememberLoginDialog()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun initClickAction() {
        binding.ivClose.setOnClickListener {
            loginViewModel.quitApp()
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

    private fun checkRegion() {
        lifecycleScope.launch(Dispatchers.Main) {
            findNavController().navigate(R.id.action_to_regionDetect_fragment)
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
                    findNavController().navigate(R.id.action_to_under_maintenance_fragment)
                }
                LoginViewModel.MaintenanceFetchResult.NETWORK_ERROR -> {
                    findNavController().navigate(R.id.action_to_no_network_fragment)
                }
                LoginViewModel.MaintenanceFetchResult.FAILED -> {
                    findNavController().navigate(R.id.action_to_maintenance_api_error_fragment)
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this.context)) {
            findNavController().navigate(R.id.action_ask_overlay_permission_fragment)
        } else {
            loginViewModel.setLoginState(LoginViewModel.LoginState.CHECK_MAINTENANCE_ANNOUNCEMENTS)
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
                        // has oAuthCode, don't need call challenge code api.
                        if (loginViewModel.hasOAuthCode()) loginViewModel.setLoginState(
                            LoginViewModel.LoginState.LOGIN
                        )
                        else loginViewModel.setLoginState(LoginViewModel.LoginState.GET_CHALLENGE_CODE)
                    }

                    is AppUpdateManager.AppAvailabilityResult.NeedToUpdate -> {
                        loginViewModel.setServerLatestAppVersionName(appAvailabilityResult.latestReleaseVersionName)
                        findNavController().navigate(R.id.action_to_update_available_fragment)
                    }

                    AppUpdateManager.AppAvailabilityResult.NotAllowedDevice -> {
                        findNavController().navigate(R.id.action_to_access_restricted_fragment)
                    }
                    AppUpdateManager.AppAvailabilityResult.NetworkError -> {
                        findNavController().navigate(R.id.action_to_otp_api_error_fragment)
                    }
                }
            }
        } else {
            findNavController().navigate(R.id.action_to_no_network_fragment)
        }
    }

    private fun showRememberLoginDialog() {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialog)
        val dialogBinding = WindowSystemDialogBinding.inflate(layoutInflater)
        dialogBinding.tvTitle.text = requireContext().getString(R.string.dialog_save_login_title)
        dialogBinding.tvMessage.text = requireContext().getString(R.string.dialog_save_login_message)
        dialogBinding.btPositive.text = requireContext().getString(R.string.common_save)
        dialogBinding.btPositive.setTextColor(requireContext().getColor(R.color.brand_blue))
        dialogBinding.btPositive.setOnClickListener {
            dialog.dismiss()
            lifecycleScope.launch(Dispatchers.IO) {
                accountManager.setRememberLogin(true)
                withContext(Dispatchers.Main) {
                    checkNeedFillAccount()
                }
            }
        }
        dialogBinding.btNegative.text = requireContext().getString(R.string.common_cancel)
        dialogBinding.btNegative.setTextColor(requireContext().getColor(R.color.cs_system_dialog_text_color))
        dialogBinding.btNegative.setOnClickListener {
            dialog.dismiss()
            checkNeedFillAccount()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
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
}
