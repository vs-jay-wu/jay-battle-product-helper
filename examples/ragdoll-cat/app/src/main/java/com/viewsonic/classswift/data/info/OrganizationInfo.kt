package com.viewsonic.classswift.data.info

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.utils.DateTimeUtils
import com.viewsonic.classswift.utils.extension.capitalizeFirstLetter

data class OrganizationInfo(
    val endDate: Int = 0,
    val isIndividual: Boolean = false,
    val orgId: String = "",
    val orgName: String = "",
    val orgDisplayName: String ="",
    val packageName: String = "",
    val packageCode: Int = 0,
    val roles: List<String>,
    val studentConcurrent: Int,
    val userDisplayName: String = "",
    val isShownOrgItem: Boolean = false
) {
    // basic and lite doesn't have expired date.
    val noExpiredPlan: Boolean
        get() {
            return packageName.uppercase() == "BASIC" || packageName.uppercase() == "LITE"
        }

    val notExpiredOrg: Boolean
        get() {
            return endDate > DateTimeUtils.getTodayMidnight() || noExpiredPlan
        }

    val isPremiumUser: Boolean
        get() {
            // packageCode > 2 is premium user
            return packageCode > 2
        }

    //顯示在organization 和 my class 的 plan 名稱
    fun displayPlanName(context: Context) =
        packageName.capitalizeFirstLetter() + if (isIndividual) " ${context.getString(R.string.select_org_individual_plan)}" else ""

}

