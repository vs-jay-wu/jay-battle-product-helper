package com.viewsonic.classswift.ui.windowmodel.quiz.enums

/**
 * VSFT-8453 — Sketch Response 視窗的兩個生命週期階段。
 *
 * [ANSWERING]：老師等待學生提交（派題 / 監控）；由 [MvbSketchResponseStartWindowModel] 管理 polling + timer。
 * [RESULT]：老師批改 / 查看結果；由 [MvbSketchResponseStartWindowModel] 的 result phase methods + [com.viewsonic.classswift.coordinator.SketchTaskCoordinator] 管理。
 *
 * 狀態轉換：ANSWERING → RESULT 由 [MvbSketchResponseStartWindowModel.collectAllAndMark] 驅動，
 * 反向不可（同 mVB Quiz family 的 QuizState 設計原則）。
 */
enum class SketchState {
    ANSWERING,
    RESULT,
}
