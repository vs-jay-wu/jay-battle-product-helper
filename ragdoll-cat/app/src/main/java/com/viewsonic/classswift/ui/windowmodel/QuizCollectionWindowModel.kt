package com.viewsonic.classswift.ui.windowmodel

import android.content.Context
import androidx.paging.DEBUG
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import co.touchlab.stately.collections.ConcurrentMutableList
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.AiApiService
import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.api.body.CreateQuizBody
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizSourceType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.api.response.AiSubjectDisplayNamesData
import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.pagingsource.QuizCollectionFolderQuizzesPagingSource
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.uimanager.PushRespondUiManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class QuizCollectionWindowModel(
    private val applicationContext: Context,
    private val accountManager: AccountManager,
    private val quizCollectionApiService: QuizCollectionApiService,
    private val aiApiService: AiApiService,
    private val networkManager: NetworkManager,
    private val toolbarManager: ToolbarManager,
    private val quizManager: QuizManager,
    private val batchQuizManager: BatchQuizManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider,
) : IWindowModel  {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val quizzesPagingDataFlowCache: MutableMap<String, Flow<PagingData<QuizInCollectionInfo>>> = mutableMapOf()
    private val quizzesPagingDataLoadStateCache: MutableMap<String, PagingDataLoadState> = mutableMapOf()

    private val _uiStateFlow = MutableStateFlow<UiState>(UiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    val quizzesPagingDataFlow: Flow<PagingData<QuizInCollectionInfo>> = _uiStateFlow
        .map { it.currentSelectedFolder }
        .filter { it.folder.id.isNotEmpty() }
        .flatMapLatest { folderInfo ->
            val folderId = folderInfo.folder.id
            quizzesPagingDataFlowCache[folderId] ?: createPagerFlowAndCache(folderId)
        }

    init {
        initCollection()
    }

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    override fun onCleared() {
        coroutineScope.cancel()
    }

    fun isInLesson(): Boolean = toolbarManager.toolbarUiState.value.participationState == ToolbarManager.ParticipationState.LESSON_STARTED

    suspend fun createBatchQuiz(quizDataList: List<QuizzesInCollectionFolderResponse.QuizInCollectionData>): Boolean = batchQuizManager.createBatchQuiz(quizDataList)

    suspend fun createQuiz(quizInCollectionInfo: QuizInCollectionInfo): Boolean =
        withContext(Dispatchers.IO) {
            val quizOptionList = quizInCollectionInfo.quizData.optionList.map {
                QuizOption(
                    content = it.content,
                    isAiAnswer = it.isAiAnswer,
                    isAnswer = it.isAnswer,
                    optionId = it.optionId,
                    reason = it.reason
                )
            }
            val quizOptionType: QuizOptionType =
                QuizOptionType.valueOf(quizInCollectionInfo.quizData.optionType)
            val quizSourceType: QuizSourceType =
                QuizSourceType.valueOf(quizInCollectionInfo.quizData.sourceType)

            val quizType: QuizType = QuizType.safeValueOf(quizInCollectionInfo.quizData.quizType)
            if (quizType == QuizType.UNSPECIFIED) {
                return@withContext false
            }

            val result = quizManager.createQuizCancellingOngoingIfNeeded(
                CreateQuizBody(
                    collectionId = quizInCollectionInfo.quizData.id,
                    imgUrl = quizInCollectionInfo.quizData.imgUrl,
                    optionType = quizOptionType,
                    quizType = quizType,
                    sourceType = quizSourceType,
                    quizOptionList = quizOptionList,
                    content = quizInCollectionInfo.quizData.content,
                    shortAnswer = quizInCollectionInfo.quizData.shortAnswer
                )
            )
            if (result != null) {
                QuizSharedUiInfo.screenshotImageUri = quizInCollectionInfo.quizData.imgUrl
                QuizSharedUiInfo.updateQuizType(quizType)
                QuizSharedUiInfo.quizOptionType = quizOptionType
                QuizSharedUiInfo.quizOptionCount = quizOptionList.size
                QuizSharedUiInfo.quizOptionList.clear()
                QuizSharedUiInfo.quizOptionList.addAll(quizOptionList)
                QuizSharedUiInfo.quizContent = quizInCollectionInfo.quizData.content
                quizManager.saveMultipleOptionInfos()
            }
            result != null
        }

    suspend fun hasBatchQuizzes(): Boolean = withContext(Dispatchers.IO) {
        val response = quizCollectionApiService.getQuizzesInCollectionFolder(
            token = accountManager.getBearerToken(),
            folderId = _uiStateFlow.value.currentSelectedFolder.folder.id,
            page = 1,
            perPage = 24,
            sourceTypes = listOf(QuizSourceType.IMPORT_CONTENT.name),
            quizType = "TRUE_FALSE,SINGLE_SELECT,MULTIPLE_SELECT"
        )
        when (response) {
            is ApiResponse.Success -> {
                response.data.quizDataList.isNotEmpty()
            }
            else -> {
                false
            }
        }
    }

    fun getCurrentPagingDataLoadState(): PagingDataLoadState? = quizzesPagingDataLoadStateCache[_uiStateFlow.value.currentSelectedFolder.folder.id]

    fun selectFolder(quizCollectionFolderInfo: QuizCollectionFolderInfo) {
        Timber.d("[selectFolder] : ${quizCollectionFolderInfo.folder.name}, ${quizCollectionFolderInfo.folder.id}")
        selectFolderWithId(quizCollectionFolderInfo.folder.id)
    }

    fun selectFolderWithId(folderId: String) {
        Timber.d("[selectFolderWithId] : folderId = $folderId")
        val resultList = _uiStateFlow.value.quizCollectionFolderInfoList.map {
            it.copy(isSelected = it.folder.id == folderId)
        }
        _uiStateFlow.update { it.copy(
            quizCollectionFolderInfoList = resultList,
            currentSelectedFolder = resultList.first{ folder -> folder.isSelected}
        )}
    }

    fun updateCurrentFolderLoadState(isInitialLoading: Boolean, isError: Boolean, isEmpty: Boolean) {
        _uiStateFlow.value.currentSelectedFolder.folder.id.takeIf { it.isNotEmpty() }?.let { folderId ->
            quizzesPagingDataLoadStateCache[folderId] = PagingDataLoadState(isInitialLoading, isError, isEmpty)
        }
    }

    suspend fun fetchSourceTypeMapping() = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            // This API is only used to check whether the backend has added any new quiz source types.
            // If the backend adds a new source type,
            // we should update the QuizSourceType enum accordingly to prevent the app from crashing.
            quizCollectionApiService.getSourceTypeMapping(accountManager.getBearerToken())
        }
    }

    suspend fun fetchSubjectDisplayNames() = withContext(Dispatchers.IO) {
        when (val response = aiApiService.getSubjectDisplayNames(accountManager.getBearerToken())) {
            is ApiResponse.Success -> {
                _uiStateFlow.update { it.copy(
                    aiSubjectDisplayNamesDataList = response.data.aiSubjectDisplayNamesDataList
                )}
            }
            else -> {}
        }
    }

    suspend fun fetchFolderList() = withContext(Dispatchers.IO) {
        val response = quizCollectionApiService.getQuizCollectionFolders(accountManager.getBearerToken(), accountManager.selectedOrg?.orgId ?: "")
        when (response) {
            is ApiResponse.Success -> {
                val remoteList = response.data
                    .map {
                        it.takeUnless { it.isDefault } ?: it.copy(name = applicationContext.getString(R.string.quiz_collection_default_folder))
                    }
                    .sortedByDescending { it.isDefault }
                val currentSelectedId = _uiStateFlow.value.quizCollectionFolderInfoList.firstOrNull{ it.isSelected }?.folder?.id ?: remoteList.first().id
                val resultList = remoteList.mapIndexed { index, quizCollectionFolderInfo ->
                    QuizCollectionFolderInfo(
                        folder = quizCollectionFolderInfo,
                        isSelected = currentSelectedId == quizCollectionFolderInfo.id
                    )
                }
                _uiStateFlow.update { it.copy(
                    quizCollectionFolderInfoList = resultList,
                    currentSelectedFolder = resultList.first{ folder -> folder.isSelected}
                ) }
            }
            else -> {
                _uiStateFlow.update { it.copy(quizCollectionFolderInfoList = emptyList()) }
            }
        }
    }

    private fun initCollection() {
         coroutineScope.launch {
            networkManager.delayInformNetworkAvailabilityState.collect { hasNetwork ->
                _uiStateFlow.update { it.copy(hasNetwork = hasNetwork) }
            }
        }
    }

    private fun createPagerFlowAndCache(folderId: String): Flow<PagingData<QuizInCollectionInfo>> {
        return quizzesPagingDataFlowCache.getOrPut(folderId) {
            Pager(
                config = PagingConfig(
                    initialLoadSize = 24,
                    pageSize = 24,
                ),
                pagingSourceFactory = {
                    QuizCollectionFolderQuizzesPagingSource(
                        folderId,
                        accountManager,
                        quizCollectionApiService,
                        _uiStateFlow.value.aiSubjectDisplayNamesDataList.toList()
                    )
                }
            ).flow.cachedIn(coroutineScope)
        }
    }

    data class PagingDataLoadState(
        val isInitialLoading: Boolean = false,
        val isError: Boolean = false,
        val isEmpty: Boolean = false
    )

    data class UiState(
        val quizCollectionFolderInfoList: List<QuizCollectionFolderInfo> = emptyList(),
        val currentSelectedFolder: QuizCollectionFolderInfo = QuizCollectionFolderInfo(),
        val aiSubjectDisplayNamesDataList: List<AiSubjectDisplayNamesData> = emptyList(),
        val hasNetwork: Boolean = true
    )

    enum class OngoingProcess {
        NONE,
        QUIZ,
        PUSH_AND_RESPOND
    }
}