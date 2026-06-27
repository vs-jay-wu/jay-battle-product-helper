package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyViewBoardBinderTokenParserTest {
    private val parser = MyViewBoardMessageParser()

    @Test
    fun `parse returns token command when MessageMvbToken has non-empty token`() {
        val json = """
            {
              "type": "MessageMvbToken",
              "request_id": "req-token-1",
              "app_version": "1.0.0",
              "payload": {
                "mvb_token": "token_123"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue("Unexpected parse result: $result", result is MyViewBoardMessage.MvbToken)
        assertEquals("req-token-1", (result as MyViewBoardMessage.MvbToken).requestId)
        assertEquals("token_123", result.token)
    }

    @Test
    fun `parse returns invalid when MessageMvbToken payload is missing`() {
        val json = """
            {
              "type": "MessageMvbToken",
              "request_id": "req-token-2",
              "app_version": "1.0.0"
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-token-2", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN, result.reasonCode)
    }

    @Test
    fun `parse returns invalid when MessageMvbToken token is blank`() {
        val json = """
            {
              "type": "MessageMvbToken",
              "request_id": "req-token-3",
              "app_version": "1.0.0",
              "payload": {
                "mvb_token": "   "
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-token-3", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN, result.reasonCode)
    }

    @Test
    fun `parse returns invalid when MessageMvbToken token is empty string`() {
        // Empty-string boundary (distinct from the whitespace case above): both collapse to
        // "" after trim and hit the same isEmpty() guard, returning invalid_mvb_token.
        val json = """
            {
              "type": "MessageMvbToken",
              "request_id": "req-token-empty",
              "app_version": "1.0.0",
              "payload": {
                "mvb_token": ""
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-token-empty", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN, result.reasonCode)
    }

    @Test
    fun `parse returns invalid when request_id is missing`() {
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
        assertEquals(null, (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID, result.reasonCode)
    }

    @Test
    fun `parse returns not handled for calculate message`() {
        val json = """
            {
              "type": "MessageCalculateTwoNumber",
              "app_version": "1.0.0",
              "number_1": 1,
              "number_2": 2
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.NotHandled)
    }

    @Test
    fun `parse trims request_id and token`() {
        val json = """
            {
              "type": "MessageMvbToken",
              "request_id": "  req-token-trim  ",
              "app_version": "1.0.0",
              "payload": {
                "mvb_token": "  token_trim  "
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue("Unexpected parse result: $result", result is MyViewBoardMessage.MvbToken)
        assertEquals("req-token-trim", (result as MyViewBoardMessage.MvbToken).requestId)
        assertEquals("token_trim", result.token)
    }

    @Test
    fun `parse returns invalid when token is string null`() {
        val json = """
            {
              "type": "MessageMvbToken",
              "request_id": "req-token-null",
              "app_version": "1.0.0",
              "payload": {
                "mvb_token": "null"
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-token-null", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_MVB_TOKEN, result.reasonCode)
    }

    @Test
    fun `parse returns not handled when json is malformed`() {
        val malformedJson = """{"type":"MessageMvbToken","payload":"""

        val result = parser.parse(malformedJson)

        assertTrue(result is MyViewBoardMessage.NotHandled)
    }
}
