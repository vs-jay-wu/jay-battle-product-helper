package com.viewsonic.classswift.ui.widget.quizcollection

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.AiSubjectDisplayNamesData
import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse
import com.viewsonic.classswift.data.batchquiz.BatchQuizRecyclerViewUiData
import com.viewsonic.classswift.databinding.WidgetCsBatchQuizListBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widgetmodel.quizcollection.CSBatchQuizListWidgetModel
import com.viewsonic.classswift.ui.window.adapter.BatchQuizHintHeaderAdapter
import com.viewsonic.classswift.ui.window.adapter.BatchQuizzesAdapter
import com.viewsonic.classswift.utils.extension.mapAndCollect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber


@SuppressLint("SetTextI18n")
class CSBatchQuizListWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), BatchQuizzesAdapter.OnItemInteractionListener {
    private var binding: WidgetCsBatchQuizListBinding =
        WidgetCsBatchQuizListBinding.inflate(LayoutInflater.from(context), this)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val widgetModel: CSBatchQuizListWidgetModel by inject(CSBatchQuizListWidgetModel::class.java)

    private val batchQuizHintHeaderAdapter: BatchQuizHintHeaderAdapter = BatchQuizHintHeaderAdapter()
    private val batchQuizzesAdapter: BatchQuizzesAdapter = BatchQuizzesAdapter(this)
    private val gridLayoutManager: GridLayoutManager = GridLayoutManager(context, 4)
    private var callback: Callback? = null

    init {
        initView()
        initRecyclerView()
        initCollection()
    }

    override fun onQuizInfoItemClicked(adapterPosition: Int, quizInfo: BatchQuizRecyclerViewUiData.QuizInfo) {
        widgetModel.selectQuiz(adapterPosition, quizInfo.quizInCollectionInfo)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun getQuizInCollectionDataList(): List<QuizzesInCollectionFolderResponse.QuizInCollectionData> {
        return widgetModel.uiStateFlow.value.selectedQuizDataList.map { it.quizInCollectionInfo.quizData }
    }

    fun updateAiSubjectDisplayNamesDataList(list: List<AiSubjectDisplayNamesData>) {
        widgetModel.setAiSubjectDisplayNamesDataList(list)
    }

    fun updateFolderId(folderId: String) {
        widgetModel.setFolderId(folderId)
    }

    fun refreshStartQuizButtonState() {
        with(binding.cslbStartBatchQuiz) {
            when (widgetModel.isInLesson() && widgetModel.uiStateFlow.value.selectedQuizDataList.size >= CSBatchQuizListWidgetModel.MIN_QUIZ_COUNT_TO_START_QUIZ) {
                true -> {
                    setEnable()
                }
                false -> {
                    setDisable()
                }
            }
        }
    }

    fun clear() {
        widgetModel.unselectAll()
    }

    fun release() {
        coroutineScope.cancel()
    }

    private fun initView() {
        with(binding) {
            cslbRefresh.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    batchQuizzesAdapter.retry()
                    cslbRefresh.setLoading()
                }
            })

            cslbSelectAll.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    widgetModel.selectAllQuizzes(batchQuizzesAdapter.snapshot().items)
                }
            })

            cslbClearAll.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    widgetModel.unselectAll()
                }
            })

            cslbStartBatchQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbStartBatchQuiz.setLoading()
                    callback?.startBatchQuizzesFlow()
                }
            })
        }
    }

    private fun initRecyclerView() {
        with(binding) {
            val concatAdapterConfig = ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .build()
            rvBatchQuizzes.adapter = ConcatAdapter(
                concatAdapterConfig,
                batchQuizHintHeaderAdapter,
                batchQuizzesAdapter
            )
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val viewType = rvBatchQuizzes.adapter?.getItemViewType(position)
                    return when (viewType) {
                        BatchQuizRecyclerViewUiData.QuizHintHeader.VIEW_TYPE -> 4
                        BatchQuizRecyclerViewUiData.QuizTypeHeader.VIEW_TYPE -> 4
                        else -> 1
                    }
                }
            }
            rvBatchQuizzes.layoutManager = gridLayoutManager
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.Main) {
            widgetModel.uiDataPagingFlow.collectLatest { pagingData ->
                batchQuizzesAdapter.submitData(pagingData)
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            widgetModel.uiStateFlow.mapAndCollect({selectedQuizDataList}) { selectedQuizDataList ->
                refreshStartQuizButtonState()
                binding.tvSelectedCount.text = context.getString(R.string.quiz_collection_quiz_selected, selectedQuizDataList.size)
                if (selectedQuizDataList.isNotEmpty()) {
                    binding.cslbClearAll.setEnable()
                } else {
                    binding.cslbClearAll.setDisable()
                }
                if (selectedQuizDataList.size < CSBatchQuizListWidgetModel.MAX_SELECTABLE_QUIZ_COUNT) {
                    binding.cslbSelectAll.setEnable()
                } else {
                    binding.cslbSelectAll.setDisable()
                }
                batchQuizzesAdapter.updateSelection(selectedQuizDataList)
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            batchQuizzesAdapter.loadStateFlow.collect { loadStates ->
                val hasAppendError = loadStates.append is LoadState.Error
                val isInitialLoading = loadStates.refresh is LoadState.Loading
                val isError = (loadStates.refresh is LoadState.Error) || hasAppendError
                val isEmpty = loadStates.refresh is LoadState.NotLoading && batchQuizzesAdapter.itemCount == 0
                Timber.d("[B][CSBatchQuizListWidget] : hasAppendError = $hasAppendError, isInitialLoading = $isInitialLoading, isError = $isError, isEmpty = $isEmpty")
                with(binding) {
                    cslbRefresh.setEnable()
                    llLoading.isVisible = isInitialLoading
                    llRetry.isVisible = isError
                    llEmptyContent.isVisible = isEmpty
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            widgetModel.uiEventFlow.collect { event ->
                when (event) {
                    CSBatchQuizListWidgetModel.UiEvent.ShowReachMaxSelectedQuizLimit -> {
                        callback?.requestShowErrorToast(context.getString(R.string.batch_quiz_error_reach_max_quiz_limit))
                    }
                }
            }
        }
    }

    interface Callback {
        fun requestShowErrorToast(message: String)
        fun startBatchQuizzesFlow()
    }
}
