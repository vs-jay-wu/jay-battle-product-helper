package com.viewsonic.classswift.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.databinding.ActivitySetLanguageBinding
import com.viewsonic.classswift.service.ClassSwiftService
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.viewmodel.SettingLanguageViewModel
import com.viewsonic.classswift.utils.LanguageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.getValue

class SetLanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetLanguageBinding

    private val viewModel: SettingLanguageViewModel by viewModel()

    private var isChangeLanguage = false

    private var isBindingService = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        enableEdgeToEdge()
        isChangeLanguage = intent.getBooleanExtra(ARG_CHANGE_LANGUAGE, false)
        binding = ActivitySetLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // let press back no action
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
        Timber.tag("language").d("SetLanguageActivity -> isChangeLanguage: $isChangeLanguage")
        if (isChangeLanguage) {
            lifecycleScope.launch(Dispatchers.Main) {
                // delay 1 sec for show the ui, only need to switch language
                delay(ONE_SEC_DELAY)
                // need to clear flag, recreate won't get true value
                intent.removeExtra(ARG_CHANGE_LANGUAGE)
                stopCSService()
                setAppLanguage()
            }
        } else {
            startCSService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBindingService) {
            this.unbindService(connection)
        }
    }

    private fun setAppLanguage() {
        lifecycleScope.launch {
            viewModel.setAppLanguage()
            withContext(Dispatchers.Main) {
                this@SetLanguageActivity.recreate()
            }
        }
    }

    // ServiceConnection to manage connection callbacks
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ClassSwiftService.LocalBinder
            lifecycleScope.launch {
                binder.getService().openNextWindow()
                isBindingService = true
                this@SetLanguageActivity.finish()
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
                        this@SetLanguageActivity.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
                    }
                }
            }
        }
    }

    private fun stopCSService() {
        val stopServiceIntent = Intent(this, ClassSwiftService::class.java)
        this.stopService(stopServiceIntent)
    }


    companion object {
        // Arguments for activity
        const val ARG_CHANGE_LANGUAGE = "arg_change_language"
    }

}