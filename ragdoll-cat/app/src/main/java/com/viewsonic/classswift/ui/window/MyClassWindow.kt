package com.viewsonic.classswift.ui.window

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.feature.servicescreens.ui.MyClassItem
import com.viewsonic.classswift.feature.servicescreens.ui.MyClassPhase
import com.viewsonic.classswift.feature.servicescreens.ui.MyClassScreen
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.MyClassWindowModel
import com.viewsonic.classswift.ui.windowmodel.MyClassWindowModel.ClassUIEvent
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.omit
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * MyClass (service path) — STEP 1: main view + primary path wired to MyClassWindowModel via a
 * ComposeView. Edit mode (inline rename), Add class, hint popups, and tutorial tooltips are
 * deferred to step 2.
 */
class MyClassWindow(val context: Context) : ComposeHostWindow(context) {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val wModel: MyClassWindowModel by inject(MyClassWindowModel::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var uiCollectJob: Job? = null
    private var dialogWindow: CSSystemDialogWindow? = null

    override var tag: WindowTag = WindowTag.CS_SELECT_MY_CLASS
    override var size: SizeInPixels = SizeInPixels(680f.dpToPx().toInt(), 393f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    private data class Ui(
        val classes: List<ClassroomInfo>,
        val selectedId: String?,
        val phase: MyClassPhase,
        val creating: Boolean = false,
    )
    private val ui = MutableStateFlow(Ui(emptyList(), null, MyClassPhase.LOADING))

    override fun onCreate() {
        initCollect()
        wModel.getClassroomList()
    }

    @Composable
    override fun Content() {
        val state by ui.collectAsState()
        val org = accountManager.selectedOrg
        val selected = state.classes.firstOrNull { it.id == state.selectedId }
        MyClassScreen(
            orgName = org?.orgDisplayName.orEmpty(),
            plan = org?.let { "${it.displayPlanName(context)}-${it.studentConcurrent}" }.orEmpty(),
            classes = state.classes.map {
                MyClassItem(id = it.id, name = it.displayName, roster = it.isRoster(), ongoing = it.isLessonOnGoing())
            },
            selectedId = state.selectedId,
            studentCount = selected?.maxStudentCount?.toString() ?: "-",
            phase = state.phase,
            enterEnabled = (selected?.maxStudentCount ?: 0) > 0 && selected?.isLessonOnGoing() != true,
            newClassEnabled = !state.creating,
            onSelect = { id -> ui.update { it.copy(selectedId = id) } },
            onDelete = { id -> state.classes.firstOrNull { it.id == id }?.let { confirmDelete(it) } },
            onEnter = { selected?.let { enterClass(it) } },
            onNewClass = { addNewClass() },
            onBack = { goBackToSelectOrg() },
            onHub = { openHub() },
            onRefresh = { ui.update { it.copy(phase = MyClassPhase.LOADING) }; wModel.refreshClassPage() },
            onClose = { close() },
        )
    }

    private fun close() {
        if (wModel.isMyViewBoardBound()) accountManager.quitApp() else csWindowManager.removeWindow(tag)
    }

    private fun initCollect() {
        uiCollectJob = coroutineScope.launch(Dispatchers.IO) {
            wModel.updateUIFlow.collect { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        is ClassUIEvent.UpdateClassList -> {
                            if (event.classList.isEmpty()) wModel.createDefaultClass()
                            ui.update {
                                it.copy(classes = event.classList, selectedId = event.classList.firstOrNull()?.id, phase = MyClassPhase.LIST)
                            }
                        }
                        is ClassUIEvent.AddClass -> ui.update { s ->
                            // New class is appended and auto-selected (mirrors the original add flow).
                            s.copy(classes = s.classes + event.classInfo, selectedId = event.classInfo.id, phase = MyClassPhase.LIST, creating = false)
                        }
                        ClassUIEvent.EnableAddClassButton -> ui.update { it.copy(creating = false) }
                        is ClassUIEvent.DeleteClassSuccess -> {
                            dialogWindow?.dismiss()
                            ui.update { s ->
                                val remaining = s.classes.filterNot { it.id == event.classInfo.id }
                                s.copy(classes = remaining, selectedId = remaining.firstOrNull()?.id)
                            }
                        }
                        is ClassUIEvent.DeleteClassFailed -> dialogWindow?.dismiss()
                        ClassUIEvent.ShowRefreshUi -> ui.update { it.copy(phase = MyClassPhase.REFRESH) }
                        else -> Unit // edit/add-button/toast events handled in step 2
                    }
                }
            }
        }
    }

    private fun confirmDelete(classInfo: ClassroomInfo) {
        coroutineScope.launch(Dispatchers.Main) {
            dialogWindow = CSSystemDialogWindow.Builder(context)
                .setTitle(context.getString(R.string.dialog_delete_class_title))
                .setMessage(String.format(context.getString(R.string.dialog_delete_class_message), classInfo.displayName.omit(20)))
                .setNegativeButton(context.getString(R.string.common_cancel), context.getColor(R.color.cs_system_dialog_text_color)) {
                    dialogWindow?.dismiss()
                }
                .setPositiveButton(context.getString(R.string.common_delete), context.getColor(R.color.window_my_class_dialog_delete)) {
                    wModel.deleteClassroom(classInfo)
                    dialogWindow?.startPositiveButtonLoading()
                }
                .build()
            dialogWindow?.show()
        }
    }

    /** +New Class — auto-name "New Class$i" (first free index), then create. Mirrors the original. */
    private fun addNewClass() {
        if (ui.value.creating) return
        val prefix = context.getString(R.string.my_class_action_new_class)
        val names = ui.value.classes.map { it.displayName }
        val count = ui.value.classes.size
        val classNum = (1..count).firstOrNull { i -> names.none { it == "$prefix$i" } } ?: -1
        val newName = if (classNum != -1) "$prefix$classNum" else "$prefix${if (count > 0) count + 1 else 1}"
        ui.update { it.copy(creating = true) }
        wModel.createClassroom(newName)
    }

    private fun enterClass(classInfo: ClassroomInfo) {
        if (!wModel.isSocketConnected()) {
            Timber.w("[MyClassWindow] socket not connected, cannot enter class")
            return
        }
        wModel.createLesson(classInfo, tag)
    }

    private fun goBackToSelectOrg() {
        wModel.disconnectSocket()
        wModel.clearClassroomData()
        wModel.stopMultipleLoginCheck()
        CSWindowManager.removeWindow(tag)
        CSWindowManager.removeWindow(WindowTag.WINDOW_QUIZ_COLLECTION)
        CSWindowManager.removeWindow(WindowTag.TOOLBAR)
        CSWindowManager.removeSubWindowsByMainWindowTag(WindowTag.TOOLBAR)
        CSWindowManager.createWindow(SelectOrgWindow(context, previousPageTag = tag), Gravity.CENTER)
    }

    private fun openHub() {
        val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
        val hubUrl = BuildConfig.CLASS_SWIFT_HUB_URL + "?lang=${LanguageUtils.webLanguageCode}"
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(context, hubUrl.toUri())
    }

    override fun onDestroy() {
        super.onDestroy()
        uiCollectJob?.cancel()
        coroutineScope.cancel()
        wModel.onCleared()
    }
}
