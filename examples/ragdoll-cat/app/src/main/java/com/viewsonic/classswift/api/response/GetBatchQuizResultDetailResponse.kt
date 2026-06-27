package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse.ShortAnswer
import com.viewsonic.classswift.api.response.UnclosedQuizResponse.QuizResults
import com.viewsonic.classswift.api.response.data.QuizAnswerData
import com.viewsonic.classswift.data.info.BatchQuizResultDetailInfo
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.utils.LanguageUtils
import kotlin.String

@JsonClass(generateAdapter = true)
data class GetBatchQuizResultDetailResponse(
    @Json(name = "data")
    val data: Data
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "source_type")
        val sourceType: String = "",
        @Json(name = "quiz_id")
        val quizId: String = "",
        @Json(name = "quiz_type")
        val quizType: String = "",
        @Json(name = "option_type")
        val optionType: String = "",
        @Json(name = "start_time")
        val startTime: Long = 0L,
        @Json(name = "end_time")
        val endTime: Long = 0L,
        @Json(name = "img_url")
        val imgUrl: String = "",
        @Json(name = "quiz_content")
        val quizContent: String = "",
        @Json(name = "quiz_results")
        val quizResults: List<QuizResults> = emptyList(),
        @Json(name = "chirp_id")
        val chirpId: String = "",
        @Json(name = "option_list")
        val optionList: List<QuizOption> = emptyList(),
        @Json(name = "ai_short_answer")
        val aiShortAnswer: String = "",
        @Json(name = "short_answer")
        val shortAnswer: ShortAnswer = ShortAnswer(),
        @Json(name = "objective_list")
        val objectiveList: List<String> = emptyList()
    ) {
        fun toBatchQuizResultInfo(studentList: List<StudentInfo>, info: BatchQuizSummaryInfo): BatchQuizResultDetailInfo {
            return BatchQuizResultDetailInfo(
                quizType = QuizType.safeValueOf(quizType, QuizType.UNSPECIFIED),
                correctStudentCount = info.correctStudentIds.size,
                inCorrectStudentCount = info.incorrectStudentIds.size,
                noAnswerStudentCount = info.noAnswerStudentIds.size,
                quizContent = quizContent,
                optionType = QuizOptionType.safeValueOf(optionType, QuizOptionType.NO_OPTION),
                optionList = optionList,
                studentAnswerResultList = getAnswerResultList(studentList, info)
            )
        }

        fun getAnswerResultList(studentList: List<StudentInfo>, summaryInfo: BatchQuizSummaryInfo): List<QuizAnswerResultInfo> {
            val resultMap = quizResults.associateBy { it.serialNumber }
            val answerInfoList = mutableListOf<QuizAnswerResultInfo>()
            val correctIdSet = summaryInfo.correctStudentIds.toSet()
            val incorrectIdSet = summaryInfo.incorrectStudentIds.toSet()
            studentList.forEach { info ->
                val answer = resultMap[info.serialNumber]
                if (answer != null) {
                    answerInfoList.add(
                        QuizAnswerResultInfo(
                            serialNumber = answer.serialNumber,
                            displaySeatNumber = answer.seatNumber,
                            displayName = answer.displayName,
                            studentId = answer.studentId,
                            answerOption = if (answer.quizAnswerData is QuizAnswerData.Numbers) answer.quizAnswerData.list else emptyList(),
                            answerStringData = if (answer.quizAnswerData is QuizAnswerData.Text) answer.quizAnswerData.content else "",
                            answerResultState =
                                if (correctIdSet.contains(info.studentId)) AnswerResultState.CORRECT
                                else if (incorrectIdSet.contains(info.studentId)) AnswerResultState.INCORRECT
                                else AnswerResultState.NO_ANSWER,
                            quizType = QuizType.safeValueOf(quizType, QuizType.UNSPECIFIED),
                            optionLanguageType = LanguageUtils.getTfOptionLanguageType(),
                            quizOptionType = QuizOptionType.safeValueOf(optionType, QuizOptionType.NO_OPTION)
                        )
                    )
                } else {
                    answerInfoList.add(
                        QuizAnswerResultInfo(
                            serialNumber = info.serialNumber,
                            displaySeatNumber = info.displaySeatNumber,
                            displayName = "",
                            studentId = "",
                            answerResultState = AnswerResultState.ABSENT,
                            quizType = QuizType.safeValueOf(quizType, QuizType.UNSPECIFIED),
                            optionLanguageType = LanguageUtils.getTfOptionLanguageType(),
                            quizOptionType = QuizOptionType.safeValueOf(optionType, QuizOptionType.NO_OPTION)
                        )
                    )
                }
            }
            return answerInfoList
        }
    }
}
