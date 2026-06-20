package com.viewsonic.classswift.ui.window

import android.content.Context
import androidx.compose.runtime.Composable
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.feature.servicescreens.ui.OrgItem
import com.viewsonic.classswift.feature.servicescreens.ui.SelectOrgScreen
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.SelectOrgWindowModel
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.toDate
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.utils.LocationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class SelectOrgWindow(
    val context: Context,
    val previousPageTag: WindowTag = WindowTag.DEFAULT,
) : ComposeHostWindow(context) {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val selectOrgWindowModel: SelectOrgWindowModel by inject(SelectOrgWindowModel::class.java)
    private val toolbarManager: ToolbarManager by inject(ToolbarManager::class.java)
    private val socketManager: SocketManager by inject(SocketManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var dialogWindow: CSSystemDialogWindow? = null

    override var tag: WindowTag = WindowTag.CS_SELECT_ORG
    override var size: SizeInPixels = SizeInPixels(350f.dpToPx().toInt(), android.view.WindowManager.LayoutParams.WRAP_CONTENT)

    // Center-gravity card; fixed estimate for centering (don't measure the detached ComposeView).
    override fun getCurrentSize(): SizeInPixels = SizeInPixels(350f.dpToPx().toInt(), 404f.dpToPx().toInt())

    private val orgs: List<OrganizationInfo>
        get() = accountManager.getUserOrganizationInfo().organizations.orEmpty()

    override fun onViewCreated() {
        super.onViewCreated()
        initCollection()
        autoSelectSingleOrg()
    }

    @Composable
    override fun Content() {
        val list = orgs
        SelectOrgScreen(
            orgs = list.map {
                OrgItem(
                    name = it.orgDisplayName,
                    plan = it.displayPlanName(context),
                    expiry = if (it.noExpiredPlan) "" else context.getString(R.string.select_org_exp, it.endDate.toDate()),
                    enabled = it.notExpiredOrg,
                )
            },
            selectedIndex = list.indexOfFirst { it.notExpiredOrg }.coerceAtLeast(0),
            signOutVisible = !selectOrgWindowModel.isMyViewBoardBound(),
            selectEnabled = list.any { it.notExpiredOrg },
            onSignOut = { logOut() },
            onSelect = { i -> list.getOrNull(i)?.let { selectOrgWindowModel.updateSelectedOrganization(it) } },
            onClose = { accountManager.quitApp() },
        )
    }

    /** Single non-expired org → auto-enter (mirrors the native setOrganization path). */
    private fun autoSelectSingleOrg() {
        val notExpired = orgs.filter { it.notExpiredOrg }
        if (notExpired.size == 1 &&
            previousPageTag != WindowTag.CS_SELECT_MY_CLASS &&
            previousPageTag != WindowTag.CS_SELECT_ORG_AND_CLASS
        ) {
            selectOrgWindowModel.updateSelectedOrganization(notExpired[0])
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            selectOrgWindowModel.selectOrgUiStateFlow.collect { uiState ->
                when (uiState) {
                    is SelectOrgWindowModel.SelectOrgUiState.GetOrgPermissionUnauthorized ->
                        orgPermissionHandle(uiState.isCooldown, uiState.isFull)
                    is SelectOrgWindowModel.SelectOrgUiState.GetOrgPermissionSuccessful ->
                        withContext(Dispatchers.Main) {
                            openMyClassWindow()
                            openToolBar()
                            socketManager.connect()
                            accountManager.checkMultipleLogin()
                        }
                    SelectOrgWindowModel.SelectOrgUiState.Idle -> {}
                    SelectOrgWindowModel.SelectOrgUiState.ShowInAppTutorial ->
                        withContext(Dispatchers.Main) {
                            csWindowManager.removeWindow(WindowTag.CS_SELECT_ORG)
                            val inAppTutorialWindow: InAppTutorialWindow =
                                get(InAppTutorialWindow::class.java, parameters = { parametersOf(false) })
                            csWindowManager.createWindow(inAppTutorialWindow, Location(0, 0), isOutOfScreen = true, isDraggable = false)
                        }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            selectOrgWindowModel.enterOrgErrorFlow.collect {
                showErrorDialog(context.getString(R.string.common_notice), context.getString(R.string.select_org_error_msg_select_org_failed))
            }
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            dialogWindow = CSSystemDialogWindow.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.common_confirm), context.getColor(R.color.neutral_900)) {
                    dialogWindow?.dismiss()
                }
                .build()
            dialogWindow?.show()
        }
    }

    private fun orgPermissionHandle(isCoolDown: Boolean, isFull: Boolean) {
        if (isCoolDown) {
            showErrorDialog(context.getString(R.string.common_notice), context.getString(R.string.dialog_permission_error_logged_out_admin_retry))
            return
        }
        if (isFull) {
            showErrorDialog(context.getString(R.string.common_notice), context.getString(R.string.dialog_permission_error_reach_maximum))
        }
    }

    private fun logOut() {
        accountManager.logout()
        csWindowManager.removeWindow(tag)
    }

    private fun openMyClassWindow() {
        CSWindowManager.removeWindow(WindowTag.CS_SELECT_ORG)
        CSWindowManager.createWindow(get(MyClassWindow::class.java), Gravity.CENTER)
    }

    private fun openToolBar() {
        val window: ToolbarWindow = get(ToolbarWindow::class.java)
        val location = LocationUtil.gravityToLocation(Gravity.CENTER_BOTTOM, window.getCurrentSize())
        csWindowManager.createWindow(window, location.apply { coordinateY -= 23.dpToPx().toInt() })
        toolbarManager.setIsExpanded(true)
        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.NOT_JOINED)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
