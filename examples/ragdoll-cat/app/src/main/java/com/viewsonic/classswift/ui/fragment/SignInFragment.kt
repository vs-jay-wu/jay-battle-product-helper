package com.viewsonic.classswift.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
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
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.info.SignInUrlInfo
import com.viewsonic.classswift.databinding.FragmentSigninBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel.GetSignInUrlEvent.Complete
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel.GetSignInUrlEvent.GetSignUrlError
import com.viewsonic.classswift.ui.widget.OnSingInLoadingClickListener
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.utils.QRCodeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class SignInFragment : Fragment() {
    private val DEVICE_ID_COPY_TRIGGER_CLICK_TIME = 5
    private lateinit var binding: FragmentSigninBinding
    private val accountUrl = BuildConfig.ACCOUNT_URL
    private val clipboardManager: ClipboardManager by inject(ClipboardManager::class.java)
    private val loginViewModel: LoginViewModel by viewModel(ownerProducer = { requireActivity() })
    private var openURL = ""
    private val qrCodeSize = 1080
    private var singInInfo: SignInUrlInfo? = null
    private var classSwiftIconClickedCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Timber.d("isLogOut value is ${loginViewModel.isLogOutAndQuit()}")
        if (loginViewModel.isLogOutAndQuit()) {
            loginViewModel.quitApp()
        }
        binding = FragmentSigninBinding.inflate(inflater, container, false)
        initClickAction()
        loginViewModel.getSignInUrlList()
        loginViewModel.startDateMonitor()
        initCollection()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loginViewModel.cancelGetOAuthCode()
        loginViewModel.cancelDateMonitor()
    }

    private fun initCollection() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.getSignInUrlFlow.collect {
                    when (it) {
                        is GetSignUrlError -> {
                            showErrorUi()
                        }

                        is Complete -> {
                            binding.cstToast.isVisible = false
                            singInInfo = it.info
                            showSignInUi()
                            loginViewModel.getOAuthCode()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.loginFlowState.collect {
                    if (it.state == LoginViewModel.LoginState.GET_AUTHS_CODE) {
                        Timber.tag("SignIn").d("collect LoginViewModel.LoginState.GET_AUTHS_CODE")
                        loginViewModel.cancelGetOAuthCode()
                        loginViewModel.setLoginState(LoginViewModel.LoginState.LOGIN)
                        navToLoginPage()
                    }
                }
            }
        }
    }

    private fun initClickAction() {
        binding.apply {
            ivNormalIcon.setOnClickListener {
                classSwiftIconClickedCount++
                if (classSwiftIconClickedCount >= DEVICE_ID_COPY_TRIGGER_CLICK_TIME) {
                    val clip = ClipData.newPlainText("FID", loginViewModel.getInstallationId())
                    clipboardManager.setPrimaryClip(clip)
                    classSwiftIconClickedCount = 0
                }
            }

            ivClose.setOnClickListener {
                loginViewModel.quitApp()
            }
            silbRetry.setOnClickListener(object : OnSingInLoadingClickListener {
                override fun onClicked() {
                    silbRetry.setLoadingStatus(true)
                    loginViewModel.getSignInUrlList()
                }
            })
            silbLoginWithViewsonic.setOnClickListener(object : OnSingInLoadingClickListener {
                override fun onClicked() {
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_METHOD_SELECTED)
                        .appendUserProperty(
                            AmplitudeConstant.UserProperties.Key.LOGIN_METHOD,
                            AmplitudeConstant.UserProperties.Value.VIEW_SONIC
                        )
                        .send()
                    loginViewModel.setLoginMethod(AmplitudeConstant.LoginMethod.ViewSonic)
                    openURL = singInInfo?.viewSonicSignInUrl.toString()
                    Timber.tag("SignIn").d("viewSonic login url is $openURL")
                    openURLByCustomTabs()
                }
            })
            silbLoginWithClasslink.setOnClickListener(object : OnSingInLoadingClickListener {
                override fun onClicked() {
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_METHOD_SELECTED)
                        .appendUserProperty(
                            AmplitudeConstant.UserProperties.Key.LOGIN_METHOD,
                            AmplitudeConstant.UserProperties.Value.CLASS_LINK
                        )
                        .send()
                    loginViewModel.setLoginMethod(AmplitudeConstant.LoginMethod.ClassLink)
                    openURL = singInInfo?.classLinkSignInUrl.toString()
                    Timber.tag("SignIn").d("classLink login url is $openURL")
                    openURLByCustomTabs()
                }
            })
            llGoogle.setOnClickListener {
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_METHOD_SELECTED)
                    .appendUserProperty(
                        AmplitudeConstant.UserProperties.Key.LOGIN_METHOD,
                        AmplitudeConstant.UserProperties.Value.GOOGLE
                    )
                    .send()
                loginViewModel.setLoginMethod(AmplitudeConstant.LoginMethod.Google)
                openURL = singInInfo?.googleSignInUrl.toString()
                Timber.tag("SignIn").d("google login url is $openURL")
                openURLByCustomTabs()
            }
            llMircosoft.setOnClickListener {
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LOGIN_METHOD_SELECTED)
                    .appendUserProperty(
                        AmplitudeConstant.UserProperties.Key.LOGIN_METHOD,
                        AmplitudeConstant.UserProperties.Value.MICROSOFT
                    )
                    .send()
                loginViewModel.setLoginMethod(AmplitudeConstant.LoginMethod.Microsoft)
                openURL = singInInfo?.mircoSoftSignInUrl.toString()
                Timber.tag("SignIn").d("mircoSoft login url is $openURL")
                openURLByCustomTabs()
            }
            tvCreateAccount.setOnClickListener {
                openURL =
                    "$accountUrl/auth/v1/signup?client_id=${BuildConfig.SIGN_IN_OAUTH_CLIENT_ID}&scope=openid+profile+email+address+offline_access&response_type=code" +
                            "&code_challenge=${singInInfo?.codeChallenge}&code_challenge_method=${singInInfo?.codeChallengeMethod}" +
                            "&redirect_uri=${BuildConfig.LOGIN_REDIRECT_URL}&prompt=consent&language=${LanguageUtils.vsAccountLanguageCode}"
                Timber.d("register url is $openURL")
                openURLByCustomTabs()
            }
        }
    }

    private fun showErrorUi() {
        binding.apply {
            if (clError.isVisible) {
                silbRetry.setLoadingStatus(false)
            } else {
                clLoading.isVisible = false
                clError.isVisible = true
                clNormal.isVisible = false
                clBottom.isVisible = false
            }
            viewLifecycleOwner.lifecycleScope.launch {
                cstToast.show()
            }
        }
    }

    private fun showSignInUi() {
        val qrCodeUrl = singInInfo?.qrSignInUrl ?: ""
        Timber.tag("SignIn").d("qr code url is $qrCodeUrl")
        binding.apply {
            QRCodeUtils.generateQRCodeWithBackground(
                text = qrCodeUrl,
                qrSize = qrCodeSize,
                bgRadius = 0f.dpToPx()
            )?.let { bitmap ->
                binding.ivQrCode.setImageBitmap(bitmap)
            }
            silbLoginWithClasslink.isVisible = !singInInfo?.classLinkSignInUrl.isNullOrBlank()
            clLoading.isVisible = false
            clError.isVisible = false
            clNormal.isVisible = true
            clBottom.isVisible = true
        }
    }

    private fun openURLByCustomTabs() {
        context?.let {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true) // show title
                .setStartAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // fade in animation
                .setExitAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // fade out animation
            // for verify Incognito mode is work or not
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(it, openURL.toUri())
        }
    }

    private fun navToLoginPage() {
        Timber.tag("SignIn").d("navToLoginPage")
        lifecycleScope.launch(Dispatchers.Main) {
            findNavController().navigate(R.id.action_to_login_fragment)
        }
    }
}