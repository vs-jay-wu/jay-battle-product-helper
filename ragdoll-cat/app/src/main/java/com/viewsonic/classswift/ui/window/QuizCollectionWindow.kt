package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.databinding.WindowQuizCollectionBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quizcollection.CSBatchQuizListWidget
import com.viewsonic.classswift.ui.widget.quizcollection.CSCreateQuizCollectionFolderWidget
import com.viewsonic.classswift.ui.window.adapter.QuizCollectionFolderListAdapter
import com.viewsonic.classswift.ui.window.adapter.QuizCollectionQuizzesAdapter
import com.viewsonic.classswift.ui.window.quiz.start.AudioQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.BatchQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.MultipleChoiceStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.PollQuizStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.ShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TextMultipleChoiceStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TextShortAnswerStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TextTrueFalseStartWindow
import com.viewsonic.classswift.ui.window.quiz.start.TrueFalseStartWindow
import com.viewsonic.classswift.ui.windowmodel.QuizCollectionWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.mapAndCollect
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.utils.extension.showEndOngoingMissionMessageDialog
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class QuizCollectionWindow(private val applicationContext: Context) : IWindow<WindowQuizCollectionBinding>,
    QuizCollectionFolderListAdapter.OnItemInteractionListener,
    QuizCollectionQuizzesAdapter.OnItemInteractionListener,
    CSCreateQuizCollectionFolderWidget.Listener,
    CSBatchQuizListWidget.Callback {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private val quizCollectionWindowModel: QuizCollectionWindowModel by inject(QuizCollectionWindowModel::class.java)
    private val quizCollectionFolderListAdapter: QuizCollectionFolderListAdapter = QuizCollectionFolderListAdapter(this)
    private val quizCollectionQuizzesAdapter: QuizCollectionQuizzesAdapter = QuizCollectionQuizzesAdapter(this)
    private var currentQuizInCollectionInfo: QuizInCollectionInfo? = null
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider by inject(MyViewBoardConnectionStateProvider::class.java)
    private var currentPage: Page = Page.List

    override var tag: WindowTag = WindowTag.WINDOW_QUIZ_COLLECTION
    override var size: SizeInPixels = SizeInPixels(933f.dpToPx().toInt(), 529f.dpToPx().toInt())

    override val binding: WindowQuizCollectionBinding = WindowQuizCollectionBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                applicationContext,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(933f.dpToPx().toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(529f.dpToPx().toInt(), View.MeasureSpec.EXACTLY)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {}

    override fun onViewCreated() {
        initView()
        initCollection()
        initData()
    }

    override fun onDestroy() {
        quizCollectionWindowModel.onCleared()
        with(binding) {
            csctfcqdvShortAnswerTextQuizDetail.release()
            csctfcqdvTrueFalseTextQuizDetail.release()
            csctfcqdvMultipleChoiceTextQuizDetail.release()
            cswBatchQuizList.release()
        }
    }

    override fun onNormalItemClicked(quizCollectionFolderInfo: QuizCollectionFolderInfo) {
        coroutineScope.launch(Dispatchers.Main) {
            if (currentPage is Page.BatchQuiz) {
                with(binding.cswMessageDialog) {
                    setTitle(applicationContext.getString(R.string.quiz_collection_folder_switch_dialog_title))
                    setMessage(applicationContext.getString(R.string.quiz_collection_folder_switch_dialog_content))
                    setPositiveButtonText(applicationContext.getString(R.string.quiz_collection_folder_switch_dialog_confirm))
                    setNegativeButtonText(applicationContext.getString(R.string.quiz_collection_folder_switch_dialog_cancel))
                    setButtonClickListeners(
                        onPositive = {
                            binding.cswBatchQuizList.clear()
                            switchPage(Page.List)
                            binding.cslbEnterBatchQuiz.setDisable()
                            quizCollectionWindowModel.selectFolder(quizCollectionFolderInfo)
                            dismiss()
                        },
                        onNegative = {
                            dismiss()
                        }
                    )
                    setMaskClickedListener {
                        coroutineScope.launch(Dispatchers.Main) {
                            csWindowManager.bringWindowToTop(windowTag = this@QuizCollectionWindow.tag)
                        }
                    }
                    show()
                }
            } else {
                binding.cslbEnterBatchQuiz.setDisable()
                quizCollectionWindowModel.selectFolder(quizCollectionFolderInfo)
            }
        }
    }

    override fun onQuizInfoItemClicked(quizInCollectionInfo: QuizInCollectionInfo) {
        switchPage(Page.Detail(quizInCollectionInfo))
    }

    override fun onCreateFolderSuccess(folderId: String) {
        binding.cscqcfwCreateFolder.hide()
        showSuccessToast(applicationContext.getString(R.string.quiz_collection_folder_create_toast_success))
        coroutineScope.launch {
            quizCollectionWindowModel.fetchFolderList()
            quizCollectionWindowModel.selectFolderWithId(folderId)
        }
    }

    override fun onCreateFolderFailed() {
        binding.cscqcfwCreateFolder.hide()
        showErrorToast(applicationContext.getString(R.string.quiz_collection_folder_create_toast_error))
    }

    override fun onCancelled() {
        binding.cscqcfwCreateFolder.hide()
    }

    override fun requestShowErrorToast(message: String) = showErrorToast(message)

    override fun startBatchQuizzesFlow() {
        coroutineScope.launch(Dispatchers.Main) {
            val unclosedMissions = unclosedMissionUiManager.getUnclosedMissions()
            if (unclosedMissions.isEmpty()) {
                createBatchQuizzes()
                return@launch
            }

            val unclosedSet = unclosedMissions.toSet()
            val hasQuiz = MissionType.QUIZ in unclosedSet
            val hasBatchQuizzes = MissionType.BATCH_QUIZZES in unclosedSet
            val hasPushAndRespond = MissionType.PUSH_AND_RESPOND_TASK in unclosedSet

            binding.cswMessageDialog.showEndOngoingMissionMessageDialog(
                coroutineScope,
                MissionType.BATCH_QUIZZES,
                onPositiveClicked = {
                    if (!hasQuiz && !hasBatchQuizzes) {
                        createBatchQuizzes(hasPushAndRespond)
                        return@showEndOngoingMissionMessageDialog
                    }
                    val isQuizClosed = !hasQuiz || unclosedMissionUiManager.closeMission(MissionType.QUIZ)
                    val isBatchQuizzesClosed = !hasBatchQuizzes || unclosedMissionUiManager.closeMission(MissionType.BATCH_QUIZZES)
                    if (isQuizClosed && isBatchQuizzesClosed) {
                        createBatchQuizzes(hasPushAndRespond)
                    } else {
                        showErrorToast(applicationContext.getString(R.string.batch_quiz_error_failed_to_start_batch_quiz))
                        refreshStartQuizButtonState()
                    }
                },
                onNegativeClicked = {
                    refreshStartQuizButtonState()
                },
                onMaskClicked = {
                    csWindowManager.bringWindowToTop(windowTag = this@QuizCollectionWindow.tag)
                }
            )
        }
    }

    fun refreshStartQuizButtonState() {
        binding.cswBatchQuizList.refreshStartQuizButtonState()
        with(binding.cslbStartQuiz) {
            when (quizCollectionWindowModel.isInLesson()) {
                true -> {
                    setEnableText(applicationContext.getString(R.string.common_start_quiz))
                    setEnable()
                }
                false -> {
                    setEnableText(applicationContext.getString(R.string.quiz_collection_preset_lesson_not_started))
                    setDisable()
                }
            }
        }
    }

    private fun initView() {
        with(binding) {
            cswBatchQuizList.setCallback(this@QuizCollectionWindow)

            switchPage(Page.List)

            // Global
            viewNetworkDisconnect.bindCloseAction(ivClose)
            WindowControlButtonsUiHelper.setup(
                ivClose = ivClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = quizCollectionWindowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = { csWindowManager.hideWindow(tag, isRecordHiddenState = true) }
            )

            ibLightBulb.setOnClickListener {
                ibLightBulb.isSelected = !ibLightBulb.isSelected
                currentQuizInCollectionInfo?.let { info ->
                    val quizType = QuizType.entries.find { it.name == info.quizData.quizType } ?: QuizType.UNSPECIFIED
                    if (info.isTextQuiz()) {
                        when (quizType) {
                            QuizType.TRUE_FALSE -> {
                                csctfcqdvTrueFalseTextQuizDetail.updateView(info,ibLightBulb.isSelected)
                            }
                            QuizType.SINGLE_SELECT,
                            QuizType.MULTIPLE_SELECT -> {
                                csctfcqdvMultipleChoiceTextQuizDetail.updateView(info, ibLightBulb.isSelected)
                            }
                            QuizType.SHORT_ANSWER -> {
                                csctfcqdvShortAnswerTextQuizDetail.updateView(info, ibLightBulb.isSelected)
                            }
                            else -> {}
                        }
                    }
                }
            }

            viewNetworkDisconnect.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }

            // Create Folder
            cscqcfwCreateFolder.setListener(this@QuizCollectionWindow)

            // Folder List Panel
            cslbCreateFolder.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cscqcfwCreateFolder.show()
                }
            })

            cslbRefresh.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    quizCollectionQuizzesAdapter.retry()
                    cslbRefresh.setLoading()
                }
            })

            rvFolder.apply {
                layoutManager = LinearLayoutManager(root.context)
                adapter = quizCollectionFolderListAdapter
            }

            rvCollectionQuizzes.apply {
                layoutManager = GridLayoutManager(root.context, 4)
                adapter = quizCollectionQuizzesAdapter
            }

            // Quiz Detail Panel
            llBackArea.setOnClickListener {
                switchPage(Page.List)
            }

            cslbStartQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbStartQuiz.setLoading()
                    coroutineScope.launch(Dispatchers.Main) {
                        val unclosedMissions = unclosedMissionUiManager.getUnclosedMissions()
                        if (unclosedMissions.isEmpty()) {
                            createQuiz()
                            return@launch
                        }

                        val unclosedSet = unclosedMissions.toSet()
                        val hasQuiz = MissionType.QUIZ in unclosedSet
                        val hasBatchQuizzes = MissionType.BATCH_QUIZZES in unclosedSet
                        val hasPushAndRespond = MissionType.PUSH_AND_RESPOND_TASK in unclosedSet

                        binding.cswMessageDialog.showEndOngoingMissionMessageDialog(
                            coroutineScope,
                            MissionType.QUIZ,
                            onPositiveClicked = {
                                if (!hasQuiz && !hasBatchQuizzes) {
                                    createQuiz(hasPushAndRespond)
                                    return@showEndOngoingMissionMessageDialog
                                }
                                val isQuizClosed = !hasQuiz || unclosedMissionUiManager.closeMission(MissionType.QUIZ)
                                val isBatchQuizzesClosed = !hasBatchQuizzes || unclosedMissionUiManager.closeMission(MissionType.BATCH_QUIZZES)
                                if (isQuizClosed && isBatchQuizzesClosed) {
                                    createQuiz(hasPushAndRespond)
                                } else {
                                    showErrorToast(applicationContext.getString(R.string.quiz_error_msg_ongoing_quiz))
                                    refreshStartQuizButtonState()
                                }
                            },
                            onNegativeClicked = {
                                refreshStartQuizButtonState()
                            },
                            onMaskClicked = {
                                csWindowManager.bringWindowToTop(windowTag = this@QuizCollectionWindow.tag)
                            }
                        )
                    }
                }
            })

            // Batch Quiz
            cslbEnterBatchQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbEnterBatchQuiz.setLoading()
                    coroutineScope.launch(Dispatchers.Main) {
                        when (quizCollectionWindowModel.hasBatchQuizzes()) {
                            true -> {
                                switchPage(Page.BatchQuiz(quizCollectionWindowModel.uiStateFlow.value.currentSelectedFolder))
                            }
                            false -> {
                                showErrorToast(applicationContext.getString(R.string.batch_quiz_error_no_batch_quizzes_in_folder))
                            }
                        }
                        cslbEnterBatchQuiz.setEnable()
                    }
                }
            })

            cslbExitBatchQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    binding.cswBatchQuizList.clear()
                    switchPage(Page.List)
                }
            })
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.Main) {
            quizCollectionWindowModel.uiStateFlow.mapAndCollect({quizCollectionFolderInfoList}) { folderInfoList ->
                quizCollectionFolderListAdapter.submitList(folderInfoList)
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            quizCollectionWindowModel.quizzesPagingDataFlow.collectLatest { pagingData ->
                quizCollectionWindowModel.getCurrentPagingDataLoadState()?.let { loadState ->
                    renderLoadState(loadState.isInitialLoading, loadState.isError, loadState.isEmpty)
                }
                quizCollectionQuizzesAdapter.submitData(pagingData)
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            quizCollectionWindowModel.uiStateFlow.mapAndCollect({ currentSelectedFolder })  { folderInfo ->
                binding.tvFolderName.text = folderInfo.folder.name
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            quizCollectionWindowModel.uiStateFlow.mapAndCollect({ hasNetwork })  { hasNetwork ->
                binding.viewNetworkDisconnect.isVisible = !hasNetwork
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            // Why do we need to cache the LoadState?
            //
            // The quizCollectionQuizzesAdapter.loadStateFlow collection block will not be triggered if the PagingData has already been loaded and cached.
            // As a result, when a user first enters QuizCollectionWindow, selects folder A, and then switches to folder B,
            // quizCollectionQuizzesAdapter.loadStateFlow is triggered as expected.
            //
            // However, when the user switches back to folder A,
            // no further events are emitted from quizCollectionQuizzesAdapter.loadStateFlow,
            // so we don’t receive any feedback at that point.
            quizCollectionQuizzesAdapter.loadStateFlow.collect { loadStates ->
                val hasAppendError = loadStates.append is LoadState.Error
                val isInitialLoading = loadStates.refresh is LoadState.Loading
                val isError = (loadStates.refresh is LoadState.Error) || hasAppendError
                val isEmpty = loadStates.refresh is LoadState.NotLoading && quizCollectionQuizzesAdapter.itemCount == 0
                renderLoadState(isInitialLoading, isError, isEmpty)
                quizCollectionWindowModel.updateCurrentFolderLoadState(isInitialLoading, isError, isEmpty)
            }
        }
    }

    private suspend fun createBatchQuizzes(isNeedToStopPushAndRespond: Boolean = false) = withContext(Dispatchers.Main) {
        val isSuccessful = quizCollectionWindowModel.createBatchQuiz(binding.cswBatchQuizList.getQuizInCollectionDataList())
        when (isSuccessful) {
            true -> {
                if (isNeedToStopPushAndRespond) {
                    unclosedMissionUiManager.closeMission(MissionType.PUSH_AND_RESPOND_TASK)
                }
                csWindowManager.hideWindow(tag, isRecordHiddenState = true)
                csWindowManager.createWindow(
                    get(BatchQuizStartWindow::class.java) as BatchQuizStartWindow,
                    Gravity.CENTER
                )
            }
            false -> {
                if (isNeedToStopPushAndRespond) {
                    showErrorToast(applicationContext.getString(R.string.error_msg_end_task_and_start_quiz))
                } else {
                    showErrorToast(applicationContext.getString(R.string.batch_quiz_error_failed_to_start_batch_quiz))
                }
            }
        }
        refreshStartQuizButtonState()
    }


    private suspend fun createQuiz(isNeedToStopPushAndRespond: Boolean = false) = withContext(Dispatchers.Main) {
        currentQuizInCollectionInfo?.let { info ->
            val isSuccessful = quizCollectionWindowModel.createQuiz(info)
            when (isSuccessful) {
                true -> {
                    if (isNeedToStopPushAndRespond) {
                        unclosedMissionUiManager.closeMission(MissionType.PUSH_AND_RESPOND_TASK)
                    }
                    val quizType: QuizType = QuizType.safeValueOf(info.quizData.quizType)
                    val iWindow: IWindow<ViewBinding>? = when (info.isTextQuiz()) {
                        true -> {
                            when (quizType) {
                                QuizType.TRUE_FALSE -> {
                                    get(TextTrueFalseStartWindow::class.java) as TextTrueFalseStartWindow
                                }
                                QuizType.SINGLE_SELECT,
                                QuizType.MULTIPLE_SELECT -> {
                                    get(TextMultipleChoiceStartWindow::class.java) as TextMultipleChoiceStartWindow
                                }
                                QuizType.SHORT_ANSWER -> {
                                    get(TextShortAnswerStartWindow::class.java) as TextShortAnswerStartWindow
                                }
                                else -> null
                            }
                        }
                        false -> {
                            when (quizType) {
                                QuizType.TRUE_FALSE -> get(TrueFalseStartWindow::class.java) as TrueFalseStartWindow
                                QuizType.SINGLE_SELECT,
                                QuizType.MULTIPLE_SELECT -> get(MultipleChoiceStartWindow::class.java) as MultipleChoiceStartWindow
                                QuizType.RECORD -> get(AudioQuizStartWindow::class.java) as AudioQuizStartWindow
                                QuizType.SHORT_ANSWER -> get(ShortAnswerStartWindow::class.java) as ShortAnswerStartWindow
                                QuizType.SINGLE_POLL,
                                QuizType.MULTIPLE_POLL -> get(PollQuizStartWindow::class.java) as PollQuizStartWindow
                                else -> null
                            }
                        }
                    }
                    iWindow?.let { window ->
                        csWindowManager.hideWindow(tag, isRecordHiddenState = true)
                        csWindowManager.createWindow(
                            window,
                            Gravity.CENTER
                        )
                        switchPage(Page.List)
                    }
                }
                false -> {
                    if (isNeedToStopPushAndRespond) {
                        showErrorToast(applicationContext.getString(R.string.error_msg_end_task_and_start_quiz))
                    } else {
                        showErrorToast(applicationContext.getString(R.string.quiz_error_msg_start_quiz))
                    }
                }
            }
        } ?: {
            showErrorToast(applicationContext.getString(R.string.quiz_error_msg_start_quiz))
        }
        refreshStartQuizButtonState()
    }

    private fun showErrorToast(message: String) {
        with(binding.cstToast) {
            setIsSuccess(false)
            setText(message)
            show(coroutineScope)
        }
    }

    private fun showSuccessToast(message: String) {
        with(binding.cstToast) {
            setIsSuccess(true)
            setText(message)
            show(coroutineScope)
        }
    }

    private fun switchPage(page: Page) {
        currentPage = page
        with(binding) {
            when (page) {
                Page.List -> {
                    currentQuizInCollectionInfo = null
                    // List Page
                    clFolderAndCollectionQuizzes.isVisible = true
                    rvCollectionQuizzes.isVisible = true
                    cslbCreateFolder.setEnable()
                    // Detail Page
                    clCollectionQuizDetail.isVisible = false
                    llCollectionQuizType.isVisible = false
                    // Batch Quiz Page
                    cslbEnterBatchQuiz.isVisible = !myViewBoardConnectionStateProvider.isBound()
                    cslbExitBatchQuiz.isVisible = false
                    cswBatchQuizList.isVisible = false
                }
                is Page.Detail -> {
                    currentQuizInCollectionInfo = page.quizInCollectionInfo
                    // List Page
                    clFolderAndCollectionQuizzes.isVisible = false
                    cslbCreateFolder.setEnable()
                    // Detail Page
                    clCollectionQuizDetail.isVisible = true
                    llCollectionQuizType.isVisible = true
                    // Batch Quiz Page
                    cslbEnterBatchQuiz.isVisible = false
                    cslbExitBatchQuiz.isVisible = false
                    cswBatchQuizList.isVisible = false
                    val quizType = QuizType.entries.find { it.name == page.quizInCollectionInfo.quizData.quizType } ?: QuizType.UNSPECIFIED
                    when (quizType) {
                        QuizType.TRUE_FALSE -> {
                            ivCollectionQuizType.setImageResource(R.drawable.ic_toolbar_irs_true_false)
                            tvCollectionQuizType.text = root.context.getString(R.string.quiz_types_true_false)
                        }
                        QuizType.SINGLE_SELECT,
                        QuizType.MULTIPLE_SELECT -> {
                            ivCollectionQuizType.setImageResource(R.drawable.ic_toolbar_irs_multiple_selection)
                            tvCollectionQuizType.text = root.context.getString(R.string.quiz_types_multiple_choice)
                        }
                        QuizType.RECORD -> {
                            ivCollectionQuizType.setImageResource(R.drawable.ic_toolbar_irs_audio)
                            tvCollectionQuizType.text = root.context.getString(R.string.quiz_types_audio)
                        }
                        QuizType.SHORT_ANSWER -> {
                            ivCollectionQuizType.setImageResource(R.drawable.ic_toolbar_irs_short_answer)
                            tvCollectionQuizType.text = root.context.getString(R.string.short_answer_capitalized_first_word)
                        }
                        QuizType.SINGLE_POLL,
                        QuizType.MULTIPLE_POLL -> {
                            ivCollectionQuizType.setImageResource(R.drawable.ic_toolbar_irs_poll)
                            tvCollectionQuizType.text = root.context.getString(R.string.quiz_types_poll)
                        }
                        else -> {}
                    }
                    when (page.quizInCollectionInfo.isTextQuiz()) {
                        true -> {
                            ibLightBulb.isVisible = true
                            ibLightBulb.isSelected = false
                            cscqdvQuizDetail.isGone = true
                            csctfcqdvTrueFalseTextQuizDetail.isGone = quizType != QuizType.TRUE_FALSE
                            csctfcqdvMultipleChoiceTextQuizDetail.isGone = quizType != QuizType.SINGLE_SELECT && quizType != QuizType.MULTIPLE_SELECT
                            csctfcqdvShortAnswerTextQuizDetail.isGone = quizType != QuizType.SHORT_ANSWER
                            when (quizType) {
                                QuizType.TRUE_FALSE -> {
                                    csctfcqdvTrueFalseTextQuizDetail.updateView(page.quizInCollectionInfo, ibLightBulb.isSelected)
                                }
                                QuizType.SINGLE_SELECT,
                                QuizType.MULTIPLE_SELECT -> {
                                    csctfcqdvMultipleChoiceTextQuizDetail.updateView(page.quizInCollectionInfo, ibLightBulb.isSelected)
                                }
                                QuizType.SHORT_ANSWER -> {
                                    csctfcqdvShortAnswerTextQuizDetail.updateView(page.quizInCollectionInfo, ibLightBulb.isSelected)
                                }
                                else -> {}
                            }
                        }
                        false -> {
                            ibLightBulb.isGone = true
                            csctfcqdvTrueFalseTextQuizDetail.isGone = true
                            csctfcqdvMultipleChoiceTextQuizDetail.isGone = true
                            csctfcqdvShortAnswerTextQuizDetail.isGone = true
                            cscqdvQuizDetail.isVisible = true
                            cscqdvQuizDetail.updateView(page.quizInCollectionInfo)
                        }
                    }
                    refreshStartQuizButtonState()
                }
                is Page.BatchQuiz -> {
                    // List Page
                    clFolderAndCollectionQuizzes.isVisible = true
                    rvCollectionQuizzes.isVisible = false
                    cslbCreateFolder.setDisable()
                    // Detail Page
                    clCollectionQuizDetail.isVisible = false
                    llCollectionQuizType.isVisible = false
                    // Batch Quiz Page
                    cslbEnterBatchQuiz.isVisible = false
                    cslbExitBatchQuiz.isVisible = !myViewBoardConnectionStateProvider.isBound()
                    cswBatchQuizList.isVisible = true
                    cswBatchQuizList.updateAiSubjectDisplayNamesDataList(quizCollectionWindowModel.uiStateFlow.value.aiSubjectDisplayNamesDataList)
                    cswBatchQuizList.updateFolderId(page.quizCollectionFolderInfo.folder.id)
                }
            }
        }
    }

    private fun renderLoadState(isInitialLoading: Boolean, isError: Boolean, isEmpty: Boolean) {
        with(binding) {
            cslbRefresh.setEnable()
            llLoading.isVisible = isInitialLoading
            llRetry.isVisible = isError
            llEmptyContent.isVisible = isEmpty
            when (isInitialLoading || isError || isEmpty) {
                true -> {
                    cslbEnterBatchQuiz.setDisable()
                }
                false -> {
                    cslbEnterBatchQuiz.setEnable()
                }
            }
        }
    }

    private fun initData() {
        coroutineScope.launch {
            quizCollectionWindowModel.fetchSourceTypeMapping()
            quizCollectionWindowModel.fetchSubjectDisplayNames()
            quizCollectionWindowModel.fetchFolderList()
        }
    }

    private sealed class Page {
        data object List : Page()
        data class Detail(val quizInCollectionInfo: QuizInCollectionInfo) : Page()
        data class BatchQuiz(val quizCollectionFolderInfo: QuizCollectionFolderInfo) : Page()
    }

}