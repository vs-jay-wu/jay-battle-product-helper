package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessage
import com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyViewBoardBinderStartQuizParserTest {
    private val parser = MyViewBoardMessageParser()

    @Test
    fun `parse returns valid start quiz message`() {
        val json = """
            {
              "type": "MessageStartQuiz",
              "request_id": "req-start-quiz-1",
              "app_version": "1.0.0",
              "payload": {
                "quiz_type": "TRUE_FALSE",
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.StartQuiz)
        assertEquals("req-start-quiz-1", (result as MyViewBoardMessage.StartQuiz).requestId)
        assertEquals(MyViewBoardMessage.StartQuiz.MvbQuizType.TRUE_FALSE, result.mvbQuizType)
    }

    @Test
    fun `parse returns invalid when request_id is missing`() {
        val json = """
            {
              "type": "MessageStartQuiz",
              "app_version": "1.0.0",
              "payload": {
                "quiz_type": "TRUE_FALSE",
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
    fun `parse returns invalid when quiz_type is missing`() {
        val json = """
            {
              "type": "MessageStartQuiz",
              "request_id": "req-start-quiz-2",
              "app_version": "1.0.0",
              "payload": {
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-start-quiz-2", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE, result.reasonCode)
        assertEquals("payload.quiz_type is missing.", result.reasonMessage)
    }

    @Test
    fun `parse returns invalid when quiz_type is unknown`() {
        val json = """
            {
              "type": "MessageStartQuiz",
              "request_id": "req-start-quiz-3",
              "app_version": "1.0.0",
              "payload": {
                "quiz_type": "ESSAY",
                "data": {}
              }
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertTrue(result is MyViewBoardMessage.Invalid)
        assertEquals("req-start-quiz-3", (result as MyViewBoardMessage.Invalid).requestId)
        assertEquals(MyViewBoardEvent.REASON_CODE_MALFORMED_MESSAGE, result.reasonCode)
        assertEquals("payload.quiz_type is invalid.", result.reasonMessage)
    }

    @Test
    fun `parse returns not handled when start quiz json is malformed`() {
        val malformedJson = """{"type":"MessageStartQuiz","payload":"""

        val result = parser.parse(malformedJson)

        assertTrue(result is MyViewBoardMessage.NotHandled)
    }
}
