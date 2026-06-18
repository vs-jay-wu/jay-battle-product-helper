package com.viewsonic.classswift.ui.widget.batchquiz.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.databinding.ItemBatchQuizResultBinding
import com.viewsonic.classswift.ui.widget.batchquiz.adapter.BatchQuizResultAdapter
import com.viewsonic.classswift.utils.extension.toPercent

class BatchQuizResultViewHolder(private val binding: ItemBatchQuizResultBinding): RecyclerView.ViewHolder(binding.root) {
    var info: BatchQuizSummaryInfo = BatchQuizSummaryInfo()
    fun onBind(resultInfo: BatchQuizSummaryInfo, listener: BatchQuizResultAdapter.OnItemTitleClickedListener) {
        info = resultInfo
        with(binding) {
            tvAnswerPercent.text = resultInfo.accuracyRate.toPercent()
            cswTitle.setOnClickListener(listener)
            cswTitle.setInfo(resultInfo)
            cswCorrectBarchart.setAnswerStatusCount(resultInfo.correctStudentIds.size, resultInfo.submittedStudentCount)
            cswIncorrectBarchart.setAnswerStatusCount(resultInfo.incorrectStudentIds.size, resultInfo.submittedStudentCount)
            cswNoAnswerBarchart.setAnswerStatusCount(resultInfo.noAnswerStudentIds.size, resultInfo.submittedStudentCount)
        }
    }

}