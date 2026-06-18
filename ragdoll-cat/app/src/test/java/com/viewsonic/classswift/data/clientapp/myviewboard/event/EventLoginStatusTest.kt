package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventLoginStatusTest {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi
        .adapter(EventLoginStatus::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )

    @Test
    fun `toJson includes payload status and reason fields`() {
        val event = EventLoginStatus(
            appVersion = "1.2.3",
            payload = EventLoginStatusPayload(
                status = MyViewBoardEvent.STATUS_SUCCESS,
                reasonCode = "",
                reasonMessage = ""
            )
        )

        val json = adapter.toJson(event)
        val jsonMap = mapAdapter.fromJson(json).orEmpty()
        val payloadMap = jsonMap["payload"] as? Map<*, *> ?: emptyMap<String, Any?>()

        assertEquals(MyViewBoardEvent.EVENT_LOGIN_STATUS, jsonMap["type"])
        assertEquals("1.2.3", jsonMap["app_version"])
        assertEquals(MyViewBoardEvent.STATUS_SUCCESS, payloadMap["status"])
        assertTrue(payloadMap.containsKey("reason_code"))
        assertTrue(payloadMap.containsKey("reason_message"))
        assertFalse(jsonMap.containsKey("status"))
    }

    @Test
    fun `fromJson parses payload status and reason fields`() {
        val json = """
            {
              "type": "EventLoginStatus",
              "app_version": "9.9.9",
              "payload": {
                "status": "failed",
                "reason_code": "invalid_mvb_token",
                "reason_message": "mvb_token is empty or malformed."
              }
            }
        """.trimIndent()

        val event = adapter.fromJson(json)!!

        assertEquals(MyViewBoardEvent.EVENT_LOGIN_STATUS, event.type)
        assertEquals("9.9.9", event.appVersion)
        assertEquals(MyViewBoardEvent.STATUS_FAILED, event.payload.status)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN, event.payload.reasonCode)
        assertEquals("mvb_token is empty or malformed.", event.payload.reasonMessage)
    }

    @Test(expected = JsonDataException::class)
    fun `fromJson throws when payload is missing`() {
        val oldJson = """
            {
              "type": "EventLoginStatus",
              "app_version": "1.0.0",
              "status": "success"
            }
        """.trimIndent()

        adapter.fromJson(oldJson)
    }
}
