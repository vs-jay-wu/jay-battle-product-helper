package com.viewsonic.classswift.data.clientapp.myviewboard

import android.content.pm.PackageManager
import com.viewsonic.classswift.service.IMyViewBoardBinder
import com.viewsonic.classswift.service.IMyViewBoardCallback
import com.viewsonic.classswift.data.clientapp.ClientAppInfo
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageHandler
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.data.clientapp.myviewboard.session.MyViewBoardSessionStore
import com.viewsonic.classswift.data.clientapp.myviewboard.transport.MyViewBoardCallbackEmitter
import com.viewsonic.classswift.manager.CoroutineManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class MyViewBoardBinder : IMyViewBoardBinder.Stub() {
    private val packageManager: PackageManager by inject(PackageManager::class.java)
    private val callbackEmitter: MyViewBoardCallbackEmitter by inject(MyViewBoardCallbackEmitter::class.java)
    private val eventNotifier: MyViewBoardEventNotifier by inject(MyViewBoardEventNotifier::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private val messageParser = MyViewBoardMessageParser()
    private val messageHandler = MyViewBoardMessageHandler()
    // FIFO channel + single consumer preserves IPC arrival order. Mutex on
    // Dispatchers.Default does not — coroutines launched per send() race to
    // lock() from different worker threads.
    private val ipcChannel = Channel<String>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            for (jsonMsg in ipcChannel) {
                processIpc(jsonMsg)
            }
        }
    }

    override fun send(jsonMsg: String) {
        enforceAllowedCaller()
        ipcChannel.trySend(jsonMsg)
    }

    private suspend fun processIpc(jsonMsg: String) {
        Timber.d("[B][send] : $jsonMsg")
        val message = messageParser.parse(jsonMsg)
        Timber.d("[B][send] : message = $message")
        val result = messageHandler.handle(message) ?: return
        when (message) {
            is MyViewBoardMessage.OpenWindow -> {
                eventNotifier.notifyOpenWindowResult(
                    requestId = message.requestId,
                    windowTag = message.windowTag,
                    status = result.status,
                    reasonCode = result.reasonCode,
                    reasonMessage = result.reasonMessage
                )
            }

            else -> {
                eventNotifier.notifyMessageResult(result)
            }
        }
        Timber.d("[B][send] : result = $result")
    }

    override fun registerCallback(cb: IMyViewBoardCallback) {
        enforceAllowedCaller()
        callbackEmitter.registerCallback(cb)
    }

    override fun unregisterCallback(cb: IMyViewBoardCallback) {
        enforceAllowedCaller()
        callbackEmitter.unregisterCallback(cb)
    }

    fun onClientUnbound() {
        MyViewBoardSessionStore.clearToken()
    }

    fun onServiceDestroyed() {
        MyViewBoardSessionStore.clearToken()
        callbackEmitter.clearCallbacks()
    }

    private fun enforceAllowedCaller() {
        val callingPackages = packageManager.getPackagesForUid(getCallingUid()).orEmpty().toList()
        val clientAppInfo = ClientAppInfo.findClientByPackageNames(callingPackages)
        if (clientAppInfo == ClientAppInfo.AppMyViewBoard) {
            return
        }
        throw SecurityException("Caller package is not allowed")
    }
}
