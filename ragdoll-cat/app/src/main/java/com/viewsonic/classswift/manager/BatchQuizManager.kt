package com.viewsonic.classswift.manager

import com.viewsonic.classswift.api.BatchQuizApiService
import com.viewsonic.classswift.api.body.CreateBatchQuizBody
import com.viewsonic.classswift.api.body.CreateQuizBody
import com.viewsonic.classswift.api.body.UpdateQuizStatusBody
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.quiz.QuizStatus
import com.viewsonic.classswift.factory.AmplitudeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BatchQuizManager(
    private val accountManager: AccountManager,
    private val classroomManager: ClassroomManager,
    private val batchQuizApiService: BatchQuizApiService
) {
    var batchQuizzesId: String = ""
        private set
    var startTimeInMillis: Long = 0
        private set

    val currentOngoingBatchQuizId: String?
        get() = batchQuizzesId.takeIf { it.isNotEmpty() }

    var amplitudeQuizDetailJsonArray: JSONArray = JSONArray()
        private set
    private var skipResultPointUpdateInCurrentWindow: Boolean = false

    fun setSkipResultPointUpdateInCurrentWindow(skip: Boolean) {
        skipResultPointUpdateInCurrentWindow = skip
    }

    fun shouldSkipResultPointUpdateInCurrentWindow(): Boolean = skipResultPointUpdateInCurrentWindow

    suspend fun createBatchQuiz(quizDataList: List<QuizzesInCollectionFolderResponse.QuizInCollectionData>): Boolean =
        withContext(Dispatchers.IO) {
            val response = batchQuizApiService.createBatchQuiz(
                token = accountManager.getBearerToken(),
                lessonId = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
                body = CreateBatchQuizBody(
                    quizzes = quizDataList.map { CreateQuizBody.fromQuizInCollectionData(it) }
                )
            )
            return@withContext when (response) {
                is ApiResponse.Success -> {
                    batchQuizzesId = response.data.data.batchQuizzesId
                    skipResultPointUpdateInCurrentWindow = false
                    amplitudeQuizDetailJsonArray = JSONArray()
                    quizDataList.forEach { quizData ->
                        amplitudeQuizDetailJsonArray.put(
                            JSONObject()
                                .put("quiz_id", quizData.id)
                                .put("quiz_type", quizData.quizType)
                        )
                    }
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.BATCH_QUIZ_START)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.STATUS, AmplitudeConstant.EventProperties.Value.SUCCESS)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.TOTAL_QUESTION_COUNT, quizDataList.size)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.BATCH_QUIZ_ID, batchQuizzesId)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.QUIZ_DETAIL_LIST, amplitudeQuizDetailJsonArray.toString())
                        .send()
                    true
                }
                else -> {
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.BATCH_QUIZ_START)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.STATUS, AmplitudeConstant.EventProperties.Value.FAILURE)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.TOTAL_QUESTION_COUNT, quizDataList.size)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.BATCH_QUIZ_ID, "")
                        .send()
                    false
                }
            }
        }

    suspend fun finishBatchQuiz(): Boolean = withContext(Dispatchers.IO) {
        val response = batchQuizApiService.updateBatchQuizStatus(
            accountManager.getBearerToken(),
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
            batchQuizzesId,
            UpdateQuizStatusBody(
                UpdateQuizStatusType.FINISH
            )
        )
        return@withContext when (response) {
            is ApiResponse.Success -> {
                true
            }
            else -> false
        }
    }

    suspend fun closeBatchQuiz(): Boolean = withContext(Dispatchers.IO) {
        val response = batchQuizApiService.updateBatchQuizStatus(
            accountManager.getBearerToken(),
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
            batchQuizzesId,
            UpdateQuizStatusBody(
                UpdateQuizStatusType.CLOSE
            )
        )
        return@withContext when (response) {
            is ApiResponse.Success -> {
                clearCurrentBatchQuizInfo()
                true
            }
            else -> false
        }
    }

    suspend fun cancelBatchQuiz() = withContext(Dispatchers.IO) {
        val response = batchQuizApiService.updateBatchQuizStatus(
            accountManager.getBearerToken(),
            classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
            batchQuizzesId,
            UpdateQuizStatusBody(
                UpdateQuizStatusType.CANCEL
            )
        )
        return@withContext when (response) {
            is ApiResponse.Success -> {
                clearCurrentBatchQuizInfo()
                true
            }
            else -> false
        }
    }

    private fun clearCurrentBatchQuizInfo() {
        batchQuizzesId = ""
        startTimeInMillis = 0
        skipResultPointUpdateInCurrentWindow = false
    }
}