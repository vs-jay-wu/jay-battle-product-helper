package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.viewsonic.classswift.R
import com.viewsonic.classswift.feature.servicescreens.ui.RandomDrawPhase
import com.viewsonic.classswift.feature.servicescreens.ui.RandomDrawScreen
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.tool.RandomDrawWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Random Draw (service path) wired via a ComposeView. Phase + picked student from
 * RandomDrawWindowModel.uiState. Dice-roll Lottie + network-disconnect overlay deferred.
 */
class RandomDrawWindow(val context: Context) : ComposeHostWindow(context) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val windowModel: RandomDrawWindowModel by inject(RandomDrawWindowModel::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)

    override var tag: WindowTag = WindowTag.RANDOM_DRAW_TOOL
    override var size: SizeInPixels = SizeInPixels(348f.dpToPx().toInt(), 336f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    private data class Ui(val phase: RandomDrawPhase, val seat: String, val name: String)
    private val ui = MutableStateFlow(Ui(RandomDrawPhase.INIT, "", ""))

    override fun onViewCreated() {
        super.onViewCreated()
        initCollection()
    }

    @Composable
    override fun Content() {
        val state by ui.collectAsState()
        RandomDrawScreen(
            phase = state.phase,
            seat = state.seat,
            name = state.name,
            onDraw = { windowModel.selectStudent() },
            onTryAgain = { ui.value = Ui(RandomDrawPhase.INIT, "", "") },
            onClose = { csWindowManager.removeWindow(tag) },
        )
    }

    private fun initCollection() {
        coroutineScope.launch {
            windowModel.uiState.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        RandomDrawWindowModel.RandomDrawUiState.InitUi -> ui.value = Ui(RandomDrawPhase.INIT, "", "")
                        RandomDrawWindowModel.RandomDrawUiState.NoAttendedStudent -> ui.value = ui.value.copy(phase = RandomDrawPhase.NO_STUDENTS)
                        RandomDrawWindowModel.RandomDrawUiState.PlayDiceAnimation -> ui.value = ui.value.copy(phase = RandomDrawPhase.ROLLING)
                        RandomDrawWindowModel.RandomDrawUiState.ShowStudentInfo -> {
                            val s = windowModel.selectedStudentInfo
                            ui.value = Ui(
                                phase = RandomDrawPhase.RESULT,
                                seat = s?.displaySeatNumber?.ifBlank { context.getString(R.string.common_empty_seat_number) }.orEmpty(),
                                name = s?.displayName.orEmpty(),
                            )
                        }
                        RandomDrawWindowModel.RandomDrawUiState.HasNetWork ->
                            if (ui.value.phase != RandomDrawPhase.RESULT) ui.value = Ui(RandomDrawPhase.INIT, "", "")
                        RandomDrawWindowModel.RandomDrawUiState.ShowNoNetworkView -> Unit // network overlay deferred
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
