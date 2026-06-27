package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers parsing of `MessageToolbarPositionChanged` IPC payloads (VSFT-7257).
 */
class MyViewBoardBinderToolbarPositionParserTest {
    private val parser = MyViewBoardMessageParser()

    @Test
    fun `parse returns ToolbarPositionChanged when payload contains TOP`() {
        val json = toolbarPositionJson(requestId = "req-tb-1", position = "TOP")

        val result = parser.parse(json)

        assertTrue("Unexpected: $result", result is MyViewBoardMessage.ToolbarPositionChanged)
        val message = result as MyViewBoardMessage.ToolbarPositionChanged
        assertEquals("req-tb-1", message.requestId)
        assertEquals(MvbToolbarPosition.TOP, message.position)
    }

    @Test
    fun `parse returns ToolbarPositionChanged for BOTTOM LEFT and RIGHT`() {
        listOf(
            "BOTTOM" to MvbToolbarPosition.BOTTOM,
            "LEFT" to MvbToolbarPosition.LEFT,
            "RIGHT" to MvbToolbarPosition.RIGHT,
        ).forEach { (raw, expected) ->
            val result = parser.parse(toolbarPositionJson(requestId = "req-$raw", position = raw))

            assertTrue("Unexpected for $raw: $result", result is MyViewBoardMessage.ToolbarPositionChanged)
            assertEquals(expected, (result as MyViewBoardMessage.ToolbarPositionChanged).position)
        }
    }

    @Test
    fun `parse accepts mixed-case toolbar_position value`() {
        val json = toolbarPositionJson(requestId = "req-tb-mixedcase", position = "rIgHt")

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.ToolbarPositionChanged)
        assertEquals(
            MvbToolbarPosition.RIGHT,
            (result as MyViewBoardMessage.ToolbarPositionChanged).position
        )
    }

    @Test
    fun `parse returns invalid when toolbar_position is unknown value`() {
        val json = toolbarPositionJson(requestId = "req-tb-bad", position = "DIAGONAL")

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        val invalid = result as MyViewBoardMessage.Invalid
        assertEquals("req-tb-bad", invalid.requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_TOOLBAR_POSITION, invalid.reasonCode)
    }

    @Test
    fun `parse returns invalid when payload is missing`() {
        val json = """
            {
              "type": "MessageToolbarPositionChanged",
              "request_id": "req-tb-nopayload",
              "app_version": "1.0.0"
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals(
            MyViewBoardEvent.REASON_CODE_INVALID_TOOLBAR_POSITION,
            (result as MyViewBoardMessage.Invalid).reasonCode
        )
    }

    @Test
    fun `parse returns invalid when toolbar_position key is missing`() {
        val json = """
            {
              "type": "MessageToolbarPositionChanged",
              "request_id": "req-tb-nokey",
              "app_version": "1.0.0",
              "payload": {}
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals(
            MyViewBoardEvent.REASON_CODE_INVALID_TOOLBAR_POSITION,
            (result as MyViewBoardMessage.Invalid).reasonCode
        )
    }

    @Test
    fun `parse picks up optional whiteboard_top_dp when present`() {
        val json = """
            {
              "type": "MessageToolbarPositionChanged",
              "request_id": "req-tb-wb",
              "app_version": "1.0.0",
              "payload": {
                "toolbar_position": "BOTTOM",
                "whiteboard_top_dp": 70.5
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.ToolbarPositionChanged)
        val message = result as MyViewBoardMessage.ToolbarPositionChanged
        assertEquals(MvbToolbarPosition.BOTTOM, message.position)
        assertEquals(70.5, message.whiteboardTopDp!!, 0.001)
    }

    @Test
    fun `parse leaves whiteboard_top_dp null when field missing`() {
        val json = toolbarPositionJson(requestId = "req-tb-nowb", position = "BOTTOM")

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.ToolbarPositionChanged)
        assertEquals(null, (result as MyViewBoardMessage.ToolbarPositionChanged).whiteboardTopDp)
    }

    @Test
    fun `parse drops negative whiteboard_top_dp as null`() {
        val json = """
            {
              "type": "MessageToolbarPositionChanged",
              "request_id": "req-tb-negwb",
              "app_version": "1.0.0",
              "payload": {
                "toolbar_position": "BOTTOM",
                "whiteboard_top_dp": -5
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.ToolbarPositionChanged)
        assertEquals(null, (result as MyViewBoardMessage.ToolbarPositionChanged).whiteboardTopDp)
    }

    @Test
    fun `parse returns invalid when request_id is missing`() {
        val json = """
            {
              "type": "MessageToolbarPositionChanged",
              "app_version": "1.0.0",
              "payload": { "toolbar_position": "TOP" }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        val invalid = result as MyViewBoardMessage.Invalid
        assertEquals(null, invalid.requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_INVALID_REQUEST_ID, invalid.reasonCode)
    }

    private fun toolbarPositionJson(requestId: String, position: String): String {
        return """
            {
              "type": "MessageToolbarPositionChanged",
              "request_id": "$requestId",
              "app_version": "1.0.0",
              "payload": {
                "toolbar_position": "$position"
              }
            }
        """.trimIndent()
    }
}
