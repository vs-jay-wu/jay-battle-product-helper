package com.viewsonic.classswift.utils.extension

import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.ui.widget.CSMessageDialog
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun CSMessageDialog.showEndOngoingMissionMessageDialog(
    coroutineScope: CoroutineScope,
    nextMissionType: MissionType,
    onPositiveClicked: suspend () -> Unit,
    onNegativeClicked: suspend () -> Unit,
    onMaskClicked: suspend () -> Unit,
) {
    val nextMissionName = when (nextMissionType) {
        MissionType.BATCH_QUIZZES -> context.getString(R.string.batch_quiz_title)
        MissionType.PUSH_AND_RESPOND_TASK -> context.getString(R.string.push_and_respond_task)
        else -> context.getString(R.string.quiz_title)
    }

    setTitle(context.getString(R.string.ongoing_mission_conflict_title))
    setMessage(context.getString(R.string.ongoing_mission_conflict_message, nextMissionName))
    setPositiveButtonText(context.getString(R.string.ongoing_mission_conflict_positive_title, nextMissionName))

    setButtonClickListeners(
        onPositive = {
            setPositiveLoading(isLoading = true)
            coroutineScope.launch(Dispatchers.Main) {
                try {
                    onPositiveClicked()
                } finally {
                    setPositiveLoading(false)
                    dismiss()
                }
            }
        },
        onNegative = {
            coroutineScope.launch(Dispatchers.Main) {
                try {
                    onNegativeClicked()
                } finally {
                    setPositiveLoading(false)
                    dismiss()
                }
            }
        }
    )

    setMaskClickedListener {
        coroutineScope.launch(Dispatchers.Main) {
            onMaskClicked()
        }
    }
    show()
}

fun CSMessageDialog.handleQuizStartWithOngoingMission(
    coroutineScope: CoroutineScope,
    unclosedMissionUiManager: UnclosedMissionUiManager,
    onStartQuiz: suspend (isNeedToStopPushAndRespond: Boolean) -> Unit,
    onCanceled: suspend () -> Unit,
    onBatchCloseFailed: suspend () -> Unit,
    onMaskClicked: suspend () -> Unit,
) {
    coroutineScope.launch(Dispatchers.Main) {
        val unclosedMissions = unclosedMissionUiManager.getUnclosedMissions().filter { it != MissionType.QUIZ }
        if (unclosedMissions.isEmpty()) {
            onStartQuiz(false)
            return@launch
        }

        val unclosedSet = unclosedMissions.toSet()
        val hasBatchQuizzes = MissionType.BATCH_QUIZZES in unclosedSet
        val hasPushAndRespond = MissionType.PUSH_AND_RESPOND_TASK in unclosedSet

        showEndOngoingMissionMessageDialog(
            coroutineScope,
            MissionType.QUIZ,
            onPositiveClicked = {
                if (!hasBatchQuizzes) {
                    onStartQuiz(hasPushAndRespond)
                    return@showEndOngoingMissionMessageDialog
                }
                if (unclosedMissionUiManager.closeMission(MissionType.BATCH_QUIZZES)) {
                    onStartQuiz(hasPushAndRespond)
                } else {
                    onBatchCloseFailed()
                }
            },
            onNegativeClicked = {
                onCanceled()
            },
            onMaskClicked = {
                onMaskClicked()
            }
        )
    }
}
