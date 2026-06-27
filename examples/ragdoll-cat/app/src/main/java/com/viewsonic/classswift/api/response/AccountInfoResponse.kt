package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.data.info.UserInfo
import com.viewsonic.classswift.data.info.UserOrganizationInfo


@JsonClass(generateAdapter = true)
data class AccountInfoResponse(
    @Json(name = "data")
    val accountInfoData: Data = Data(userId = "")
)

@JsonClass(generateAdapter = true)
data class Data(
    val email: String = "",
    @Json(name = "first_name")
    val firstName: String = "",
    @Json(name = "last_name")
    val lastName: String = "",
    val organizations: List<Organization> = listOf(),
    @Json(name = "user_id")
    val userId: String
)

@JsonClass(generateAdapter = true)
data class Organization(
    @Json(name = "end_date")
    val endDate: Int = 0,
    @Json(name = "is_individual")
    val isIndividual: Boolean = false,
    @Json(name = "org_id")
    val orgId: String = "",
    @Json(name = "org_name")
    val orgName: String = "",
    @Json(name = "org_display_name")
    val orgDisplayName: String = "",
    @Json(name = "package")
    val packageName: String = "",
    @Json(name = "package_code")
    val packageCode: Int = 0,
    val roles: List<String>,
    @Json(name = "student_concurrent")
    val studentConcurrent: Int,
    @Json(name = "user_display_name")
    val userDisplayName: String = "",
    @Json(name = "is_shown")
    val isShownOrgItem: Boolean = false
)

fun Organization.toOrgInfo(): OrganizationInfo {
    return OrganizationInfo(
        endDate = endDate,
        isIndividual = isIndividual,
        orgId = orgId,
        orgName = orgName,
        orgDisplayName = orgDisplayName,
        packageName = packageName,
        packageCode = packageCode,
        roles = roles,
        studentConcurrent = studentConcurrent,
        userDisplayName = userDisplayName,
        isShownOrgItem = isShownOrgItem
    )
}

fun AccountInfoResponse.toUserOrgInfo(): UserOrganizationInfo {
    val updatedStudentInfoList = ArrayList<OrganizationInfo>()
    accountInfoData.organizations.forEach { org ->
        if (org.isShownOrgItem) updatedStudentInfoList.add(org.toOrgInfo())
    }
    return UserOrganizationInfo(updatedStudentInfoList, accountInfoData.userId)
}

fun AccountInfoResponse.toUserInfo(): UserInfo {
    return UserInfo(accountInfoData.email, accountInfoData.firstName, accountInfoData.lastName, accountInfoData.userId)
}


