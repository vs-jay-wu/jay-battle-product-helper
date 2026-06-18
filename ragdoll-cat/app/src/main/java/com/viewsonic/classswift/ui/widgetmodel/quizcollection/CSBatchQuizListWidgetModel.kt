package com.viewsonic.classswift.ui.widgetmodel.quizcollection

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.api.response.AiSubjectDisplayNamesData
import com.viewsonic.classswift.data.batchquiz.BatchQuizRecyclerViewUiData
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.pagingsource.BatchQuizzesPagingSource
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.quiz.QuizType.Companion.isMultipleChoice
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CSBatchQuizListWidgetModel(
    private val accountManager: AccountManager,
    private val toolbarManager: ToolbarManager,
    private val quizCollectionApiService: QuizCollectionApiService
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _uiStateFlow = MutableStateFlow<UiState>(UiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val batchQuizzesPagingDataFlow: Flow<PagingData<QuizInCollectionInfo>> = _uiStateFlow
        .map { it.folderId }
        .distinctUntilChanged()
        .filter { it.isNotEmpty() }
        .flatMapLatest { folderId ->
            Pager(
                config = PagingConfig(
                    initialLoadSize = 24,
                    pageSize = 24,
                ),
                pagingSourceFactory = {
                    BatchQuizzesPagingSource(
                        folderId,
                        accountManager,
                        quizCollectionApiService,
                        _uiStateFlow.value.aiSubjectDisplayNamesDataList
                    )
                }
            ).flow.cachedIn(coroutineScope)
        }

    val uiDataPagingFlow: Flow<PagingData<BatchQuizRecyclerViewUiData>> =
        batchQuizzesPagingDataFlow
            .map { paging ->
                paging
                    .map { quiz ->
                        BatchQuizRecyclerViewUiData.QuizInfo(
                            selectedSequenceNumber = 0,
                            selectionState = BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_NORMAL,
                            quizInCollectionInfo = quiz
                        )
                    }
                    .insertSeparators { before, after ->
                        val beforeType =
                            QuizType.safeValueOf(before?.quizInCollectionInfo?.quizData?.quizType)
                        val afterType =
                            QuizType.safeValueOf(after?.quizInCollectionInfo?.quizData?.quizType)
                        val isBothMultipleChoiceType =
                            beforeType.isMultipleChoice() && afterType.isMultipleChoice()
                        if (afterType != QuizType.UNSPECIFIED && afterType != beforeType && !isBothMultipleChoiceType) {
                            BatchQuizRecyclerViewUiData.QuizTypeHeader(afterType)
                        } else null
                    }
            }

    fun isInLesson(): Boolean = toolbarManager.toolbarUiState.value.participationState == ToolbarManager.ParticipationState.LESSON_STARTED

    fun setAiSubjectDisplayNamesDataList(list: List<AiSubjectDisplayNamesData>) {
        _uiStateFlow.update {
            it.copy(
                aiSubjectDisplayNamesDataList = list
            )
        }
    }

    fun setFolderId(folderId: String) {
        _uiStateFlow.update {
            it.copy(
                folderId = folderId
            )
        }
    }

    fun selectQuiz(adapterPosition: Int, quizInCollectionInfo: QuizInCollectionInfo) {
        _uiStateFlow.update { state ->
            val updatedList = state.selectedQuizDataList.toMutableList()
            val quizId = quizInCollectionInfo.quizData.id
            val existingData = updatedList.firstOrNull { it.quizInCollectionInfo.quizData.id == quizId }
            if (existingData != null) {
                updatedList.removeIf { it.quizInCollectionInfo.quizData.id == quizId }
                val removedSelectedSequenceNumber = existingData.selectedSequenceNumber
                updatedList
                    .mapIndexed { index, selectedQuizData -> index to selectedQuizData }
                    .filter { entry -> entry.second.selectedSequenceNumber > removedSelectedSequenceNumber }
                    .forEach { entry ->
                        updatedList[entry.first] = entry.second.copy(
                            selectedSequenceNumber = entry.second.selectedSequenceNumber - 1
                        )
                    }
            } else {
                updatedList.add(
                    SelectedQuizData(
                        adapterPosition = adapterPosition,
                        quizInCollectionInfo = quizInCollectionInfo,
                        selectedSequenceNumber = updatedList.size + 1
                    )
                )
            }
            state.copy(
                selectedQuizDataList = updatedList
            )
        }
    }

    fun selectAllQuizzes(batchQuizUiDataList: List<BatchQuizRecyclerViewUiData>) {
        _uiStateFlow.update { state ->
            val updatedList = state.selectedQuizDataList.toMutableList()
            if (updatedList.size >= MAX_SELECTABLE_QUIZ_COUNT) {
                return@update state
            }

            for (index in batchQuizUiDataList.indices) {
                val quizUiData = batchQuizUiDataList[index]
                if (quizUiData !is BatchQuizRecyclerViewUiData.QuizInfo) {
                    continue
                }

                val quizId = quizUiData.quizInCollectionInfo.quizData.id
                val isAlreadySelected =
                    updatedList.any { it.quizInCollectionInfo.quizData.id == quizId }
                if (isAlreadySelected) {
                    continue
                }

                updatedList.add(
                    SelectedQuizData(
                        adapterPosition = index,
                        quizInCollectionInfo = quizUiData.quizInCollectionInfo,
                        selectedSequenceNumber = updatedList.size + 1
                    )
                )

                if (updatedList.size >= MAX_SELECTABLE_QUIZ_COUNT) {
                    coroutineScope.launch {
                        Timber.d("[B][selectAllQuizzes] : _uiEventFlow.emit(UiEvent.ShowReachMaxSelectedQuizLimit)")
                        _uiEventFlow.emit(UiEvent.ShowReachMaxSelectedQuizLimit)
                    }
                    break
                }
            }

            state.copy(
                selectedQuizDataList = updatedList
            )
        }
    }

    fun unselectAll() {
        _uiStateFlow.update { state ->
            state.copy(
                selectedQuizDataList = emptyList()
            )
        }
    }

    data class SelectedQuizData(
        val adapterPosition: Int,
        val quizInCollectionInfo: QuizInCollectionInfo,
        val selectedSequenceNumber: Int
    )

    data class UiState(
        val folderId: String = "",
        val selectedQuizDataList: List<SelectedQuizData> = emptyList(),
        val aiSubjectDisplayNamesDataList: List<AiSubjectDisplayNamesData> = emptyList()
    )

    sealed class UiEvent {
        data object ShowReachMaxSelectedQuizLimit : UiEvent()
    }

    companion object {
        const val MIN_QUIZ_COUNT_TO_START_QUIZ = 2
        const val MAX_SELECTABLE_QUIZ_COUNT = 20
    }
}
