package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventOpenWindowResultTest {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(EventOpenWindowResult::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )

    @Test
    fun `toJson includes request_id response_to and payload window_tag`() {
        val event = EventOpenWindowResult(
            requestId = "req-open-100",
            responseTo = "MessageOpenWindow",
            appVersion = "2.0.0",
            payload = EventOpenWindowResultPayload(
                windowTag = "CS_SELECT_ORG",
                status = MyViewBoardEvent.STATUS_SUCCESS
            )
        )

        val json = adapter.toJson(event)
        val jsonMap = mapAdapter.fromJson(json).orEmpty()
        val payloadMap = jsonMap["payload"] as? Map<*, *> ?: emptyMap<String, Any?>()

        assertEquals(MyViewBoardEvent.EVENT_OPEN_WINDOW_RESULT, jsonMap["type"])
        assertEquals("req-open-100", jsonMap["request_id"])
        assertEquals("MessageOpenWindow", jsonMap["response_to"])
        assertEquals("2.0.0", jsonMap["app_version"])
        assertEquals("CS_SELECT_ORG", payloadMap["window_tag"])
        assertEquals(MyViewBoardEvent.STATUS_SUCCESS, payloadMap["status"])
        assertTrue(payloadMap.containsKey("reason_code"))
        assertTrue(payloadMap.containsKey("reason_message"))
        assertFalse(jsonMap.containsKey("window_tag"))
    }

    @Test
    fun `fromJson parses failed payload`() {
        val json = """
            {
              "type": "EventOpenWindowResult",
              "request_id": "req-open-200",
              "response_to": "MessageOpenWindow",
              "app_version": "3.0.0",
              "payload": {
                "window_tag": "CS_SELECT_ORG",
                "status": "failed",
                "reason_code": "open_window_failed",
                "reason_message": "Failed to open window."
              }
            }
        """.trimIndent()

        val event = adapter.fromJson(json)!!

        assertEquals(MyViewBoardEvent.EVENT_OPEN_WINDOW_RESULT, event.type)
        assertEquals("req-open-200", event.requestId)
        assertEquals("MessageOpenWindow", event.responseTo)
        assertEquals("3.0.0", event.appVersion)
        assertEquals("CS_SELECT_ORG", event.payload.windowTag)
        assertEquals(MyViewBoardEvent.STATUS_FAILED, event.payload.status)
        assertEquals(MyViewBoardEvent.REASON_CODE_OPEN_WINDOW_FAILED, event.payload.reasonCode)
        assertEquals("Failed to open window.", event.payload.reasonMessage)
    }

    @Test(expected = JsonDataException::class)
    fun `fromJson throws when payload is missing`() {
        val invalidJson = """
            {
              "type": "EventOpenWindowResult",
              "request_id": "req-open-300",
              "response_to": "MessageOpenWindow",
              "app_version": "1.0.0"
            }
        """.trimIndent()

        adapter.fromJson(invalidJson)
    }
}
