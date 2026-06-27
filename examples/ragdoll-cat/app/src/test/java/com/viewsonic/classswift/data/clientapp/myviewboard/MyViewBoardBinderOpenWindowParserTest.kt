package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyViewBoardBinderOpenWindowParserTest {
    private val parser = MyViewBoardMessageParser()

    @Test
    fun `parse returns valid when request contains window_tag`() {
        val json = """
            {
              "type": "MessageOpenWindow",
              "request_id": "req-window-1",
              "app_version": "1.0.0",
              "payload": {
                "window_tag": "quiz_tool",
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.OpenWindow)
        assertEquals("req-window-1", (result as MyViewBoardMessage.OpenWindow).requestId)
        assertEquals("quiz_tool", result.windowTag)
    }

    @Test
    fun `parse returns invalid when request_id is missing`() {
        val json = """
            {
              "type": "MessageOpenWindow",
              "app_version": "1.0.0",
              "payload": {
                "window_tag": "quiz_tool",
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals(null, (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID, result.reasonCode)
    }

    @Test
    fun `parse returns invalid when window_tag is missing`() {
        val json = """
            {
              "type": "MessageOpenWindow",
              "request_id": "req-window-2",
              "app_version": "1.0.0",
              "payload": {
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-window-2", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_UNKNOWN_WINDOW_TAG, result.reasonCode)
    }

    @Test
    fun `parse returns invalid for token message without request id`() {
        val json = """
            {
              "type": "MessageMvbToken",
              "app_version": "1.0.0",
              "payload": {
                "mvb_token": "token_123"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID, (result as MyViewBoardMessage.Invalid).reasonCode)
    }

    @Test
    fun `parse returns invalid when window_tag is blank`() {
        val json = """
            {
              "type": "MessageOpenWindow",
              "request_id": "req-window-3",
              "app_version": "1.0.0",
              "payload": {
                "window_tag": "   "
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-window-3", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_UNKNOWN_WINDOW_TAG, result.reasonCode)
        assertEquals("window_tag is required.", result.reasonMessage)
    }

    @Test
    fun `parse returns not handled when open window json is malformed`() {
        val malformedJson = """{"type":"MessageOpenWindow","payload":"""

        val result = parser.parse(malformedJson)

        assertTrue(result is MyViewBoardMessage.NotHandled)
    }
}
