package com.viewsonic.classswift.manager

import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.info.StudentInfo.Status
import com.viewsonic.classswift.data.info.StudentQuizzingInfo
import com.viewsonic.classswift.data.socket.quiz.data.AnswerData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * VSFT-8612: 驗證 [QuizManager.mergeQuizzingListWithBackend] 的 pure merge 邏輯。
 *
 * Debounce 觸發、座位事件訂閱、DISCLOSE_ANSWER freeze 等狀態驅動行為，因專案目前不引入
 * MockK / kotlinx-coroutines-test，留由手動驗證涵蓋。
 */
class QuizManagerStudentSyncTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun student(
        serialNumber: Int,
        displayName: String = "Student$serialNumber",
        studentId: String = "id_$serialNumber",
        status: Status = Status.ACTIVE
    ) = StudentInfo(
        serialNumber = serialNumber,
        displayName = displayName,
        studentId = studentId,
        status = status
    )

    private fun quizzing(
        serialNumber: Int,
        displayName: String = "Student$serialNumber",
        studentId: String = "id_$serialNumber",
        status: Status = Status.ACTIVE,
        answerStringData: String = "",
        answerDataList: List<AnswerData> = emptyList(),
        canShowAnswer: Boolean = false
    ) = StudentQuizzingInfo(
        serialNumber = serialNumber,
        displayName = displayName,
        studentId = studentId,
        status = status,
        answerStringData = answerStringData,
        answerDataList = answerDataList.toMutableList(),
        canShowAnswer = canShowAnswer
    )

    /** Builder fake：避開 Context 依賴，仿照 StudentQuizzingInfo.fromStudentInfo 輸出。 */
    private val builder: (StudentInfo) -> StudentQuizzingInfo = { info ->
        StudentQuizzingInfo(
            serialNumber = info.serialNumber,
            displayName = info.displayName,
            studentId = info.studentId,
            status = info.status
        )
    }

    // ── append 新加入的學生 (VSFT-8612 核心) ───────────────────────────────────

    @Test
    fun `mergeQuizzingListWithBackend appends student not in current list`() {
        val current = listOf(quizzing(1), quizzing(3))
        val backend = listOf(student(1), student(2), student(3))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        assertEquals(listOf(1, 2, 3), result.map { it.serialNumber })
        assertEquals(Status.ACTIVE, result[1].status)
        assertEquals("Student2", result[1].displayName)
    }

    // ── 保留既有學生的答題資料 ────────────────────────────────────────────────

    @Test
    fun `mergeQuizzingListWithBackend preserves existing answerStringData when merging`() {
        val current = listOf(
            quizzing(1, answerStringData = "my answer"),
            quizzing(2)
        )
        val backend = listOf(student(1, displayName = "Renamed"), student(2))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        val merged1 = result.first { it.serialNumber == 1 }
        assertEquals("my answer", merged1.answerStringData)
        assertEquals("Renamed", merged1.displayName)
    }

    @Test
    fun `mergeQuizzingListWithBackend preserves existing answerDataList and canShowAnswer`() {
        val previousAnswers = listOf(AnswerData(optionId = 42, content = "opt"))
        val current = listOf(
            quizzing(
                1,
                answerDataList = previousAnswers,
                canShowAnswer = true
            )
        )
        val backend = listOf(student(1))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        assertEquals(1, result.size)
        assertEquals(previousAnswers, result[0].answerDataList)
        assertEquals(true, result[0].canShowAnswer)
    }

    // ── 既有但 backend 未回傳的學生：離席 → 從清單移除 ──────────────────────

    @Test
    fun `mergeQuizzingListWithBackend removes student absent from backend`() {
        val current = listOf(
            quizzing(1),
            quizzing(2, answerStringData = "old answer", canShowAnswer = true)
        )
        val backend = listOf(student(1))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        // serial=2 離席後消失，與 Refresh / 後端 source-of-truth 對齊
        assertEquals(listOf(1), result.map { it.serialNumber })
    }

    @Test
    fun `mergeQuizzingListWithBackend rejoined student appears as new entry with no previous answer`() {
        // 學生 1 先離席（前一輪 fetch 已移除），這輪 backend 又回 → 視為新加入
        val current = emptyList<StudentQuizzingInfo>()
        val backend = listOf(student(1, displayName = "Rejoined"))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        assertEquals(1, result.size)
        assertEquals(Status.ACTIVE, result[0].status)
        assertEquals("Rejoined", result[0].displayName)
        assertEquals("", result[0].answerStringData)
        assertEquals(emptyList<AnswerData>(), result[0].answerDataList)
    }

    @Test
    fun `mergeQuizzingListWithBackend backend status takes precedence over current list`() {
        // 既有清單 serial=1 是 INACTIVE 殘影；backend 仍回 ACTIVE → 結果應該 follow backend
        val current = listOf(quizzing(1, status = Status.INACTIVE, studentId = ""))
        val backend = listOf(student(1, status = Status.ACTIVE))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        assertEquals(1, result.size)
        assertEquals(Status.ACTIVE, result[0].status)
        assertEquals("id_1", result[0].studentId)
    }

    @Test
    fun `mergeQuizzingListWithBackend reflects INACTIVE status when backend returns INACTIVE student`() {
        // Contract test: 雖然 occupied_only=true 後端理論上不回 INACTIVE，但 builder 仍應如實反映
        val current = listOf(quizzing(1, status = Status.ACTIVE))
        val backend = listOf(student(1, status = Status.INACTIVE, studentId = ""))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        assertEquals(1, result.size)
        assertEquals(Status.INACTIVE, result[0].status)
        assertEquals("", result[0].studentId)
    }

    // ── sort 依 serialNumber 升冪 ────────────────────────────────────────────

    @Test
    fun `mergeQuizzingListWithBackend sorts result by serialNumber ascending`() {
        val current = emptyList<StudentQuizzingInfo>()
        val backend = listOf(student(3), student(1), student(2))

        val result = QuizManager.mergeQuizzingListWithBackend(current, backend, builder)

        assertEquals(listOf(1, 2, 3), result.map { it.serialNumber })
    }

    // ── 空 list 的邊界 ───────────────────────────────────────────────────────

    @Test
    fun `mergeQuizzingListWithBackend with empty current list adds all backend students`() {
        val backend = listOf(student(1), student(2))

        val result = QuizManager.mergeQuizzingListWithBackend(emptyList(), backend, builder)

        assertEquals(listOf(1, 2), result.map { it.serialNumber })
        assertEquals(Status.ACTIVE, result[0].status)
    }

    @Test
    fun `mergeQuizzingListWithBackend with empty backend returns empty list`() {
        val current = listOf(
            quizzing(1, answerStringData = "kept"),
            quizzing(2)
        )

        val result = QuizManager.mergeQuizzingListWithBackend(current, emptyList(), builder)

        // 全員離席 → 後端為 source of truth，清單清空
        assertEquals(emptyList<StudentQuizzingInfo>(), result)
    }
}
