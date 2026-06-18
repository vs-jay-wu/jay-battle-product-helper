package com.viewsonic.classswift.ui.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.clientapp.ClientAppInfo
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.databinding.ActivityMvbEntryBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.AppUpdateManager
import com.viewsonic.classswift.service.ClassSwiftService
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.utils.extension.dump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MvbEntryActivity : AppCompatActivity() {
    private val accountManager: AccountManager by inject()
    private val loginViewModel: LoginViewModel by viewModel()
    private val appUpdateManager: AppUpdateManager by inject()
    private val myViewBoardEventNotifier: MyViewBoardEventNotifier by inject()
    private lateinit var binding: ActivityMvbEntryBinding

    private var isBindingService = false
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        // 結束 StandAlone 版本
        tearDownStandaloneSession()

        enableEdgeToEdge()
        loginViewModel.isLogout = intent.getBooleanExtra("is_logout", false)
        loginViewModel.logoutIDToken = intent.getStringExtra("logout_id_token") ?: ""
        binding = ActivityMvbEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        collectFlowEvent()
        // 註冊一個 callback，在Activity裡按Back鍵無反應
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
        navController = binding.fcvContainer.getFragment<NavHostFragment>().navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(R.id.mvbActivateFragment)
        navController.graph = navGraph
        handleIntent(intent)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        if (isBindingService) {
            this.unbindService(connection)
        }
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("onNewIntent")
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun tearDownStandaloneSession() {
        LoginActivity.getInstance()
            ?.takeUnless { it.isFinishing }
            ?.finish()
        if (ClassSwiftService.isServiceStarted()) {
            accountManager.clearSessionForMvbTakeover()
        }
    }

    private fun handleIntent(intent: Intent) {
        Timber.d("handleIntent")
        intent.dump()
        if (!loginViewModel.isMyViewBoardBound() && ClassSwiftService.isServiceStarted()) {
            Timber.d("[B][handleIntent] : service already started in default floating mode, close MvbEntryActivity")
            finish()
            return
        }
        when (intent.action) {
            Intent.ACTION_VIEW -> handleDeepLinkIntent(intent.data)
            ClientAppInfo.AppMyViewBoard.launchIntentAction -> Timber.d("[B][handleIntent] : ${ClientAppInfo.AppMyViewBoard.launchIntentAction}")
            else -> handleNonMvbLaunch(intent.action)
        }
    }

    private fun handleDeepLinkIntent(appLinkData: Uri?) {
        Timber.d("[B][handleDeepLinkIntent] : Intent.ACTION_VIEW")
        val code = appLinkData?.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            loginViewModel.isLogout = false
            loginViewModel.logoutIDToken = ""
            navController.navigate(R.id.loginFragment)
            return
        }
        Timber.d("login api auth code: $code")
        accountManager.setOAuthsCode(code)
        lifecycleScope.launch(Dispatchers.IO) {
            accountManager.setRememberLogin(false)
            Timber.tag("loginFlow").d("MvbEntryActivity -> OTA is check: ${appUpdateManager.isChecked}")
            withContext(Dispatchers.Main) {
                if (appUpdateManager.isChecked) loginViewModel.setLoginState(LoginViewModel.LoginState.LOGIN)
                else loginViewModel.setLoginState(LoginViewModel.LoginState.SET_LANGUAGE)
                navController.navigate(R.id.loginFragment)
            }
        }
    }

    private fun handleNonMvbLaunch(action: String?) {
        Timber.d("[B][handleIntent] : $action")
        if (!loginViewModel.isMyViewBoardBound()) {
            return
        }
        Timber.d("[B][handleIntent] : myViewBoard has bound, only allow action ${ClientAppInfo.AppMyViewBoard.launchIntentAction} now")
        finish()
    }

    private fun collectFlowEvent() {
        lifecycleScope.launch(Dispatchers.IO) {
            loginViewModel.activityEventFlow.collect { event ->
               when (event) {
                   is LoginViewModel.LoginModelEvent.QuitApp -> {
                       withContext(Dispatchers.Main) {
                           accountManager.quitApp(false)
                       }
                   }

                   is LoginViewModel.LoginModelEvent.SelectOrg -> {
                       //before show select org window, need to check get overlay permission
                       if (Settings.canDrawOverlays(this@MvbEntryActivity)) {
                           loginViewModel.goSelectOrgWindow = false
                           //start service first.
                           startCSService()
                       } else {
                           loginViewModel.goSelectOrgWindow = true
                           withContext(Dispatchers.Main) {
                               navController.navigate(R.id.askOverlayPermissionFragment)
                           }
                       }
                   }

                   is LoginViewModel.LoginModelEvent.ChangeLanguage -> {
                       val localeList = LocaleListCompat.forLanguageTags(event.language)
                       Timber.tag("language").d("ChangeLanguage localeList: $localeList")
                       AppCompatDelegate.setApplicationLocales(localeList)
                       withContext(Dispatchers.Main) {
                           this@MvbEntryActivity.recreate()
                       }
                   }
               }
            }
        }
    }

    // ServiceConnection to manage connection callbacks
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ClassSwiftService.LocalBinder
            lifecycleScope.launch {
                val classSwiftService = binder.getService()
                if (loginViewModel.isMyViewBoardBound()) {
                    myViewBoardEventNotifier.notifyLoginCompleted()
                }
                classSwiftService.openNextWindow()
                isBindingService = true
                this@MvbEntryActivity.finish()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBindingService = false
        }
    }

    private fun startCSService() {
        val serviceIntent = ClassSwiftService.getStartIntent()
        if (!ClassSwiftService.isServiceStarted()) {
            this.startForegroundService(serviceIntent)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                ClassSwiftService.isServiceStartedFlow.collect { isServiceStarted ->
                    if (isServiceStarted) {
                        this@MvbEntryActivity.bindService(serviceIntent, connection, BIND_AUTO_CREATE)
                    }
                }
            }
        }
    }
}
