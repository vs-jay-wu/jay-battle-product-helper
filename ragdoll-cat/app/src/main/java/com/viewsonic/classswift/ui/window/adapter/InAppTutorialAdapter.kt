package com.viewsonic.classswift.ui.window.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ItemInAppTutorialCompleteBinding
import com.viewsonic.classswift.databinding.ItemInAppTutorialVideoGuideBinding
import com.viewsonic.classswift.databinding.ItemInAppTutorialWelcomeBinding
import com.viewsonic.classswift.ui.window.viewholder.InAppTutorialCompleteViewHolder
import com.viewsonic.classswift.ui.window.viewholder.InAppTutorialVideoGuideViewHolder
import com.viewsonic.classswift.ui.window.viewholder.InAppTutorialWelcomeViewHolder

class InAppTutorialAdapter(
    private val onItemInteractionListener: OnItemInteractionListener,
    private val onItemCallback: OnItemCallback,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var player: ExoPlayer
    private val pageList: List<InAppTutorialPage> = listOf(
        InAppTutorialPage.Welcome,
        InAppTutorialPage.VideGuide(R.string.tutorial_pick_class_video_url, R.string.tutorial_walkthrough_pick_title, R.string.tutorial_walkthrough_pick_subtitle, false),
        InAppTutorialPage.VideGuide(R.string.tutorial_let_student_join_video_url, R.string.tutorial_walkthrough_join_title, R.string.tutorial_let_student_join_description, false),
        InAppTutorialPage.VideGuide(R.string.tutorial_open_teaching_material_video_url, R.string.tutorial_walkthrough_open_title, R.string.tutorial_walkthrough_open_subtitle, false),
        InAppTutorialPage.VideGuide(R.string.tutorial_add_sparkles_video_url, R.string.tutorial_walkthrough_sparkles_title, R.string.tutorial_walkthrough_sparkles_subtitle, false),
        InAppTutorialPage.VideGuide(R.string.tutorial_use_smart_tool_video_url, R.string.tutorial_walkthrough_enhance_title, R.string.tutorial_walkthrough_enhance_subtitle, true),
        InAppTutorialPage.Complete(onItemCallback)
    )

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        player = ExoPlayer.Builder(recyclerView.context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inAppTutorialPage = pageList[viewType]
        return when (inAppTutorialPage) {
            InAppTutorialPage.Welcome -> InAppTutorialWelcomeViewHolder(
                ItemInAppTutorialWelcomeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            is InAppTutorialPage.VideGuide -> InAppTutorialVideoGuideViewHolder(
                ItemInAppTutorialVideoGuideBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            is InAppTutorialPage.Complete -> InAppTutorialCompleteViewHolder(
                ItemInAppTutorialCompleteBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onItemInteractionListener = object : InAppTutorialCompleteViewHolder.OnItemInteractionListener {
                    override fun onThumbUpClicked() {
                        onItemInteractionListener.onThumbUpClicked()
                    }

                    override fun onThumbDownClicked() {
                        onItemInteractionListener.onThumbDownClicked()
                    }
                },
                onItemCallback = object : InAppTutorialCompleteViewHolder.OnItemCallback {
                    override fun isNeedToShowFeedbackUI(): Boolean = onItemCallback.isNeedToShowFeedbackUI()
                }
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return pageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val inAppTutorialPage = pageList[position]) {
            InAppTutorialPage.Welcome -> {}
            is InAppTutorialPage.VideGuide -> {
                val videoGuideViewHolder = holder as InAppTutorialVideoGuideViewHolder
                videoGuideViewHolder.onBind(inAppTutorialPage, player)
            }
            is InAppTutorialPage.Complete -> {}
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is InAppTutorialWelcomeViewHolder -> {
                onItemInteractionListener.onWelcomeItemShown()
            }
            is InAppTutorialVideoGuideViewHolder -> {
                holder.playVideo()
                onItemInteractionListener.onVideoGuideItemShown(holder.bindingAdapterPosition)
            }
            is InAppTutorialCompleteViewHolder -> {
                onItemInteractionListener.onCompeteItemShown()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is InAppTutorialVideoGuideViewHolder) {
            holder.stopVideo()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        player.stop()
        player.release()
    }

    fun getFirstVideoGuidePageIndex() = 1

    sealed class InAppTutorialPage {
        data object Welcome: InAppTutorialPage()
        data class VideGuide(val videoUrlResId: Int, val titleResId: Int, val descriptionResId: Int, val hasHint: Boolean): InAppTutorialPage()
        data class Complete(val onItemCallback: OnItemCallback): InAppTutorialPage()
    }

    interface OnItemInteractionListener {
        fun onWelcomeItemShown()
        fun onVideoGuideItemShown(index: Int)
        fun onCompeteItemShown()
        fun onThumbUpClicked()
        fun onThumbDownClicked()
    }

    interface OnItemCallback {
        fun isNeedToShowFeedbackUI(): Boolean
    }
}