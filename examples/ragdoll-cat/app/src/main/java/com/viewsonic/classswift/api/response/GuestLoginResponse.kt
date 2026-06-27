package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.info.GuestOrganizationInfo
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.data.info.UserInfo
import kotlin.Int

@JsonClass(generateAdapter = true)
data class GuestLoginResponse(
    @Json(name = "data")
    val data: GuestLoginData,
)

@JsonClass(generateAdapter = true)
data class GuestLoginData(
    @Json(name = "access_token")
    val accessToken: String = "",
    @Json(name = "user_id")
    val userId: String = "",
    @Json(name = "organization")
    val organization: GuestLoginOrganizationData,
)

@JsonClass(generateAdapter = true)
data class GuestLoginOrganizationData(
    @Json(name = "user_display_name")
    val userDisplayName: String = "",
    @Json(name = "org_display_name")
    val orgDisplayName: String = "",
    @Json(name = "org_id")
    val orgId: String = "",
    @Json(name = "package")
    val packageName: String = "",
    @Json(name = "org_name")
    val orgName: String = "",
    @Json(name = "org_type")
    val orgType: String = "",
    @Json(name = "entity_name")
    val entityName: String = "",
    @Json(name = "roles")
    val roles: List<String> = emptyList(),
    @Json(name = "package_code")
    val packageCode: Int = 0,
    @Json(name = "package_type")
    val packageType: String = "",
    @Json(name = "end_date")
    val endDate: Int? = null,
    @Json(name = "student_concurrent")
    val studentConcurrent: Int = 0,
    @Json(name = "is_individual")
    val isIndividual: Boolean = false,
    @Json(name = "country_code")
    val countryCode: String = "",
    @Json(name = "is_shown")
    val isShown: Boolean = false,
    @Json(name = "is_support_standard")
    val isSupportStandard: Boolean = false,
    @Json(name = "is_default")
    val isDefault: Boolean = false,
    @Json(name = "mvb_role")
    val mvbRole: String = "",
)

fun GuestLoginOrganizationData.toGuestOrgInfo(): GuestOrganizationInfo {
    return GuestOrganizationInfo(
        userDisplayName = userDisplayName,
        orgDisplayName = orgDisplayName,
        orgId = orgId,
        packageName = packageName,
        orgName = orgName,
        orgType = orgType,
        entityName = entityName,
        roles = roles,
        packageCode = packageCode,
        packageType = packageType,
        endDate = endDate,
        studentConcurrent = studentConcurrent,
        isIndividual = isIndividual,
        countryCode = countryCode,
        isShown = isShown,
        isSupportStandard = isSupportStandard,
        isDefault = isDefault,
        mvbRole = mvbRole,
    )
}

fun GuestLoginOrganizationData.toOrganizationInfo(): OrganizationInfo {
    return OrganizationInfo(
        endDate = endDate ?: 0,
        isIndividual = isIndividual,
        orgId = orgId,
        orgName = orgName,
        orgDisplayName = orgDisplayName,
        packageName = packageName,
        packageCode = 0,
        roles = roles,
        studentConcurrent = studentConcurrent,
        userDisplayName = userDisplayName,
        isShownOrgItem = false,
    )
}

fun GuestLoginData.toUserInfo() : UserInfo {
    return UserInfo(
        userId = userId,
    )
}