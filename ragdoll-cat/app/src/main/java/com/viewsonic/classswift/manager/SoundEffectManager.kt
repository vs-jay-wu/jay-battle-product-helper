package com.viewsonic.classswift.manager

import android.content.Context
import android.media.SoundPool
import androidx.annotation.RawRes
import timber.log.Timber

class SoundEffectManager(val applicationContext: Context) {
    private var soundPool: SoundPool? = null
    private val soundIdMap = mutableMapOf<Int, Int>()
    private val streamIdMap = mutableMapOf<Int, Int>()
    private val pendingPlays = mutableSetOf<Int>()   // 等待播放的 ResId


    init {
        ensureSoundPool()
        //確定 soundPool 音效載入成功
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            val soundResId = soundIdMap.entries.find { it.value == sampleId }?.key
            Timber.d("[**][SoundEffectManager][16] : load sound id : $soundResId , status : $status , sampleId :$sampleId")
            if (status == 0 && soundResId != null && pendingPlays.contains(soundResId)) {
                soundIdMap[soundResId]?.let {
                    val streamId = soundPool?.play(it, 1f, 1f, 1, 0, 1f) ?: 0
                    if (streamId != 0) {
                        streamIdMap[soundResId] = streamId
                    }
                }
                Timber.d("[**][SoundEffectManager][22] : play sound id : $soundResId")
                pendingPlays.remove(soundResId)
            }
        }
    }

    private fun ensureSoundPool() {
        if (soundPool == null) {
            soundPool = SoundPool.Builder()
                .setMaxStreams(4) // 可同時播放數量
                .build()
        }
    }

    //先預載，之後可以直接 play，速度比較快
    fun preload(@RawRes resId: Int) {
        ensureSoundPool()
        if (!soundIdMap.containsKey(resId)) {
            val id = soundPool?.load(applicationContext, resId, 1) ?: 0
            if (id != 0) {
                soundIdMap[resId] = id
            }
        }
    }

    fun play(resId: Int) {
        ensureSoundPool()
        soundIdMap[resId]?.let {
            val streamId = soundPool?.play(it, 1f, 1f, 1, 0, 1f) ?: 0
            if (streamId != 0) {
                streamIdMap[resId] = streamId
            }
        } ?: run {
            // 動態載入未預載的音效
            val id = soundPool?.load(applicationContext, resId, 1) ?: 0
            if (id != 0) {
                soundIdMap[resId] = id
                pendingPlays.add(resId) // 等載入完成後播放
            }
        }
    }

    fun stop(@RawRes resId: Int) {
        streamIdMap[resId]?.let { soundPool?.stop(it) }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundIdMap.clear()
        streamIdMap.clear()
        pendingPlays.clear()
    }

}