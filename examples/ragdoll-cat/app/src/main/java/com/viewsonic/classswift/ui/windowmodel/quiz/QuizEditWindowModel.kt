package com.viewsonic.classswift.ui.windowmodel.quiz

import com.viewsonic.classswift.api.body.CreateQuizBody
import com.viewsonic.classswift.coordinator.UploadFileHandler
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizSourceType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.data.task.HasTaskInProgressInfo
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.uimanager.PushRespondUiManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import timber.log.Timber

class QuizEditWindowModel(
    private val quizManager: QuizManager,
    private val classroomManager: ClassroomManager,
    private val uploadFileHandler: UploadFileHandler,
    private val pushRespondUiManager: PushRespondUiManager
): IWindowModel {
    private val coroutineScope = CoroutineManager.getScope(this)

    val imageUploadSharedFlow = uploadFileHandler.uploadImageSharedFlow

    suspend fun createQuiz(optionType: QuizOptionType, quizType: QuizType, quizOptions: List<QuizOption>): Boolean {
        return withContext(Dispatchers.IO) {
            val result = quizManager.createQuizCancellingOngoingIfNeeded(
                CreateQuizBody(
                    imgUrl = uploadFileHandler.awsPreSignedUrl.s3GetUrl,
                    optionType = optionType,
                    quizType = quizType,
                    sourceType = QuizSourceType.MANUAL,
                    quizOptionList = quizOptions
                )
            )
            if (result != null) {
                QuizSharedUiInfo.updateQuizType(quizType)
            }
            result != null
        }
    }

    fun startUploadImage(imageUri: String) {
        uploadFileHandler.fetchPreSignedUrl(classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId, imageUri)
    }

    fun saveMultipleOptionInfos() {
        quizManager.saveMultipleOptionInfos()
    }

    fun stopPushRespond(): Boolean {
        return pushRespondUiManager.stopPushRespond()
    }

    suspend fun isTaskInProgress(): HasTaskInProgressInfo {
        return pushRespondUiManager.hasTaskInProgress()
    }

    override fun onCleared() {
        Timber.d("[QuizEditWindowModel] onCleared")
        coroutineScope.cancel()
    }
}