package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.api.BatchQuizApiService
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.response.GetBatchQuizSummaryResponse
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.getValue


class BatchQuizResultWindowModel(
    private val apiService: BatchQuizApiService,
    private val accountManager: AccountManager,
    private val classroomManager: ClassroomManager,
    private val batchQuizManager: BatchQuizManager
) : IWindowModel {
    private val studentManager: StudentManager by inject(StudentManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _resultInfoFlow = MutableStateFlow<BatchQuizResultDataState>(BatchQuizResultDataState.Init)
    val resultInfoFlow = _resultInfoFlow.asStateFlow()
    private val _updateUiEventFlow = MutableSharedFlow<BatchQuizResultUiEvent>(1)
    val updateUiEventFlow = _updateUiEventFlow.asSharedFlow()
    var batchQuizSummaryResultList: List<BatchQuizSummaryInfo> = emptyList()
        private set
    private var updatedBatchQuizId: String? = null
    private val shouldSkipPointUpdate: Boolean = batchQuizManager.shouldSkipResultPointUpdateInCurrentWindow()


    fun getBatchQuizSummary() {
        coroutineScope.launch {
            val response = apiService.getBatchQuizSummary(
                accountManager.getBearerToken(),
                classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
                batchQuizManager.batchQuizzesId
            )
            when (response) {
                is ApiResponse.Success -> {
                    val summaryData = response.data.data
                    if (!shouldSkipPointUpdate && updatedBatchQuizId != summaryData.batchQuizzesId) {
                        val studentPoints = summaryData.quizzes.toBatchStudentPointMap()
                        if (studentPoints.isEmpty()) {
                            updatedBatchQuizId = summaryData.batchQuizzesId
                        } else {
                            val isSuccessful =
                                studentManager.batchUpdateStudentPoint(summaryData.lessonId, studentPoints)
                            if (isSuccessful) {
                                updatedBatchQuizId = summaryData.batchQuizzesId
                            } else {
                                Timber.w("[getBatchQuizSummary] batch update student points failed")
                            }
                        }
                    }
                    batchQuizSummaryResultList = summaryData.quizzes.map { it.toBatchQuizSummaryInfo() }
                    _resultInfoFlow.emit(BatchQuizResultDataState.ResultData(batchQuizSummaryResultList))
                }
                else -> _updateUiEventFlow.emit(BatchQuizResultUiEvent.GetResultFailed)
            }
        }
    }

    fun closeBatchQuiz() {
        coroutineScope.launch {
            _updateUiEventFlow.emit(BatchQuizResultUiEvent.EndQuizResult(batchQuizManager.closeBatchQuiz()))
        }
    }

    override fun onCleared() {
        coroutineScope.cancel()
        batchQuizSummaryResultList = emptyList()
        updatedBatchQuizId = null
    }

    private fun List<GetBatchQuizSummaryResponse.QuizSummary>.toBatchStudentPointMap(): Map<String, Int> {
        val studentPointMap = mutableMapOf<String, Int>()
        forEach { quizSummary ->
            quizSummary.correctStudentIds.forEach { studentId ->
                if (studentId.isNotEmpty()) {
                    studentPointMap[studentId] = (studentPointMap[studentId] ?: 0) + 1
                }
            }
        }
        return studentPointMap
    }

    sealed class BatchQuizResultUiEvent {
        data object Init : BatchQuizResultUiEvent()
        data object GetResultFailed : BatchQuizResultUiEvent()
        data class EndQuizResult(val result: Boolean) : BatchQuizResultUiEvent()
    }

    sealed class BatchQuizResultDataState {
        data object Init : BatchQuizResultDataState()
        data class ResultData(val data: List<BatchQuizSummaryInfo>) : BatchQuizResultDataState()
    }
}