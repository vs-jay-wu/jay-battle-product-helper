package com.viewsonic.classswift.data.clientapp.myviewboard.message

import com.viewsonic.classswift.data.enum.MvbToolbarPosition

sealed class MyViewBoardMessage {
    data object NotHandled : MyViewBoardMessage()

    data class Invalid(
        val requestId: String?,
        val responseTo: String,
        val reasonCode: String,
        val reasonMessage: String
    ) : MyViewBoardMessage()

    data class MvbToken(
        val requestId: String,
        val token: String
    ) : MyViewBoardMessage()

    data class MvbVisibility(
        val requestId: String,
        val action: Action
    ) : MyViewBoardMessage() {
        enum class Action {
            AllShow,
            AllHide
        }
    }

    data class MinimizeAllWindows(
        val requestId: String,
    ) : MyViewBoardMessage()

    data class OpenWindow(
        val requestId: String,
        val windowTag: String
    ) : MyViewBoardMessage()

    /**
     * VSFT-8429: 要求在老師「進班成功後」才開啟 [windowTag] 視窗。
     * 若收到時尚未進班，CS 端暫存、待進班時開啟（且不另開 JoinClass）；若已在課堂中則立即開啟。
     */
    data class OpenWindowAfterClassEntry(
        val requestId: String,
        val windowTag: String
    ) : MyViewBoardMessage()

    data class StartQuiz(
        val requestId: String,
        val mvbQuizType: MvbQuizType
    ) : MyViewBoardMessage() {
        enum class MvbQuizType {
            TRUE_FALSE,
            MULTIPLE_SELECTION,
            AUDIO,
            SHORT_ANSWER,
            SKETCH_RESPONSE,
            POLL
        }
    }

    data class BringOngoingMissionToTop(
        val requestId: String,
    ) : MyViewBoardMessage()

    data class MvbClosed(
        val requestId: String,
    ) : MyViewBoardMessage()

    data class MvbUserSignOut(
        val requestId: String,
    ) : MyViewBoardMessage()

    data class ToolbarPositionChanged(
        val requestId: String,
        val position: MvbToolbarPosition,
        /** mVB 白板區上緣距離螢幕頂的 dp 值。VSFT-7257 起的 optional 欄位；
         *  缺欄位代表 mVB 端未實作此擴充，CS 端 fallback 到 hardcode。 */
        val whiteboardTopDp: Double? = null
    ) : MyViewBoardMessage()
}

data class MyViewBoardMessageResult(
    val requestId: String?,
    val responseTo: String,
    val status: String,
    val reasonCode: String = "",
    val reasonMessage: String = ""
)

object MyViewBoardProtocolKeys {
    const val PAYLOAD_KEY_TYPE = "type"
    const val PAYLOAD_KEY_REQUEST_ID = "request_id"
    const val PAYLOAD_KEY_PAYLOAD = "payload"
    const val PAYLOAD_KEY_MVB_TOKEN = "mvb_token"
    const val PAYLOAD_KEY_ACTION = "action"
    const val PAYLOAD_KEY_WINDOW_TAG = "window_tag"
    const val PAYLOAD_KEY_QUIZ_TYPE = "quiz_type"
    const val PAYLOAD_KEY_TOOLBAR_POSITION = "toolbar_position"
    const val PAYLOAD_KEY_WHITEBOARD_TOP_DP = "whiteboard_top_dp"
}

object MyViewBoardResponseTo {
    const val MvbToken = MyViewBoardMessageType.MESSAGE_MVB_TOKEN
    const val MvbVisibility = MyViewBoardMessageType.MESSAGE_MVB_VISIBILITY
    const val MinimizeAllWindows = MyViewBoardMessageType.MESSAGE_MINIMIZE_ALL_WINDOWS
    const val OpenWindow = MyViewBoardMessageType.MESSAGE_OPEN_WINDOW
    const val OpenWindowAfterClassEntry = MyViewBoardMessageType.MESSAGE_OPEN_WINDOW_AFTER_CLASS_ENTRY
    const val StartQuiz = MyViewBoardMessageType.MESSAGE_START_QUIZ
    const val BringOngoingMissionToTop = MyViewBoardMessageType.MESSAGE_BRING_ONGOING_MISSION_TO_TOP
    const val MvbClosed = MyViewBoardMessageType.MESSAGE_MVB_CLOSED
    const val MvbUserSignOut = MyViewBoardMessageType.MESSAGE_MVB_USER_SIGN_OUT
    const val ToolbarPositionChanged = MyViewBoardMessageType.MESSAGE_TOOLBAR_POSITION_CHANGED
}
