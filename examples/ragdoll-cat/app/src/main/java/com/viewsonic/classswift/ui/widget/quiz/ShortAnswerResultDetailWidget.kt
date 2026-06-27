package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.google.android.material.card.MaterialCardView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.WidgetShortAnswerResultDetailBinding
import com.viewsonic.classswift.ui.widget.quiz.adapter.ShortAnswerResultDetailAdapter

class ShortAnswerResultDetailWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr), ShortAnswerResultDetailAdapter.OnItemInteractionListener {
    private val pagerSnapHelper = PagerSnapHelper()
    private val binding: WidgetShortAnswerResultDetailBinding = WidgetShortAnswerResultDetailBinding.inflate(LayoutInflater.from(
        ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_MaterialComponents
        )
    ), this)
    private val shortAnswerResultDetailAdapter: ShortAnswerResultDetailAdapter = ShortAnswerResultDetailAdapter(this)
    private val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
        override fun canScrollHorizontally(): Boolean {
            return false
        }
    }
    private var onInteractionListener: OnInteractionListener = object : OnInteractionListener {}

    init {
        this@ShortAnswerResultDetailWidget.setCardBackgroundColor(context.getColor(R.color.black_a40))
        with(binding) {
            rvContainer.apply {
                pagerSnapHelper.attachToRecyclerView(this)
                layoutManager = linearLayoutManager
                adapter = shortAnswerResultDetailAdapter
            }
            tbEye.setOnCheckedChangeListener { _, isChecked ->
                onInteractionListener.onEyeToggleButtonClicked(isChecked)
            }
        }
    }

    // region [Override] - ShortAnswerResultDetailAdapter.OnItemInteractionListener
    override fun onLeftButtonClicked() {
        if (shortAnswerResultDetailAdapter.currentList.isNotEmpty()) {
            val currentIndex = linearLayoutManager.findFirstVisibleItemPosition() - 1
            if (currentIndex < 0) {
                binding.rvContainer.scrollToPosition(shortAnswerResultDetailAdapter.currentList.size - 1)
            } else {
                binding.rvContainer.scrollToPosition(currentIndex)
            }
        }
    }

    override fun onRightButtonClicked() {
        if (shortAnswerResultDetailAdapter.currentList.isNotEmpty()) {
            val currentIndex = linearLayoutManager.findFirstVisibleItemPosition() + 1
            if (currentIndex == shortAnswerResultDetailAdapter.currentList.size) {
                binding.rvContainer.scrollToPosition(0)
            } else {
                binding.rvContainer.scrollToPosition(currentIndex)
            }
        }
    }

    override fun onCloseButtonClicked() {
        onInteractionListener.onClosedButtonClicked()
    }
    // endregion

    fun setOnInteractionListener(listener: OnInteractionListener) {
        onInteractionListener = listener
    }

    fun updateIsEyeButtonChecked(isChecked: Boolean) {
        binding.tbEye.isChecked = isChecked
    }

    fun updateShortAnswerResultInfos(answerList: List<QuizAnswerResultInfo>) {
        shortAnswerResultDetailAdapter.submitList(answerList)
    }

    fun switchToPositionByInfo(info: QuizAnswerResultInfo) {
        val index = shortAnswerResultDetailAdapter.currentList.indexOfFirst { it.studentId == info.studentId }.coerceAtLeast(0)
        binding.rvContainer.scrollToPosition(index)
    }

    interface OnInteractionListener {
        fun onEyeToggleButtonClicked(isChecked: Boolean) {}
        fun onClosedButtonClicked() {}
    }
}