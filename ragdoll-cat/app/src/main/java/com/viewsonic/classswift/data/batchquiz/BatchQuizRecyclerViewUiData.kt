package com.viewsonic.classswift.data.batchquiz

import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.info.QuizInCollectionInfo

sealed class BatchQuizRecyclerViewUiData() {
    data object QuizHintHeader : BatchQuizRecyclerViewUiData() {
        const val VIEW_TYPE = 0
    }

    data class QuizTypeHeader(
        val quizType: QuizType
    ) : BatchQuizRecyclerViewUiData() {
        companion object {
            const val VIEW_TYPE = 1
        }
    }

    data class QuizInfo(
        val selectedSequenceNumber: Int,
        val selectionState: SelectionState,
        val quizInCollectionInfo: QuizInCollectionInfo
    ) : BatchQuizRecyclerViewUiData() {

        enum class SelectionState {
            STATE_NORMAL,
            STATE_SELECTED,
            STATE_DISABLED
        }

        companion object {
            const val VIEW_TYPE = 2
        }
    }
}