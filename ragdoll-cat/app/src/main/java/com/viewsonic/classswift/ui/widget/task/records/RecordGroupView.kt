package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.RecordGroupInfo
import com.viewsonic.classswift.data.task.TaskResultInfo
import com.viewsonic.classswift.databinding.ViewRecordGroupBinding
import com.viewsonic.classswift.ui.widget.task.content.TaskItemDecoration
import com.viewsonic.classswift.utils.extension.setDebouncedClickListener

class RecordGroupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var groupInfo: RecordGroupInfo? = null
    private var adapter: RecordGroupGridAdapter? = null
    private var groupEventListener: GroupItemEventListener? = null
    private val binding: ViewRecordGroupBinding = ViewRecordGroupBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    interface GroupItemEventListener {
        fun onItemThumbnailClicked(group: RecordGroupInfo, data: TaskResultInfo)
        fun onRecordDataSelectUpdate(group: RecordGroupInfo, data: TaskResultInfo)
        fun onAddPointClicked(group: RecordGroupInfo, data: TaskResultInfo)
        fun onAddGroupPointClicked(group: RecordGroupInfo)
        fun onSubtractGroupPointClicked(group: RecordGroupInfo)
        fun onRefetchStudentRecord(data: TaskResultInfo)
    }

    init {
        initClickAction()
        initRecyclerView()
        initAdapter()
    }

    fun bindData(data: RecordGroupInfo) {

        if (data.data.isEmpty()) return
        groupInfo = data
        binding.tvGroupNumber.text = data.groupId.toString()
        adapter?.let { gridAdapter ->
            val oldData = gridAdapter.currentList
            if (!sameGroupData(oldData, data.data)) {

                gridAdapter.submitList(null) {
                    gridAdapter.updateAll(taskId = data.taskId, data = data.data)
                }
            }
        }
    }

    fun setGroupItemEventListener(listener: GroupItemEventListener) {
        groupEventListener = listener
    }

    private fun initAdapter() {
        if (adapter == null) {
            adapter = RecordGroupGridAdapter().apply {
                setRecordItemEventListener(
                    object : RecordGroupGridAdapter.GroupItemEventListener {
                        override fun onThumbnailClicked(data: TaskResultInfo.Content) {
                            groupInfo?.let {
                                groupEventListener?.onItemThumbnailClicked(group = it, data = data)
                            }
                        }

                        override fun onRecordDataSelectUpdate(data: TaskResultInfo) {
                            groupInfo?.let {
                                groupEventListener?.onRecordDataSelectUpdate(group = it, data = data)
                            }
                        }

                        override fun onAddPointClicked(data: TaskResultInfo) {
                            groupInfo?.let {
                                groupEventListener?.onAddPointClicked(group = it, data = data)
                            }
                        }

                        override fun onRefetchRecordData(data: TaskResultInfo.ApiFail) {
                            groupEventListener?.onRefetchStudentRecord(data)
                        }
                    }
                )
            }
            binding.recyclerview.adapter = adapter
        }
    }

    private fun initClickAction() {
        with(binding) {
            acbAddPoint.setDebouncedClickListener(RecordsConstants.UPDATE_POINT_DEBOUNCE) {
                if (isEnabled) {
                    showAddPointAnimation()
                    groupInfo?.let {
                        groupEventListener?.onAddGroupPointClicked(group = it)
                    }
                }
            }

            acbSubtractPoint.setDebouncedClickListener(RecordsConstants.UPDATE_POINT_DEBOUNCE) {
                if (isEnabled) {
                    groupInfo?.let {
                        groupEventListener?.onSubtractGroupPointClicked(group = it)
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        val spaceHorizontal = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_group_grid_item_horizontal_space
        )
        val spaceVertical = context.resources.getDimensionPixelSize(
            R.dimen.push_respond_group_grid_item_vertical_space
        )

        val itemDecoration = TaskItemDecoration(
            spanCount = GRID_SPAN_COUNT,
            spacingStart = spaceHorizontal,
            spacingEnd = spaceHorizontal,
            spacingTop = spaceVertical,
            spacingBottom = spaceVertical,
            includeEdge = false
        )

        binding.recyclerview.apply {
            layoutManager = object : GridLayoutManager(context, GRID_SPAN_COUNT) {
                override fun canScrollVertically(): Boolean = false
            }
            addItemDecoration(itemDecoration)
            itemAnimator = null
        }
    }

    private fun showAddPointAnimation() {
        with(binding.tvAddPointAnimator) {
            alpha = 1f
            translationY = 0f
            visibility = VISIBLE

            val floatDistance = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics
            )

            animate()
                .translationY(-floatDistance)
                .alpha(0f)
                .setDuration(RecordsConstants.ANIMATION_DURATION)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { visibility = INVISIBLE }
                .start()
        }
    }

    private fun sameGroupData(oldList: List<TaskResultInfo>, newList: List<TaskResultInfo>): Boolean {
        if (oldList.size != newList.size) return false
        for (i in oldList.indices) {
            if (oldList[i] != newList[i]) return false
        }
        return true
    }

    companion object {
        private const val GRID_SPAN_COUNT = 4
    }
}