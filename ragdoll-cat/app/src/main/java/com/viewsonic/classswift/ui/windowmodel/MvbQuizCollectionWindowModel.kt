package com.viewsonic.classswift.ui.windowmodel

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.api.body.CreateQuizBody
import com.viewsonic.classswift.api.response.data.QuizCollectionFolder
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.pagingsource.QuizCollectionFolderQuizzesPagingSource
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizSourceType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.uimanager.QuizUiManager
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MvbQuizCollectionWindowModel(
    private val applicationContext: Context,
    private val accountManager: AccountManager,
    private val quizCollectionApiService: QuizCollectionApiService,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider,
    private val quizManager: QuizManager,
    private val batchQuizManager: BatchQuizManager,
    private val quizUiManager: QuizUiManager,
    private val unclosedMissionUiManager: UnclosedMissionUiManager,
) : IWindowModel {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val quizzesPagingDataFlowCache: MutableMap<String, Flow<PagingData<QuizInCollectionInfo>>> = mutableMapOf()

    // canUseStandards is intentionally snapshotted at construction time per delta-spec.md AC-4
    // (panel ignores org-capability changes while open; next open re-reads).
    private val _uiStateFlow = MutableStateFlow(UiState(canUseStandards = accountManager.canUseStandards))
    val uiStateFlow = _uiStateFlow.asStateFlow()

    val quizzesPagingDataFlow: Flow<PagingData<QuizInCollectionInfo>> = _uiStateFlow
        .map { it.selectedFolderId }
        .filter { !it.isNullOrEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { folderId ->
            quizzesPagingDataFlowCache[folderId!!] ?: createPagerFlowAndCache(folderId)
        }

    val canUseStandards: Boolean
        get() = _uiStateFlow.value.canUseStandards

    fun isMyViewBoardBound(): Boolean = myViewBoardConnectionStateProvider.isBound()

    override fun onCleared() {
        coroutineScope.cancel()
    }

    fun loadFolders() {
        _uiStateFlow.update { it.copy(isLoadingFolders = true, folderLoadFailed = false) }
        coroutineScope.launch {
            val response = withContext(Dispatchers.IO) {
                quizCollectionApiService.getQuizCollectionFolders(
                    token = accountManager.getBearerToken(),
                    orgId = accountManager.selectedOrg?.orgId.orEmpty(),
                )
            }
            when (response) {
                is ApiResponse.Success -> applyFoldersResponse(response.data)
                else -> _uiStateFlow.update {
                    it.copy(isLoadingFolders = false, folderLoadFailed = true)
                }
            }
        }
    }

    fun selectFolder(folderId: String) {
        if (_uiStateFlow.value.selectedFolderId == folderId) return
        _uiStateFlow.update { state ->
            state.copy(
                selectedFolderId = folderId,
                folders = state.folders.map { it.copy(isSelected = it.folder.id == folderId) },
            )
        }
    }

    fun selectQuiz(info: QuizInCollectionInfo) {
        _uiStateFlow.update { it.copy(selectedQuiz = info) }
    }

    fun goBackToList() {
        _uiStateFlow.update { it.copy(selectedQuiz = null) }
    }

    fun toggleYourFoldersExpanded() {
        _uiStateFlow.update { it.copy(isYourFoldersExpanded = !it.isYourFoldersExpanded) }
    }

    /**
     * Dispatches the currently selected quiz to students. Mirrors the standalone
     * `QuizCollectionWindowModel.createQuiz` conversion but adds an ongoing-mission pre-check
     * via [UnclosedMissionUiManager.getUnclosedMissions] (covers sketch quizzing without a legacy
     * quizId). Batch-quiz or window-open conflicts return [DispatchResult.OngoingConflict];
     * an ongoing quiz with no open window returns [DispatchResult.CancelOngoingAndRetry] so the
     * caller can clear the orphan and retry.
     */
    suspend fun dispatchSelectedQuiz(): DispatchResult = withContext(Dispatchers.IO) {
        val quiz = _uiStateFlow.value.selectedQuiz ?: return@withContext DispatchResult.SystemError
        val unclosedMissions = unclosedMissionUiManager.getUnclosedMissions()
        val hasBatchOngoing = MissionType.BATCH_QUIZZES in unclosedMissions
        val hasQuizOngoing = MissionType.QUIZ in unclosedMissions
        if (hasBatchOngoing || hasQuizOngoing) {
            val windowOpen = quizUiManager.getCurrentOpenedQuizWindowTag() != WindowTag.NONE
            return@withContext if (hasBatchOngoing || windowOpen) {
                DispatchResult.OngoingConflict
            } else {
                DispatchResult.CancelOngoingAndRetry
            }
        }

        val quizType = QuizType.safeValueOf(quiz.quizData.quizType)
        if (quizType == QuizType.UNSPECIFIED) {
            return@withContext DispatchResult.SystemError
        }
        val quizOptionList = quiz.quizData.optionList.map {
            QuizOption(
                content = it.content,
                isAiAnswer = it.isAiAnswer,
                isAnswer = it.isAnswer,
                optionId = it.optionId,
                reason = it.reason,
            )
        }
        // valueOf throws on unknown server-side enum values; treat as SystemError instead of crashing.
        val quizOptionType = runCatching { QuizOptionType.valueOf(quiz.quizData.optionType) }
            .getOrElse {
                Timber.e(it, "[dispatch] unknown optionType '${quiz.quizData.optionType}'")
                return@withContext DispatchResult.SystemError
            }
        val quizSourceType = runCatching { QuizSourceType.valueOf(quiz.quizData.sourceType) }
            .getOrElse {
                Timber.e(it, "[dispatch] unknown sourceType '${quiz.quizData.sourceType}'")
                return@withContext DispatchResult.SystemError
            }

        _uiStateFlow.update { it.copy(isDispatching = true) }
        val response = try {
            quizManager.createQuizCancellingOngoingIfNeeded(
                CreateQuizBody(
                    collectionId = quiz.quizData.id,
                    imgUrl = quiz.quizData.imgUrl,
                    optionType = quizOptionType,
                    quizType = quizType,
                    sourceType = quizSourceType,
                    quizOptionList = quizOptionList,
                    content = quiz.quizData.content,
                    shortAnswer = quiz.quizData.shortAnswer,
                ),
            )
        } finally {
            _uiStateFlow.update { it.copy(isDispatching = false) }
        }

        if (response == null) {
            return@withContext DispatchResult.SystemError
        }
        QuizSharedUiInfo.screenshotImageUri = quiz.quizData.imgUrl
        QuizSharedUiInfo.updateQuizType(quizType)
        QuizSharedUiInfo.quizOptionType = quizOptionType
        QuizSharedUiInfo.quizOptionCount = quizOptionList.size
        QuizSharedUiInfo.quizOptionList.clear()
        QuizSharedUiInfo.quizOptionList.addAll(quizOptionList)
        QuizSharedUiInfo.quizContent = quiz.quizData.content
        quizManager.saveMultipleOptionInfos()
        DispatchResult.Success(quiz)
    }

    sealed class DispatchResult {
        data class Success(val quiz: QuizInCollectionInfo) : DispatchResult()
        object SystemError : DispatchResult()
        object OngoingConflict : DispatchResult()
        object CancelOngoingAndRetry : DispatchResult()
    }

    suspend fun cancelOngoingQuiz(): Boolean = withContext(Dispatchers.IO) {
        quizManager.updateQuizStatus(UpdateQuizStatusType.CANCEL) != null
    }

    fun refreshAfterError() {
        if (_uiStateFlow.value.folderLoadFailed || _uiStateFlow.value.folders.isEmpty()) {
            loadFolders()
            return
        }
        _uiStateFlow.value.selectedFolderId?.let { folderId ->
            quizzesPagingDataFlowCache.remove(folderId)
            // Re-emit the folderId by toggling: clear then re-set so flatMapLatest creates a new Pager.
            _uiStateFlow.update { it.copy(selectedFolderId = null) }
            _uiStateFlow.update { it.copy(selectedFolderId = folderId) }
        }
    }

    private fun applyFoldersResponse(remote: List<QuizCollectionFolder>) {
        if (remote.isEmpty()) {
            _uiStateFlow.update {
                it.copy(isLoadingFolders = false, folderLoadFailed = true)
            }
            return
        }
        val defaultFolder = findDefaultFolder(remote)
        val folderInfos = remote.map { folder ->
            val displayed = if (folder.isDefault) {
                folder.copy(name = applicationContext.getString(R.string.mvb_qc_folder_default))
            } else {
                folder
            }
            QuizCollectionFolderInfo(
                folder = displayed,
                isSelected = displayed.id == defaultFolder.id,
            )
        }.sortedByDescending { it.folder.isDefault }
        _uiStateFlow.update {
            it.copy(
                folders = folderInfos,
                selectedFolderId = defaultFolder.id,
                isLoadingFolders = false,
                folderLoadFailed = false,
            )
        }
    }

    private fun findDefaultFolder(folders: List<QuizCollectionFolder>): QuizCollectionFolder {
        folders.firstOrNull { it.isDefault }?.let { return it }
        folders.firstOrNull { it.name.equals(DEFAULT_FOLDER_NAME, ignoreCase = true) }?.let { return it }
        Timber.w("[MvbQuizCollectionWindowModel] no default folder flag, falling back to first item")
        return folders.first()
    }

    private fun createPagerFlowAndCache(folderId: String): Flow<PagingData<QuizInCollectionInfo>> {
        return quizzesPagingDataFlowCache.getOrPut(folderId) {
            Pager(
                config = PagingConfig(
                    initialLoadSize = PAGE_SIZE,
                    pageSize = PAGE_SIZE,
                    // Default prefetchDistance == pageSize (24) — causes append.Loading to fire
                    // immediately after initial 24-item load (user at item 0 is exactly 24 from end =
                    // threshold), making the paging footer appear before any user scroll. Lower
                    // to 6 so prefetch only triggers when user is ~1-2 rows from the bottom.
                    prefetchDistance = 6,
                ),
                pagingSourceFactory = {
                    QuizCollectionFolderQuizzesPagingSource(
                        folderId = folderId,
                        accountManager = accountManager,
                        quizCollectionApiService = quizCollectionApiService,
                        aiSubjectDisplayNamesDataList = emptyList(),
                    )
                },
            ).flow.cachedIn(coroutineScope)
        }
    }

    data class UiState(
        val folders: List<QuizCollectionFolderInfo> = emptyList(),
        val selectedFolderId: String? = null,
        val isLoadingFolders: Boolean = false,
        val folderLoadFailed: Boolean = false,
        val canUseStandards: Boolean = false,
        val selectedQuiz: QuizInCollectionInfo? = null,
        val isDispatching: Boolean = false,
        val isYourFoldersExpanded: Boolean = true,
    )

    companion object {
        private const val PAGE_SIZE = 24
        private const val DEFAULT_FOLDER_NAME = "default"
    }
}
