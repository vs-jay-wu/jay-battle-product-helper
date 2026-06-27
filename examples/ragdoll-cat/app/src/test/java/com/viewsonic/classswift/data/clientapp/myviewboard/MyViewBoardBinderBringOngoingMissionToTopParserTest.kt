package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyViewBoardBinderBringOngoingMissionToTopParserTest {
    private val parser = MyViewBoardMessageParser()

    @Test
    fun `parse returns valid bring ongoing mission to top message`() {
        val json = """
            {
              "type": "MessageBringOngoingMissionToTop",
              "request_id": "req-bring-mission-1",
              "app_version": "1.0.0",
              "payload": {
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.BringOngoingMissionToTop)
        assertEquals(
            "req-bring-mission-1",
            (result as MyViewBoardMessage.BringOngoingMissionToTop).requestId
        )
    }

    @Test
    fun `parse returns invalid when request_id is missing`() {
        val json = """
            {
              "type": "MessageBringOngoingMissionToTop",
              "app_version": "1.0.0",
              "payload": {
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
    fun `parse returns not handled when bring ongoing mission to top json is malformed`() {
        val malformedJson = """{"type":"MessageBringOngoingMissionToTop","payload":"""

        val result = parser.parse(malformedJson)

        assertTrue(result is MyViewBoardMessage.NotHandled)
    }
}
