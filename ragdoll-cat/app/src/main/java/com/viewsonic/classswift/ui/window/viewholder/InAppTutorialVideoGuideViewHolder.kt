package com.viewsonic.classswift.ui.window.viewholder

import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ItemInAppTutorialVideoGuideBinding
import com.viewsonic.classswift.databinding.ViewVideoGuideHintBinding
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.adapter.InAppTutorialAdapter.InAppTutorialPage
import com.viewsonic.classswift.utils.QRCodeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.getLocationOnScreenWithoutStatusBar

class InAppTutorialVideoGuideViewHolder(
    val binding: ItemInAppTutorialVideoGuideBinding
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var videoGuidePage: InAppTutorialPage.VideGuide
    private lateinit var mediaItem: MediaItem
    private lateinit var player: ExoPlayer
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                binding.llPlayerRefreshContainer.visibility = View.GONE
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            binding.llPlayerRefreshContainer.visibility = View.VISIBLE
            binding.cslbRefresh.setState(LoadingButtonState.ENABLE)
        }
    }
    var title: String = ""
    private val viewOutlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, 40f.dpToPx())
        }
    }
    private val hintBinding = ViewVideoGuideHintBinding.inflate(
        LayoutInflater.from(binding.root.context)
    )

    private val hintPopupWindow = PopupWindow(
        hintBinding.root,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        true  // Focusable
    )

    fun onBind(videoGuidePage: InAppTutorialPage.VideGuide, sharedPlayer: ExoPlayer) {
        this.videoGuidePage = videoGuidePage
        this.player = sharedPlayer
        hintPopupWindow.dismiss()
        binding.apply {
            title = root.context.getString(videoGuidePage.titleResId)
            cslbRefresh.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbRefresh.setState(LoadingButtonState.LOADING)
                    playVideo()
                }
            }
            )
            ivHint.isVisible = videoGuidePage.hasHint
            if (videoGuidePage.hasHint) {
                QRCodeUtils.generateQRCodeWithBackground(
                    text = binding.root.context.getString(R.string.tutorial_hint_url, BuildConfig.HELP_URL),
                    qrSize = 120f.dpToPx().toInt(),
                    bgRadius = 3.2f.dpToPx()
                )?.let { bitmap ->
                    hintBinding.ivQrCode.setImageBitmap(bitmap)
                }
                hintBinding.root.setOnClickListener {
                    hintPopupWindow.dismiss()
                }
                ivHint.setOnClickListener {
                    val (hintPopupWindowMeasureWidth, hintPopupWindowMeasureHeight) =
                        hintBinding.root.measure(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        ).let {
                            hintBinding.root.measuredWidth to hintBinding.root.measuredHeight
                        }
                    val anchorPosition = ivHint.getLocationOnScreenWithoutStatusBar().let {
                        it.first + (ivHint.width / 2) - (hintPopupWindowMeasureWidth / 2) to it.second - hintPopupWindowMeasureHeight
                    }
                    hintPopupWindow.showAtLocation(
                        ivHint,
                        (ivHint.width - ivHint.measuredWidth) / 2,
                        anchorPosition.first,
                        anchorPosition.second
                    )
                }
            }
            pvPlayer.useController = false
            tvTitle.text = root.context.getString(videoGuidePage.titleResId)
            tvDescription.text = root.context.getString(videoGuidePage.descriptionResId)
            flPlayerContainer.outlineProvider = viewOutlineProvider
            flPlayerContainer.clipToOutline = true
            mediaItem = MediaItem.fromUri(root.context.getString(videoGuidePage.videoUrlResId, BuildConfig.DOCUMENT_URL))
        }
    }

    fun playVideo() {
        player.stop()
        binding.pvPlayer.player = player
        player.addListener(playerListener)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun stopVideo() {
        binding.pvPlayer.player = null
        player.removeListener(playerListener)
    }
}