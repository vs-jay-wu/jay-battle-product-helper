package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.data.info.QuizCollectionFolderInfo
import com.viewsonic.classswift.databinding.ItemMvbQcFolderBinding
import com.viewsonic.classswift.databinding.ItemMvbQcSidebarDividerBinding
import com.viewsonic.classswift.databinding.ItemMvbQcSidebarHeaderBinding
import com.viewsonic.classswift.ui.window.adapter.MvbQuizCollectionFolderListAdapter.SidebarItem
import com.viewsonic.classswift.ui.window.viewholder.MvbQuizCollectionFolderViewHolder
import com.viewsonic.classswift.ui.window.viewholder.MvbQuizCollectionSidebarDividerViewHolder
import com.viewsonic.classswift.ui.window.viewholder.MvbQuizCollectionSidebarHeaderViewHolder

class MvbQuizCollectionFolderListAdapter(
    private val onFolderClick: (QuizCollectionFolderInfo) -> Unit,
    private val onYourFoldersHeaderClick: () -> Unit,
) : ListAdapter<SidebarItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    sealed class SidebarItem {
        data class DefaultFolder(val info: QuizCollectionFolderInfo) : SidebarItem()
        data object Divider : SidebarItem()
        data class YourFoldersHeader(val isExpanded: Boolean) : SidebarItem()
        data class Folder(val info: QuizCollectionFolderInfo) : SidebarItem()
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SidebarItem.DefaultFolder -> TYPE_DEFAULT
        SidebarItem.Divider -> TYPE_DIVIDER
        is SidebarItem.YourFoldersHeader -> TYPE_HEADER
        is SidebarItem.Folder -> TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DEFAULT -> MvbQuizCollectionFolderViewHolder(
                ItemMvbQcFolderBinding.inflate(inflater, parent, false),
                MvbQuizCollectionFolderViewHolder.Role.DEFAULT,
            )
            TYPE_FOLDER -> MvbQuizCollectionFolderViewHolder(
                ItemMvbQcFolderBinding.inflate(inflater, parent, false),
                MvbQuizCollectionFolderViewHolder.Role.FOLDER,
            )
            TYPE_DIVIDER -> MvbQuizCollectionSidebarDividerViewHolder(
                ItemMvbQcSidebarDividerBinding.inflate(inflater, parent, false),
            )
            TYPE_HEADER -> MvbQuizCollectionSidebarHeaderViewHolder(
                ItemMvbQcSidebarHeaderBinding.inflate(inflater, parent, false),
            )
            else -> error("Unknown viewType=$viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SidebarItem.DefaultFolder -> (holder as MvbQuizCollectionFolderViewHolder).bind(
                info = item.info,
                role = MvbQuizCollectionFolderViewHolder.Role.DEFAULT,
                onClick = onFolderClick,
            )
            is SidebarItem.Folder -> (holder as MvbQuizCollectionFolderViewHolder).bind(
                info = item.info,
                role = MvbQuizCollectionFolderViewHolder.Role.FOLDER,
                onClick = onFolderClick,
            )
            is SidebarItem.YourFoldersHeader -> (holder as MvbQuizCollectionSidebarHeaderViewHolder).bind(
                isExpanded = item.isExpanded,
                onClick = onYourFoldersHeaderClick,
            )
            SidebarItem.Divider -> Unit
        }
    }

    companion object {
        private const val TYPE_DEFAULT = 0
        private const val TYPE_DIVIDER = 1
        private const val TYPE_HEADER = 2
        private const val TYPE_FOLDER = 3

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SidebarItem>() {
            override fun areItemsTheSame(oldItem: SidebarItem, newItem: SidebarItem): Boolean = when {
                oldItem is SidebarItem.DefaultFolder && newItem is SidebarItem.DefaultFolder ->
                    oldItem.info.folder.id == newItem.info.folder.id
                oldItem is SidebarItem.Folder && newItem is SidebarItem.Folder ->
                    oldItem.info.folder.id == newItem.info.folder.id
                else -> oldItem::class == newItem::class
            }

            override fun areContentsTheSame(oldItem: SidebarItem, newItem: SidebarItem): Boolean =
                oldItem == newItem
        }

        fun buildItems(
            folders: List<QuizCollectionFolderInfo>,
            isYourFoldersExpanded: Boolean,
        ): List<SidebarItem> {
            if (folders.isEmpty()) return emptyList()
            val default = folders.firstOrNull { it.folder.isDefault }
            val rest = folders.filterNot { it.folder.isDefault }
            return buildList {
                default?.let {
                    add(SidebarItem.DefaultFolder(it))
                    add(SidebarItem.Divider)
                }
                add(SidebarItem.YourFoldersHeader(isExpanded = isYourFoldersExpanded))
                if (isYourFoldersExpanded) {
                    rest.forEach { add(SidebarItem.Folder(it)) }
                }
            }
        }
    }
}
