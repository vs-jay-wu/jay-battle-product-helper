package com.viewsonic.classswift.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

object TimeUtils {
    /**
     * Convert Unix timestamp (seconds) to ISO8601 string.
     */
    fun unixToIso(unixSeconds: Long,
                  zone: ZoneId = ZoneId.of("UTC"),
                  formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME): String {
        return Instant.ofEpochSecond(unixSeconds)
            .atZone(zone)
            .format(formatter)
    }

    /**
     * Convert ISO8601 string to Unix timestamp (seconds).
     * Supports both local time (no timezone) and UTC (with Z).
     */
    fun isoToUnix(isoString: String, zone: ZoneId = ZoneId.systemDefault()): Long {
        return try {
            // Try parsing with Instant first (handles "Z")
            Instant.parse(isoString).epochSecond
        } catch (e: Exception) {
            // If no "Z", treat it as local date-time in given zone
            val localDateTime = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME)
            localDateTime.atZone(zone).toEpochSecond()
        }
    }

    fun getTimeDiffFromCurrentTimeInMillis(startTimeInMillis: Long) = max(0, startTimeInMillis - System.currentTimeMillis())
}