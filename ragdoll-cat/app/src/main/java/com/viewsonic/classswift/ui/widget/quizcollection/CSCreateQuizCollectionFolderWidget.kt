package com.viewsonic.classswift.ui.widget.quizcollection

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WidgetCsCreateQuizCollectionFolderBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widgetmodel.quizcollection.CSCreateQuizCollectionFolderWidgetModel
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


@SuppressLint("SetTextI18n")
class CSCreateQuizCollectionFolderWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: WidgetCsCreateQuizCollectionFolderBinding =
        WidgetCsCreateQuizCollectionFolderBinding.inflate(LayoutInflater.from(context), this)

    private val widgetModel: CSCreateQuizCollectionFolderWidgetModel by inject(CSCreateQuizCollectionFolderWidgetModel::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val parentWindowTag: WindowTag = WindowTag.WINDOW_QUIZ_COLLECTION
    private var listener: Listener? = null

    init {
        with(binding) {
            setOnClickListener {
                etFolderName.clearFocus()
            }
            tvFolderNameCount.text = "${etFolderName.length()}/30"

            cslbCancel.setOnCustomClickListener(
                object : OnLoadingButtonStateListener {
                    override fun onEnableClicked() {
                        listener?.onCancelled()
                    }
                }
            )

            cslbCreate.setOnCustomClickListener(
                object : OnLoadingButtonStateListener {
                    override fun onEnableClicked() {
                        cslbCreate.setLoading()
                        etFolderName.isEnabled = false
                        createFolder(etFolderName.text.toString())
                    }
                }
            )

            ivClose.setOnClickListener {
                hide()
            }

            etFolderName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable) {
                    hideEditTextError()
                    val length = s.length
                    if (length == 0) {
                        showEditTextError(context.getString(R.string.quiz_collection_folder_input_error_message_empty))
                        cslbCreate.setDisable()
                    } else {
                        cslbCreate.setEnable()
                    }
                    tvFolderNameCount.text = "$length/30"
                }
            })

            etFolderName.setOnFocusChangeListener { _, hasFocus ->
                csWindowManager.getWindow(parentWindowTag)?.getLayoutParam()?.let {
                    if (hasFocus) {
                        it.flags = it.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                        csWindowManager.getWindow(parentWindowTag)?.updateLayoutParam(it)
                    } else {
                        // 输入完成后恢复不可聚焦状态
                        it.flags = it.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        csWindowManager.getWindow(parentWindowTag)?.updateLayoutParam(it)
                    }
                }
            }

        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun show() {
        binding.root.visibility = VISIBLE
    }

    fun hide(isNeedToReset: Boolean = true) {
        if (isNeedToReset) {
            with(binding) {
                hideEditTextError()
                etFolderName.setText(R.string.untitled_folder)
                etFolderName.isEnabled = true
                cslbCreate.setEnable()
            }
        }
        binding.root.visibility = GONE
    }

    private fun showEditTextError(msg: String) {
        with(binding) {
            etFolderName.setBackgroundResource(R.drawable.bg_neutral0_radius200_line_f02b2b_border200)
            tvErrorMsg.text = msg
            tvErrorMsg.visibility = VISIBLE
        }
    }

    private fun hideEditTextError() {
        with(binding) {
            etFolderName.setBackgroundResource(R.drawable.selector_create_folder_edittext)
            tvErrorMsg.text = ""
            tvErrorMsg.visibility = INVISIBLE
        }
    }


    private fun createFolder(folderName: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = widgetModel.createFolder(folderName)
            when (result) {
                CSCreateQuizCollectionFolderWidgetModel.CreateFolderResult.Error -> {
                    listener?.onCreateFolderFailed()
                }
                CSCreateQuizCollectionFolderWidgetModel.CreateFolderResult.IsReservedFolderError -> {
                    showEditTextError(context.getString(R.string.quiz_collection_folder_input_error_message_reserved))
                }
                is CSCreateQuizCollectionFolderWidgetModel.CreateFolderResult.Success -> {
                    listener?.onCreateFolderSuccess(result.folderId)
                }
            }
            binding.etFolderName.isEnabled = true
            binding.cslbCreate.setEnable()
        }
    }


    interface Listener {
        fun onCreateFolderSuccess(folderId: String)
        fun onCreateFolderFailed()
        fun onCancelled()
    }
}

