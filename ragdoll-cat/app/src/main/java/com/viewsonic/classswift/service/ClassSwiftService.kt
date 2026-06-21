package com.viewsonic.classswift.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.clientapp.ClientAppInfo
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardBinder
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventWindowStateChangedPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.PendingClassEntryWindowManager
import com.viewsonic.classswift.ui.helper.JoinClassWindowOpener
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.ui.window.SelectOrgAndSelectClassWindow
import com.viewsonic.classswift.ui.window.UpcomingMaintenanceWindow
import com.viewsonic.classswift.uimanager.maintenance.MaintenanceAnnouncementsUiManager
import com.viewsonic.classswift.utils.extension.dump
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber

class ClassSwiftService : Service() {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    enum class ForegroundServiceType(val sdkForegroundServiceType: Int) {
        DEFAULT(ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING),
        COMBINE_MEDIA_PROJECTION(ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    companion object {
        const val CHANNEL_ID = "ClassSwiftServiceChannel"
        const val NOTIFICATION_ID = 1
        private var _currentBoundClientSet: MutableSet<ClientAppInfo> = mutableSetOf()
        private val _isServiceStartedFlow = MutableStateFlow(false)
        val isServiceStartedFlow: StateFlow<Boolean> = _isServiceStartedFlow.asStateFlow()
        fun isServiceStarted() = _isServiceStartedFlow.value
        fun isMyViewBoardBound() = _currentBoundClientSet.contains(ClientAppInfo.AppMyViewBoard)

        // VSFT-8437: 反應式 MVB bind 狀態，供 UI（如 JoinClass spinner 按鈕）在 bind/unbind 時即時更新；
        // 每次 _currentBoundClientSet 變動後呼叫 refreshMyViewBoardBoundState() 同步。
        private val _isMyViewBoardBoundFlow = MutableStateFlow(false)
        val isMyViewBoardBoundFlow: StateFlow<Boolean> = _isMyViewBoardBoundFlow.asStateFlow()
        private fun refreshMyViewBoardBoundState() {
            _isMyViewBoardBoundFlow.value = isMyViewBoardBound()
        }

        fun getStartIntent(): Intent {
            val applicationContext: Context = get(Context::class.java)
            // Explicit-component bind for in-app callers (LoginActivity, ScreenshotActivity, etc.).
            // No action set — onBind() treats action == null as a self-bind and returns the LocalBinder.
            return Intent(applicationContext, ClassSwiftService::class.java)
        }
    }

    var mediaProjectionData: Pair<Int, Intent>? = null
    private val binder = LocalBinder()
    private val myViewBoardBinder = MyViewBoardBinder()
    private val maintenanceAnnouncementsUiManager: MaintenanceAnnouncementsUiManager by inject()
    private val accountManager: AccountManager by inject()
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider by inject()
    private val myViewBoardEventNotifier: MyViewBoardEventNotifier by inject()
    private val pendingClassEntryWindowManager: PendingClassEntryWindowManager by inject()
    private val myViewBoardWindowStateListener = object : OnCSWindowChangedListener {
        override fun onCSWindowCountChanged() = Unit

        override fun onCSWindowHiddenCountChange() = Unit

        override fun onCSWindowStateChanged(
            windowTag: WindowTag,
            state: CSWindowManager.WindowState
        ) {
            myViewBoardEventNotifier.notifyWindowStateChanged(
                windowTag,
                when (state) {
                    CSWindowManager.WindowState.VISIBLE -> EventWindowStateChangedPayload.State.VISIBLE
                    CSWindowManager.WindowState.HIDDEN -> EventWindowStateChangedPayload.State.HIDDEN
                    CSWindowManager.WindowState.TEMPORARILY_HIDDEN -> EventWindowStateChangedPayload.State.TEMPORARILY_HIDDEN
                    CSWindowManager.WindowState.MINIMIZED -> EventWindowStateChangedPayload.State.MINIMIZED
                    CSWindowManager.WindowState.CLOSED -> EventWindowStateChangedPayload.State.CLOSED
                }
            )
        }
    }


    override fun onCreate() {
        super.onCreate()
        Timber.d("[onCreate]")
        createNotificationChannel()
        CSWindowManager.reset()
        CSWindowManager.addOnWindowChangedListener(myViewBoardWindowStateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("[onStartCommand] : ")
        intent?.dump()
        startForegroundWithDefault()
        _isServiceStartedFlow.update { true }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("[onDestroy]")
        _isServiceStartedFlow.update { false }
        // VSFT-8429: 服務結束即清除待開視窗，避免殘留到下一個 session 誤觸發
        pendingClassEntryWindowManager.clear()
        _currentBoundClientSet.clear()
        refreshMyViewBoardBoundState()
        myViewBoardBinder.onServiceDestroyed()
        CSWindowManager.removeOnWindowChangedListener(myViewBoardWindowStateListener)
        clearAllWindow()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Binder to expose the service instance to the client
    inner class LocalBinder : Binder() {
        fun getService(): ClassSwiftService = this@ClassSwiftService
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("[B][onBind] : intent = ")
        intent?.dump()
        // Self-bind: explicit component, no action. Return the in-process LocalBinder.
        val bindAction = intent?.action ?: return binder
        // External client: identify by action.
        val clientAppInfo = ClientAppInfo.findClientByBindAction(bindAction)
        Timber.d("[B][onBind] : bindAction = $bindAction")
        Timber.d("[B][onBind] : clientAppInfo = $clientAppInfo")
        if (clientAppInfo != ClientAppInfo.NotAllowed) {
            _currentBoundClientSet.add(clientAppInfo)
            refreshMyViewBoardBoundState()
        }
        return when (clientAppInfo) {
            ClientAppInfo.AppMyViewBoard -> {
                if (isServiceStarted()) {
                    clearAllWindow()
                }
                myViewBoardBinder
            }
            ClientAppInfo.NotAllowed -> null
        } as IBinder?
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("[B][onUnbind]: intent = ")
        intent?.dump()
        val bindAction = intent?.action ?: return super.onUnbind(intent)
        val clientAppInfo = ClientAppInfo.findClientByBindAction(bindAction)
        Timber.d("[B][onUnbind] : bindAction = $bindAction")
        Timber.d("[B][onUnbind] : clientAppInfo = $clientAppInfo")
        if (clientAppInfo != ClientAppInfo.NotAllowed) {
            _currentBoundClientSet.remove(clientAppInfo)
            refreshMyViewBoardBoundState()
        }
        when (clientAppInfo) {
            ClientAppInfo.AppMyViewBoard -> {
                myViewBoardBinder.onClientUnbound()
                accountManager.quitApp(false)
            }
            else -> {}
        }
        return super.onUnbind(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ClassSwift Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClassSwift Service")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    suspend fun openNextWindow() {
        maintenanceAnnouncementsUiManager.checkIsInFiveMinutesMaintenancePhase()
        if (maintenanceAnnouncementsUiManager.isInMaintenancePhase(MaintenanceAnnouncementsUiManager.MaintenancePhase.TWO_DAYS_BEFORE)) {
            CSWindowManager.createWindow(
                get(UpcomingMaintenanceWindow::class.java),
                Gravity.CENTER
            )
        } else {
            // Standalone (not-bound) path removed — ragdoll runs MVB-bound only. Auto-select org,
            // then go to SelectOrgAndSelectClassWindow (guest → JoinClass). (CLSWAN-1256)
            if (accountManager.selectedOrg == null) {
                val orgs = accountManager.getUserOrganizationInfo().organizations
                accountManager.selectedOrg = orgs?.firstOrNull { it.notExpiredOrg } ?: orgs?.firstOrNull()
            }
            if (accountManager.isGuestMode) {
                JoinClassWindowOpener.open(get(JoinClassWindow::class.java))
            } else {
                CSWindowManager.createWindow(
                    get(SelectOrgAndSelectClassWindow::class.java),
                    Gravity.CENTER
                )
            }
        }
    }

    private fun clearAllWindow() {
        CSWindowManager.reset()
    }

    fun startMediaProjectionForegroundServiceCompat() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ForegroundServiceType.COMBINE_MEDIA_PROJECTION.sdkForegroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun startForegroundWithDefault() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ForegroundServiceType.DEFAULT.sdkForegroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

}
