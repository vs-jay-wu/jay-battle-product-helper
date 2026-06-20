package com.viewsonic.classswift.ui.window

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.feature.servicescreens.ui.JoinAttendee
import com.viewsonic.classswift.feature.servicescreens.ui.JoinClassScreen
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.JoinClassWindowModel
import com.viewsonic.classswift.utils.QRCodeUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class JoinClassWindow(val context: Context) : ComposeHostWindow(context) {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val wModel: JoinClassWindowModel by inject(JoinClassWindowModel::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override var tag: WindowTag = WindowTag.JOIN_CLASS
    override var size: SizeInPixels = SizeInPixels(WINDOW_WIDTH_DP.dpToPx().toInt(), WINDOW_HEIGHT_DP.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    override fun onCreate() {
        if (!wModel.isMyViewBoardBound()) csWindowManager.showWindow(WindowTag.TOOLBAR)
    }

    override fun onViewCreated() {
        if (!wModel.isSelectedClassroomInfoExisted()) {
            binding.root.post {
                CSSystemDialogWindow.Builder(context)
                    .setTitle(context.getString(R.string.error_msg_general))
                    .setMessage(context.getString(R.string.error_msg_general_try_again))
                    .setPositiveButton(
                        context.getString(R.string.common_ok),
                        context.getColor(R.color.color_2E3133),
                    ) { csWindowManager.removeWindow(tag) }
                    .build()
                    .show()
            }
            return
        }
        super.onViewCreated() // attach the Compose body
        coroutineScope.launch(Dispatchers.IO) { wModel.fetchStudentInfoList() }
    }

    @Composable
    override fun Content() {
        val rawList by wModel.studentInfoListFlow.collectAsState()
        val spinnerVisible by wModel.isSpinnerButtonVisible.collectAsState()
        val display = wModel.getDisplayList(rawList).sortedBy { it.serialNumber }
        val joined = display.count { it.isJoinedClass() }
        val hasPreRoster = display.any { it.getParticipationState() == StudentInfo.ParticipationState.NOT_JOINED }
        val roomLink = wModel.getRoomLink()

        JoinClassScreen(
            className = wModel.getClassName(),
            joinUrl = wModel.getDisplayRoomLink(),
            classCode = wModel.getClassCode(),
            attendees = display.map {
                JoinAttendee(
                    name = it.displayName.ifBlank { it.displaySeatNumber },
                    seat = it.displaySeatNumber,
                    joined = it.isJoinedClass(),
                )
            },
            joinedCount = joined,
            capacity = wModel.getMaxStudentCount(),
            isGuestMode = wModel.isGuestMode(),
            showFractionCount = wModel.getClassType().showFractionCount(hasPreRoster),
            spinnerVisible = spinnerVisible,
            qr = { m ->
                // Real scannable QR generated from the full room link (same as the native window).
                val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, roomLink) {
                    value = withContext(Dispatchers.IO) {
                        QRCodeUtils.generateQRCodeWithBackground(roomLink, qrSize = 1080, bgRadius = 10f.dpToPx())?.asImageBitmap()
                    }
                }
                bitmap?.let { Image(it, contentDescription = null, modifier = m.fillMaxSize()) }
            },
            onClose = { csWindowManager.removeWindow(tag) },
            onCopyLink = { wModel.onCopyLink() },
            onSwitchClass = { wModel.onSwitchClass() },
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private companion object {
        private const val WINDOW_WIDTH_DP = 333.33f
        private const val WINDOW_HEIGHT_DP = 565.33f
    }
}
