package com.viewsonic.classswift.ui.window.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.databinding.ViewItemSelectClassBinding
import timber.log.Timber

class SelectClassAdapter(
    val context: Context,
    private val onItemInteractionListener: OnItemInteractionListener
) : ListAdapter<ClassroomInfo, SelectClassAdapter.ViewHolder>(diffCallback){
    private var selectedPosition: Int = 0
    private var isInEditMode = false

    // 只剩一個 Classroom 時，不能刪除
    // Ongoing Classroom 不能刪除
    // Ongoing Classroom 排序在最前面

    inner class ViewHolder(val binding: ViewItemSelectClassBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(classroomInfo: ClassroomInfo, position: Int) {
            Timber.d("[B][bind] : classroomInfo = $classroomInfo, position = $position")
            binding.tvClassName.text = classroomInfo.displayName
            binding.tvClassSubTitle.visibility = if (classroomInfo.isLessonOnGoing()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            if (!isInEditMode) {
                binding.ivDelete.isEnabled = if (itemCount == 1) false else !classroomInfo.isLessonOnGoing()
                setEnable(true)
            } else {
                binding.ivDelete.isEnabled = false
                setEnable(false)
            }
            binding.ivDelete.setOnClickListener {
                onItemInteractionListener.onItemDelete(classroomInfo)
            }
            //在編輯模式下，不能回傳onItemSelected，會影響Edit button狀態
            if (position == selectedPosition && !isInEditMode) {
                onItemInteractionListener.onItemSelected(classroomInfo)
            }
            val isSelected = position == selectedPosition
            binding.ivDelete.isVisible = !classroomInfo.isRoster()
            when (classroomInfo.originType) {
                ClassroomInfo.OriginType.NONE,
                ClassroomInfo.OriginType.CANVAS -> {
                    binding.ivIcon.visibility = View.GONE
                }
                ClassroomInfo.OriginType.CLASS_LINK -> {
                    binding.ivIcon.visibility = View.VISIBLE
                    binding.ivIcon.setImageResource(R.drawable.selector_class_classlink)
                }
                ClassroomInfo.OriginType.GOOGLE_CLASSROOM -> {
                    binding.ivIcon.visibility = View.VISIBLE
                    binding.ivIcon.setImageResource(R.drawable.ic_google_classroom)
                }
            }
            binding.clSelectClass.isSelected = isSelected
            binding.tvClassSubTitle.isSelected = isSelected
            binding.clSelectClass.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                onItemInteractionListener.onItemSelected(classroomInfo)
                notifyItemChanged(previousPosition) // 更新舊選中項目
                notifyItemChanged(selectedPosition)
            }
        }

        private fun setEnable(isEnable: Boolean) {
            binding.clSelectClass.isEnabled = isEnable
            binding.tvClassName.isEnabled = isEnable
        }
    }

    fun addItem(item: ClassroomInfo) {
        //當class只有一個的時候，要強制更新第一個 ViewHolder item.(只能將原本的array copy到另一個記憶體位置)
        val resultList = if (currentList.toMutableList().size == 1) {
             currentList.map { it.copy() }.toMutableList()
        } else {
            currentList.toMutableList()
        }
        resultList.add(item)
        submitList(resultList)
    }

    fun addItems(list: List<ClassroomInfo>) {
        val resultList = currentList.toMutableList()
        resultList.addAll(list)
        submitList(resultList)
    }

    fun getSelectedItem():ClassroomInfo = getItem(selectedPosition)

    fun removeItem(item: ClassroomInfo) {
        val itemPosition = currentList.indexOf(item)
        val resultList = currentList.map { it.copy() }.toMutableList()
        if (itemPosition in 0..selectedPosition && selectedPosition != 0) {
            selectedPosition -= 1
        }
        resultList.removeAt(itemPosition)
        submitList(resultList)
    }

    fun updateSelectItem(item: ClassroomInfo) {
        val resultList = currentList.toMutableList()
        resultList[selectedPosition] = item
        submitList(resultList)
    }

    @SuppressLint("NotifyDataSetChanged")
    //在編輯的情況下，把所有的點擊都 disable
    fun setIsInEditMode(value: Boolean) {
        //isInEditMode狀態改變，整個List就是要刷新
        if (value != isInEditMode) {
            isInEditMode = value
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewItemSelectClassBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    interface OnItemInteractionListener {
        fun onItemSelected(classroomInfo: ClassroomInfo)
        fun onItemDelete(classroomInfo: ClassroomInfo)
    }
    
    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ClassroomInfo>() {
            override fun areItemsTheSame(
                oldItem: ClassroomInfo,
                newItem: ClassroomInfo
            ): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(
                oldItem: ClassroomInfo,
                newItem: ClassroomInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}