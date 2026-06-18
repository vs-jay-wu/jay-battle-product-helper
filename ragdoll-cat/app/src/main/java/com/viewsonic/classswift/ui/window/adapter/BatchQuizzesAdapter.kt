package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.batchquiz.BatchQuizRecyclerViewUiData
import com.viewsonic.classswift.databinding.ItemBatchQuizTextQuizBinding
import com.viewsonic.classswift.databinding.ItemBatchQuizTypeHeaderBinding
import com.viewsonic.classswift.ui.widget.KatexView
import com.viewsonic.classswift.ui.widgetmodel.quizcollection.CSBatchQuizListWidgetModel
import com.viewsonic.classswift.ui.widgetmodel.quizcollection.CSBatchQuizListWidgetModel.SelectedQuizData
import com.viewsonic.classswift.ui.window.viewholder.BatchQuizTextQuizViewHolder
import com.viewsonic.classswift.ui.window.viewholder.BatchQuizTypeHeaderViewHolder

class BatchQuizzesAdapter(private val listener: OnItemInteractionListener) : PagingDataAdapter<BatchQuizRecyclerViewUiData, RecyclerView.ViewHolder>(diffCallback) {
    private var selectedQuizDataList: List<SelectedQuizData> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            BatchQuizRecyclerViewUiData.QuizTypeHeader.VIEW_TYPE -> BatchQuizTypeHeaderViewHolder(
                ItemBatchQuizTypeHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> BatchQuizTextQuizViewHolder(
                ItemBatchQuizTextQuizBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                listener
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        getItem(position)?.let { batchQuizRecyclerViewUiData ->
            when (batchQuizRecyclerViewUiData) {
                is BatchQuizRecyclerViewUiData.QuizTypeHeader -> (holder as BatchQuizTypeHeaderViewHolder).onBind(batchQuizRecyclerViewUiData.quizType)
                is BatchQuizRecyclerViewUiData.QuizInfo -> (holder as BatchQuizTextQuizViewHolder).onBind(position, applySelectionState(batchQuizRecyclerViewUiData))
                else -> {}
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION_CHANGED)) {
            val item = getItem(position)
            if (holder is BatchQuizTextQuizViewHolder && item is BatchQuizRecyclerViewUiData.QuizInfo) {
                holder.onBindSelection( position, applySelectionState(item))
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    fun updateSelection(updatedQuizDataList: List<SelectedQuizData>) {
        if (selectedQuizDataList == updatedQuizDataList) {
            return
        }

        val wasAtLimit = selectedQuizDataList.size >= CSBatchQuizListWidgetModel.MAX_SELECTABLE_QUIZ_COUNT
        val isAtLimit = updatedQuizDataList.size >= CSBatchQuizListWidgetModel.MAX_SELECTABLE_QUIZ_COUNT
        val affectedQuizDataList = (selectedQuizDataList + updatedQuizDataList)
            .associateBy { it.quizInCollectionInfo.quizData.id }
            .values
            .toList()
        selectedQuizDataList = updatedQuizDataList

        val affectedPositions = LinkedHashSet<Int>()
        affectedQuizDataList.forEach {
            affectedPositions.add(it.adapterPosition)
        }

        if (wasAtLimit != isAtLimit) {
            // TODO: Investigate whether using PagingDataAdapter.snapshot() could cause data state inconsistencies.
            snapshot().items.forEachIndexed { index, item ->
                if (item is BatchQuizRecyclerViewUiData.QuizInfo) {
                    val quizId = item.quizInCollectionInfo.quizData.id
                    val isSelected = updatedQuizDataList.any { it.quizInCollectionInfo.quizData.id == quizId }
                    if (!isSelected) {
                        affectedPositions.add(index)
                    }
                }
            }
        }

        affectedPositions.forEach {
            notifyItemChanged(it, PAYLOAD_SELECTION_CHANGED)
        }
    }

    private fun applySelectionState(quizInfo: BatchQuizRecyclerViewUiData.QuizInfo): BatchQuizRecyclerViewUiData.QuizInfo {
        val quizId = quizInfo.quizInCollectionInfo.quizData.id
        val selectedQuizData = selectedQuizDataList.firstOrNull { it.quizInCollectionInfo.quizData.id == quizId }
        val selectionState = when {
            selectedQuizData != null ->
                BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_SELECTED

            selectedQuizDataList.size >= CSBatchQuizListWidgetModel.MAX_SELECTABLE_QUIZ_COUNT ->
                BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_DISABLED

            else ->
                BatchQuizRecyclerViewUiData.QuizInfo.SelectionState.STATE_NORMAL
        }
        return quizInfo.copy(
            selectedSequenceNumber = selectedQuizData?.selectedSequenceNumber ?: 0,
            selectionState = selectionState
        )
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.children.forEach { child ->
            child.findViewById<KatexView>(R.id.cskv_question)?.release()
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (peek(position)) {
            is BatchQuizRecyclerViewUiData.QuizInfo -> {
                BatchQuizRecyclerViewUiData.QuizInfo.VIEW_TYPE
            }
            else -> {
                BatchQuizRecyclerViewUiData.QuizTypeHeader.VIEW_TYPE
            }
        }
    }

    interface OnItemInteractionListener {
        fun onQuizInfoItemClicked(adapterPosition: Int, quizInfo: BatchQuizRecyclerViewUiData.QuizInfo)
    }

    companion object {
        const val PAYLOAD_SELECTION_CHANGED = "payload_selection_changed"
        private val diffCallback = object : DiffUtil.ItemCallback<BatchQuizRecyclerViewUiData>() {
            override fun areItemsTheSame(
                oldItem: BatchQuizRecyclerViewUiData,
                newItem: BatchQuizRecyclerViewUiData
            ): Boolean {
                return when {
                    oldItem is BatchQuizRecyclerViewUiData.QuizTypeHeader &&
                            newItem is BatchQuizRecyclerViewUiData.QuizTypeHeader ->
                        oldItem.quizType == newItem.quizType

                    oldItem is BatchQuizRecyclerViewUiData.QuizInfo &&
                            newItem is BatchQuizRecyclerViewUiData.QuizInfo ->
                        oldItem.quizInCollectionInfo.quizData.id == newItem.quizInCollectionInfo.quizData.id

                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: BatchQuizRecyclerViewUiData,
                newItem: BatchQuizRecyclerViewUiData
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: BatchQuizRecyclerViewUiData,
                newItem: BatchQuizRecyclerViewUiData
            ): Any? {
                if (oldItem is BatchQuizRecyclerViewUiData.QuizInfo &&
                    newItem is BatchQuizRecyclerViewUiData.QuizInfo
                ) {
                    if ((oldItem.selectionState != newItem.selectionState ||
                        oldItem.selectedSequenceNumber != newItem.selectedSequenceNumber) &&
                        oldItem.quizInCollectionInfo == newItem.quizInCollectionInfo
                    ) {
                        return PAYLOAD_SELECTION_CHANGED
                    }
                }
                return null
            }
        }
    }
}
