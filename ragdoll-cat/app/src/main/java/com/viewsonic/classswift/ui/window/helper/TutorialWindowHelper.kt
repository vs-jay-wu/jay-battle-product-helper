package com.viewsonic.classswift.ui.window.helper

import com.viewsonic.classswift.R
import com.viewsonic.classswift.ui.widget.CSTooltipView
import com.viewsonic.classswift.ui.window.TutorialWindow

object TutorialWindowHelper {
    enum class PhaseType {
        CLASS_PAGE_ROSTER_CLASS_LIST_PHASE,
        CLASS_PAGE_ROSTER_ENTER_CLASS_PHASE,
        STUDENT_PAGE_ROSTER_STUDENT_LIST_PHASE,
        STUDENT_PAGE_ROSTER_INVITE_STUDENT_PHASE,
        STUDENT_PAGE_ROSTER_START_LESSON_PHASE,
        CLASS_PAGE_CLASS_LIST_PHASE,
        CLASS_PAGE_ENTER_CLASS_PHASE,
        STUDENT_PAGE_STUDENT_LIST_PHASE,
        STUDENT_PAGE_INVITE_STUDENT_PHASE,
        STUDENT_PAGE_START_LESSON_PHASE,
    }

    fun initWithPhaseType(tutorialWindow: TutorialWindow, phaseType: PhaseType, anchorPosition: Pair<Int, Int>)  {
        val context = tutorialWindow.binding.root.context
        when (phaseType) {
            PhaseType.CLASS_PAGE_ROSTER_CLASS_LIST_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_roster_class_list_phase_title))
                    setDescription(context.getString(R.string.onboarding_roster_class_list_phase_description))
                    setLottieImage(R.drawable.ic_roster_data)
                    setArrowPosition(CSTooltipView.ArrowPosition.LEFT)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.TOP_LEFT
                    )
                    showNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.CLASS_PAGE_ROSTER_ENTER_CLASS_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_roster_enter_class_phase_title))
                    setDescription(context.getString(R.string.onboarding_roster_enter_class_phase_description))
                    setLottieAnimation(R.raw.animation_enter_class)
                    setArrowPosition(CSTooltipView.ArrowPosition.BOTTOM)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.BOTTOM_CENTER
                    )
                    hideNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.STUDENT_PAGE_ROSTER_STUDENT_LIST_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_roster_student_list_phase_title))
                    setDescription(context.getString(R.string.onboarding_roster_student_list_phase_description))
                    setLottieImage(R.drawable.ic_roster_data)
                    setArrowPosition(CSTooltipView.ArrowPosition.RIGHT)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.TOP_RIGHT
                    )
                    showNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.STUDENT_PAGE_ROSTER_INVITE_STUDENT_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_roster_invite_student_phase_title))
                    setDescription(context.getString(R.string.onboarding_roster_invite_student_phase_description))
                    setLottieAnimation(R.raw.animation_join_class)
                    setArrowPosition(CSTooltipView.ArrowPosition.LEFT)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.MIDDLE_LEFT
                    )
                    showNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.STUDENT_PAGE_ROSTER_START_LESSON_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_start_lesson_phase_title))
                    setDescription(context.getString(R.string.onboarding_start_lesson_phase_description))
                    setLottieAnimation(R.raw.animation_start_lesson)
                    setArrowPosition(CSTooltipView.ArrowPosition.BOTTOM)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.BOTTOM_CENTER
                    )
                    hideNegativeButton()
                    setPositiveButtonTitle(R.string.common_understand)
                }
            }
            PhaseType.CLASS_PAGE_CLASS_LIST_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_class_list_phase_title))
                    setDescription(context.getString(R.string.onboarding_class_list_phase_description))
                    setLottieImage(R.drawable.ic_roster_data)
                    setArrowPosition(CSTooltipView.ArrowPosition.LEFT)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.TOP_LEFT
                    )
                    showNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.CLASS_PAGE_ENTER_CLASS_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_enter_class_phase_title))
                    setDescription(context.getString(R.string.onboarding_enter_class_phase_description))
                    setLottieAnimation(R.raw.animation_enter_class)
                    setArrowPosition(CSTooltipView.ArrowPosition.BOTTOM)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.BOTTOM_CENTER
                    )
                    hideNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.STUDENT_PAGE_STUDENT_LIST_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_student_list_phase_title))
                    setDescription(context.getString(R.string.onboarding_student_list_phase_description))
                    setLottieImage(R.drawable.ic_roster_data)
                    setArrowPosition(CSTooltipView.ArrowPosition.RIGHT)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.TOP_RIGHT
                    )
                    showNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.STUDENT_PAGE_INVITE_STUDENT_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_invite_student_phase_title))
                    setDescription(context.getString(R.string.onboarding_invite_student_phase_description))
                    setLottieAnimation(R.raw.animation_join_class)
                    setArrowPosition(CSTooltipView.ArrowPosition.LEFT)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.TOP_LEFT
                    )
                    showNegativeButton()
                    setPositiveButtonTitle(R.string.common_got_it)
                }
            }
            PhaseType.STUDENT_PAGE_START_LESSON_PHASE -> {
                tutorialWindow.apply {
                    setTitle(context.getString(R.string.onboarding_start_lesson_phase_title))
                    setDescription(context.getString(R.string.onboarding_start_lesson_phase_description))
                    setLottieAnimation(R.raw.animation_start_lesson)
                    setArrowPosition(CSTooltipView.ArrowPosition.BOTTOM)
                    setAnchorPosition(
                        anchorPosition.first,
                        anchorPosition.second,
                        TutorialWindow.AnchorGravity.BOTTOM_CENTER
                    )
                    hideNegativeButton()
                    setPositiveButtonTitle(R.string.common_understand)
                }
            }
        }
    }
}