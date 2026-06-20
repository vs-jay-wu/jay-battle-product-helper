package com.viewsonic.classswift.ui.window.quiz.mvb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.WindowMvbQuizCollectionBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.quizcollection.mvb.MvbCollectionQuizDetailView
import com.viewsonic.classswift.ui.widget.quizcollection.mvb.MvbQuizDetailViewFactory
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

    /** Lifecycle/savedstate owner for the ComposeView body (this is not an Activity). */
    private val composeHost = ComposeWindowHost()

    /** Separate host for the folder sidemenu ComposeView (one lifecycle per ComposeView). */
    private val sidebarHost = ComposeWindowHost()

    /** Currently displayed detail view (null while in list mode). Re-created on quiz change. */
    private var detailView: MvbCollectionQuizDetailView? = null
    private var currentDetailQuizId: String? = null

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
        // Fixed size from the layout dimens — do NOT measure the view tree here. The framework
        // calls getCurrentSize() before the view is attached (and off the main thread); measuring
        // a detached ComposeView crashes with "Cannot locate windowRecomposer". The shell is a
        // fixed-size ConstraintLayout (quiz_mvb_qc_window_*) wrapped in a mvb_spacing_400 padding.
        val res = context.resources
        val padding = res.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        val width = res.getDimensionPixelSize(R.dimen.quiz_mvb_qc_window_width) + padding * 2
        val height = res.getDimensionPixelSize(R.dimen.quiz_mvb_qc_window_height) + padding * 2
        return SizeInPixels(width, height)
    }

    override fun onViewCreated() {
        super.onViewCreated()
        initHeader()
        setupComposeBody()
        observeUiState()
        windowModel.loadFolders()
    }

    private fun setupComposeBody() {
        sidebarHost.attach(binding.cvMqcwSidemenu) {
            MvbQuizCollectionSidebar(windowModel)
        }
        composeHost.attach(binding.cvMqcwComposeBody) {
            MvbQuizCollectionComposeBody(windowModel)
        }
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

    private fun observeUiState() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.uiStateFlow.collectLatest { state ->
                // Folder sidemenu is now rendered by MvbQuizCollectionSidebar (ComposeView),
                // which observes uiState directly — no adapter to feed here.
                applyFolderLoading(state.isLoadingFolders)
                binding.tvMqcwBreadcrumbCurrent.text =
                    state.folders.firstOrNull { it.folder.id == state.selectedFolderId }
                        ?.folder?.name.orEmpty()
                applyDetailMode(state.selectedQuiz)
                // Quiz body (loading / empty / error / list) is now owned by the Compose body,
                // which observes uiState + the paging flow directly.
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
        composeHost.destroy()
        sidebarHost.destroy()
        coroutineScope.cancel()
    }
}
