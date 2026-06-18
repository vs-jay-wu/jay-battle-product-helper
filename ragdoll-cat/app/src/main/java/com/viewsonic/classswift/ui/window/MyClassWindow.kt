package com.viewsonic.classswift.ui.window

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.view.ContextThemeWrapper
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.databinding.ViewSelectClassPopupWindowBinding
import com.viewsonic.classswift.databinding.WindowMyClassBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.adapter.SelectClassAdapter
import com.viewsonic.classswift.ui.window.helper.TutorialWindowHelper
import com.viewsonic.classswift.ui.windowmodel.MyClassWindowModel
import com.viewsonic.classswift.ui.windowmodel.MyClassWindowModel.ClassUIEvent
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.getLocationOnScreenWithoutStatusBar
import com.viewsonic.classswift.utils.extension.omit
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import androidx.core.net.toUri
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager


class MyClassWindow(
    val context: Context
) : IWindow<WindowMyClassBinding>, SelectClassAdapter.OnItemInteractionListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var uiCollectJob: Job? = null
    private var dialogWindow: CSSystemDialogWindow? = null
    override var tag: WindowTag = WindowTag.CS_SELECT_MY_CLASS
    private var buttonMode = MyClassMode.ENTER_CLASS
    private val wModel: MyClassWindowModel by inject(MyClassWindowModel::class.java)
    private val selectClassAdapter: SelectClassAdapter = SelectClassAdapter(context, this@MyClassWindow)
    private var isNeedToShowClassPageTutorial: Boolean = false

    override var size: SizeInPixels = SizeInPixels(680f.dpToPx().toInt(), 393f.dpToPx().toInt())

    override val binding: WindowMyClassBinding = WindowMyClassBinding.inflate(
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
                View.MeasureSpec.makeMeasureSpec(680f.dpToPx().toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(393f.dpToPx().toInt(), View.MeasureSpec.EXACTLY)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        coroutineScope.launch(Dispatchers.Main) {
            initCollect()
            val isMyViewBoardBound = wModel.isMyViewBoardBound()
            val closeAction: (() -> Unit)? = when {
                isMyViewBoardBound -> {
                    { accountManager.quitApp() }
                }
                !isNeedToShowClassPageTutorial -> {
                    { csWindowManager.removeWindow(tag) }
                }
                else -> {
                    null
                }
            }

            isNeedToShowClassPageTutorial = wModel.isNeedToShowClassPageTutorial()
            if (isNeedToShowClassPageTutorial) {
                csWindowManager.hideWindow(WindowTag.TOOLBAR, isRecordHiddenState = true)
                binding.buttonClose.isEnabled = false
                binding.ivToolbarBringToFront.isEnabled = false
            } else {
                csWindowManager.showWindow(WindowTag.TOOLBAR)
                //關閉視窗
                binding.buttonClose.isEnabled = true
                binding.ivToolbarBringToFront.isEnabled = true
                binding.buttonClose.setOnClickListener {
                    csWindowManager.removeWindow(tag)
                }
                binding.ivToolbarBringToFront.setOnClickListener {
                    coroutineScope.launch(Dispatchers.Main) {
                        csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                    }
                }
            }
            if (isMyViewBoardBound) {
                csWindowManager.hideWindow(WindowTag.TOOLBAR, isRecordHiddenState = true)
                binding.ivToolbarBringToFront.visibility = View.INVISIBLE
            }
            closeAction?.let { action ->
                binding.buttonClose.setOnClickListener { action() }
            }
            initView()
            wModel.getClassroomList()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiCollectJob?.cancel()
        coroutineScope.cancel()
        wModel.onCleared()
    }

    override fun onItemSelected(classroomInfo: ClassroomInfo) {
        setEditClassName(classroomInfo.displayName)
        with(binding) {
            tvNumberOfStudentsValue.text = "${classroomInfo.maxStudentCount}"
            if (classroomInfo.isRoster()) {
                ivNumberHint.isVisible = false
                buttonEdit.setState(LoadingButtonState.DISABLE)
            } else {
                ivNumberHint.isVisible = true
                if (classroomInfo.isLessonOnGoing()) {
                    buttonEdit.setState(LoadingButtonState.DISABLE)
                } else {
                    buttonEdit.setState(LoadingButtonState.ENABLE)
                }
            }
            if (classroomInfo.maxStudentCount == 0) {
                buttonEnterClass.setDisable()
            } else {
                buttonEnterClass.setEnable()
            }
        }
    }

    override fun onItemDelete(classroomInfo: ClassroomInfo) {
        showDeleteClassDialog(
            context.getString(R.string.dialog_delete_class_title),
            String.format(
                context.getString(R.string.dialog_delete_class_message),
                classroomInfo.displayName.omit(20)
            )
        ) {
            Timber.d(
                "** delete Room success Api:  ${classroomInfo.displayName} - ${classroomInfo.id}"
            )
            wModel.deleteClassroom(classroomInfo)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        with(binding) {
            buttonEdit.setState(LoadingButtonState.DISABLE)
            buttonEnterClass.setState(LoadingButtonState.DISABLE)
            buttonAddClass.setState(LoadingButtonState.DISABLE)
            accountManager.selectedOrg?.let {
                tvOrganizationValue.text = it.orgDisplayName
                tvPlanValue.text = "${it.displayPlanName(context)}-${it.studentConcurrent}"
            }
            clInputClassName.isEnabled = false
            tvClassSwiftHub.text = Html.fromHtml(
                "<u>${context.getString(R.string.my_class_action_link_to_hub)}</u>",
                Html.FROM_HTML_MODE_LEGACY
            )
            clBack.setOnClickListener {
                // 回 select org 頁面
                wModel.disconnectSocket()
                wModel.clearClassroomData()
                wModel.stopMultipleLoginCheck()
                CSWindowManager.removeWindow(tag)
                CSWindowManager.removeWindow(WindowTag.WINDOW_QUIZ_COLLECTION)
                CSWindowManager.removeWindow(WindowTag.TOOLBAR)
                CSWindowManager.removeSubWindowsByMainWindowTag(WindowTag.TOOLBAR)
                CSWindowManager.createWindow(
                    SelectOrgWindow(
                        context, previousPageTag = tag
                    ),
                    Gravity.CENTER
                )
            }

            clClassSwiftHub.setOnClickListener {
                //開 Browser 到班級設定頁面
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true) // 顯示標題
                    .build()
                val hubUrl = BuildConfig.CLASS_SWIFT_HUB_URL + "?lang=${LanguageUtils.webLanguageCode}"
                Timber.tag("Hub").d("hub url: $hubUrl")
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                customTabsIntent.launchUrl(context, hubUrl.toUri())
            }

            rvSelectClass.apply {
                layoutManager = LinearLayoutManager(context)
                val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                ContextCompat.getDrawable(context, R.drawable.shape_window_select_org_divider)
                    ?.let {
                        divider.setDrawable(it) // 設置自訂的 drawable 作為分隔線
                    }
                addItemDecoration(divider)
                adapter = selectClassAdapter
            }

            ivPlanHint.setOnClickListener {
                showPopupWindow(
                    ivPlanHint,
                    String.format(
                        context.getString(R.string.my_class_info_plan_maximum),
                        accountManager.selectedOrg?.displayPlanName(context),
                        accountManager.selectedOrg?.studentConcurrent.toString()
                    ), this
                )
            }

            ivNumberHint.setOnClickListener {
                showPopupWindow(
                    ivNumberHint,
                    context.getString(R.string.my_class_info_customize_class), this
                )
            }

            buttonEdit.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onClickedWithState(state: LoadingButtonState) {
                    super.onClickedWithState(state)
                    if (state == LoadingButtonState.DISABLE) {
                        return
                    }
                    when (buttonMode) {
                        MyClassMode.EDIT -> {
                            //do Cancel
                            disableEditClass()
                            with(selectClassAdapter) {
                                etClassName.setText(getSelectedItem().displayName)
                            }
                        }

                        MyClassMode.ENTER_CLASS -> {
                            //do Edit
                            enableEditClass()
                        }
                    }
                }
                override fun onEnableClicked() {
                }
            })

            buttonEnterClass.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onClickedWithState(state: LoadingButtonState) {
                    super.onClickedWithState(state)
                    if (state == LoadingButtonState.DISABLE) {
                        return
                    }
                    when (buttonMode) {
                        MyClassMode.EDIT -> {
                            //do Save
                            with(selectClassAdapter) {
                                val name = etClassName.text.toString()
                                if (name.isBlank()) {
                                    return
                                }
                                Timber.d("** MyClassMode.EDIT ${getSelectedItem().displayName} to $name")
                                if (name == getSelectedItem().displayName) {
                                    disableEditClass()
                                    return
                                }
                                if (currentList.none { it.displayName == name }) {
                                    // 打API 去 update 資料
                                    buttonEnterClass.setState(LoadingButtonState.LOADING)
                                    wModel.updateClassroom(
                                        getSelectedItem().id,
                                        etClassName.text.toString()
                                    )
                                } else {
                                    showErrorToast(context.getString(R.string.my_class_error_msg_class_already_exists))
                                }
                            }
                        }

                        MyClassMode.ENTER_CLASS -> {
                            if (!wModel.isSocketConnected()) {
                                binding.cstErrorToast.run {
                                    setText(context.getString(R.string.error_msg_general_try_again))
                                    show(coroutineScope)
                                }
                                return
                            }
                            buttonEnterClass.setLoading()
                            wModel.createLesson(selectClassAdapter.getSelectedItem(), tag)
                        }
                    }
                }
                override fun onEnableClicked() {
                }
            })

            buttonAddClass.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    with(selectClassAdapter) {
                        val classNum = (1..this.itemCount).find { i ->
                            this.currentList.none { it.displayName == "${context.getString(R.string.my_class_action_new_class)}$i" }
                        } ?: -1

                        val newClassName = if (classNum != -1) {
                            "${context.getString(R.string.my_class_action_new_class)}$classNum"
                        } else {
                            "${context.getString(R.string.my_class_action_new_class)}${if (this.itemCount > 0) itemCount + 1 else 1}"
                        }
                        buttonAddClass.setLoading()
                        wModel.createClassroom(newClassName)
                    }
                }
            })


            etClassName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    if (length == 0) {
                        classEditTextStatus(MyClassEditTextStatus.ERROR)
                    } else {
                        if (clInputClassName.isEnabled) {
                            classEditTextStatus(MyClassEditTextStatus.FOCUS)
                        } else {
                            classEditTextStatus(MyClassEditTextStatus.DISABLE)
                        }
                    }
                    tvClassNameCount.text = "$length/50"
                }
            })

            etClassName.setOnFocusChangeListener { _, hasFocus ->
                Timber.d("** hasFocus : $hasFocus")
                csWindowManager.getWindow(tag)?.getLayoutParam()?.let {
                    if (hasFocus) {
                        it.flags = it.flags and LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                        csWindowManager.getWindow(tag)?.updateLayoutParam(it)
                        classEditTextStatus(MyClassEditTextStatus.FOCUS)
                    } else {
                        // 输入完成后恢复不可聚焦状态
                        classEditTextStatus(MyClassEditTextStatus.ACTIVE)
                        it.flags = it.flags or LayoutParams.FLAG_NOT_FOCUSABLE
                        csWindowManager.getWindow(tag)?.updateLayoutParam(it)
                    }
                }
            }

            tvRefreshPage.setOnClickListener {
                setLoadingUi()
                wModel.refreshClassPage()
            }
        }
    }

    private fun initCollect() {
        uiCollectJob = coroutineScope.launch(Dispatchers.IO) {
            wModel.updateUIFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is ClassUIEvent.UpdateClassList -> {
                            if (isNeedToShowClassPageTutorial) {
                                showClassListTooltip()
                            }

                            if (state.classList.isEmpty()) {
                                wModel.createDefaultClass()
                            }

                            (selectClassAdapter).apply {
                                addItems(state.classList)
                                if (itemCount > 0) {
                                    binding.apply {
                                        if (!buttonEdit.isEnableState()) buttonEdit.setState(LoadingButtonState.ENABLE)
                                        if (!buttonEnterClass.isEnableState()) buttonEnterClass.setState(LoadingButtonState.ENABLE)
                                        if (!buttonAddClass.isEnableState()) buttonAddClass.setState(LoadingButtonState.ENABLE)
                                        setEditClassName(currentList[0].displayName)
                                        tvNumberOfStudentsValue.text = "${currentList[0].maxStudentCount}"
                                    }
                                }
                            }
                            showClassListUi()
                        }
                        is ClassUIEvent.AddClass -> {
                            with(binding.rvSelectClass.adapter as SelectClassAdapter) {
                                addItem(state.classInfo)
                                if (itemCount == 1) {
                                    //如果只有default 要把名字也加進 class Name 欄位
                                    setEditClassName(state.createRoomBody.displayName)
                                    binding.tvNumberOfStudentsValue.text = "${state.createRoomBody.studentCount}"
                                }
                            }
                        }
                        is ClassUIEvent.ShowRefreshUi -> {
                            setRefreshPageUi()
                        }
                        is ClassUIEvent.UpdateClassInfo -> {
                            showSuccessToast(context.getString(R.string.my_class_success_msg_save_class_name))
                            disableEditClass()
                            selectClassAdapter.updateSelectItem(state.classInfo)
                        }
                        is ClassUIEvent.UpdateClassInfoFailed -> {
                            showErrorToast(context.getString(R.string.my_class_error_msg_save_classname))
                            binding.buttonEnterClass.setState(LoadingButtonState.ALERT)
                        }
                        is ClassUIEvent.DeleteClassSuccess -> {
                            dialogWindow?.dismiss()
                            selectClassAdapter.removeItem(state.classInfo)
                            showSuccessToast(context.getString(R.string.my_class_success_msg_delete_class))
                            setEditClassName(selectClassAdapter.getSelectedItem().displayName)
                            binding.tvNumberOfStudentsValue.text = "${selectClassAdapter.getSelectedItem().maxStudentCount}"
                        }
                        is ClassUIEvent.DeleteClassFailed -> {
                            dialogWindow?.dismiss()
                            showErrorToast(String.format(context.getString(R.string.my_class_error_msg_delete_class), state.className))
                        }
                        is ClassUIEvent.EnableAddClassButton -> {
                            binding.buttonAddClass.setState(LoadingButtonState.ENABLE)
                        }

                        is ClassUIEvent.EnableEnterClassButton ->{
                            binding.buttonEnterClass.setState(LoadingButtonState.ENABLE)
                        }

                        is ClassUIEvent.ShowErrorToast -> {
                            showErrorToast(state.msg)
                        }

                        ClassUIEvent.ShouldCheckUnclosedMission -> {
                            unclosedMissionUiManager.setShouldCheckUnclosedMission(true)
                        }
                    }
                }
            }
        }
    }

    //編輯狀態- button 為 Cancel 和 Save
    private fun enableEditClass() {
        (binding.rvSelectClass.adapter as SelectClassAdapter).setIsInEditMode(true)
        buttonMode = MyClassMode.EDIT
        binding.buttonAddClass.setState(LoadingButtonState.DISABLE)
        binding.buttonEdit.setState(LoadingButtonState.ALERT)
        binding.buttonEnterClass.setState(LoadingButtonState.ALERT)
        binding.tvClassNameCount.visibility = View.VISIBLE
        binding.etClassName.visibility = View.VISIBLE
        binding.tvClassName.visibility = View.GONE
        binding.etClassName.apply {
            isEnabled = true
            requestFocus()
            setSelection(this.text?.length ?: 0)
        }
        classEditTextStatus(MyClassEditTextStatus.ACTIVE)
    }

    //取消編輯- button 為 Edit 和 Enter Class
    private fun disableEditClass() {
        (binding.rvSelectClass.adapter as SelectClassAdapter).setIsInEditMode(false)
        buttonMode = MyClassMode.ENTER_CLASS
        binding.buttonEnterClass.setState(LoadingButtonState.ENABLE)
        binding.buttonAddClass.setState(LoadingButtonState.ENABLE)
        binding.buttonEdit.setState(LoadingButtonState.ENABLE)
        binding.tvClassNameCount.visibility = View.GONE
        binding.tvClassName.visibility = View.VISIBLE
        binding.etClassName.visibility = View.GONE
        binding.etClassName.isEnabled = false
        binding.etClassName.setSelection(0)//文字太長就從頭顯示
        classEditTextStatus(MyClassEditTextStatus.DISABLE)
        val params = csWindowManager.getWindow(tag)!!.getLayoutParam()
        params.flags = params.flags and LayoutParams.FLAG_NOT_FOCUSABLE
        csWindowManager.getWindow(tag)?.updateLayoutParam(params)
    }


    private fun showDeleteClassDialog(
        title: String,
        message: String,
        deleteListener: (() -> Unit)? = null
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            dialogWindow =
                CSSystemDialogWindow.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton(
                        context.getString(R.string.common_cancel),
                        context.getColor(R.color.cs_system_dialog_text_color)
                    ) {
                        dialogWindow?.dismiss()
                    }
                    .setPositiveButton(
                        context.getString(R.string.common_delete),
                        context.getColor(R.color.window_my_class_dialog_delete)
                    ) {
                        deleteListener?.invoke()
                        dialogWindow?.startPositiveButtonLoading()
                    }
                    .build()
            dialogWindow?.show()
        }
    }

    private fun showPopupWindow(
        anchorView: View,
        message: String,
        myClassBinding: WindowMyClassBinding
    ) {
        // 加载 PopupWindow 的布局
        val binding = ViewSelectClassPopupWindowBinding.inflate(LayoutInflater.from(context))
        // 初始化 PopupWindow
        val popupWindow = PopupWindow(
            binding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            false // 设置是否可以点击外部关闭
        )
        // 设置阴影高度
        popupWindow.elevation = 2f
        binding.tvPopupWindowMessage.text = message
        binding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        if (anchorView == myClassBinding.ivPlanHint) {
            // 显示於右方
            popupWindow.showAsDropDown(
                anchorView,
                anchorView.width + 4,
                -(anchorView.height)
            )
        } else {
            popupWindow.showAsDropDown(
                anchorView,
                (anchorView.width - binding.root.measuredWidth) / 2,
                4
            )
        }

        coroutineScope.launch(Dispatchers.IO) {
            delay(THREE_SEC_DELAY)
            withContext(Dispatchers.Main) {
                popupWindow.dismiss()
            }
        }
    }

    private fun classEditTextStatus(status: MyClassEditTextStatus) {
        when (status) {
            MyClassEditTextStatus.ACTIVE -> {
                binding.clInputClassName.isEnabled = true
                binding.clInputClassName.setBackgroundResource(R.drawable.bg_window_my_class_name_edit)
            }

            MyClassEditTextStatus.FOCUS -> {
                binding.clInputClassName.isEnabled = true
                binding.clInputClassName.setBackgroundResource(R.drawable.bg_window_my_class_name_edit_focus)
            }

            MyClassEditTextStatus.ERROR -> {
                binding.clInputClassName.isEnabled = true
                binding.clInputClassName.setBackgroundResource(R.drawable.bg_window_my_class_name_edit_error)
            }

            MyClassEditTextStatus.DISABLE -> {
                binding.clInputClassName.isEnabled = false
                binding.clInputClassName.setBackgroundResource(R.drawable.bg_window_my_class_name_edit_disable)
            }
        }
    }

    private fun setEditClassName(name: String) {
        binding.tvClassName.text = name
        binding.etClassName.setText(name)
    }

    private fun setLoadingUi() {
        binding.apply {
            cpiProgress.visibility = View.VISIBLE
            llRefresh.visibility = View.GONE
            rvSelectClass.visibility = View.GONE
        }
    }

    private fun setRefreshPageUi() {
        binding.apply {
            cpiProgress.visibility = View.GONE
            llRefresh.visibility = View.VISIBLE
            rvSelectClass.visibility = View.GONE
        }
    }

    private fun showClassListUi() {
        binding.apply {
            cpiProgress.visibility = View.GONE
            llRefresh.visibility = View.GONE
            rvSelectClass.visibility = View.VISIBLE
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            binding.cstErrorToast.visibility = View.VISIBLE
            binding.cstErrorToast.setText(message)
            withContext(Dispatchers.IO) {
                delay(THREE_SEC_DELAY)
            }
            binding.cstErrorToast.visibility = View.GONE
        }
    }

    private fun showSuccessToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            binding.cstSuccessToast.visibility = View.VISIBLE
            binding.cstSuccessToast.setText(message)
            withContext(Dispatchers.IO) {
                delay(THREE_SEC_DELAY)
            }
            binding.cstSuccessToast.visibility = View.GONE
        }
    }

    private fun showClassListTooltip() {
        val tutorialWindow: TutorialWindow = (csWindowManager.getWindow(WindowTag.WINDOW_TUTORIAL)?.customWindow as? TutorialWindow) ?: get(TutorialWindow::class.java)
        val anchorPosition = binding.clSelectClass.getLocationOnScreenWithoutStatusBar().let {
            it.first + binding.clSelectClass.width to it.second
        }

        if (wModel.hasRosterClassroom()) {
            TutorialWindowHelper.initWithPhaseType(tutorialWindow, TutorialWindowHelper.PhaseType.CLASS_PAGE_ROSTER_CLASS_LIST_PHASE, anchorPosition)
        } else {
            TutorialWindowHelper.initWithPhaseType(tutorialWindow, TutorialWindowHelper.PhaseType.CLASS_PAGE_CLASS_LIST_PHASE, anchorPosition)
        }
        tutorialWindow.apply {
            setNegativeOnClickListener {
                csWindowManager.removeWindow(WindowTag.WINDOW_TUTORIAL)
            }
            setPositiveOnClickListener {
                showEnterClassTooltip()
            }
        }
        csWindowManager.createWindow(
            window = tutorialWindow,
            location = Location(0, 0),
            isOutOfScreen = true,
            isDraggable = false
        )
    }

    private fun showEnterClassTooltip() {
        val tutorialWindow: TutorialWindow = (csWindowManager.getWindow(WindowTag.WINDOW_TUTORIAL)?.customWindow as? TutorialWindow) ?: get(TutorialWindow::class.java)
        val anchorPosition = binding.buttonEnterClass.getLocationOnScreenWithoutStatusBar().let {
            it.first + (binding.buttonEnterClass.width / 2) to it.second
        }

        if (wModel.hasRosterClassroom()) {
            TutorialWindowHelper.initWithPhaseType(tutorialWindow, TutorialWindowHelper.PhaseType.CLASS_PAGE_ROSTER_ENTER_CLASS_PHASE, anchorPosition)
        } else {
            TutorialWindowHelper.initWithPhaseType(tutorialWindow, TutorialWindowHelper.PhaseType.CLASS_PAGE_ENTER_CLASS_PHASE, anchorPosition)
        }

        tutorialWindow.apply {
            setPositiveOnClickListener {
                wModel.setClassPagePhaseLooked()
                csWindowManager.removeWindow(WindowTag.WINDOW_TUTORIAL)
            }
        }
        csWindowManager.createWindow(
            window = tutorialWindow,
            location = Location(0, 0),
            isOutOfScreen = true,
            isDraggable = false
        )
    }
}

enum class MyClassMode {
    EDIT,
    ENTER_CLASS
}

enum class MyClassEditTextStatus {
    ACTIVE, FOCUS, ERROR, DISABLE
}
