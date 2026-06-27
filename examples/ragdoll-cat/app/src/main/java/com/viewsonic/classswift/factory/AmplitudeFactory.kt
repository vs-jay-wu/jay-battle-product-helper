package com.viewsonic.classswift.factory

import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.quiz.QuizSourceType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.utils.TimeUtils
import org.koin.java.KoinJavaComponent.inject
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AmplitudeFactory(
    private val moshiProvider: MoshiProvider
) {
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val classroomManager: ClassroomManager by inject(ClassroomManager::class.java)
    private val studentManager: StudentManager by inject(StudentManager::class.java)
    private val quizManager: QuizManager by inject(QuizManager::class.java)


    enum class EventPropertyType{
        ROOM_DATA,
        LESSON_DATA,
        QUIZ_DATA
    }

    enum class UserPropertyType{
        LOGIN_DATA,
        USER_DATA,
        CURRENT_ORG_DATA,
        ORGS_DETAIL_DATA,
    }

    fun generateEventPropertiesMap(eventPropertyType: EventPropertyType): Map<String, Any?> {
        return when (eventPropertyType) {
            EventPropertyType.ROOM_DATA -> {
                with(classroomManager.classroomDataStateFlow.value) {
                    mutableMapOf(
                        AmplitudeConstant.EventProperties.Key.ROOM_ID to selectedClassroomInfo.id,
                        AmplitudeConstant.EventProperties.Key.TEACHER_ID to accountManager.userInfo.userId,
                        AmplitudeConstant.EventProperties.Key.ROOM_NAME to selectedClassroomInfo.displayName,
                    )
                }
            }

            EventPropertyType.LESSON_DATA -> {
                val attendantList = studentManager.getCurrentAttendantList()
                with(classroomManager.classroomDataStateFlow.value) {
                    mutableMapOf(
                        AmplitudeConstant.EventProperties.Key.LESSON_ID to selectedClassroomInfo.lessonId,
                        AmplitudeConstant.EventProperties.Key.LESSON_START_TIMESTAMP to selectedClassroomInfo.lessonStartTime,
                        AmplitudeConstant.EventProperties.Key.LESSON_START_DATETIME to TimeUtils.unixToIso(
                            selectedClassroomInfo.lessonStartTime,
                            formatter = isoFormatter
                        ),
                        AmplitudeConstant.EventProperties.Key.IS_SCORE_IN_LESSON to selectedClassroomInfo.hasAddedScoreManually,
                        AmplitudeConstant.EventProperties.Key.STUDENT_DEFAULT_COUNT to selectedClassroomInfo.maxStudentCount,
                        AmplitudeConstant.EventProperties.Key.STUDENT_ATTEND_COUNT to attendantList.size,
                        AmplitudeConstant.EventProperties.Key.STUDENT_ATTEND_LIST to attendantList.toString(),
                    )
                }
            }

            EventPropertyType.QUIZ_DATA -> {
                val result = mutableMapOf(
                    AmplitudeConstant.EventProperties.Key.QUIZ_ID to quizManager.quizId,
                    AmplitudeConstant.EventProperties.Key.IMG_KEY to quizManager.quizImageKey,
                    AmplitudeConstant.EventProperties.Key.QUIZ_TYPE to QuizSharedUiInfo.quizType.name,
                    AmplitudeConstant.EventProperties.Key.QUIZ_SOURCE to QuizSourceType.MANUAL.name,
                    AmplitudeConstant.EventProperties.Key.QUIZ_STATUS to quizManager.quizStatus.name,
                    AmplitudeConstant.EventProperties.Key.QUIZ_START_TIMESTAMP to quizManager.quizStartTimeInMillis / 1000L,
                    AmplitudeConstant.EventProperties.Key.QUIZ_START_DATETIME to TimeUtils.unixToIso(
                        quizManager.quizStartTimeInMillis / 1000L,
                        formatter = isoFormatter
                    ),
                    AmplitudeConstant.EventProperties.Key.STUDENT_ANSWER to quizManager.quizzingUiState.value.answerCount,
                    AmplitudeConstant.EventProperties.Key.STUDENT_UNANSWER to (quizManager.quizzingUiState.value.attendanceCount - quizManager.quizzingUiState.value.answerCount)
                )
                if (QuizSharedUiInfo.quizType == QuizType.TRUE_FALSE ||
                    QuizSharedUiInfo.quizType == QuizType.SINGLE_SELECT ||
                    QuizSharedUiInfo.quizType == QuizType.MULTIPLE_SELECT ) {
                    result[AmplitudeConstant.EventProperties.Key.STUDENT_ANSWER_CORRECT] = quizManager.quizResultInfoList.count { it.answerResultState == AnswerResultState.CORRECT }
                }
                result
            }
        }
    }

    fun generateUserPropertiesMap(userPropertyType: UserPropertyType): Map<String, Any?> {
        return when (userPropertyType) {
            UserPropertyType.LOGIN_DATA -> {
                mutableMapOf(
                    AmplitudeConstant.UserProperties.Key.USER_ID to accountManager.userInfo.userId,
                    AmplitudeConstant.UserProperties.Key.DISPLAY_NAME to "${accountManager.userInfo.firstName} ${accountManager.userInfo.lastName}",
                    AmplitudeConstant.UserProperties.Key.CLASSSWIFT_COUNTRY to accountManager.country,
                )
            }
            UserPropertyType.USER_DATA -> {
                mutableMapOf(
                    AmplitudeConstant.UserProperties.Key.LOGIN_METHOD to accountManager.loginMethod.amplitudePropertyValue,
                )
            }
            UserPropertyType.CURRENT_ORG_DATA -> {
                with(accountManager) {
                    mutableMapOf(
                        AmplitudeConstant.UserProperties.Key.CURRENT_ORG_ID to selectedOrg?.orgId,
                        AmplitudeConstant.UserProperties.Key.CURRENT_ORG_NAME to selectedOrg?.orgDisplayName,
                        AmplitudeConstant.UserProperties.Key.CURRENT_ORG_IS_INDIVIDUAL to selectedOrg?.isIndividual,
                        AmplitudeConstant.UserProperties.Key.CURRENT_PLAN_TYPE to selectedOrg?.packageName,
                        AmplitudeConstant.UserProperties.Key.CURRENT_PLAN_END_DATE to TimeUtils.unixToIso(
                            selectedOrg?.endDate?.toLong() ?: 0L,
                            formatter = isoFormatter
                        ),
                    )
                }
            }
            UserPropertyType.ORGS_DETAIL_DATA -> {
                mutableMapOf(
                    AmplitudeConstant.UserProperties.Key.ORGS_DETAIL to moshiProvider.toJson(accountManager.organizationList, true)
                )
            }
        }
    }
}