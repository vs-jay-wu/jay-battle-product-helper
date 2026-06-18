package com.viewsonic.classswift.ui.window

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.clientapp.myviewboard.event.EventWindowStateChangedPayload
import com.viewsonic.classswift.data.clientapp.myviewboard.notifier.MyViewBoardEventNotifier
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.databinding.ItemOrgDropdownRowBinding
import com.viewsonic.classswift.databinding.WindowDeleteClassDialogBinding
import com.viewsonic.classswift.databinding.WindowRenameClassDialogBinding
import com.viewsonic.classswift.databinding.WindowSelectOrgAndSelectClassBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.window.adapter.SelectOrgAndClassAdapter
import com.viewsonic.classswift.ui.windowmodel.SelectOrgAndSelectClassWindowModel
import com.viewsonic.classswift.ui.windowmodel.SelectOrgAndSelectClassWindowModel.ClassUIEvent
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale

class SelectOrgAndSelectClassWindow(
    val context: Context
) : IWindow<WindowSelectOrgAndSelectClassBinding>, SelectOrgAndClassAdapter.OnItemInteractionListener {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val myViewBoardEventNotifier: MyViewBoardEventNotifier by inject(MyViewBoardEventNotifier::class.java)
    private val socketManager: SocketManager by inject(SocketManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var uiCollectJob: Job? = null
    private val deleteDialogTag: WindowTag = WindowTag.CS_NORMAL_DIALOG
    private val renameDialogTag: WindowTag = WindowTag.CS_NORMAL_DIALOG

    override var tag: WindowTag = WindowTag.CS_SELECT_ORG_AND_CLASS
    private val wModel: SelectOrgAndSelectClassWindowModel by inject(SelectOrgAndSelectClassWindowModel::class.java)
    private val selectClassAdapter = SelectOrgAndClassAdapter(context, this)

    private val widthPx: Int = WINDOW_WIDTH_DP.dpToPx().toInt()
    private val heightPx: Int = WINDOW_HEIGHT_DP.dpToPx().toInt()

    override var size: SizeInPixels = SizeInPixels(widthPx, heightPx)

    override val binding: WindowSelectOrgAndSelectClassBinding = WindowSelectOrgAndSelectClassBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_Bridge)
        )
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        socketManager.connect()
        accountManager.checkMultipleLogin()
        initCollect()
        initView()
        wModel.getClassroomList()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiCollectJob?.cancel()
        coroutineScope.cancel()
        wModel.onCleared()
    }

    override fun onItemSelected(classroomInfo: ClassroomInfo) {
        if (classroomInfo.maxStudentCount > 0) {
            binding.buttonEnterClass.setState(LoadingButtonState.ENABLE)
        } else {
            binding.buttonEnterClass.setState(LoadingButtonState.DISABLE)
        }
    }

    override fun onItemDelete(classroomInfo: ClassroomInfo) {
        showDeleteClassDialog(classroomInfo)
    }

    override fun onItemRename(classroomInfo: ClassroomInfo) {
        showRenameClassDialog(classroomInfo)
    }

    override fun onCreateClass() {
        createNewClass()
    }

    private fun initView() {
        setupOrgDropdown()
        bindPlanBadge(accountManager.selectedOrg)
        setupRecyclerView()
        setupButtons()
        setLoadingUi()
        // Clear AllCaps after all setup (setState may re-apply transformation)
        binding.root.postDelayed({
            binding.buttonExit.clearTextTransformation()
            binding.buttonEnterClass.clearTextTransformation()
        }, INITIAL_CLEAR_TRANSFORMATION_DELAY_MS)
    }

    private fun setupOrgDropdown() {
        if (wModel.isMultiOrg) {
            binding.llOrgDropdown.visibility = View.VISIBLE
            binding.tvOrgName.text = wModel.selectedOrg?.orgDisplayName ?: ""
            binding.llOrgDropdown.setOnClickListener { showOrgPopup() }
        } else {
            binding.llOrgDropdown.visibility = View.GONE
        }
    }

    private fun showOrgPopup() {
        // Active state: show blue border on dropdown button
        binding.llOrgDropdown.setBackgroundResource(R.drawable.bg_white_radius400_line_3c5aaa_border2dp)

        val orgList = wModel.orgList
        val popupPaddingPx = ORG_POPUP_PADDING_DP.dpToPx().toInt()
        val popupView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_white_radius800_shadow)
            setPadding(popupPaddingPx, popupPaddingPx, popupPaddingPx, popupPaddingPx)
        }

        val popup = PopupWindow(
            popupView,
            binding.llOrgDropdown.width.coerceAtLeast(ORG_POPUP_MIN_WIDTH_DP.dpToPx().toInt()),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 8f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnDismissListener {
                // Revert to normal border when popup closes
                binding.llOrgDropdown.setBackgroundResource(R.drawable.bg_white_radius400_line_4d4d4d_border1dp)
            }
        }

        val inflater = LayoutInflater.from(context)
        orgList.forEach { org ->
            val isSelected = org.orgId == wModel.orgId
            val isExpired = !org.notExpiredOrg

            val rowBinding = ItemOrgDropdownRowBinding.inflate(inflater, popupView, false)
            if (isSelected && !isExpired) {
                rowBinding.root.setBackgroundResource(R.drawable.bg_neutral300_radius600)
            }

            rowBinding.tvOrgName.apply {
                text = org.orgDisplayName
                setTextColor(
                    if (isExpired) context.getColor(R.color.neutral_500)
                    else context.getColor(R.color.neutral_900)
                )
            }

            if (isExpired) {
                rowBinding.ivClockAlert.visibility = View.VISIBLE
            }

            if (!isExpired) {
                rowBinding.root.setOnClickListener {
                    popup.dismiss()
                    if (!isSelected) {
                        wModel.switchOrg(org)
                    }
                }
            }

            popupView.addView(rowBinding.root)
        }

        popup.showAsDropDown(binding.llOrgDropdown, 0, ORG_POPUP_VERTICAL_OFFSET_DP.dpToPx().toInt())
    }

    private fun bindPlanBadge(org: OrganizationInfo?) {
        org ?: return
        binding.tvPlanName.text = org.displayPlanName(context)
        if (org.noExpiredPlan || org.endDate <= 0) {
            binding.tvExpirationDate.visibility = View.GONE
        } else {
            binding.tvExpirationDate.visibility = View.VISIBLE
            binding.tvExpirationDate.text = formatEndDate(org.endDate)
        }
    }

    private fun formatEndDate(endDateSeconds: Int): String {
        val millis = endDateSeconds.toLong() * MILLIS_PER_SECOND
        return SimpleDateFormat(END_DATE_PATTERN, Locale.ROOT).format(Date(millis))
    }

    private fun setupRecyclerView() {
        binding.rvSelectClass.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = selectClassAdapter
            addItemDecoration(ItemTopSpacingDecoration(ITEM_TOP_SPACING_DP.dpToPx().toInt()))
        }
    }

    /** Top spacing applied to every row except the first (the create-class header). */
    private class ItemTopSpacingDecoration(private val topPx: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position != 0) outRect.top = topPx
        }
    }

    private fun setupButtons() {
        binding.buttonClose.setOnClickListener {
            handleExitClassSwift()
        }

        binding.buttonExit.setOnClickListener {
            handleExitClassSwift()
        }

        binding.buttonEnterClass.setState(LoadingButtonState.DISABLE)
        binding.buttonEnterClass.setOnClickListener {
            if (!wModel.isSocketConnected()) {
                showErrorToast(context.getString(R.string.error_msg_general_try_again))
                return@setOnClickListener
            }
            val selected = selectClassAdapter.getSelectedItemOrNull() ?: return@setOnClickListener
            binding.buttonEnterClass.setState(LoadingButtonState.LOADING)
            wModel.createLesson(selected, tag)
        }

        binding.tvRefreshPage.setOnClickListener {
            setLoadingUi()
            wModel.refreshClassPage()
        }
    }

    private fun handleExitClassSwift() {
        // hideWindow 不帶 isRecordHiddenState → 送 TEMPORARILY_HIDDEN（MVB 會忽略）
        csWindowManager.hideWindow(tag)
        // 手動送 HIDDEN + shouldToggleOff=true → MVB toggle off
        myViewBoardEventNotifier.notifyWindowStateChanged(
            tag,
            EventWindowStateChangedPayload.State.HIDDEN,
            shouldToggleOff = true
        )
    }

    private fun createNewClass() {
        setClassCreatingState(true)
        val now = LocalDateTime.now()
        val existingNames = selectClassAdapter.currentList.map { it.displayName }
        wModel.createTimestampClassroom(now, existingNames)
    }

    private fun setClassCreatingState(isCreating: Boolean) {
        selectClassAdapter.showLoadingPlaceholder = isCreating
        if (isCreating) {
            binding.buttonEnterClass.setState(LoadingButtonState.DISABLE)
        } else {
            binding.buttonEnterClass.setState(LoadingButtonState.ENABLE)
            binding.root.postDelayed(
                { binding.buttonEnterClass.clearTextTransformation() },
                CLEAR_TRANSFORMATION_DELAY_MS
            )
        }
    }

    private fun initCollect() {
        uiCollectJob = coroutineScope.launch(Dispatchers.IO) {
            wModel.updateUIFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    handleUiEvent(state)
                }
            }
        }
    }

    private fun handleUiEvent(state: ClassUIEvent) {
        when (state) {
            is ClassUIEvent.UpdateClassList -> onUpdateClassList(state)
            is ClassUIEvent.AddClass -> onAddClass(state)
            is ClassUIEvent.ShowRefreshUi -> setRefreshPageUi()
            is ClassUIEvent.UpdateClassInfo -> onUpdateClassInfo(state)
            is ClassUIEvent.UpdateClassInfoFailed -> onUpdateClassInfoFailed()
            is ClassUIEvent.DeleteClassSuccess -> onDeleteClassSuccess(state)
            is ClassUIEvent.DeleteClassFailed -> onDeleteClassFailed(state)
            is ClassUIEvent.EnableAddClassButton -> setClassCreatingState(false)
            is ClassUIEvent.EnableEnterClassButton -> {
                binding.buttonEnterClass.setState(LoadingButtonState.ENABLE)
                binding.root.postDelayed(
                    { binding.buttonEnterClass.clearTextTransformation() },
                    CLEAR_TRANSFORMATION_DELAY_MS
                )
            }
            is ClassUIEvent.ShowErrorToast -> showErrorToast(state.msg)
            is ClassUIEvent.ShouldCheckUnclosedMission -> Unit
            is ClassUIEvent.OrgSwitched -> onOrgSwitched(state)
        }
    }

    private fun onOrgSwitched(state: ClassUIEvent.OrgSwitched) {
        binding.tvOrgName.text = state.org.orgDisplayName
        bindPlanBadge(state.org)
        // Clear previous org's classes so AddClass doesn't append to stale list
        // when the new org is empty and a default class is auto-created.
        selectClassAdapter.setItems(emptyList())
        setLoadingUi()
        binding.buttonEnterClass.setState(LoadingButtonState.DISABLE)
    }

    private fun onUpdateClassList(state: ClassUIEvent.UpdateClassList) {
        if (state.classList.isEmpty()) {
            wModel.createDefaultClass()
            return
        }
        selectClassAdapter.setItems(state.classList) {
            updateEnterClassButtonState()
        }
        showClassListUi()
    }

    private fun onAddClass(state: ClassUIEvent.AddClass) {
        selectClassAdapter.addItemToTopOfNonOngoingAndSelect(state.classInfo) {
            updateEnterClassButtonState()
        }
        showClassListUi()
    }

    private fun onUpdateClassInfo(state: ClassUIEvent.UpdateClassInfo) {
        showSuccessToast(context.getString(R.string.my_class_success_msg_save_class_name))
        selectClassAdapter.updateSelectItem(state.classInfo)
    }

    private fun onUpdateClassInfoFailed() {
        showErrorToast(context.getString(R.string.my_class_error_msg_save_classname))
    }

    private fun onDeleteClassSuccess(state: ClassUIEvent.DeleteClassSuccess) {
        csWindowManager.removeWindow(deleteDialogTag)
        selectClassAdapter.removeItem(state.classInfo)
        showSuccessToast(context.getString(R.string.my_class_success_msg_delete_class))
        updateEnterClassButtonState()
    }

    private fun onDeleteClassFailed(state: ClassUIEvent.DeleteClassFailed) {
        csWindowManager.removeWindow(deleteDialogTag)
        showErrorToast(
            String.format(context.getString(R.string.my_class_error_msg_delete_class), state.className)
        )
    }

    private fun showDeleteClassDialog(classroomInfo: ClassroomInfo) {
        coroutineScope.launch(Dispatchers.Main) {
            val screenWidth = DisplayUtils.getScreenSize().first
            val screenHeight = DisplayUtils.getScreenSize().second
            val dialogBinding = WindowDeleteClassDialogBinding.inflate(
                LayoutInflater.from(
                    ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_Bridge)
                )
            )

            val dialogWindow = object : IWindow<WindowDeleteClassDialogBinding> {
                override var tag = deleteDialogTag
                override var size = SizeInPixels(screenWidth, screenHeight)
                override val binding = dialogBinding
            }

            dialogBinding.buttonClose.setOnClickListener {
                csWindowManager.removeWindow(deleteDialogTag)
            }

            dialogBinding.buttonCancel.setOnClickListener {
                csWindowManager.removeWindow(deleteDialogTag)
            }

            dialogBinding.buttonDelete.setOnClickListener {
                Timber.d("** delete class: ${classroomInfo.displayName} - ${classroomInfo.id}")
                wModel.deleteClassroom(classroomInfo)
            }

            csWindowManager.createWindow(dialogWindow, Gravity.CENTER, isOutOfScreen = false)
        }
    }

    private fun showRenameClassDialog(classroomInfo: ClassroomInfo) {
        coroutineScope.launch(Dispatchers.Main) {
            val screenWidth = DisplayUtils.getScreenSize().first
            val screenHeight = DisplayUtils.getScreenSize().second
            val dialogBinding = WindowRenameClassDialogBinding.inflate(
                LayoutInflater.from(
                    ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_Bridge)
                )
            )

            val dialogWindow = object : IWindow<WindowRenameClassDialogBinding> {
                override var tag = renameDialogTag
                override var size = SizeInPixels(screenWidth, screenHeight)
                override val binding = dialogBinding
            }

            // Pre-fill current class name
            dialogBinding.etClassName.setText(classroomInfo.displayName)
            dialogBinding.etClassName.setSelection(classroomInfo.displayName.length)

            // Clear button
            dialogBinding.ivClear.setOnClickListener {
                dialogBinding.etClassName.text?.clear()
            }

            // Text watcher for empty validation
            dialogBinding.etClassName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    val isEmpty = s.isNullOrBlank()
                    if (isEmpty) {
                        dialogBinding.tvError.text = context.getString(R.string.my_class_rename_error_empty)
                        dialogBinding.tvError.visibility = View.VISIBLE
                        dialogBinding.etClassName.setBackgroundResource(R.drawable.bg_rename_input_error)
                        dialogBinding.tvRename.isEnabled = false
                        dialogBinding.tvRename.setBackgroundResource(R.drawable.bg_rename_button_disabled)
                        dialogBinding.tvRename.setTextColor(context.getColor(R.color.neutral_500))
                    } else {
                        dialogBinding.tvError.visibility = View.GONE
                        dialogBinding.etClassName.setBackgroundResource(R.drawable.bg_rename_input)
                        dialogBinding.tvRename.isEnabled = true
                        dialogBinding.tvRename.setBackgroundResource(R.drawable.bg_blue_radius400)
                        dialogBinding.tvRename.setTextColor(context.getColor(R.color.white))
                    }
                }
            })

            dialogBinding.buttonClose.setOnClickListener {
                csWindowManager.removeWindow(renameDialogTag)
            }

            dialogBinding.tvCancel.setOnClickListener {
                csWindowManager.removeWindow(renameDialogTag)
            }

            dialogBinding.tvRename.setOnClickListener {
                val newName = dialogBinding.etClassName.text.toString().trim()
                if (newName.isBlank()) return@setOnClickListener
                if (newName == classroomInfo.displayName) {
                    csWindowManager.removeWindow(renameDialogTag)
                    return@setOnClickListener
                }
                if (selectClassAdapter.currentList.any { it.displayName == newName && it.id != classroomInfo.id }) {
                    // Show inline error, stay in dialog
                    dialogBinding.tvError.text = context.getString(R.string.my_class_error_msg_class_already_exists)
                    dialogBinding.tvError.visibility = View.VISIBLE
                    dialogBinding.etClassName.setBackgroundResource(R.drawable.bg_rename_input_error)
                    return@setOnClickListener
                }
                wModel.updateClassroom(classroomInfo.id, newName)
                csWindowManager.removeWindow(renameDialogTag)
            }

            csWindowManager.createWindow(dialogWindow, Gravity.CENTER, isOutOfScreen = false)

            // Remove FLAG_NOT_FOCUSABLE so EditText can receive keyboard input
            csWindowManager.getWindow(renameDialogTag)?.let { container ->
                val params = container.floatWindowLayoutParam
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                container.updateLayoutParam(params)
            }
        }
    }

    private fun updateEnterClassButtonState() {
        val selected = selectClassAdapter.getSelectedItemOrNull() ?: return
        val canEnter = selected.maxStudentCount > 0
        binding.buttonEnterClass.setState(if (canEnter) LoadingButtonState.ENABLE else LoadingButtonState.DISABLE)
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
        binding.cstErrorToast.setText(message)
        binding.cstErrorToast.show(coroutineScope)
    }

    private fun showSuccessToast(message: String) {
        binding.cstSuccessToast.setText(message)
        binding.cstSuccessToast.show(coroutineScope)
    }

    companion object {
        // Window portrait size — matches window_select_org_and_select_class.xml root
        // (333.33dp x 453.33dp). Kept inline because IWindow.size is required at
        // construction time and the layout XML defines the same dimensions.
        private const val WINDOW_WIDTH_DP: Float = 333.33f
        private const val WINDOW_HEIGHT_DP: Float = 453.33f

        private const val ITEM_TOP_SPACING_DP: Float = 8f

        private const val ORG_POPUP_PADDING_DP: Float = 5.33f
        private const val ORG_POPUP_MIN_WIDTH_DP: Float = 133.33f
        private const val ORG_POPUP_VERTICAL_OFFSET_DP: Float = 2.67f

        // Defer clearTextTransformation until after the LoadingButton finishes applying
        // its state-driven style — see ClassSwiftLoadingButton.setState.
        private const val CLEAR_TRANSFORMATION_DELAY_MS: Long = 50L
        private const val INITIAL_CLEAR_TRANSFORMATION_DELAY_MS: Long = 200L

        private const val MILLIS_PER_SECOND: Long = 1000L
        private const val END_DATE_PATTERN: String = "yyyy/MM/dd"
    }
}
