package com.viewsonic.classswift.uimanager.maintenance

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.MaintenanceAnnouncementsApiService
import com.viewsonic.classswift.api.response.MaintenanceAnnouncementsResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.datastore.AccountDataStore
import com.viewsonic.classswift.data.datastore.DebugDataStore
import com.viewsonic.classswift.data.datastore.MaintenanceAnnouncementsDataStore
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.service.ClassSwiftService
import com.viewsonic.classswift.ui.window.UnderMaintenanceWindow
import com.viewsonic.classswift.ui.window.UpcomingMaintenanceCornerPromptWindow
import com.viewsonic.classswift.utils.TimeUtils
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date


class MaintenanceAnnouncementsUiManager(
    private val applicationContext: Context,
    private val maintenanceAnnouncementsApiService: MaintenanceAnnouncementsApiService,
    private val maintenanceAnnouncementsDataStore: MaintenanceAnnouncementsDataStore,
    private val accountDataStore: AccountDataStore,
    private val debugDataStore: DebugDataStore,
    private val csWindowManager: CSWindowManager,
    private val maintenancePreDowntimeEligibility: MaintenancePreDowntimeEligibility
) {
    private val jsonFileName = "maintenance.json"
    private val testFileName = "android_test.json"
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var maintenanceAnnouncementsResponse: MaintenanceAnnouncementsResponse = MaintenanceAnnouncementsResponse()
    private val debugDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private var checkCronJob: Job? = null
    private lateinit var lastDailyCheckedTime: LocalDateTime
    // Keys: all field names from "time_variables" and "variables" in maintenance.json
    // Values: the converted values of those fields from maintenance.json
    private val convertedVariableMap: MutableMap<String, String> = mutableMapOf()
    private val convertedDescriptionMap: MutableMap<MaintenancePhase, SpannableString> = mutableMapOf()
    private var hasViewedFiveMinutesBeforeMaintenanceAnnouncement = false
    private val maintenancePhaseDecisionEvaluator = MaintenancePhaseDecisionEvaluator()

    suspend fun fetchMaintenanceAnnouncements(): ApiResponse<MaintenanceAnnouncementsResponse> = withContext(Dispatchers.IO) {
        val isUseAndroidTestJsonForMaintenanceAnnouncements = debugDataStore.isUseAndroidTestJsonForMaintenanceAnnouncements()
        val response = when (isUseAndroidTestJsonForMaintenanceAnnouncements) {
            true -> maintenanceAnnouncementsApiService.fetchMaintenanceAnnouncements(testFileName)
            false -> maintenanceAnnouncementsApiService.fetchMaintenanceAnnouncements(jsonFileName)
        }
        when (response) {
            is ApiResponse.Success -> {
                maintenanceAnnouncementsResponse = response.data
                withContext(Dispatchers.Main) {
                    generateConvertedMap()
                }
            }
            else -> {}
        }
        return@withContext response
    }

    fun viewedFiveMinutesBeforeMaintenanceAnnouncement() {
        hasViewedFiveMinutesBeforeMaintenanceAnnouncement = true
    }

    fun resetHasViewedFiveMinutesBeforeMaintenanceAnnouncement() {
        hasViewedFiveMinutesBeforeMaintenanceAnnouncement = false
    }

    fun startCheckCronJob() {
        if (checkCronJob?.isActive == true) return
        checkCronJob = coroutineScope.launch(Dispatchers.IO) {
            lastDailyCheckedTime = LocalDateTime.now()
            while (isActive) {
                // Daily Check
                val nowLocalDateTime = LocalDateTime.now()
                Timber.d("[startDailyCheckCronJob] : Now Time = ${nowLocalDateTime.format(debugDateTimeFormatter)}")
                Timber.d("[startDailyCheckCronJob] : LastDailyChecked Time = ${lastDailyCheckedTime.format(debugDateTimeFormatter)}")
                if (lastDailyCheckedTime.toLocalDate() != nowLocalDateTime.toLocalDate()) {
                    lastDailyCheckedTime = nowLocalDateTime
                    fetchMaintenanceAnnouncements()
                }

                // within 5 minutes check
                checkIsInFiveMinutesMaintenancePhase()

                // within downtime check
                checkIsInDowntimePhase()

                delay(60_000L)
            }
        }
    }

    fun stopCheckCronJob() {
        checkCronJob?.cancel()
        checkCronJob = null
    }

    fun getMaintenanceTitle(phase: MaintenancePhase): String {
        return when (phase) {
            MaintenancePhase.TWO_DAYS_BEFORE -> {
                maintenanceAnnouncementsResponse.en.phases.early.content.title
            }
            MaintenancePhase.FIVE_MINUTES_BEFORE -> {
                maintenanceAnnouncementsResponse.en.phases.recent.content.title
            }
            MaintenancePhase.DURING_DOWNTIME -> {
                maintenanceAnnouncementsResponse.en.phases.ongoing.content.title
            }
        }
    }

    fun getMaintenanceDescription(phase: MaintenancePhase): SpannableString {
        return convertedDescriptionMap[phase] ?: SpannableString("")
    }

    suspend fun checkIsInFiveMinutesMaintenancePhase() {
        if (isInMaintenancePhase(MaintenancePhase.FIVE_MINUTES_BEFORE)) {
            // Need to fetch and check again before showing prompt window.
            fetchMaintenanceAnnouncements()
            if (isInMaintenancePhase(MaintenancePhase.FIVE_MINUTES_BEFORE)) {
                if (!csWindowManager.isWindowExisted(WindowTag.WINDOW_UPCOMING_MAINTENANCE_CORNER_PROMPT)) {
                    withContext(Dispatchers.Main) {
                        val window: UpcomingMaintenanceCornerPromptWindow = get(UpcomingMaintenanceCornerPromptWindow::class.java)
                        csWindowManager.createWindow(
                            window,
                            window.getInitLocation(),
                            isOutOfScreen = true,
                            isDraggable = false
                        )
                    }
                }
            }
        }
    }

    suspend fun checkIsInDowntimePhase() {
        if (isInMaintenancePhase(MaintenancePhase.DURING_DOWNTIME)) {
            // Need to fetch and check again before showing window.
            fetchMaintenanceAnnouncements()
            if (isInMaintenancePhase(MaintenancePhase.DURING_DOWNTIME)) {
                if (!csWindowManager.isWindowExisted(WindowTag.WINDOW_UNDER_MAINTENANCE)) {
                    withContext(Dispatchers.Main) {
                        CSWindowManager.createWindow(
                            get(UnderMaintenanceWindow::class.java),
                            Gravity.CENTER
                        )
                    }
                }
            }
        }
    }
    suspend fun isInMaintenancePhase(phase: MaintenancePhase): Boolean = withContext(Dispatchers.IO) {
        val userId = accountDataStore.getUserId()
        val isServiceStarted = ClassSwiftService.isServiceStarted()
        Timber.d("[isInMaintenancePhase] : accountDataStore.getUserId() = $userId")
        Timber.d("[isInMaintenancePhase] : ClassSwiftService.isServiceStarted() = $isServiceStarted")
        if (!isPreDowntimeEligible(
                phase = phase,
                userId = userId,
                isServiceStarted = isServiceStarted
            )
        ) {
            return@withContext false
        }
        if (!isMaintenanceAnnouncementsResponseValid()) {
            return@withContext false
        }
        val nowTimeInSeconds = Instant.now().epochSecond
        val phaseShowtime = resolvePhaseShowtimeInSeconds()
        Timber.d(
            "[isInMaintenancePhase] : - Now Time = ${
                LocalDateTime.ofEpochSecond(nowTimeInSeconds, 0, ZonedDateTime.now().offset).format(debugDateTimeFormatter)
            }"
        )
        Timber.d(
            "[isInMaintenancePhase] : - Two Days Before Showtime = ${
                LocalDateTime.ofEpochSecond(
                    phaseShowtime.twoDaysBeforeShowtimeInSeconds,
                    0,
                    ZonedDateTime.now().offset
                ).format(debugDateTimeFormatter)
            }"
        )
        Timber.d(
            "[isInMaintenancePhase] : - Five Minutes Before Showtime = ${
                LocalDateTime.ofEpochSecond(
                    phaseShowtime.fiveMinutesBeforeShowtimeInSeconds,
                    0,
                    ZonedDateTime.now().offset
                ).format(debugDateTimeFormatter)
            }"
        )
        Timber.d(
            "[isInMaintenancePhase] : - Downtime Showtime = ${
                LocalDateTime.ofEpochSecond(
                    phaseShowtime.duringDowntimeShowtimeInSeconds,
                    0,
                    ZonedDateTime.now().offset
                ).format(debugDateTimeFormatter)
            }"
        )
        return@withContext evaluateMaintenancePhase(
            phase = phase,
            userId = userId,
            nowTimeInSeconds = nowTimeInSeconds,
            phaseShowtimeInSeconds = phaseShowtime
        )
    }

    private fun isPreDowntimeEligible(
        phase: MaintenancePhase,
        userId: String,
        isServiceStarted: Boolean
    ): Boolean {
        return when (phase) {
            MaintenancePhase.TWO_DAYS_BEFORE -> {
                val isEligible = maintenancePreDowntimeEligibility.shouldShowTwoDaysBeforePrompt(
                    userId = userId,
                    isServiceStarted = isServiceStarted
                )
                if (!isEligible) {
                    Timber.d("[isInMaintenancePhase] : - isTwoDaysBeforeEligible = false")
                }
                isEligible
            }
            MaintenancePhase.FIVE_MINUTES_BEFORE -> {
                val isEligible = maintenancePreDowntimeEligibility.shouldShowFiveMinutesBeforePrompt(
                    userId = userId,
                    isServiceStarted = isServiceStarted
                )
                if (!isEligible) {
                    Timber.d("[isInMaintenancePhase] : - isFiveMinutesBeforeEligible = false")
                }
                isEligible
            }
            MaintenancePhase.DURING_DOWNTIME -> true
        }
    }

    private suspend fun evaluateMaintenancePhase(
        phase: MaintenancePhase,
        userId: String,
        nowTimeInSeconds: Long,
        phaseShowtimeInSeconds: PhaseShowtimeInSeconds
    ): Boolean {
        return when (phase) {
            MaintenancePhase.TWO_DAYS_BEFORE -> {
                val isTwoDaysBeforeAnnouncementViewed = maintenanceAnnouncementsDataStore.isTwoDaysBeforeAnnouncementViewed(
                    userId,
                    phaseShowtimeInSeconds.twoDaysBeforeShowtimeInSeconds
                )
                val twoDaysDecision = maintenancePhaseDecisionEvaluator.evaluateTwoDaysBefore(
                    isEligible = true,
                    isAlreadyViewed = isTwoDaysBeforeAnnouncementViewed,
                    isCurrentTimeWithinPromptWindow = nowTimeInSeconds >= phaseShowtimeInSeconds.twoDaysBeforeShowtimeInSeconds
                )
                if (twoDaysDecision.shouldMarkAsViewed) {
                    maintenanceAnnouncementsDataStore.setTwoDaysBeforeAnnouncementViewed(
                        userId,
                        phaseShowtimeInSeconds.twoDaysBeforeShowtimeInSeconds,
                        true
                    )
                }
                Timber.d("[isInMaintenancePhase] : - isTwoDaysBeforeEligible = true")
                Timber.d("[isInMaintenancePhase] : - isTwoDaysBeforeAnnouncementViewed = $isTwoDaysBeforeAnnouncementViewed")
                Timber.d("[isInMaintenancePhase] : - shouldMarkTwoDaysAsViewed = ${twoDaysDecision.shouldMarkAsViewed}")
                Timber.d("[isInMaintenancePhase] : - Is in two days before downtime = ${twoDaysDecision.shouldShow}")
                twoDaysDecision.shouldShow
            }
            MaintenancePhase.FIVE_MINUTES_BEFORE -> {
                val isCurrentTimeWithinFiveMinutesPromptWindow =
                    nowTimeInSeconds >= phaseShowtimeInSeconds.fiveMinutesBeforeShowtimeInSeconds
                val shouldShow = maintenancePhaseDecisionEvaluator.evaluateFiveMinutesBefore(
                    isEligible = true,
                    hasViewedAnnouncement = hasViewedFiveMinutesBeforeMaintenanceAnnouncement,
                    isCurrentTimeWithinPromptWindow = isCurrentTimeWithinFiveMinutesPromptWindow
                )
                Timber.d("[isInMaintenancePhase] : - hasViewedFiveMinutesBeforeMaintenanceAnnouncement = $hasViewedFiveMinutesBeforeMaintenanceAnnouncement")
                Timber.d("[isInMaintenancePhase] : - isCurrentTimeWithinFiveMinutesPromptWindow = $isCurrentTimeWithinFiveMinutesPromptWindow")
                Timber.d("[isInMaintenancePhase] : - Is in five minutes before downtime = $shouldShow")
                shouldShow
            }
            MaintenancePhase.DURING_DOWNTIME -> {
                val isInDowntime = nowTimeInSeconds >= phaseShowtimeInSeconds.duringDowntimeShowtimeInSeconds
                Timber.d("[isInMaintenancePhase] : - Is in downtime = $isInDowntime")
                isInDowntime
            }
        }
    }

    private fun resolvePhaseShowtimeInSeconds(): PhaseShowtimeInSeconds {
        return PhaseShowtimeInSeconds(
            twoDaysBeforeShowtimeInSeconds = TimeUtils.isoToUnix(maintenanceAnnouncementsResponse.en.phases.early.showtime, ZoneId.of("UTC")),
            fiveMinutesBeforeShowtimeInSeconds = TimeUtils.isoToUnix(
                maintenanceAnnouncementsResponse.en.phases.recent.showtime,
                ZoneId.of("UTC")
            ),
            duringDowntimeShowtimeInSeconds = TimeUtils.isoToUnix(maintenanceAnnouncementsResponse.en.phases.ongoing.showtime, ZoneId.of("UTC"))
        )
    }

    private fun isMaintenanceAnnouncementsResponseValid(): Boolean {
        // Currently, our app only supports the English language, so we use English content exclusively to validate responses.
        return maintenanceAnnouncementsResponse.en.timeVariablesJSONObject.length() > 0 &&
            maintenanceAnnouncementsResponse.en.variablesJSONObject.length() > 0
    }

    private fun generateConvertedMap() {
        convertedVariableMap.clear()
        val timeVariablesJson = maintenanceAnnouncementsResponse.en.timeVariablesJSONObject
        val variablesJson = maintenanceAnnouncementsResponse.en.variablesJSONObject

        // Replace time variables
        timeVariablesJson.keys().forEach { key ->
            val value = timeVariablesJson[key] as String
            convertedVariableMap[key] = formatEpochToReadableDate(TimeUtils.isoToUnix(value))
        }

        // Replace variables
        variablesJson.keys().forEach { key ->
            convertedVariableMap[key] = variablesJson[key] as String
        }

        convertedDescriptionMap.clear()
        // Replace placeholders
        convertedDescriptionMap[MaintenancePhase.TWO_DAYS_BEFORE] = replacePlaceholders(maintenanceAnnouncementsResponse.en.phases.early.content.body.joinToString(separator = "\n"))
        convertedDescriptionMap[MaintenancePhase.FIVE_MINUTES_BEFORE] = replacePlaceholders(maintenanceAnnouncementsResponse.en.phases.recent.content.body.joinToString(separator = "\n"))
        convertedDescriptionMap[MaintenancePhase.DURING_DOWNTIME] = replacePlaceholders(maintenanceAnnouncementsResponse.en.phases.ongoing.content.body.joinToString(separator = "\n"))
    }

    private fun formatEpochToReadableDate(
        epochSeconds: Long
    ): String {
        val date = Date(epochSeconds * 1000) // Convert seconds -> milliseconds
        val dateFormat = DateFormat.getDateFormat(applicationContext)
        val timeFormat = DateFormat.getTimeFormat(applicationContext)
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

    private fun replacePlaceholders(template: String): SpannableString {
        val sb = SpannableStringBuilder(template)
        convertedVariableMap.forEach { (key, value) ->
            val placeholder = "{$key}"
            val start = sb.indexOf(placeholder)

            if (start == -1) {
                Timber.d("[replacePlaceholders] : {$key} not found in template")
            } else {
                sb.replace(start, start + placeholder.length, value)
                sb.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(applicationContext, R.color.color_0A8CF0)),
                    start,
                    start + value.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return SpannableString(sb)
    }

    enum class MaintenancePhase {
        TWO_DAYS_BEFORE,
        FIVE_MINUTES_BEFORE,
        DURING_DOWNTIME
    }

    private data class PhaseShowtimeInSeconds(
        val twoDaysBeforeShowtimeInSeconds: Long,
        val fiveMinutesBeforeShowtimeInSeconds: Long,
        val duringDowntimeShowtimeInSeconds: Long
    )

}
