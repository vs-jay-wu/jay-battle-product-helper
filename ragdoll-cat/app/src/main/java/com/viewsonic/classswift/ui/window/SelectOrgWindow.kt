package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.databinding.WindowSelectOrgBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.adapter.SelectOrganizationAdapter
import com.viewsonic.classswift.ui.windowmodel.SelectOrgWindowModel
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.utils.LocationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class SelectOrgWindow(
    val context: Context,
    val previousPageTag: WindowTag = WindowTag.DEFAULT
) : IWindow<WindowSelectOrgBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val selectOrgWindowModel: SelectOrgWindowModel by inject(SelectOrgWindowModel::class.java)
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private val socketManager: SocketManager by inject(SocketManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var dialogWindow: CSSystemDialogWindow? = null

    override var tag: WindowTag = WindowTag.CS_SELECT_ORG
    override var size: SizeInPixels =
        SizeInPixels(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    override val binding: WindowSelectOrgBinding = WindowSelectOrgBinding.inflate(
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
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onViewCreated() {
        initCollection()
        initView()
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            selectOrgWindowModel.selectOrgUiStateFlow.collect { uiState ->
                when (uiState) {
                    is SelectOrgWindowModel.SelectOrgUiState.GetOrgPermissionUnauthorized -> {
                        orgPermissionHandle(uiState.isCooldown, uiState.isFull)
                    }
                    is SelectOrgWindowModel.SelectOrgUiState.GetOrgPermissionSuccessful -> {
                        withContext(Dispatchers.Main) {
                            openMyClassWindow()
                            openToolBar()
                            socketManager.connect()
                            accountManager.checkMultipleLogin()
                            binding.buttonSelect.setEnable()
                        }
                    }

                    SelectOrgWindowModel.SelectOrgUiState.Idle -> {}
                    SelectOrgWindowModel.SelectOrgUiState.ShowInAppTutorial -> {
                        withContext(Dispatchers.Main) {
                            csWindowManager.removeWindow(WindowTag.CS_SELECT_ORG)
                            val inAppTutorialWindow: InAppTutorialWindow =
                                get(InAppTutorialWindow::class.java, parameters = { parametersOf(false) })
                            csWindowManager.createWindow(
                                window = inAppTutorialWindow,
                                location = Location(0, 0),
                                isOutOfScreen = true,
                                isDraggable = false
                            )
                        }
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            selectOrgWindowModel.enterOrgErrorFlow.collect { errorMsg ->
                showToast(context.getString(R.string.select_org_error_msg_select_org_failed))
            }
        }
    }

    private fun initView() {
        with(binding) {
            //關閉視窗
            buttonClose.setOnClickListener {
                accountManager.quitApp()
            }

            setOrganization(this, accountManager.getUserOrganizationInfo().organizations)

            if (selectOrgWindowModel.isMyViewBoardBound()) {
                buttonSignOut.isInvisible = true
            } else {
                buttonSignOut.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                    override fun onEnableClicked() {
                        logOut()
                    }
                })
            }

            buttonSelect.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    val org = (rvSelectOrg.adapter as SelectOrganizationAdapter).getSelectedOrganization()
                    if (rvSelectOrg.adapter == null || (rvSelectOrg.adapter as SelectOrganizationAdapter).itemCount == 0 || org == null) {
                        return
                    }
                    selectOrgWindowModel.updateSelectedOrganization(org)
                    buttonSelect.setLoading()
                }
            })
        }
    }

    private suspend fun showToast(message: String) = withContext(Dispatchers.Main) {
        binding.buttonSelect.setEnable()
        binding.cstToast.visibility = View.VISIBLE
        binding.cstToast.setText(message)
        withContext(Dispatchers.IO) {
            delay(3000)
        }
        binding.cstToast.visibility = View.GONE
    }

    private fun showErrorDialog(
        title: String,
        message: String,
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            dialogWindow =
                CSSystemDialogWindow.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(
                        context.getString(R.string.common_confirm),
                        context.getColor(R.color.neutral_900)
                    ) {
                        binding.buttonSelect.setEnable()
                        dialogWindow?.dismiss()
                    }
                    .build()
            dialogWindow?.show()
        }
    }

    private fun orgPermissionHandle(isCoolDown: Boolean, isFull: Boolean) {
        //如果2個都是true , only show  cooldown dialog
        if (isCoolDown) {
            showErrorDialog(
                context.getString(R.string.common_notice),
                context.getString(R.string.dialog_permission_error_logged_out_admin_retry))
            return
        }
        if (isFull) {
            showErrorDialog(
                context.getString(R.string.common_notice),
                context.getString(R.string.dialog_permission_error_reach_maximum)
            )
        }
    }

    private fun logOut() {
        accountManager.logout()
        csWindowManager.removeWindow(tag)
    }

    private fun openMyClassWindow() {
        CSWindowManager.removeWindow(WindowTag.CS_SELECT_ORG)
        val window: MyClassWindow = get(MyClassWindow::class.java)
        CSWindowManager.createWindow(window, Gravity.CENTER)
    }

    private fun openToolBar() {
        val window: ToolbarWindow = get(ToolbarWindow::class.java)
        val location = LocationUtil.gravityToLocation(Gravity.CENTER_BOTTOM, window.getCurrentSize())
        csWindowManager.createWindow(
            window,
            location.apply { coordinateY -= 23.dpToPx().toInt()}
        )
        toolbarManager.setIsExpanded(true)
        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.NOT_JOINED)
    }

    private fun setOrganization(binding: WindowSelectOrgBinding, orgs: List<OrganizationInfo>?) {
        if (orgs.isNullOrEmpty()) {
            binding.buttonSelect.setDisable()
            binding.rvSelectOrg.visibility = View.INVISIBLE
            binding.tvNullOrg.visibility = View.VISIBLE
            return
        } else {
            binding.buttonSelect.setEnable()
            binding.rvSelectOrg.visibility = View.VISIBLE
            binding.tvNullOrg.visibility = View.GONE
        }
        val notExpiredOrgs = orgs.filter { it.notExpiredOrg }

        binding.rvSelectOrg.apply {
            layoutManager = LinearLayoutManager(context)

            val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.shape_window_select_org_divider)?.let {
                divider.setDrawable(it) // 設置自訂的 drawable 作為分隔線
            }
            addItemDecoration(divider)

            adapter = SelectOrganizationAdapter(context, orgs).apply {
                setItemSelectedListener {
                    //選中組織
                    Timber.d("[setOrganization]: SelectOrganizationAdapter ClickListener ${getSelectedOrganization()?.orgDisplayName}")
                }
                setSelectedPosition(orgs.indexOfFirst { it.notExpiredOrg })
            }

            if (notExpiredOrgs.size == 1) {
                if (previousPageTag != WindowTag.CS_SELECT_MY_CLASS && previousPageTag != WindowTag.CS_SELECT_ORG_AND_CLASS) {
                    // 直接跳轉到MY Class,如果是My_CLASS回來，就不用轉跳頁
                    notExpiredOrgs[0].apply {
                        selectOrgWindowModel.updateSelectedOrganization(this)
                    }
                }
            } else if (notExpiredOrgs.isEmpty()) {
                //判斷如果 orgs 的全都到期，select 就 disable
                binding.buttonSelect.setDisable()
            }
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
    }
}

