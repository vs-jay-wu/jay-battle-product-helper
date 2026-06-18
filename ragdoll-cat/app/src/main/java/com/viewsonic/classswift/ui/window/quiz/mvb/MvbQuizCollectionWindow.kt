package com.viewsonic.classswift.ui.window.quiz.mvb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.WindowMvbQuizCollectionBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.quizcollection.mvb.MvbCollectionQuizDetailView
import com.viewsonic.classswift.ui.widget.quizcollection.mvb.MvbQuizDetailViewFactory
import com.viewsonic.classswift.ui.window.adapter.MvbQuizCollectionFolderListAdapter
import com.viewsonic.classswift.ui.window.adapter.MvbQuizCollectionQuizzesAdapter
import com.viewsonic.classswift.ui.window.adapter.MvbQuizCollectionQuizzesLoadStateAdapter
import com.viewsonic.classswift.ui.windowmodel.MvbQuizCollectionWindowModel
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

class MvbQuizCollectionWindow(
    private val context: Context,
) : IWindow<WindowMvbQuizCollectionBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val windowModel: MvbQuizCollectionWindowModel by inject(MvbQuizCollectionWindowModel::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private lateinit var folderAdapter: MvbQuizCollectionFolderListAdapter
    private lateinit var quizzesAdapter: MvbQuizCollectionQuizzesAdapter

    /** Currently displayed detail view (null while in list mode). Re-created on quiz change. */
    private var detailView: MvbCollectionQuizDetailView? = null
    private var currentDetailQuizId: String? = null

    /** Last known refresh state — used by `addOnPagesUpdatedListener` to decide visibility. */
    private var lastRefreshState: LoadState = LoadState.NotLoading(endOfPaginationReached = false)

    override var tag: WindowTag = WindowTag.WINDOW_MVB_QUIZ_COLLECTION
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
    )

    override val binding: WindowMvbQuizCollectionBinding =
        WindowMvbQuizCollectionBinding.inflate(
            LayoutInflater.from(
                ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents),
            ),
        )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onViewCreated() {
        super.onViewCreated()
        initHeader()
        initFolderList()
        initQuizList()
        initErrorState()
        observeUiState()
        observeQuizzesPagingData()
        windowModel.loadFolders()
    }

    private fun initHeader() {
        WindowControlButtonsUiHelper.setup(
            ivClose = binding.ivClose,
            ivMinimizeWindow = binding.ivMinimizeWindow,
            ivToolbarBringToFront = binding.ivToolbarBringToFront,
            windowTag = tag,
            isMvbBound = windowModel.isMyViewBoardBound(),
            csWindowManager = csWindowManager,
            coroutineScope = coroutineScope,
            onCloseClick = { csWindowManager.removeWindow(tag) },
        )
    }

    private fun initFolderList() {
        folderAdapter = MvbQuizCollectionFolderListAdapter(
            onFolderClick = { folderInfo -> windowModel.selectFolder(folderInfo.folder.id) },
            onYourFoldersHeaderClick = { windowModel.toggleYourFoldersExpanded() },
        )
        binding.rvMqcwFolders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = folderAdapter
        }
    }

    private fun initQuizList() {
        quizzesAdapter = MvbQuizCollectionQuizzesAdapter(
            canUseStandards = windowModel.canUseStandards,
            onQuizClick = { info -> windowModel.selectQuiz(info) },
        )
        val footerAdapter = MvbQuizCollectionQuizzesLoadStateAdapter()
        val concatAdapter = quizzesAdapter.withLoadStateFooter(footerAdapter)
        val gridLayoutManager = GridLayoutManager(context, QUIZ_GRID_SPAN)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val isFooter = position == quizzesAdapter.itemCount && footerAdapter.itemCount > 0
                return if (isFooter) QUIZ_GRID_SPAN else 1
            }
        }
        binding.rvMqcwQuizzes.apply {
            layoutManager = gridLayoutManager
            adapter = concatAdapter
        }
    }

    private fun initErrorState() {
        binding.cvMqcwErrorState.onRefreshClick = {
            windowModel.refreshAfterError()
        }
    }

    private fun observeUiState() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.uiStateFlow.collectLatest { state ->
                folderAdapter.submitList(
                    MvbQuizCollectionFolderListAdapter.buildItems(
                        folders = state.folders,
                        isYourFoldersExpanded = state.isYourFoldersExpanded,
                    ),
                )
                applyFolderLoading(state.isLoadingFolders)
                binding.tvMqcwBreadcrumbCurrent.text =
                    state.folders.firstOrNull { it.folder.id == state.selectedFolderId }
                        ?.folder?.name.orEmpty()
                applyDetailMode(state.selectedQuiz)
                when {
                    state.folderLoadFailed -> applyBodyVisibility(
                        showError = true,
                        showEmpty = false,
                        showQuizzes = false,
                    )
                    state.isLoadingFolders -> applyBodyVisibility(
                        showError = false,
                        showEmpty = false,
                        showQuizzes = false,
                        showLoading = true,
                    )
                    // Otherwise leave the body driven by the paging load state observer.
                    else -> Unit
                }
            }
        }
    }

    private fun applyDetailMode(info: QuizInCollectionInfo?) {
        if (info == null) {
            if (currentDetailQuizId != null) {
                binding.cvMqcwDetail.removeAllViews()
                binding.cvMqcwDetail.visibility = View.GONE
                detailView = null
                currentDetailQuizId = null
            }
            return
        }
        if (info.quizData.id == currentDetailQuizId) {
            return
        }
        binding.cvMqcwDetail.removeAllViews()
        val quizType = QuizType.safeValueOf(info.quizData.quizType)
        val view = MvbQuizDetailViewFactory.createDetailView(context, quizType, info.isTextQuiz())
        view.bind(info)
        view.setOnBackClicked { windowModel.goBackToList() }
        view.setOnStartClicked { handleStartClick(view) }
        binding.cvMqcwDetail.addView(view)
        binding.cvMqcwDetail.visibility = View.VISIBLE
        detailView = view
        currentDetailQuizId = info.quizData.id
    }

    private fun applyFolderLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.laMqcwFoldersLoading.visibility = View.VISIBLE
            binding.laMqcwFoldersLoading.playAnimation()
        } else {
            binding.laMqcwFoldersLoading.cancelAnimation()
            binding.laMqcwFoldersLoading.visibility = View.GONE
        }
    }

    private fun observeQuizzesPagingData() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.quizzesPagingDataFlow.collectLatest { pagingData ->
                quizzesAdapter.submitData(pagingData)
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            quizzesAdapter.loadStateFlow.collectLatest { loadState ->
                lastRefreshState = loadState.refresh
                applyLoadState(refreshState = loadState.refresh, appendState = loadState.append)
                // Re-attempt empty/quizzes visibility — pagesUpdated may have already fired while
                // we were still in Loading and short-circuited. Now that we're NotLoading, retry.
                applyEmptyOrQuizzesVisibility()
            }
        }
        // Empty/quizzes visibility must wait until pages are committed to the adapter — checking
        // itemCount inside loadStateFlow's NotLoading branch races with submitData's diff and can
        // see the previous folder's stale count (showing empty state for a non-empty folder).
        // addOnPagesUpdatedListener fires AFTER the adapter commits, so itemCount is reliable here.
        // Both signals call applyEmptyOrQuizzesVisibility(); whichever arrives later wins.
        quizzesAdapter.addOnPagesUpdatedListener {
            applyEmptyOrQuizzesVisibility()
        }
    }

    private fun applyLoadState(refreshState: LoadState, appendState: LoadState) {
        if (windowModel.uiStateFlow.value.folderLoadFailed) return
        // Any API failure (initial refresh OR pagination append) escalates to the full error state.
        if (refreshState is LoadState.Error || appendState is LoadState.Error) {
            applyBodyVisibility(showError = true, showEmpty = false, showQuizzes = false)
            return
        }
        when (refreshState) {
            is LoadState.Loading -> applyBodyVisibility(
                showError = false,
                showEmpty = false,
                showQuizzes = false,
                showLoading = true,
            )
            // NotLoading: defer to addOnPagesUpdatedListener so itemCount reflects committed pages.
            is LoadState.NotLoading -> Unit
            is LoadState.Error -> Unit // Already handled above.
        }
    }

    private fun applyEmptyOrQuizzesVisibility() {
        if (windowModel.uiStateFlow.value.folderLoadFailed) return
        // Skip while still loading or in error — those paths are owned by applyLoadState.
        if (lastRefreshState !is LoadState.NotLoading) return
        val isEmpty = quizzesAdapter.itemCount == 0
        applyBodyVisibility(
            showError = false,
            showEmpty = isEmpty,
            showQuizzes = !isEmpty,
        )
    }

    private fun applyBodyVisibility(
        showError: Boolean,
        showEmpty: Boolean,
        showQuizzes: Boolean,
        showLoading: Boolean = false,
    ) {
        binding.rvMqcwQuizzes.visibility = if (showQuizzes) View.VISIBLE else View.GONE
        binding.cvMqcwEmptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.cvMqcwErrorState.visibility = if (showError) View.VISIBLE else View.GONE
        binding.cvMqcwLoadingState.visibility = if (showLoading) View.VISIBLE else View.GONE
    }

    private fun handleStartClick(view: MvbCollectionQuizDetailView) {
        view.setStartButtonLoading()
        coroutineScope.launch(Dispatchers.Main) {
            when (val result = windowModel.dispatchSelectedQuiz()) {
                is MvbQuizCollectionWindowModel.DispatchResult.Success -> openQuizWindow(result.quiz)
                MvbQuizCollectionWindowModel.DispatchResult.SystemError -> {
                    showDispatchToast(R.string.mvb_qc_detail_toast_start_failed)
                    view.setStartButtonEnabled(true)
                }
                MvbQuizCollectionWindowModel.DispatchResult.OngoingConflict -> {
                    showDispatchToast(R.string.mvb_qc_detail_toast_ongoing_conflict)
                    view.setStartButtonEnabled(true)
                }
                MvbQuizCollectionWindowModel.DispatchResult.CancelOngoingAndRetry -> {
                    val cancelled = windowModel.cancelOngoingQuiz()
                    if (cancelled) {
                        when (val retry = windowModel.dispatchSelectedQuiz()) {
                            is MvbQuizCollectionWindowModel.DispatchResult.Success -> openQuizWindow(retry.quiz)
                            else -> {
                                showDispatchToast(R.string.mvb_qc_detail_toast_start_failed)
                                view.setStartButtonEnabled(true)
                            }
                        }
                    } else {
                        showDispatchToast(R.string.mvb_qc_detail_toast_start_failed)
                        view.setStartButtonEnabled(true)
                    }
                }
            }
        }
    }

    private fun openQuizWindow(quiz: QuizInCollectionInfo) {
        csWindowManager.removeWindow(tag)
        val startWindowClass = MvbQuizDetailViewFactory.resolveStartWindow(quiz)
        @Suppress("UNCHECKED_CAST")
        val startWindow = KoinJavaComponent.get<Any>(startWindowClass) as IWindow<ViewBinding>
        csWindowManager.createWindow(startWindow, Gravity.CENTER)
    }

    private fun showDispatchToast(messageRes: Int) {
        with(binding.mtMqcwToast) {
            setText(context.getString(messageRes))
            show(coroutineScope)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    companion object {
        private const val QUIZ_GRID_SPAN = 4
    }
}
