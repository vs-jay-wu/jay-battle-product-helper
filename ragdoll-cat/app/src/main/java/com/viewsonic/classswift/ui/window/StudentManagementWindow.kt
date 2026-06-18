package com.viewsonic.classswift.ui.window

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.info.StudentDisplayInfo
import com.viewsonic.classswift.data.info.StudentGroupInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.databinding.ViewCopyPromptBinding
import com.viewsonic.classswift.databinding.ViewStudentMoreMenuBinding
import com.viewsonic.classswift.databinding.WindowStudentManagementBinding
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.adapter.StudentGroupListAdapter
import com.viewsonic.classswift.ui.window.adapter.StudentListAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentGroupItemDecoration
import com.viewsonic.classswift.ui.window.decoration.StudentListItemDecoration
import com.viewsonic.classswift.ui.window.helper.TutorialWindowHelper
import com.viewsonic.classswift.ui.window.leaderboard.LeaderboardWindow
import com.viewsonic.classswift.ui.windowmodel.StudentManagementWindowModel
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.QRCodeUtils
import com.viewsonic.classswift.utils.SpannableStringUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.getLocationOnScreenWithoutStatusBar
import com.viewsonic.classswift.utils.extension.mapAndCollect
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.util.Locale

class StudentManagementWindow(
    context: Context
) : IWindow<WindowStudentManagementBinding>,
    StudentListAdapter.OnItemInteractionListener,
    StudentGroupListAdapter.OnGroupItemInteractionListener {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val clipboardManager: ClipboardManager by inject(ClipboardManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider by inject(MyViewBoardConnectionStateProvider::class.java)
    private val studentListAdapter: StudentListAdapter =
        StudentListAdapter(this@StudentManagementWindow)
    private val studentGroupListAdapter: StudentGroupListAdapter =
        StudentGroupListAdapter(this@StudentManagementWindow, this@StudentManagementWindow)
    private val studentManagementWindowModel: StudentManagementWindowModel by inject(
        StudentManagementWindowModel::class.java
    )
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private var dialogWindow: CSSystemDialogWindow? = null
    private var removeStudentDialogWindow: CSSystemDialogWindow? = null
    private var upgradePlanDialogWindow: CSSystemDialogWindow? = null
    private var dismissPopupWindowJob: Job? = null
    private var studentDisplayInfo = StudentDisplayInfo()
    private var isStudentPagePhaseCompleted: Boolean = true
    private val copyCompletedPopupWindow = PopupWindow(
        ViewCopyPromptBinding.inflate(
            LayoutInflater.from(context)
        ).root.apply {
            measure(
                MeasureSpec.UNSPECIFIED,
                MeasureSpec.UNSPECIFIED,
            )
        },
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        true  // Focusable
    )
    private val moreMenuPopupWindow = PopupWindow(
        ViewStudentMoreMenuBinding.inflate(
            LayoutInflater.from(context)
        ).root.apply {
            measure(
                MeasureSpec.UNSPECIFIED,
                MeasureSpec.UNSPECIFIED,
            )
        },
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        true  // Focusable
    ).apply {
        elevation = 6f.dpToPx()
    }

    override var tag: WindowTag = WindowTag.STUDENT_MANAGEMENT

    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )

    @SuppressLint("ClickableViewAccessibility")
    override val binding: WindowStudentManagementBinding = WindowStudentManagementBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        if (!myViewBoardConnectionStateProvider.isBound()) {
            csWindowManager.showWindow(WindowTag.TOOLBAR)
        }
        coroutineScope.launch(Dispatchers.Main) {
            studentManagementWindowModel.setClassPagePhaseCompleted()
            isStudentPagePhaseCompleted = studentManagementWindowModel.isStudentPagePhaseCompleted()
            if (!isStudentPagePhaseCompleted) {
                csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                showStudentListTooltip()
            }
        }
    }

    override fun onViewCreated() {
        dialogWindow =
            CSSystemDialogWindow.Builder(binding.root.context)
                .setTitle(binding.root.context.getString(R.string.error_msg_general))
                .setMessage(binding.root.context.getString(R.string.error_msg_general_try_again))
                .setPositiveButton(
                    binding.root.context.getString(R.string.common_ok),
                    binding.root.context.getColor(R.color.window_my_class_dialog_delete)
                ) {
                    backToMyClass()
                    dialogWindow?.dismiss()
                }
                .build()

        if (!studentManagementWindowModel.isSelectedClassroomInfoExisted()) {
            binding.root.post {
                dialogWindow?.show()
            }
        } else {
            initView()
            initCollection()
            initData()
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        studentManagementWindowModel.onCleared()
    }

    override fun onIncreasePoint(studentInfo: StudentInfo) {
        Timber.d("[B][onIncreaseScore] : studentInfo = $studentInfo")
        coroutineScope.launch {
            val isSuccess =
                studentManagementWindowModel.increaseSpecificStudentPointByOnePoint(studentInfo.studentId)
            withContext(Dispatchers.Main) {
                if (!isSuccess) {
                    showToast(
                        SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                            R.string.student_list_error_msg_failed_to_add_point,
                            "${studentInfo.getActualDisplaySeatNumber()} ${
                                studentInfo.getActualDisplayName(binding.root.context)
                            }"
                        ),
                        false
                    )
                }
            }
        }
    }

    override fun onDecreasePoint(studentInfo: StudentInfo) {
        Timber.d("[B][onDecreaseScore] : studentInfo = $studentInfo")
        coroutineScope.launch {
            val isSuccess =
                studentManagementWindowModel.decreaseSpecificStudentPointByOnePoint(studentInfo.studentId)
            withContext(Dispatchers.Main) {
                if (!isSuccess) {
                    showToast(
                        SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                            R.string.student_list_error_msg_failed_to_subtract_point,
                            "${studentInfo.getActualDisplaySeatNumber()} ${
                                studentInfo.getActualDisplayName(binding.root.context)
                            }"
                        ),
                        false
                    )
                }
            }
        }
    }

    override fun onRemoveStudent(studentInfo: StudentInfo) {
        Timber.d("[B][onRemoveStudent] : studentInfo = $studentInfo")
        val studentName = studentInfo.getActualDisplayName(binding.root.context)
        val displayStudentName = "${studentInfo.getActualDisplaySeatNumber()} $studentName"

        removeStudentDialogWindow?.dismiss()
        removeStudentDialogWindow =
            CSSystemDialogWindow.Builder(binding.root.context)
                .setTitle(binding.root.context.getString(R.string.student_list_remove_student_title))
                .setMessage(
                    SpannableStringUtils.replaceStringFirstArgAsColorStyle(
                        R.string.student_list_remove_student_message,
                        displayStudentName,
                        R.color.color_0A8CF0
                    ),
                )
                .setNegativeButton(
                    binding.root.context.getString(R.string.common_cancel),
                    binding.root.context.getColor(R.color.color_2E3133)
                ) {
                    removeStudentDialogWindow?.dismiss()
                }
                .setPositiveButton(
                    binding.root.context.getString(R.string.common_remove),
                    binding.root.context.getColor(R.color.color_F02B2B)
                ) {
                    if (studentManagementWindowModel.isStudentExisted(studentInfo)) {
                        coroutineScope.launch(Dispatchers.Main) {
                            showLoadingAnimation()
                            val result = studentManagementWindowModel.removeStudent(
                                studentInfo.studentId
                            )
                            hideLoadingAnimation()
                            if (result) {
                                showToast(
                                    SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                                        R.string.student_list_remove_student_success,
                                        displayStudentName
                                    ),
                                    true
                                )
                            } else {
                                showToast(
                                    SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                                        R.string.student_list_error_msg_failed_removed,
                                        displayStudentName
                                    ),
                                    false
                                )
                            }
                        }
                    } else {
                        showToast(
                            SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                                R.string.student_list_error_msg_student_leave,
                                displayStudentName
                            ),
                            false
                        )
                    }
                    exitEditMode()
                    removeStudentDialogWindow?.dismiss()
                }
                .build()
        removeStudentDialogWindow?.show()
    }

    override fun onIncreaseGroupPoint(groupInfo: StudentGroupInfo) {
        Timber.d("onIncreaseGroupPoint -> group number: ${groupInfo.groupId}")
        coroutineScope.launch {
            val isSuccess =
                studentManagementWindowModel.increaseGroupPointByOnePoint(groupInfo.getAttendingStudentIDs())
            withContext(Dispatchers.Main) {
                if (!isSuccess) {
                    showToast(
                        SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                            R.string.student_list_error_msg_failed_to_add_point,
                            binding.root.context.getString(R.string.common_group) + " ${groupInfo.groupId}"
                        ),
                        false
                    )
                }
            }
        }
    }

    override fun onDecreaseGroupPoint(groupInfo: StudentGroupInfo) {
        Timber.d("onDecreaseGroupPoint -> group number: ${groupInfo.groupId}")
        coroutineScope.launch {
            val isSuccess =
                studentManagementWindowModel.decreaseGroupPointByOnePoint(groupInfo.getHasScoreStudentIDs())
            withContext(Dispatchers.Main) {
                if (!isSuccess) {
                    showToast(
                        SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                            R.string.student_list_error_msg_failed_to_subtract_point,
                            binding.root.context.getString(R.string.common_group) + " ${groupInfo.groupId}"
                        ),
                        false
                    )
                }
            }
        }
    }

    private fun initView() {
        showLoadingProgress(isShow = true)

        with(binding) {
            Timber.d("[B][initView] : selectedClassInfo = ${studentManagementWindowModel.getSelectedClassroomInfo()}")
            val selectedClassroomInfo = studentManagementWindowModel.getSelectedClassroomInfo()
            tvJoinClassTitle.text = root.context.getString(
                R.string.join_class_title_join,
                selectedClassroomInfo.displayName
            )
            tvJoinClassId.text = root.context.getString(
                R.string.join_class_info_id,
                selectedClassroomInfo.number
            )
            updateStudentNumber(0, selectedClassroomInfo.maxStudentCount)

            with(tvStudentListTab) {
                isClickable = true
                isSelected = true
                setOnClickListener {
                    studentManagementWindowModel.setGroupState(false)
                }
            }

            with(tvStudentGroupTab) {
                isClickable = true
                isSelected = false
                setOnClickListener {
                    studentManagementWindowModel.setGroupState(true)
                }
            }

            val versionName = BuildConfig.VERSION_NAME
            tvVersion.text =
                root.context.getString(R.string.join_class_info_version, versionName)
            if (BuildConfig.DEBUG) {
                tvVersion.setOnClickListener {
                    val clip = ClipData.newPlainText("Version Name", versionName)
                    clipboardManager.setPrimaryClip(clip)
                    showPopupWindow(tvVersion)
                }
            }

            val lessonLink = selectedClassroomInfo.roomLink
            Timber.d("[B][initView] : lessonLink = $lessonLink")
            QRCodeUtils.generateQRCodeWithBackground(
                text = lessonLink,
                qrSize = 1080,
                bgRadius = 10f.dpToPx()
            )?.let { bitmap ->
                ivQrCode.setImageBitmap(bitmap)
            }

            viewNetworkDisconnect.bindCloseAction(ivClose)
            WindowControlButtonsUiHelper.setup(
                ivClose = ivClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = studentManagementWindowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = { csWindowManager.removeWindow(tag) }
            )
            if (studentManagementWindowModel.isGuestMode()) {
                // 使用 INVISIBLE 維持 Layout 位置一致
                llBackToClassList.visibility = View.INVISIBLE
            }
            llBackToClassList.setOnClickListener {
                goBackToPreviousWindow()
            }
            buttonCancel.setOnClickListener {
                exitEditMode()
            }
            viewNetworkDisconnect.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }

            val viewStudentMoreMenuBinding =
                ViewStudentMoreMenuBinding.bind(moreMenuPopupWindow.contentView)
            viewStudentMoreMenuBinding.tvIncreaseScoreForAll.setOnClickListener {
                coroutineScope.launch {
                    val isSuccess =
                        studentManagementWindowModel.increaseAllStudentsPointByOnePoint()
                    withContext(Dispatchers.Main) {
                        if (!isSuccess) {
                            showToast(
                                SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                                    R.string.student_list_error_msg_failed_to_add_point,
                                    root.context.getString(R.string.student_list_all_students)
                                ),
                                false
                            )
                        }
                        moreMenuPopupWindow.dismiss()
                    }
                }
            }
            viewStudentMoreMenuBinding.tvDecreaseScoreForAll.setOnClickListener {
                coroutineScope.launch {
                    val isSuccess =
                        studentManagementWindowModel.decreaseAllStudentsPointByOnePoint()
                    withContext(Dispatchers.Main) {
                        if (!isSuccess) {
                            showToast(
                                SpannableStringUtils.replaceStringFirstArgAsBoldStyle(
                                    R.string.student_list_error_msg_failed_to_subtract_point,
                                    root.context.getString(R.string.student_list_all_students)
                                ),
                                false
                            )
                        }
                        moreMenuPopupWindow.dismiss()
                    }
                }
            }
            viewStudentMoreMenuBinding.tvRemoveStudent.setOnClickListener {
                enterEditMode()
            }

            ivMore.setOnClickListener {
                if (!moreMenuPopupWindow.isShowing) {
                    moreMenuPopupWindow.showAsDropDown(
                        ivMore,
                        (ivMore.width - moreMenuPopupWindow.contentView.measuredWidth) / 2 - 30.66f.dpToPx()
                            .toInt(),
                        0
                    )
                }
            }

            ivCopyLink.setOnClickListener {
                val clip = ClipData.newPlainText("Class Link", lessonLink)
                clipboardManager.setPrimaryClip(clip)
                showPopupWindow(ivCopyLink)
            }

            ivCopyId.setOnClickListener {
                val clip = ClipData.newPlainText("Classroom ID", selectedClassroomInfo.number)
                clipboardManager.setPrimaryClip(clip)
                showPopupWindow(ivCopyId)
            }

            cslbFaileReload.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    showLoadingProgress(isShow = true)
                    showStudentListLoadFailedHint(isShow = false)
                    initData()
                }
            })
            
            ivPremiumIcon.isVisible = !toolbarManager.toolbarUiState.value.isPremiumUser
            ivLeaderboardIcon.isSelected = toolbarManager.toolbarUiState.value.isPremiumUser
            ivLeaderboardIcon.setOnClickListener {
                if (toolbarManager.toolbarUiState.value.isPremiumUser) {
                    coroutineScope.launch(Dispatchers.Main) {
                        csWindowManager.getWindow(WindowTag.LEADERBOARD_WINDOW)?.hoistWindowZOrder() ?: run {
                            val window: LeaderboardWindow = get(LeaderboardWindow::class.java)
                            csWindowManager.createWindow(
                                window, Gravity.CENTER
                            )
                            AmplitudeEventBuilder(AmplitudeConstant.EventName.LEADERBOARD_CLICKED)
                                .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                                .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                                .send()
                        }
                    }
                } else {
                    showUpgradePlanDialog()
                }
            }
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            studentManagementWindowModel.studentManagementUiState.mapAndCollect({ studentInfoList }) { studentInfoList ->
                Timber.d("[B][initCollection] : collect studentInfoList size = ${studentInfoList.size}")
                if (studentInfoList.isEmpty()) {
                    return@mapAndCollect
                }
                val attendedList = studentInfoList.filter { studentInfo ->
                    studentInfo.status == StudentInfo.Status.ACTIVE
                }
                studentDisplayInfo =
                    studentManagementWindowModel.studentManagementUiState.value.toStudentDisplayInfo()
                Timber.d("[B][initCollection] : studentDisplayInfo: $studentDisplayInfo")
                withContext(Dispatchers.Main) {
                    updateStudentNumber(
                        attendedList.size,
                        studentManagementWindowModel.getSelectedClassroomInfo().maxStudentCount
                    )
                    // update adapter student data.
                    studentListAdapter.submitList(studentDisplayInfo.studentList)
                    studentGroupListAdapter.submitList(studentDisplayInfo.groupList)

                    unclosedMissionUiManager.recoverMissionState()
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            studentManagementWindowModel.studentManagementUiState.mapAndCollect({ isGroup }) { isGroup ->
                Timber.d("[B][initCollection] : collect isGroup: $isGroup}")
                withContext(Dispatchers.Main) {
                    with(binding) {
                        tvStudentListTab.isSelected = !isGroup
                        tvStudentGroupTab.isSelected = isGroup
                        rvStudentList.isVisible = !isGroup
                        rvGroupList.isVisible = isGroup
                    }
                    studentDisplayInfo.isGroup = isGroup
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            studentManagementWindowModel.studentManagementUiState.mapAndCollect({ hasNetwork })  { hasNetwork ->
                Timber.d("[B][initCollection] : collect hasNetwork: $hasNetwork}")
                withContext(Dispatchers.Main) {
                    binding.viewNetworkDisconnect.isVisible = !hasNetwork
                    //When network is unavailable, all PopupWindow should be dismissed
                    if (!hasNetwork) {
                        removeStudentDialogWindow?.dismiss()
                        moreMenuPopupWindow.dismiss()
                        upgradePlanDialogWindow?.dismiss()
                        dismissPopupWindowJob?.cancel()
                        copyCompletedPopupWindow.dismiss()
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT_PROMPT_SHOWN).send()
                    }
                }
            }
        }
    }

    private fun initData() {
        coroutineScope.launch {
            val isSuccess = studentManagementWindowModel.fetchStudentInfoList()
            val context = binding.root.context
            withContext(Dispatchers.Main) {
                showLoadingProgress(isShow = false)
                showStudentListLoadFailedHint(isShow = !isSuccess)

                if (!isSuccess) {
                    showToast(
                        context.getString(R.string.student_list_error_msg_failed_to_find_student),
                        false
                    )
                } else {
                    initStudentList()
                }
            }
        }
    }

    private fun initStudentList() {

        with(binding) {

            rvStudentList.apply {
                layoutManager = GridLayoutManager(root.context, 5)
                addItemDecoration(StudentListItemDecoration(5, 20.6f.dpToPx().toInt()))
                adapter = studentListAdapter
            }

            rvGroupList.apply {
                layoutManager = GridLayoutManager(root.context, 1)
                addItemDecoration(StudentGroupItemDecoration(8f.dpToPx().toInt()))
                adapter = studentGroupListAdapter
            }

            val isStudentTabSelected = tvStudentListTab.isSelected
            rvStudentList.isVisible = isStudentTabSelected
            rvGroupList.isVisible = !isStudentTabSelected
        }
    }

    private fun generateStudentPlaceholderList(): List<StudentInfo> {
        val placeholderList = mutableListOf<StudentInfo>()
        studentManagementWindowModel.getSelectedClassroomInfo().maxStudentCount.takeIf { it >= 0 }
            ?.let { studentCount ->
                for (index in 0 until studentCount) {
                    placeholderList.add(
                        StudentInfo(
                            serialNumber = index + 1
                        )
                    )
                }
            }
        return placeholderList
    }

    private fun enterEditMode() {
        with(binding) {
            studentManagementWindowModel.setEditState(true)
            ivMore.isVisible = false
            buttonCancel.isVisible = true
            moreMenuPopupWindow.dismiss()
        }
    }

    private fun exitEditMode() {
        with(binding) {
            studentManagementWindowModel.setEditState(false)
            ivMore.isVisible = true
            buttonCancel.isVisible = false
        }
    }

    private fun updateStudentNumber(current: Int, total: Int) {
        binding.tvJoinClassStudentNumber.text =
            String.format(Locale.getDefault(), "%02d/%02d", current, total)
    }

    private fun goBackToPreviousWindow() {
        if (toolbarManager.toolbarUiState.value.participationState == ToolbarManager.ParticipationState.LESSON_STARTED) {
            val context = binding.root.context
            csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
            CSSystemDialogWindow.Builder(context)
                .setTitle(context.getString(R.string.dialog_buttons_end_in_session))
                .setMessage(context.getString(R.string.dialog_end_lesson_with_return))
                .setNegativeButton(
                    text = context.getString(R.string.common_cancel),
                    color = ContextCompat.getColor(context, R.color.color_2E3133),
                    listener = {
                        csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
                    }
                )
                .setPositiveButton(
                    text = context.getString(R.string.dialog_buttons_end_lesson),
                    color = ContextCompat.getColor(context, R.color.color_F02B2B),
                    listener = {
                        coroutineScope.launch(Dispatchers.Main) {
                            val isSucceeded = toolbarManager.endLesson()
                            if (isSucceeded) {
                                backToMyClass()
                            }
                            csWindowManager.removeWindow(WindowTag.CS_NORMAL_DIALOG)
                        }
                    }
                )
                .build()
                .show()
        } else {
            backToMyClass()
        }
    }

    private fun showUpgradePlanDialog() {
        upgradePlanDialogWindow?.dismiss()
        val context = binding.root.context
        upgradePlanDialogWindow = CSSystemDialogWindow.Builder(context)
            .setTitle(context.getString(R.string.common_upgrade_your_plan_title))
            .setMessage(context.getString(R.string.common_upgrade_for_leaderboard))
            .setPositiveButton(
                text = context.getString(R.string.common_close),
                color = ContextCompat.getColor(context, R.color.color_2E3133),
                listener = {
                    upgradePlanDialogWindow?.dismiss()
                }
            ).build()
        upgradePlanDialogWindow?.show()
    }

    private fun backToMyClass() {
        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.NOT_JOINED)
        csWindowManager.removeAllWindowsExcept(
            listOf(
                WindowTag.TOOLBAR,
                WindowTag.CS_NORMAL_DIALOG
            )
        )
        if (myViewBoardConnectionStateProvider.isBound()) {
            val window: SelectOrgAndSelectClassWindow = get(SelectOrgAndSelectClassWindow::class.java)
            CSWindowManager.createWindow(window, Gravity.CENTER)
        } else {
            val window: MyClassWindow = get(MyClassWindow::class.java)
            CSWindowManager.createWindow(window, Gravity.CENTER)
        }
    }

    private fun showToast(message: CharSequence, isSuccess: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            with(binding) {
                cstToast.setIsSuccess(isSuccess)
                cstToast.setText(message)
                cstToast.isVisible = true
                withContext(Dispatchers.IO) {
                    delay(3000)
                }
                cstToast.isVisible = false
            }
        }
    }

    private fun showPopupWindow(view: View) {
        dismissPopupWindowJob?.cancel()
        copyCompletedPopupWindow.showAsDropDown(
            view,
            (view.width - copyCompletedPopupWindow.contentView.measuredWidth) / 2,
            0
        )
        delayDismissPopupWindow()
    }

    private fun delayDismissPopupWindow() {
        dismissPopupWindowJob = coroutineScope.launch {
            delay(3000)
            withContext(Dispatchers.Main) {
                copyCompletedPopupWindow.dismiss()
            }
        }
    }

    private fun showLoadingAnimation() {
        binding.cardviewMask.isVisible = true
        binding.csLoadingAnimation.isVisible = true
        binding.csLoadingAnimation.playAnimation()
    }

    private fun hideLoadingAnimation() {
        binding.cardviewMask.isVisible = false
        binding.csLoadingAnimation.isVisible = false
        binding.csLoadingAnimation.cancelAnimation()
    }

    private fun showStudentListTooltip() {
        val tutorialWindow: TutorialWindow =
            (csWindowManager.getWindow(WindowTag.WINDOW_TUTORIAL)?.customWindow as? TutorialWindow)
                ?: get(TutorialWindow::class.java)
        val anchorPosition = binding.rvStudentList.getLocationOnScreenWithoutStatusBar().let {
            it.first + 22.6f.dpToPx().toInt() to it.second + 26.6f.dpToPx().toInt()
        }

        if (studentManagementWindowModel.getSelectedClassroomInfo().isRoster()) {
            TutorialWindowHelper.initWithPhaseType(
                tutorialWindow = tutorialWindow,
                phaseType = TutorialWindowHelper.PhaseType.STUDENT_PAGE_ROSTER_STUDENT_LIST_PHASE,
                anchorPosition = anchorPosition
            )
        } else {
            TutorialWindowHelper.initWithPhaseType(
                tutorialWindow = tutorialWindow,
                phaseType = TutorialWindowHelper.PhaseType.STUDENT_PAGE_STUDENT_LIST_PHASE,
                anchorPosition = anchorPosition
            )
        }
        tutorialWindow.apply {
            setNegativeOnClickListener {
                completeTutorialPhase()
            }
            setPositiveOnClickListener {
                showInviteStudentTooltip()
            }
        }
        csWindowManager.createWindow(
            window = tutorialWindow,
            location = Location(0, 0),
            isOutOfScreen = true,
            isDraggable = false
        )
    }

    private fun showInviteStudentTooltip() {
        val tutorialWindow: TutorialWindow =
            (csWindowManager.getWindow(WindowTag.WINDOW_TUTORIAL)?.customWindow as? TutorialWindow)
                ?: get(TutorialWindow::class.java)

        if (studentManagementWindowModel.getSelectedClassroomInfo().isRoster()) {
            val anchorPosition = binding.ivCopyLink.getLocationOnScreenWithoutStatusBar().let {
                it.first + binding.ivCopyLink.width + 4f.dpToPx()
                    .toInt() to it.second + (binding.ivCopyLink.height / 2)
            }
            TutorialWindowHelper.initWithPhaseType(
                tutorialWindow = tutorialWindow,
                phaseType = TutorialWindowHelper.PhaseType.STUDENT_PAGE_ROSTER_INVITE_STUDENT_PHASE,
                anchorPosition = anchorPosition
            )
        } else {
            val anchorPosition = binding.ivQrCode.getLocationOnScreenWithoutStatusBar().let {
                it.first + binding.ivQrCode.width + 4f.dpToPx().toInt() to it.second
            }
            TutorialWindowHelper.initWithPhaseType(
                tutorialWindow = tutorialWindow,
                phaseType = TutorialWindowHelper.PhaseType.STUDENT_PAGE_INVITE_STUDENT_PHASE,
                anchorPosition = anchorPosition
            )
        }
        tutorialWindow.apply {
            setNegativeOnClickListener {
                completeTutorialPhase()
            }
            setPositiveOnClickListener {
                showStartLessonTooltip()
            }
        }
        csWindowManager.createWindow(
            window = tutorialWindow,
            location = Location(0, 0),
            isOutOfScreen = true,
            isDraggable = false
        )
    }

    private fun showStartLessonTooltip() {
        val tutorialWindow: TutorialWindow =
            (csWindowManager.getWindow(WindowTag.WINDOW_TUTORIAL)?.customWindow as? TutorialWindow)
                ?: get(TutorialWindow::class.java)
        val toolbarWindow: ToolbarWindow =
            csWindowManager.getWindow(WindowTag.TOOLBAR)?.customWindow as? ToolbarWindow ?: get(
                ToolbarWindow::class.java
            )
        val anchorPosition =
            toolbarWindow.binding.cstabStartLesson.getLocationOnScreenWithoutStatusBar().let {
                it.first + toolbarWindow.binding.cstabStartLesson.width / 2 to it.second - 4f.dpToPx()
                    .toInt()
            }

        if (studentManagementWindowModel.getSelectedClassroomInfo().isRoster()) {
            TutorialWindowHelper.initWithPhaseType(
                tutorialWindow,
                TutorialWindowHelper.PhaseType.STUDENT_PAGE_ROSTER_START_LESSON_PHASE,
                anchorPosition
            )
        } else {
            TutorialWindowHelper.initWithPhaseType(
                tutorialWindow,
                TutorialWindowHelper.PhaseType.STUDENT_PAGE_START_LESSON_PHASE,
                anchorPosition
            )
        }
        tutorialWindow.apply {
            setNegativeOnClickListener {
                completeTutorialPhase()
            }
            setPositiveOnClickListener {
                completeTutorialPhase()
            }
        }
        csWindowManager.createWindow(
            window = tutorialWindow,
            location = Location(0, 0),
            isOutOfScreen = true,
            isDraggable = false
        )
    }

    private fun completeTutorialPhase() {
        coroutineScope.launch(Dispatchers.Main) {
            studentManagementWindowModel.setStudentPagePhaseCompleted()
            csWindowManager.removeWindow(WindowTag.WINDOW_TUTORIAL)
        }
    }

    private fun showLoadingProgress(isShow: Boolean) {
        binding.cpiStudentProgressIndicator.isVisible = isShow
    }

    private fun showStudentListLoadFailedHint(isShow: Boolean) {
        binding.clFailedHint.isVisible = isShow
    }
}