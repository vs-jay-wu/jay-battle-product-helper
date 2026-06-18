package com.viewsonic.classswift.data.info

data class GuestOrganizationInfo(
    val userDisplayName: String = "",
    val orgDisplayName: String = "",
    val orgId: String = "",
    val packageName: String = "",
    val orgName: String = "",
    val orgType: String = "",
    val entityName: String = "",
    val roles: List<String> = emptyList(),
    val packageCode: Int = 0,
    val packageType: String = "",
    val endDate: Int? = null,
    val studentConcurrent: Int = 0,
    val isIndividual: Boolean = false,
    val countryCode: String = "",
    val isShown: Boolean = false,
    val isSupportStandard: Boolean = false,
    val isDefault: Boolean = false,
    val mvbRole: String = "",
)

