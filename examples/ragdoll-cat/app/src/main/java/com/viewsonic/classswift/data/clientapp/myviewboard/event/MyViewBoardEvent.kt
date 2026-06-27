package com.viewsonic.classswift.data.clientapp.myviewboard.event

object MyViewBoardEvent {
    // CS -> MyViewBoard
    const val EVENT_COMMAND_RESULT = "EventCommandResult"
    const val EVENT_OPEN_WINDOW_RESULT = "EventOpenWindowResult"
    const val EVENT_LOGIN_STATUS = "EventLoginStatus"
    const val EVENT_WINDOW_STATE_CHANGED = "EventWindowStateChanged"
    const val EVENT_MISSION_STATUS = "EventMissionStatus"

    const val STATUS_SUCCESS = "success"
    const val STATUS_FAILED = "failed"

    const val REASON_CODE_INVALID_MVB_TOKEN = "invalid_mvb_token"
    const val REASON_CODE_INVALID_REQUEST_ID = "invalid_request_id"
    const val REASON_CODE_INVALID_VISIBILITY_ACTION = "invalid_visibility_action"
    const val REASON_CODE_UNKNOWN_WINDOW_TAG = "unknown_window_tag"
    const val REASON_CODE_OPEN_WINDOW_FAILED = "open_window_failed"
    const val REASON_CODE_MALFORMED_MESSAGE = "malformed_message"
    const val REASON_CODE_SERVICE_NOT_STARTED = "service_not_started"
    const val REASON_CODE_INVALID_TOOLBAR_POSITION = "invalid_toolbar_position"
}
