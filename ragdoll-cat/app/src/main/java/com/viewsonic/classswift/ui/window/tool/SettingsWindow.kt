package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.databinding.WindowSettingsBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.activity.SetLanguageActivity
import com.viewsonic.classswift.ui.fragment.adapter.StringInfoItemAdapter
import com.viewsonic.classswift.ui.widget.CSSpinner
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.ui.window.InAppTutorialWindow
import com.viewsonic.classswift.ui.windowmodel.tool.SettingsWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

class SettingsWindow(val context: Context) : IWindow<WindowSettingsBinding> {

    override var tag: WindowTag = WindowTag.SETTING_WINDOW
    override var size: SizeInPixels = SizeInPixels(400f.dpToPx().toInt(), LayoutParams.WRAP_CONTENT)
    override val binding: WindowSettingsBinding = WindowSettingsBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val windowModel: SettingsWindowModel by inject(SettingsWindowModel::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var showErrorToastJob: Job? = null
    private val isProdReleaseBuild = BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "prod"
    val languageList = context.resources.getStringArray(R.array.language_list).toList()
    val languageAdapter = StringInfoItemAdapter(context, languageList)
    private var languageIndex = if (windowModel.isChineseLanguage(context)) 1 else 0
    private var switchLanguageDialogWindow: CSSystemDialogWindow? = null

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(400f.dpToPx().toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onViewCreated() {
        initUi()
        setLanguageAdapter()
        initCollection()
        initClickAction()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
    }

    private fun initUi() {
        binding.apply {
            if (windowModel.canUsingTranslationTool()) {
                clTranslationTool.background = ResourcesCompat.getDrawable(root.resources, R.drawable.bg_neutral0_no_border_radius400, null)
                tvTranslationMsg.setTextColor(ContextCompat.getColor(context, R.color.cs_main_black_text_color))
                switchTranslate.visibility = View.INVISIBLE // don't using Gone, tvTranslationMsg according to align.
                cpiProgress.visibility = View.VISIBLE
                tvUpgrade.visibility = View.GONE
                // has translation need to get preference
                windowModel.getUserPreference()
            } else {
                clTranslationTool.background = ResourcesCompat.getDrawable(root.resources, R.drawable.bg_neutral200_no_border_radius400, null)
                tvTranslationMsg.setTextColor(ContextCompat.getColor(context, R.color.cs_disable_text_color))
                switchTranslate.visibility = View.VISIBLE
                switchTranslate.isEnabled = false
                cpiProgress.visibility = View.GONE
                tvUpgrade.visibility = View.VISIBLE
            }

            if (isProdReleaseBuild) {
                clDebugTool.visibility = View.GONE
            } else {
                clDebugTool.visibility = View.VISIBLE
                updateSocketToggleButtonState()
                coroutineScope.launch {
                    updateMaintenanceJsonFileSwitchState()
                }
            }
            cssLanguage.setPopUpWindowHeight(86.dpToPx().toInt())
        }
    }

    private fun setLanguageAdapter() {
        binding.apply {
            cssLanguage.displayString = languageList[languageIndex]
            languageAdapter.setSelectedPosition(languageIndex)
            cssLanguage.setAdapter(languageAdapter, object : CSSpinner.OnSpinnerItemSelectedListener {
                override fun onItemSelected(position: Int) {
                    if (position != languageIndex) {
                        showSwitchLanguageDialog()
                    }
                }
            })
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.updateUIFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is SettingsWindowModel.SettingUiEvent.UpdateTranslationSetting -> {
                            setSuccessUi()
                        }

                        is SettingsWindowModel.SettingUiEvent.GetUserPreferenceFailed -> {
                            setRetryUi()
                        }

                        is SettingsWindowModel.SettingUiEvent.UpdateTranslationPreferenceFailed -> {
                            setUpdateTranslationFailedUi()
                        }
                    }
                }
            }
        }
        if (!isProdReleaseBuild) {
            coroutineScope.launch(Dispatchers.IO) {
                windowModel.socketConnectionStateSharedFlow.collect { state ->
                    withContext(Dispatchers.Main) {
                        updateSocketToggleButtonState()
                    }
                }
            }
        }
    }

    private fun initClickAction() {
        binding.apply {
            buttonClose.setOnClickListener {
                csWindowManager.removeWindow(tag)
            }
            actvButtonRetry.setOnClickListener {
                setCallingApiUi()
                windowModel.getUserPreference()
            }
            switchTranslate.setOnClickListener {
                setCallingApiUi()
                //not switch states after click, should check api success or not
                binding.switchTranslate.isChecked = windowModel.isTranslationOn
                windowModel.getSetTranslationPreference()
            }
            tvWatch.setOnClickListener {
                csWindowManager.removeWindow(tag)
                val inAppTutorialWindow: InAppTutorialWindow = get(InAppTutorialWindow::class.java, parameters = { parametersOf(true) })
                csWindowManager.createWindow(
                    window = inAppTutorialWindow,
                    location = Location(0, 0),
                    isOutOfScreen = true,
                    isDraggable = false
                )
            }
            ivLanguageHint.setOnClickListener {
                if (cvLanguageHint.isVisible) {
                    return@setOnClickListener
                }
                cvLanguageHint.visibility = View.VISIBLE
                coroutineScope.launch {
                    delay(THREE_SEC_DELAY)
                    withContext(Dispatchers.Main) {
                        cvLanguageHint.visibility = View.GONE
                    }
                }
            }
            cslbMaintenanceJsonFile.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    cslbMaintenanceJsonFile.setLoading()
                    coroutineScope.launch(Dispatchers.Main) {
                        windowModel.setUseAndroidTestJsonForMaintenanceAnnouncements(!windowModel.isUseAndroidTestJsonForMaintenanceAnnouncements())
                        updateMaintenanceJsonFileSwitchState()
                    }
                }
            })
        }
    }

    private suspend fun updateMaintenanceJsonFileSwitchState() = withContext(Dispatchers.Main) {
        with(binding.cslbMaintenanceJsonFile) {
            setLoading()
            val isUseAndroidTestJsonForMaintenanceAnnouncements = windowModel.isUseAndroidTestJsonForMaintenanceAnnouncements()
            when (isUseAndroidTestJsonForMaintenanceAnnouncements) {
                true -> {
                    setEnableText(context.getString(R.string.eng_debug_tool_maintenance_json_file_from_test))
                }
                false -> {
                    setEnableText(context.getString(R.string.eng_debug_tool_maintenance_json_file_from_server))
                }
            }
            setEnable()
        }
    }

    private fun updateSocketToggleButtonState() {
        with(binding.cslbSocketToggle) {
            when (windowModel.getSocketConnectionState()) {
                SocketManager.ConnectionState.Connected -> {
                    setEnableText(binding.root.context.getString(R.string.common_disconnect))
                    setEnable()
                    setOnCustomClickListener(object : OnLoadingButtonStateListener {
                        override fun onEnableClicked() {
                            setLoading()
                            windowModel.disconnectSocket()
                        }
                    })
                }

                SocketManager.ConnectionState.Connecting -> {
                    setLoading()
                }

                SocketManager.ConnectionState.Disconnected -> {
                    setEnableText(binding.root.context.getString(R.string.common_connect))
                    setEnable()
                    setOnCustomClickListener(object : OnLoadingButtonStateListener {
                        override fun onEnableClicked() {
                            windowModel.connectSocket()
                        }
                    })
                }

                is SocketManager.ConnectionState.Error -> {
                    setEnableText(binding.root.context.getString(R.string.connection_reconnect))
                    setEnable()
                    setOnCustomClickListener(object : OnLoadingButtonStateListener {
                        override fun onEnableClicked() {
                            windowModel.connectSocket()
                        }
                    })
                }
                SocketManager.ConnectionState.Uninitialized -> {}
            }
        }
    }

    private fun setCallingApiUi() {
        binding.apply {
            switchTranslate.visibility = View.INVISIBLE
            cpiProgress.visibility = View.VISIBLE
            actvButtonRetry.visibility = View.GONE
        }
    }

    private fun setRetryUi() {
        binding.apply {
            switchTranslate.visibility = View.INVISIBLE // don't using Gone, tvTranslationMsg according to align.
            cpiProgress.visibility = View.GONE
            actvButtonRetry.visibility = View.VISIBLE
            showErrorToast(context.getString(R.string.settings_translation_tool_error_msg_fetch_translation_tool))
        }
    }

    private fun setUpdateTranslationFailedUi() {
        binding.apply {
            switchTranslate.visibility = View.VISIBLE // don't using Gone, tvTranslationMsg according to align.
            switchTranslate.isSelected = windowModel.isTranslationOn
            cpiProgress.visibility = View.GONE
            actvButtonRetry.visibility = View.GONE
            showErrorToast(context.getString(R.string.settings_translation_tool_error_msg_toggle_translation_tool))
        }
    }

    private fun setSuccessUi() {
        binding.apply {
            switchTranslate.isChecked = windowModel.isTranslationOn
            switchTranslate.visibility = View.VISIBLE
            cpiProgress.visibility = View.GONE
            actvButtonRetry.visibility = View.GONE
        }
    }

    private fun showErrorToast(msg: String) {
        showErrorToastJob?.cancel()
        showErrorToastJob = coroutineScope.launch(Dispatchers.Main) {
            binding.cstToast.visibility = View.VISIBLE
            binding.cstToast.setText(msg)
            withContext(Dispatchers.IO) {
                delay(THREE_SEC_DELAY)
            }
            binding.cstToast.visibility = View.GONE
        }
    }


    private fun showSwitchLanguageDialog() {
        switchLanguageDialogWindow =
            CSSystemDialogWindow.Builder(context)
                .setTitle(context.getString(R.string.switch_language_title))
                .setMessage(context.getString(R.string.switch_language_message))
                .setNegativeButton(
                    context.getString(R.string.common_cancel),
                    context.getColor(R.color.cs_system_dialog_text_color)
                ) {
                    switchLanguageDialogWindow?.dismiss()
                }
                .setPositiveButton(
                    context.getString(R.string.switch_language_select),
                    context.getColor(R.color.color_0A8CF0)
                ) {
                    switchLanguageDialogWindow?.dismiss()
                    csWindowManager.reset()
                    startSetLanguageActivity()
                }
                .build()
        switchLanguageDialogWindow?.show()
    }

    private fun startSetLanguageActivity() {
        context.startActivity(
            Intent(context, SetLanguageActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(SetLanguageActivity.ARG_CHANGE_LANGUAGE, true)
            }
        )
    }
}