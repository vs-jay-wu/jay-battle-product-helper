package com.viewsonic.classswift.utils

import java.time.LocalDate
import java.time.ZoneId

object DateTimeUtils {

    fun getTodayMidnight(): Long {
        val todayMidnight = LocalDate.now() // 獲取今天的日期
            .atStartOfDay(ZoneId.systemDefault()) // 設定為當地時區的午夜
        return todayMidnight.toInstant().epochSecond  // 返回秒時間戳跟api比對才會正確
    }

    /**
     * return Pair<Minutes: Long, Seconds: Long>
     */
    fun formatToMinuteSecondPair(milliseconds: Long): Pair<Long, Long> {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return minutes to seconds
    }

}