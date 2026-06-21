package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.viewsonic.classswift.R
import com.viewsonic.classswift.feature.servicescreens.ui.BuzzerPhase
import com.viewsonic.classswift.feature.servicescreens.ui.BuzzerScreen
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.tool.BuzzerWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.millionSecondToStopwatch
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Buzzer (service path) wired via a ComposeView. State (init/running/answered + time + winner)
 * comes from BuzzerWindowModel.updateUIFlow. Network-disconnect overlay + error toast deferred.
 */
class BuzzerWindow(val context: Context) : ComposeHostWindow(context) {

    override var tag: WindowTag = WindowTag.BUZZER_TOOL
    override var size: SizeInPixels = SizeInPixels(348f.dpToPx().toInt(), 336f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    private val windowModel: BuzzerWindowModel by inject(BuzzerWindowModel::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private data class Ui(val phase: BuzzerPhase, val time: String, val seat: String, val name: String)
    private val ui = MutableStateFlow(Ui(BuzzerPhase.INIT, 0L.millionSecondToStopwatch(), "", ""))

    override fun onViewCreated() {
        super.onViewCreated()
        initCollection()
    }

    @Composable
    override fun Content() {
        val state by ui.collectAsState()
        BuzzerScreen(
            phase = state.phase,
            time = state.time,
            seat = state.seat,
            name = state.name,
            onStart = { if (windowModel.startBuzzer()) ui.update { it.copy(phase = BuzzerPhase.RUNNING) } },
            onTryAgain = {
                if (windowModel.stopBuzzer()) ui.update { Ui(BuzzerPhase.INIT, 0L.millionSecondToStopwatch(), "", "") }
            },
            onClose = { if (windowModel.stopBuzzer()) csWindowManager.removeWindow(tag) },
        )
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.updateUIFlow.collect { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        is BuzzerWindowModel.BuzzerUIEvent.UpdateTime ->
                            if (!windowModel.isStop) ui.update { it.copy(time = event.time) }
                        is BuzzerWindowModel.BuzzerUIEvent.AnsweredStudentInfo ->
                            ui.update {
                                it.copy(
                                    phase = BuzzerPhase.ANSWERED,
                                    seat = event.studentInfo.displaySeatNumber.ifBlank { context.getString(R.string.common_empty_seat_number) },
                                    name = event.studentInfo.displayName,
                                )
                            }
                        is BuzzerWindowModel.BuzzerUIEvent.NetworkAvailabilityState -> Unit // network overlay deferred
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowModel.onCleared()
        coroutineScope.cancel()
    }
}
