package com.viewsonic.classswift.manager

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.constant.AmplitudeConstant.EventProperties.Key.CANCEL_METHOD
import com.viewsonic.classswift.constant.AmplitudeConstant.EventProperties.Key.SCREENSHOT_SOURCE
import com.viewsonic.classswift.constant.AmplitudeConstant.EventProperties.Value.CANCEL
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.ui.activity.ScreenshotActivity
import com.viewsonic.classswift.utils.extension.getParcelableCompat
import com.viewsonic.classswift.utils.extension.getSerializableCompat
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ScreenshotManager(
    private val applicationContext: Context,
    private val csWindowManager: CSWindowManager,
) {
    private var screenshotImageUri: String = ""
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val _screenshotDataFlow = MutableSharedFlow<String>()
    val screenshotDataFlow = _screenshotDataFlow.asSharedFlow()

    fun getScreenshotImageUri(): String {
        return screenshotImageUri
    }

    fun resetScreenshotImageUri() {
        screenshotImageUri = ""
    }

    fun startCaptureScreenshot(
        screenshotSource: String,
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        onCancel: () -> Unit
    ) {
        csWindowManager.hideAllWindows(emptyList())
        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                csWindowManager.showAllWindows()

                val result: ScreenshotActivity.Result = resultData?.getSerializableCompat(
                    ScreenshotActivity.BUNDLE_KEY_RESULT
                ) ?: ScreenshotActivity.Result.NONE

                if (result == ScreenshotActivity.Result.SUCCESSFUL) {
                    resultData?.getParcelableCompat<Uri>(ScreenshotActivity.BUNDLE_KEY_IMAGE_URI)?.let { imageUri ->
                        screenshotImageUri = imageUri.toString()
                        coroutineScope.launch(Dispatchers.Default) {
                            _screenshotDataFlow.emit(imageUri.toString())
                        }
                        onSuccess()
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.SCREENSHOT_ENDED)
                            .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                            .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                            .appendEventProperty(SCREENSHOT_SOURCE, screenshotSource)
                            .send()
                        return
                    }
                }
                if (result == ScreenshotActivity.Result.CANCELED) {
                    onCancel()
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.SCREENSHOT_CANCELED)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                        .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                        .appendEventProperty(SCREENSHOT_SOURCE, screenshotSource)
                        .appendEventProperty(CANCEL_METHOD, CANCEL)
                        .send()
                    return
                }
                onFailed()
            }
        }
        val screenshotPath = applicationContext.cacheDir.absolutePath + File.separator +
                DEFAULT_SCREENSHOT_FILE_NAME + "_${UUID.randomUUID()}" + FILE_TYPE

        applicationContext.startActivity(
            ScreenshotActivity.createIntent(
                context = applicationContext,
                resultReceiver = resultReceiver,
                screenshotSavedPath = screenshotPath
            )
        )
        AmplitudeEventBuilder(AmplitudeConstant.EventName.SCREENSHOT_STARTED)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
            .appendEventProperty(SCREENSHOT_SOURCE, screenshotSource)
            .send()
    }

    fun getScreenShotSource(tag: WindowTag): String {
        return when (tag) {
            WindowTag.TRUE_FALSE_EDIT_QUIZ,
            WindowTag.MVB_TRUE_FALSE_EDIT_QUIZ -> {
                "TRUE FALSE"
            }
            WindowTag.MULTIPLE_CHOICE_EDIT_QUIZ,
            WindowTag.MVB_MULTIPLE_CHOICE_EDIT_QUIZ -> {
                "SELECT"
            }
            WindowTag.SHORT_ANSWER_EDIT_QUIZ,
            WindowTag.MVB_SHORT_ANSWER_EDIT_QUIZ -> {
                "SHORT ANSWER"
            }
            WindowTag.AUDIO_EDIT_QUIZ,
            WindowTag.MVB_AUDIO_EDIT_QUIZ -> {
                "RECORD"
            }
            WindowTag.POLL_EDIT_QUIZ -> {
                "POLL"
            }
            else -> "ADD NEW SCREENSHOT SOURCE"
        }
    }


    companion object {
        private const val DEFAULT_SCREENSHOT_FILE_NAME = "temp_screenshot"
        private const val FILE_TYPE = ".jpg"
    }
}