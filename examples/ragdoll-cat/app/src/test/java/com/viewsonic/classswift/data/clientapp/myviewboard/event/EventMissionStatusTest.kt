package com.viewsonic.classswift.data.clientapp.myviewboard.event

import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.data.enum.MissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMissionStatusTest {
    @Test
    fun `toJson includes mission_type and status in payload`() {
        val json = MoshiProvider.toJson(
            EventMissionStatus(
                appVersion = "1.0.0",
                payload = EventMissionStatusPayload(
                    missionType = MissionType.QUIZ.serverValue,
                    status = EventMissionStatusPayload.Status.ONGOING
                )
            )
        )

        assertTrue(json.contains("\"type\":\"EventMissionStatus\""))
        assertTrue(json.contains("\"app_version\":\"1.0.0\""))
        assertTrue(json.contains("\"payload\""))
        assertTrue(json.contains("\"mission_type\":\"quiz\""))
        assertTrue(json.contains("\"status\":\"ONGOING\""))
    }

    @Test
    fun `fromJson parses payload`() {
        val event = MoshiProvider.fromJson(
            EventMissionStatus::class.java,
            """
                {
                  "type": "EventMissionStatus",
                  "app_version": "1.0.0",
                  "payload": {
                    "mission_type": "task",
                    "status": "CLOSED"
                  }
                }
            """.trimIndent()
        )

        assertEquals(MyViewBoardEvent.EVENT_MISSION_STATUS, event.type)
        assertEquals("1.0.0", event.appVersion)
        assertEquals(MissionType.PUSH_AND_RESPOND_TASK.serverValue, event.payload.missionType)
        assertEquals(EventMissionStatusPayload.Status.CLOSED, event.payload.status)
    }
}
