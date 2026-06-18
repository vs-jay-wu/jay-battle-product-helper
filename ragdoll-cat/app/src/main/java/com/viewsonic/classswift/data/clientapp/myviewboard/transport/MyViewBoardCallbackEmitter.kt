package com.viewsonic.classswift.data.clientapp.myviewboard.transport

import android.os.RemoteCallbackList
import com.viewsonic.classswift.service.IMyViewBoardCallback
import timber.log.Timber

class MyViewBoardCallbackEmitter {
    private val callbacks = RemoteCallbackList<IMyViewBoardCallback>()
    private val broadcastLock = Any()

    fun registerCallback(callback: IMyViewBoardCallback) {
        callbacks.register(callback)
    }

    fun unregisterCallback(callback: IMyViewBoardCallback) {
        callbacks.unregister(callback)
    }

    fun clearCallbacks() {
        callbacks.kill()
    }

    fun emit(jsonMessage: String) {
        Timber.d("[B][broadcastRawEvent] : jsonMessage = $jsonMessage")
        val callbackSnapshot = mutableListOf<IMyViewBoardCallback>()
        synchronized(broadcastLock) {
            val callbackCount = callbacks.beginBroadcast()
            try {
                for (index in 0 until callbackCount) {
                    callbackSnapshot += callbacks.getBroadcastItem(index)
                }
            } finally {
                callbacks.finishBroadcast()
            }
        }

        callbackSnapshot.forEach { callback ->
            try {
                callback.onEvent(jsonMessage)
            } catch (e: Exception) {
                Timber.d("[B] Error sending to client: $e")
            }
        }
    }
}
