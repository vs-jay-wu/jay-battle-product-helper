package com.viewsonic.classswift

import android.app.Application
import android.content.Intent
import android.webkit.WebView
import android.content.ComponentName
import com.viewsonic.classswift.data.clientapp.ClientAppInfo
import com.viewsonic.classswift.di.KoinModules
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.FirebaseManager
import com.viewsonic.classswift.manager.FirebaseRemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class ClassSwiftApplication : Application() {
    val ACTION_APP_STARTED = "com.viewsonic.classswift.service.ACTION_APP_STARTED"
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override fun onCreate() {
        super.onCreate()
        Timber.d("[onCreate] : Version = ${BuildConfig.VERSION_NAME}")
        // Initialize Koin before using any injected dependencies
        KoinModules.initKoin(this@ClassSwiftApplication)
        initTimber()
        initFirebase()
        setWebViewDebugMode()
        val appStartedIntent = Intent(ACTION_APP_STARTED).apply {
            component = ComponentName(
                ClientAppInfo.AppMyViewBoard.packageName,
                "com.viewsonic.droid.ClassSwiftReceiver"
            )
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        sendBroadcast(appStartedIntent)
    }

    private fun initFirebase() {
        // Safe to inject now that Koin is initialized
        val firebaseManager = inject<FirebaseManager>(FirebaseManager::class.java).value
        val remoteConfigManager = inject<FirebaseRemoteConfigManager>(FirebaseRemoteConfigManager::class.java).value
        coroutineScope.launch(Dispatchers.IO) {
            firebaseManager.initialize()
            remoteConfigManager.initialize()
        }
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant()
        }
    }

    private fun setWebViewDebugMode() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
