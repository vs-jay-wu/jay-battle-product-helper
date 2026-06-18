package com.viewsonic.classswift.data.clientapp.myviewboard.message

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.enum.MvbToolbarPosition

class MyViewBoardMessageParser {
    private val mapAdapter = Moshi.Builder().build().adapter<Map<String, Any?>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )

    fun parse(jsonMsg: String): MyViewBoardMessage {
        val messageType = parseMessageType(jsonMsg)
        return when (messageType) {
            MyViewBoardMessageType.MESSAGE_MVB_TOKEN -> parseMvbToken(jsonMsg)
            MyViewBoardMessageType.MESSAGE_MVB_VISIBILITY -> parseMvbVisibility(jsonMsg)
            MyViewBoardMessageType.MESSAGE_MINIMIZE_ALL_WINDOWS -> parseMinimizeAllWindows(jsonMsg)
            MyViewBoardMessageType.MESSAGE_OPEN_WINDOW -> parseOpenWindow(jsonMsg)
            MyViewBoardMessageType.MESSAGE_OPEN_WINDOW_AFTER_CLASS_ENTRY -> parseOpenWindowAfterClassEntry(jsonMsg)
            MyViewBoardMessageType.MESSAGE_START_QUIZ -> parseStartQuiz(jsonMsg)
            MyViewBoardMessageType.MESSAGE_BRING_ONGOING_MISSION_TO_TOP -> parseBringOngoingMissionToTop(jsonMsg)
            MyViewBoardMessageType.MESSAGE_MVB_CLOSED -> parseMvbClosed(jsonMsg)
            MyViewBoardMessageType.MESSAGE_MVB_USER_SIGN_OUT -> parseMvbUserSignOut(jsonMsg)
            MyViewBoardMessageType.MESSAGE_TOOLBAR_POSITION_CHANGED -> parseToolbarPositionChanged(jsonMsg)
            else -> MyViewBoardMessage.NotHandled
        }
    }

    private fun parseToolbarPositionChanged(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.ToolbarPositionChanged,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageToolbarPositionChanged format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.ToolbarPositionChanged,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            val payloadObject = jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_PAYLOAD] as? Map<*, *>
                ?: return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.ToolbarPositionChanged,
                    reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_TOOLBAR_POSITION,
                    reasonMessage = "payload.toolbar_position is missing."
                )
            val rawPosition = payloadObject[MyViewBoardProtocolKeys.PAYLOAD_KEY_TOOLBAR_POSITION] as? String
            val position = MvbToolbarPosition.fromIpcValue(rawPosition) ?: return invalid(
                requestId = requestId,
                responseTo = MyViewBoardResponseTo.ToolbarPositionChanged,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_TOOLBAR_POSITION,
                reasonMessage = "toolbar_position must be TOP, BOTTOM, LEFT, or RIGHT."
            )
            // VSFT-7257 optional: mVB 端可選擇附帶白板上緣 dp。缺欄位代表
            // 舊版 mVB，CS 端會 fallback 到 hardcoded VERTICAL_MARGIN_DP。
            // Moshi 對 JSON number 預設解 Double。
            val whiteboardTopDp = (payloadObject[MyViewBoardProtocolKeys.PAYLOAD_KEY_WHITEBOARD_TOP_DP] as? Number)
                ?.toDouble()
                ?.takeIf { it >= 0 }
            MyViewBoardMessage.ToolbarPositionChanged(
                requestId = requestId,
                position = position,
                whiteboardTopDp = whiteboardTopDp
            )
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.ToolbarPositionChanged,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageToolbarPositionChanged format is malformed."
            )
        }
    }

    private fun parseMvbToken(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbToken,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageMvbToken format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbToken,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            val payloadObject = jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_PAYLOAD] as? Map<*, *>
                ?: return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.MvbToken,
                    reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN,
                    reasonMessage = "payload.mvb_token is missing."
                )
            val token = payloadObject[MyViewBoardProtocolKeys.PAYLOAD_KEY_MVB_TOKEN] as? String ?: return invalid(
                requestId = requestId,
                responseTo = MyViewBoardResponseTo.MvbToken,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN,
                reasonMessage = "payload.mvb_token is missing."
            )
            val mvbToken = token.trim()
            if (mvbToken.isEmpty()) {
                return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.MvbToken,
                    reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN,
                    reasonMessage = "mvb_token is required."
                )
            }
            if (mvbToken.equals("null", ignoreCase = true)) {
                return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.MvbToken,
                    reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN,
                    reasonMessage = "mvb_token is malformed."
                )
            }
            MyViewBoardMessage.MvbToken(requestId = requestId, token = mvbToken)
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbToken,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageMvbToken format is malformed."
            )
        }
    }

    private fun parseMvbVisibility(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbVisibility,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageMvbVisibility format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbVisibility,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            val payloadObject = jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_PAYLOAD] as? Map<*, *>
                ?: return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.MvbVisibility,
                    reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_VISIBILITY_ACTION,
                    reasonMessage = "payload.action is missing."
                )
            val action = (payloadObject[MyViewBoardProtocolKeys.PAYLOAD_KEY_ACTION] as? String)?.trim().orEmpty()
            when (action) {
                MyViewBoardMessageType.ACTION_ALL_SHOW -> MyViewBoardMessage.MvbVisibility(
                    requestId = requestId,
                    action = MyViewBoardMessage.MvbVisibility.Action.AllShow
                )
                MyViewBoardMessageType.ACTION_ALL_HIDE -> MyViewBoardMessage.MvbVisibility(
                    requestId = requestId,
                    action = MyViewBoardMessage.MvbVisibility.Action.AllHide
                )
                else -> invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.MvbVisibility,
                    reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_VISIBILITY_ACTION,
                    reasonMessage = "action must be all_show or all_hide."
                )
            }
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbVisibility,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageMvbVisibility format is malformed."
            )
        }
    }

    private fun parseOpenWindow(jsonMsg: String): MyViewBoardMessage =
        parseWindowTagPayload(jsonMsg, MyViewBoardResponseTo.OpenWindow) { requestId, windowTag ->
            MyViewBoardMessage.OpenWindow(requestId = requestId, windowTag = windowTag)
        }

    private fun parseOpenWindowAfterClassEntry(jsonMsg: String): MyViewBoardMessage =
        parseWindowTagPayload(jsonMsg, MyViewBoardResponseTo.OpenWindowAfterClassEntry) { requestId, windowTag ->
            MyViewBoardMessage.OpenWindowAfterClassEntry(requestId = requestId, windowTag = windowTag)
        }

    /**
     * 共用解析：payload 帶 window_tag 的訊息（OpenWindow / OpenWindowAfterClassEntry）。
     * 兩者格式相同，僅 responseTo 與最終建構的 message 型別不同。
     */
    private fun parseWindowTagPayload(
        jsonMsg: String,
        responseTo: String,
        build: (requestId: String, windowTag: String) -> MyViewBoardMessage
    ): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = responseTo,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "$responseTo format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = responseTo,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            val payloadObject = jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_PAYLOAD] as? Map<*, *>
                ?: return invalid(
                    requestId = requestId,
                    responseTo = responseTo,
                    reasonCode = MyViewBoardEvent.REASON_CODE_UNKNOWN_WINDOW_TAG,
                    reasonMessage = "window_tag is required."
                )
            val windowTag = (payloadObject[MyViewBoardProtocolKeys.PAYLOAD_KEY_WINDOW_TAG] as? String)?.trim().orEmpty()
            if (windowTag.isEmpty()) {
                return invalid(
                    requestId = requestId,
                    responseTo = responseTo,
                    reasonCode = MyViewBoardEvent.REASON_CODE_UNKNOWN_WINDOW_TAG,
                    reasonMessage = "window_tag is required."
                )
            }
            build(requestId, windowTag)
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = responseTo,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "$responseTo format is malformed."
            )
        }
    }

    private fun parseMinimizeAllWindows(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MinimizeAllWindows,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageMinimizeAllWindows format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MinimizeAllWindows,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            MyViewBoardMessage.MinimizeAllWindows(requestId = requestId)
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MinimizeAllWindows,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageMinimizeAllWindows format is malformed."
            )
        }
    }

    private fun parseStartQuiz(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.StartQuiz,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageStartQuiz format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.StartQuiz,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            val payloadObject = jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_PAYLOAD] as? Map<*, *>
                ?: return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.StartQuiz,
                    reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                    reasonMessage = "payload.quiz_type is missing."
                )
            val quizType = (payloadObject[MyViewBoardProtocolKeys.PAYLOAD_KEY_QUIZ_TYPE] as? String)?.trim().orEmpty()
            if (quizType.isEmpty()) {
                return invalid(
                    requestId = requestId,
                    responseTo = MyViewBoardResponseTo.StartQuiz,
                    reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                    reasonMessage = "payload.quiz_type is missing."
                )
            }
            val mvbQuizType = runCatching {
                MyViewBoardMessage.StartQuiz.MvbQuizType.valueOf(quizType)
            }.getOrNull() ?: return invalid(
                requestId = requestId,
                responseTo = MyViewBoardResponseTo.StartQuiz,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "payload.quiz_type is invalid."
            )
            MyViewBoardMessage.StartQuiz(
                requestId = requestId,
                mvbQuizType = mvbQuizType
            )
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.StartQuiz,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageStartQuiz format is malformed."
            )
        }
    }

    private fun parseMvbClosed(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbClosed,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MvbClosed format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbClosed,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            MyViewBoardMessage.MvbClosed(requestId = requestId)
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbClosed,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MvbClosed format is malformed."
            )
        }
    }

    private fun parseBringOngoingMissionToTop(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.BringOngoingMissionToTop,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageBringOngoingMissionToTop format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.BringOngoingMissionToTop,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            MyViewBoardMessage.BringOngoingMissionToTop(requestId = requestId)
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.BringOngoingMissionToTop,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MessageBringOngoingMissionToTop format is malformed."
            )
        }
    }

    private fun parseMvbUserSignOut(jsonMsg: String): MyViewBoardMessage {
        return try {
            val jsonMap = mapAdapter.fromJson(jsonMsg) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbUserSignOut,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MvbUserSignOut format is malformed."
            )
            val requestId = parseRequestId(jsonMap) ?: return invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbUserSignOut,
                reasonCode = MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID,
                reasonMessage = "request_id is required."
            )
            MyViewBoardMessage.MvbUserSignOut(requestId = requestId)
        } catch (e: Exception) {
            invalid(
                requestId = null,
                responseTo = MyViewBoardResponseTo.MvbUserSignOut,
                reasonCode = MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE,
                reasonMessage = "MvbUserSignOut format is malformed."
            )
        }
    }

    private fun parseMessageType(jsonMsg: String): String {
        return runCatching {
            val jsonMap = mapAdapter.fromJson(jsonMsg).orEmpty()
            jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_TYPE] as? String ?: ""
        }.getOrDefault("")
    }

    private fun parseRequestId(jsonMap: Map<String, Any?>): String? {
        val requestId = (jsonMap[MyViewBoardProtocolKeys.PAYLOAD_KEY_REQUEST_ID] as? String)?.trim().orEmpty()
        return requestId.takeIf { it.isNotEmpty() }
    }

    private fun invalid(
        requestId: String?,
        responseTo: String,
        reasonCode: String,
        reasonMessage: String
    ): MyViewBoardMessage.Invalid {
        return MyViewBoardMessage.Invalid(
            requestId = requestId,
            responseTo = responseTo,
            reasonCode = reasonCode,
            reasonMessage = reasonMessage
        )
    }
}
