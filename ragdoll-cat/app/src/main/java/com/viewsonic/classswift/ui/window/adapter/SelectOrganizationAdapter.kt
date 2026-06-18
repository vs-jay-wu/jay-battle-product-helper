package com.viewsonic.classswift.ui.window.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.databinding.ViewItemSelectOrgBinding
import com.viewsonic.classswift.utils.extension.toDate

class SelectOrganizationAdapter(
    val context: Context,
    private val organizations: List<OrganizationInfo> = mutableListOf(),
) : RecyclerView.Adapter<SelectOrganizationAdapter.ViewHolder>() {
    private var selectedPosition: Int = 0
    private var onItemSelected: ((Int) -> Unit)? = null


    inner class ViewHolder(val binding: ViewItemSelectOrgBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: OrganizationInfo, position: Int) {
            val packageName = item.displayPlanName(context)
            binding.tvOrganizationName.text = item.orgDisplayName
            binding.tvMembershipLevel.text = packageName
            binding.tvExpiryDate.text = if (item.noExpiredPlan) ""
            else String.format(context.getString(R.string.select_org_exp), item.endDate.toDate())

            isSelected(position == selectedPosition)
            isEnable(item.notExpiredOrg)

            binding.clSelectOrg.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                onItemSelected?.invoke(position)
                notifyItemChanged(previousPosition) // 更新舊選中項目
                notifyItemChanged(selectedPosition)
            }
        }

        private fun isSelected(flag: Boolean) {
            binding.clSelectOrg.isSelected = flag
            binding.tvOrganizationName.isSelected = flag
        }

        private fun isEnable(flag: Boolean) {
            binding.clSelectOrg.isEnabled = flag
            binding.tvOrganizationName.isEnabled = flag
        }
    }

    fun setItemSelectedListener(itemSelected: ((Int) -> Unit)) {
        this.onItemSelected = itemSelected
    }

    fun getSelectedOrganization() = if (selectedPosition != -1) {
        organizations[selectedPosition]
    } else {
        null
    }

    fun setSelectedPosition(position: Int) {
        if (position == -1) {//表示全都到期
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            return
        }
        val previousPosition = selectedPosition
        selectedPosition = position
        onItemSelected?.invoke(position)
        notifyItemChanged(previousPosition) // 更新舊選中項目
        notifyItemChanged(selectedPosition)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewItemSelectOrgBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(organizations[position], position)
    }

    override fun getItemCount(): Int = organizations.size
}
