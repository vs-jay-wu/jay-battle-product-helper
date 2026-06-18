package com.viewsonic.classswift.ui.widgetmodel.batchquiz

import com.viewsonic.classswift.api.BatchQuizApiService
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.info.BatchQuizResultDetailInfo
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.ui.widgetmodel.IWidgetModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CSBatchQuizDetailsResultWidgetModel(
    private val apiService: BatchQuizApiService,
    private val accountManager: AccountManager,
    private val classroomManager: ClassroomManager,
    private val quizManager: QuizManager,
    private val studentManager: StudentManager
): IWidgetModel {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val _detailInfoFlow = MutableStateFlow<BatchQuizResultDetailInfo?>(null)
    val detailInfoFlow = _detailInfoFlow.asStateFlow()
    private val _updateUiEventFlow = MutableSharedFlow<BatchQuizDetailResultUiEvent>()
    val updateUiEventFlow = _updateUiEventFlow.asSharedFlow()
    private val studentList = studentManager.getCurrentList()
    private var summaryInfo: BatchQuizSummaryInfo? = null
    private var apiJob: Job? = null
    private var addPoint: Job? = null

    fun getQuizDetailInfo(info: BatchQuizSummaryInfo) {
        summaryInfo = info
        apiJob?.cancel()
        apiJob = coroutineScope.launch {
            val response = apiService.getBatchQuizResultDetailInfo(
                accountManager.getBearerToken(),
                classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId,
                info.quizId
            )
            when (response) {
                is ApiResponse.Success -> {
                    val responseData = response.data.data
                    val apiInfo = responseData.toBatchQuizResultInfo(studentList, info)
                    quizManager.setBatchAnswerResultInfos(apiInfo.studentAnswerResultList)
                    _detailInfoFlow.update { apiInfo }
                }
                else -> {
                    _updateUiEventFlow.emit(BatchQuizDetailResultUiEvent.GetDetailResultError)
                }
            }
        }
    }

    fun addCorrectStudentsPoints() {
        summaryInfo?.let {
            addPoint = coroutineScope.launch(Dispatchers.IO) {
                if(!quizManager.updateStudentsPoint(it.correctStudentIds, 1))
                {
                    _updateUiEventFlow.emit(BatchQuizDetailResultUiEvent.AddPointFailed)
                }
            }
        }
    }

    override fun onCleared() {
        apiJob?.cancel()
        addPoint?.cancel()
        _detailInfoFlow.update { null }
        summaryInfo = null
    }

    sealed class BatchQuizDetailResultUiEvent {
        data object GetDetailResultError : BatchQuizDetailResultUiEvent()
        data object AddPointFailed : BatchQuizDetailResultUiEvent()
    }
}