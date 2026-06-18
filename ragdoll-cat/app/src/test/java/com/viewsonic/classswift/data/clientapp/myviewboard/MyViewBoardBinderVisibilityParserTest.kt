package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyViewBoardBinderVisibilityParserTest {
    private val parser = MyViewBoardMessageParser()

    @Test
    fun `parse returns all show command`() {
        val json = """
            {
              "type": "MessageMvbVisibility",
              "request_id": "req-visibility-1",
              "app_version": "1.0.0",
              "payload": {
                "action": "all_show"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.MvbVisibility)
        assertEquals("req-visibility-1", (result as MyViewBoardMessage.MvbVisibility).requestId)
        assertEquals(MyViewBoardMessage.MvbVisibility.Action.AllShow, result.action)
    }

    @Test
    fun `parse returns all hide command`() {
        val json = """
            {
              "type": "MessageMvbVisibility",
              "request_id": "req-visibility-2",
              "app_version": "1.0.0",
              "payload": {
                "action": "all_hide"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.MvbVisibility)
        assertEquals("req-visibility-2", (result as MyViewBoardMessage.MvbVisibility).requestId)
        assertEquals(MyViewBoardMessage.MvbVisibility.Action.AllHide, result.action)
    }

    @Test
    fun `parse returns invalid when action is unknown`() {
        val json = """
            {
              "type": "MessageMvbVisibility",
              "request_id": "req-visibility-3",
              "app_version": "1.0.0",
              "payload": {
                "action": "show_window"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-visibility-3", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_VISIBILITY_ACTION, result.reasonCode)
    }

    @Test
    fun `parse returns invalid when request_id is missing`() {
        val json = """
            {
              "type": "MessageMvbVisibility",
              "app_version": "1.0.0",
              "payload": {
                "action": "all_show"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals(null, (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID, result.reasonCode)
    }

    @Test
    fun `parse returns not handled for token message`() {
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
    fun `parse returns invalid when payload is missing`() {
        val json = """
            {
              "type": "MessageMvbVisibility",
              "request_id": "req-visibility-4",
              "app_version": "1.0.0"
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-visibility-4", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_VISIBILITY_ACTION, result.reasonCode)
        assertEquals("payload.action is missing.", result.reasonMessage)
    }

    @Test
    fun `parse returns not handled when visibility json is malformed`() {
        val malformedJson = """{"type":"MessageMvbVisibility","payload":"""

        val result = parser.parse(malformedJson)

        assertTrue(result is MyViewBoardMessage.NotHandled)
    }
}
