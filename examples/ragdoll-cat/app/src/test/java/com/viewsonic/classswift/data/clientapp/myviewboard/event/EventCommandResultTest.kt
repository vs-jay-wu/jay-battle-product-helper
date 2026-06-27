package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCommandResultTest {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(EventCommandResult::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )

    @Test
    fun `toJson includes request_id response_to and payload`() {
        val event = EventCommandResult(
            requestId = "req-100",
            responseTo = "MessageMvbToken",
            appVersion = "2.0.0",
            payload = EventCommandResultPayload(
                status = MyViewBoardEvent.STATUS_SUCCESS
            )
        )

        val json = adapter.toJson(event)
        val jsonMap = mapAdapter.fromJson(json).orEmpty()
        val payloadMap = jsonMap["payload"] as? Map<*, *> ?: emptyMap<String, Any?>()

        assertEquals(MyViewBoardEvent.EVENT_COMMAND_RESULT, jsonMap["type"])
        assertEquals("req-100", jsonMap["request_id"])
        assertEquals("MessageMvbToken", jsonMap["response_to"])
        assertEquals("2.0.0", jsonMap["app_version"])
        assertEquals(MyViewBoardEvent.STATUS_SUCCESS, payloadMap["status"])
        assertTrue(payloadMap.containsKey("reason_code"))
        assertTrue(payloadMap.containsKey("reason_message"))
        assertFalse(jsonMap.containsKey("status"))
    }

    @Test
    fun `fromJson parses failed payload`() {
        val json = """
            {
              "type": "EventCommandResult",
              "request_id": "req-200",
              "response_to": "MessageMvbVisibility",
              "app_version": "3.0.0",
              "payload": {
                "status": "failed",
                "reason_code": "invalid_visibility_action",
                "reason_message": "action must be all_show or all_hide."
              }
            }
        """.trimIndent()

        val event = adapter.fromJson(json)!!

        assertEquals(MyViewBoardEvent.EVENT_COMMAND_RESULT, event.type)
        assertEquals("req-200", event.requestId)
        assertEquals("MessageMvbVisibility", event.responseTo)
        assertEquals("3.0.0", event.appVersion)
        assertEquals(MyViewBoardEvent.STATUS_FAILED, event.payload.status)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_VISIBILITY_ACTION, event.payload.reasonCode)
        assertEquals("action must be all_show or all_hide.", event.payload.reasonMessage)
    }

    @Test(expected = JsonDataException::class)
    fun `fromJson throws when payload is missing`() {
        val invalidJson = """
            {
              "type": "EventCommandResult",
              "request_id": "req-300",
              "response_to": "MessageMvbToken",
              "app_version": "1.0.0"
            }
        """.trimIndent()

        adapter.fromJson(invalidJson)
    }
}
