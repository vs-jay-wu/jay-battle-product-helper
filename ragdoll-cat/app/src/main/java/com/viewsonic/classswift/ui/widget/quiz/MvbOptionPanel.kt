package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.databinding.ItemMvbPopupOptionBinding
import com.viewsonic.classswift.databinding.ViewMvbOptionPanelBinding
import com.viewsonic.classswift.databinding.ViewMvbOptionPanelDropdownBinding
import com.viewsonic.classswift.databinding.ViewOptionBoxBinding
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quiz.enums.MvbOptionPanelMode
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import com.viewsonic.classswift.utils.extension.dpToPx

class MvbOptionPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMvbOptionPanelBinding =
        ViewMvbOptionPanelBinding.inflate(LayoutInflater.from(context), this)

    private var optionCount: Int = MIN_OPTIONS
    private var optionValueType: OptionValueType = OptionValueType.ALPHABET
    private var selectionOptionType: SelectionOptionType = SelectionOptionType.SINGLE
    private var mode: MvbOptionPanelMode = MvbOptionPanelMode.DEFAULT
    private var changeListener: OnChangeListener? = null

    private val optionBoxBindings: MutableList<ViewOptionBoxBinding> = mutableListOf()

    private val answerTypesDropdownBinding: ViewMvbOptionPanelDropdownBinding by lazy {
        ViewMvbOptionPanelDropdownBinding.inflate(LayoutInflater.from(context)).also {
            it.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        }
    }

    private val answerOptionsDropdownBinding: ViewMvbOptionPanelDropdownBinding by lazy {
        ViewMvbOptionPanelDropdownBinding.inflate(LayoutInflater.from(context)).also {
            it.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        }
    }

    private val answerTypesPopup: PopupWindow by lazy {
        PopupWindow(
            answerTypesDropdownBinding.root,
            156.66f.dpToPx().toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply { elevation = 6f.dpToPx() }
    }

    private val answerOptionsPopup: PopupWindow by lazy {
        PopupWindow(
            answerOptionsDropdownBinding.root,
            156.66f.dpToPx().toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply { elevation = 6f.dpToPx() }
    }

    init {
        syncOptionBoxes()
        updateAnswerTypesValue()
        updateAnswerOptionsValue()

        binding.cslbAddOption.setOnCustomClickListener( object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                if (optionCount < MAX_OPTIONS) {
                    optionCount++
                    syncOptionBoxes()
                    changeListener?.onOptionCountChanged(optionCount)
                }
            }
        })

        binding.clAnswerTypesInput.setOnClickListener { showAnswerTypesPopup() }
        binding.clAnswerOptionsInput.setOnClickListener { showAnswerOptionsPopup() }
    }

    // region Public API

    fun getOptionNumber(): Int = optionCount

    fun getOptionValueType(): OptionValueType = optionValueType

    fun getSelectionOptionType(): SelectionOptionType = selectionOptionType

    fun setOptionNumber(count: Int) {
        optionCount = count.coerceIn(MIN_OPTIONS, MAX_OPTIONS)
        syncOptionBoxes()
    }

    fun setOptionValueType(type: OptionValueType) {
        optionValueType = type
        syncOptionBoxLabels()
        updateAnswerTypesValue()
    }

    fun setSelectionOptionType(type: SelectionOptionType) {
        selectionOptionType = type
        updateAnswerOptionsValue()
    }

    fun setMode(mode: MvbOptionPanelMode = MvbOptionPanelMode.DEFAULT) {
        this.mode = mode
        updateAnswerOptionsValue()
    }

    fun setOnChangeListener(listener: OnChangeListener) {
        changeListener = listener
    }

    interface OnChangeListener {
        fun onOptionValueTypeChanged(type: OptionValueType)
        fun onSelectionOptionTypeChanged(type: SelectionOptionType)
        fun onOptionCountChanged(count: Int)
    }

    // endregion

    // region Option Boxes

    private fun syncOptionBoxes() {
        val wrapper = binding.llOptionBoxesWrapper
        val existingBoxCount = optionBoxBindings.size

        when {
            existingBoxCount < optionCount -> {
                for (i in existingBoxCount until optionCount) {
                    val boxBinding = buildOptionBox(i)
                    optionBoxBindings.add(boxBinding)
                    wrapper.addView(boxBinding.root, wrapper.childCount - 1)
                }
            }
            existingBoxCount > optionCount -> {
                repeat(existingBoxCount - optionCount) {
                    wrapper.removeViewAt(wrapper.childCount - 2)
                    optionBoxBindings.removeAt(optionBoxBindings.lastIndex)
                }
            }
        }

        syncOptionBoxLabels()

        val trashVisibility = if (optionCount <= MIN_OPTIONS) View.INVISIBLE else View.VISIBLE
        optionBoxBindings.forEach { it.ivTrash.visibility = trashVisibility }

        binding.cslbAddOption.visibility = if (optionCount < MAX_OPTIONS) View.VISIBLE else View.GONE
    }

    private fun syncOptionBoxLabels() {
        optionBoxBindings.forEachIndexed { i, boxBinding ->
            boxBinding.tvOptionLabel.text = when (optionValueType) {
                OptionValueType.ALPHABET -> ('A' + i).toString()
                OptionValueType.NUMBER -> (i + 1).toString()
            }
        }
    }

    private fun buildOptionBox(index: Int): ViewOptionBoxBinding {
        return ViewOptionBoxBinding.inflate(LayoutInflater.from(context)).also { boxBinding ->
            boxBinding.root.layoutParams = LinearLayout.LayoutParams(0, 106.67f.dpToPx().toInt(), 1f).also {
                it.marginEnd = 10.67f.dpToPx().toInt()
            }
            boxBinding.tvOptionLabel.text = when (optionValueType) {
                OptionValueType.ALPHABET -> ('A' + index).toString()
                OptionValueType.NUMBER -> (index + 1).toString()
            }
            boxBinding.ivTrash.setOnClickListener {
                if (optionCount > MIN_OPTIONS) {
                    optionCount--
                    syncOptionBoxes()
                    changeListener?.onOptionCountChanged(optionCount)
                }
            }
        }
    }

    // endregion

    // region Dropdowns

    private fun showAnswerTypesPopup() {
        val root = answerTypesDropdownBinding.llDropdownRoot
        root.removeAllViews()
        listOf(
            OptionValueType.ALPHABET to context.getString(R.string.quiz_option_letter),
            OptionValueType.NUMBER to context.getString(R.string.quiz_option_number)
        ).forEach { (type, label) ->
            root.addView(buildPopupItem(label, isSelected = type == optionValueType) {
                optionValueType = type
                syncOptionBoxLabels()
                updateAnswerTypesValue()
                changeListener?.onOptionValueTypeChanged(type)
                answerTypesPopup.dismiss()
            })
        }
        answerTypesPopup.setOnDismissListener {
            binding.clAnswerTypesInput.isSelected = false
        }
        binding.clAnswerTypesInput.isSelected = true
        answerTypesPopup.showAsDropDown(binding.clAnswerTypesInput, 0, 0)
    }

    private fun showAnswerOptionsPopup() {
        val root = answerOptionsDropdownBinding.llDropdownRoot
        root.removeAllViews()
        listOf(
            SelectionOptionType.SINGLE to selectionOptionLabel(SelectionOptionType.SINGLE),
            SelectionOptionType.MULTIPLE to selectionOptionLabel(SelectionOptionType.MULTIPLE)
        ).forEach { (type, label) ->
            root.addView(buildPopupItem(label, isSelected = type == selectionOptionType) {
                selectionOptionType = type
                updateAnswerOptionsValue()
                changeListener?.onSelectionOptionTypeChanged(type)
                answerOptionsPopup.dismiss()
            })
        }
        answerOptionsPopup.setOnDismissListener {
            binding.clAnswerOptionsInput.isSelected = false
        }
        binding.clAnswerOptionsInput.isSelected = true
        answerOptionsPopup.showAsDropDown(binding.clAnswerOptionsInput, 0, 0)
    }

    private fun buildPopupItem(label: String, isSelected: Boolean, onClick: () -> Unit): View {
        val itemBinding = ItemMvbPopupOptionBinding.inflate(LayoutInflater.from(context))
        itemBinding.root.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        itemBinding.tvPopupOption.text = label
        if (isSelected) {
            itemBinding.tvPopupOption.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(context.getColor(R.color.neutral_300))
                cornerRadius = 5.33f.dpToPx()
            }
        }
        itemBinding.root.setOnClickListener { onClick() }
        return itemBinding.root
    }

    // endregion

    private fun updateAnswerTypesValue() {
        binding.tvAnswerTypesValue.text = when (optionValueType) {
            OptionValueType.ALPHABET -> context.getString(R.string.quiz_option_letter)
            OptionValueType.NUMBER -> context.getString(R.string.quiz_option_number)
        }
    }

    private fun updateAnswerOptionsValue() {
        binding.tvAnswerOptionsValue.text = selectionOptionLabel(selectionOptionType)
    }

    private fun selectionOptionLabel(type: SelectionOptionType): String = when (mode) {
        MvbOptionPanelMode.POLL -> when (type) {
            SelectionOptionType.SINGLE -> context.getString(R.string.quiz_types_poll_single_vote)
            else -> context.getString(R.string.quiz_types_poll_multiple_votes)
        }
        MvbOptionPanelMode.DEFAULT -> when (type) {
            SelectionOptionType.SINGLE -> context.getString(R.string.quiz_mvb_single_select)
            else -> context.getString(R.string.quiz_mvb_multi_select)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (answerTypesPopup.isShowing) answerTypesPopup.dismiss()
        if (answerOptionsPopup.isShowing) answerOptionsPopup.dismiss()
    }

    companion object {
        private const val MIN_OPTIONS = 2
        private const val MAX_OPTIONS = 6
    }
}
