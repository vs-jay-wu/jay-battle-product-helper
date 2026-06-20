package com.viewsonic.classswift.ui.window

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.feature.servicescreens.ui.SmPhase
import com.viewsonic.classswift.feature.servicescreens.ui.Student
import com.viewsonic.classswift.feature.servicescreens.ui.StudentManagementScreen
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.window.leaderboard.LeaderboardWindow
import com.viewsonic.classswift.ui.windowmodel.StudentManagementWindowModel
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.utils.QRCodeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

/**
 * StudentManagement (service path) — STEP 1: Student List tab wired via a ComposeView. Class
 * info (title/count/ID/Link/QR/leaderboard), 5-col student grid with score +/-, copy. Groups tab,
 * more-menu, edit-mode (remove students), and the back end-lesson-confirm are deferred to step 2.
 */
class StudentManagementWindow(val context: Context) : ComposeHostWindow(context) {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val clipboardManager: ClipboardManager by inject(ClipboardManager::class.java)
    private val wModel: StudentManagementWindowModel by inject(StudentManagementWindowModel::class.java)
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var dialogWindow: CSSystemDialogWindow? = null

    override var tag: WindowTag = WindowTag.STUDENT_MANAGEMENT
    override var size: SizeInPixels = SizeInPixels(977f.dpToPx().toInt(), 556f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    private val phase = MutableStateFlow(SmPhase.LOADING)

    override fun onCreate() {
        if (!wModel.isMyViewBoardBound()) csWindowManager.showWindow(WindowTag.TOOLBAR)
    }

    override fun onViewCreated() {
        if (!wModel.isSelectedClassroomInfoExisted()) {
            csWindowManager.removeWindow(tag)
            return
        }
        super.onViewCreated()
        coroutineScope.launch(Dispatchers.IO) {
            val ok = wModel.fetchStudentInfoList()
            withContext(Dispatchers.Main) { phase.value = if (ok) SmPhase.LIST else SmPhase.FAILED }
        }
    }

    @Composable
    override fun Content() {
        val uiState by wModel.studentManagementUiState.collectAsState()
        val ph by phase.collectAsState()
        val classroom = wModel.getSelectedClassroomInfo()
        val list = uiState.studentInfoList
        val joined = list.count { it.isJoinedClass() }
        val roomLink = classroom.roomLink

        StudentManagementScreen(
            classTitle = context.getString(R.string.join_class_title_join, classroom.displayName),
            classId = context.getString(R.string.join_class_info_id, classroom.number),
            countText = String.format(Locale.getDefault(), "%02d/%02d", joined, classroom.maxStudentCount),
            students = list.map { Student(id = it.studentId, seat = it.displaySeatNumber, name = it.displayName, score = it.points) },
            phase = ph,
            backVisible = !wModel.isGuestMode(),
            qr = { m ->
                val bitmap by produceState<ImageBitmap?>(null, roomLink) {
                    value = withContext(Dispatchers.IO) {
                        QRCodeUtils.generateQRCodeWithBackground(roomLink, qrSize = 1080, bgRadius = 10f.dpToPx())?.asImageBitmap()
                    }
                }
                bitmap?.let { Image(it, contentDescription = null, modifier = m.fillMaxSize()) }
            },
            onIncrease = { id -> coroutineScope.launch { wModel.increaseSpecificStudentPointByOnePoint(id) } },
            onDecrease = { id -> coroutineScope.launch { wModel.decreaseSpecificStudentPointByOnePoint(id) } },
            onCopyId = { clipboardManager.setPrimaryClip(ClipData.newPlainText("Classroom ID", classroom.number)) },
            onCopyLink = { clipboardManager.setPrimaryClip(ClipData.newPlainText("Class Link", roomLink)) },
            onLeaderboard = { openLeaderboard() },
            onBack = { csWindowManager.removeWindow(tag) }, // step 2: end-lesson confirm on back during a lesson
            onClose = { csWindowManager.removeWindow(tag) },
        )
    }

    private fun openLeaderboard() {
        if (toolbarManager.toolbarUiState.value.isPremiumUser) {
            coroutineScope.launch(Dispatchers.Main) {
                csWindowManager.getWindow(WindowTag.LEADERBOARD_WINDOW)?.hoistWindowZOrder() ?: run {
                    csWindowManager.createWindow(get(LeaderboardWindow::class.java), Gravity.CENTER)
                }
            }
        } else {
            dialogWindow = CSSystemDialogWindow.Builder(context)
                .setTitle(context.getString(R.string.common_notice))
                .setMessage(context.getString(R.string.common_upgrade_for_leaderboard))
                .setPositiveButton(context.getString(R.string.common_confirm), context.getColor(R.color.neutral_900)) {
                    dialogWindow?.dismiss()
                }
                .build()
            dialogWindow?.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wModel.onCleared()
        coroutineScope.cancel()
    }
}
