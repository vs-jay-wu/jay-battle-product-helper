package com.viewsonic.classswift.data.clientapp.myviewboard.notifier

import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventCommandResult
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventCommandResultPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventLoginStatus
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventLoginStatusPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventMissionStatus
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventMissionStatusPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventOpenWindowResult
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventOpenWindowResultPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventWindowStateChanged
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventWindowStateChangedPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageType
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageResult
import com.viewsonic.classswift.data.clientapp.myviewboard.transport.MyViewBoardCallbackEmitter
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.windowframework.core.enums.WindowTag

class MyViewBoardEventNotifier(
    private val moshiProvider: MoshiProvider,
    private val callbackEmitter: MyViewBoardCallbackEmitter
) {
    fun notifyMessageResult(result: MyViewBoardMessageResult) {
        if (result.requestId.isNullOrBlank()) {
            return
        }
        emit(
            EventCommandResult(
                requestId = result.requestId,
                responseTo = result.responseTo,
                payload = EventCommandResultPayload(
                    status = result.status,
                    reasonCode = result.reasonCode,
                    reasonMessage = result.reasonMessage
                )
            )
        )
    }

    fun notifyOpenWindowResult(
        requestId: String,
        windowTag: String,
        status: String,
        reasonCode: String = "",
        reasonMessage: String = ""
    ) {
        emit(
            EventOpenWindowResult(
                requestId = requestId,
                responseTo = MyViewBoardMessageType.MESSAGE_OPEN_WINDOW,
                payload = EventOpenWindowResultPayload(
                    windowTag = windowTag,
                    status = status,
                    reasonCode = reasonCode,
                    reasonMessage = reasonMessage
                )
            )
        )
    }

    fun notifyLoginCompleted() {
        emit(
            EventLoginStatus(
                payload = EventLoginStatusPayload(
                    status = MyViewBoardEvent.STATUS_SUCCESS
                )
            )
        )
    }

    fun notifyLoginFailed(
        reasonCode: String = "",
        reasonMessage: String = ""
    ) {
        emit(
            EventLoginStatus(
                payload = EventLoginStatusPayload(
                    status = MyViewBoardEvent.STATUS_FAILED,
                    reasonCode = reasonCode,
                    reasonMessage = reasonMessage
                )
            )
        )
    }

    fun notifyWindowStateChanged(
        windowTag: WindowTag,
        state: EventWindowStateChangedPayload.State,
        shouldToggleOff: Boolean = false
    ) {
        emit(
            EventWindowStateChanged(
                payload = EventWindowStateChangedPayload(
                    windowTag = windowTag,
                    state = state,
                    shouldToggleOff = shouldToggleOff
                )
            )
        )
    }

    fun notifyMissionStatus(
        missionType: MissionType,
        status: EventMissionStatusPayload.Status
    ) {
        if (missionType == MissionType.NONE) {
            return
        }
        emit(
            EventMissionStatus(
                payload = EventMissionStatusPayload(
                    missionType = missionType.serverValue,
                    status = status
                )
            )
        )
    }

    private fun emit(event: Any) {
        callbackEmitter.emit(moshiProvider.toJson(event))
    }
}
