package com.viewsonic.classswift.manager

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.manager.AudioPlayerHelper.PlayerState.Complete
import com.viewsonic.classswift.manager.AudioPlayerHelper.PlayerState.GetDuration
import com.viewsonic.classswift.manager.AudioPlayerHelper.PlayerState.UpdateRemainTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class AudioPlayerHelper(private val applicationContext: Context) {
    private lateinit var exoPlayer: ExoPlayer

    var currentUrl: String? = null
        private set
        get() = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Init(""))
    val playerState: StateFlow<PlayerState> = _playerState

    private val _errorEvent = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 1)
    val errorEvent: SharedFlow<PlayerEvent> = _errorEvent
    var nowState = STATE_IDLE

    init {
        // ExoPlayer init need in main thread, in worker thread will crush
        coroutineScope.launch(Dispatchers.Main) {
            exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
            exoPlayer.addListener(object : Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    nowState = state
                    when (state) {
                        STATE_READY -> {
                            val duration = exoPlayer.duration
                            if (duration != C.TIME_UNSET) {
                                _playerState.update { GetDuration((exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()), duration) }
                            }
                        }
                        STATE_ENDED -> {
                            _playerState.update { Complete((exoPlayer.currentMediaItem?.localConfiguration?.uri.toString())) }
                            stopProgressUpdate()
                        }
                        STATE_BUFFERING -> {}
                        STATE_IDLE -> {
                            stopProgressUpdate()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        _playerState.update { PlayerState.Playing((exoPlayer.currentMediaItem?.localConfiguration?.uri.toString())) }
                        startProgressUpdate()
                    } else {
                        Timber.tag("playerState").d("exoPlayer is pause uri: ${ exoPlayer.currentMediaItem?.localConfiguration?.uri}")
                        // audio is already complete, doesn't need set pause state
                        if (nowState != STATE_ENDED) {
                            _playerState.update { PlayerState.Pause(exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()) }
                        }
                        stopProgressUpdate()
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    _errorEvent.tryEmit(PlayerEvent.Error(error))
                    stopProgressUpdate()
                }
            })
        }
    }

    @OptIn(UnstableApi::class)
    fun play(url: String) {
        if (exoPlayer.isReleased) {
            exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
        }
        val mediaItem = MediaItem.fromUri(url.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
        currentUrl = null
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun isPlaying(): Boolean = exoPlayer.isPlaying

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = coroutineScope.launch {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    _playerState.update {
                        UpdateRemainTime(
                            (exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()),
                            exoPlayer.duration - exoPlayer.currentPosition
                        )
                    }
                }
                delay(ONE_SEC_DELAY)
            }
        }
    }

    fun getDuration(): Long {
        return exoPlayer.duration
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Must call this function to avoid memory leak
     */
    fun release() {
        exoPlayer.release()
        stopProgressUpdate()
    }

    // Player UI State
    sealed class PlayerState() {
        data class Init(val audioUrl: String): PlayerState()
        data class Complete(val audioUrl: String) : PlayerState()
        data class Playing(val audioUrl: String) : PlayerState()
        data class Pause(val audioUrl: String) : PlayerState()
        data class GetDuration(val audioUrl: String, val duration: Long) : PlayerState()
        data class UpdateRemainTime(val audioUrl: String, val remainTime: Long) : PlayerState()
    }

    // One-time Player Events
    sealed class PlayerEvent {
        data class Error(val exception: PlaybackException) : PlayerEvent()
    }
}