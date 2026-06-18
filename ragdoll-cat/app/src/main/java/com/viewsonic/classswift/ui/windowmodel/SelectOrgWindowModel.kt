package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.api.SelectOrgApiService
import com.viewsonic.classswift.api.SettingsApiService
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SelectOrgWindowModel(
    private val apiService: SelectOrgApiService,
    private val accountManager: AccountManager,
    private val settingsApiService: SettingsApiService,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) : IWindowModel {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _enterOrgErrorFlow = MutableSharedFlow<Unit>()
    val enterOrgErrorFlow = _enterOrgErrorFlow

    private val _selectOrgUiStateFlow = MutableStateFlow<SelectOrgUiState>(SelectOrgUiState.Idle)
    val selectOrgUiStateFlow: StateFlow<SelectOrgUiState> = _selectOrgUiStateFlow.asStateFlow()

    override fun onCleared() {
    }

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    fun updateSelectedOrganization(org: OrganizationInfo) {
        coroutineScope.launch {
            Timber.d("[B][updateSelectedOrganization] : org = $org")
            accountManager.selectedOrg = org
            AmplitudeEventBuilder(AmplitudeConstant.EventName.ORGANIZATION_SELECTED)
                .appendUserProperty(AmplitudeFactory.UserPropertyType.CURRENT_ORG_DATA)
                .appendUserProperty(AmplitudeFactory.UserPropertyType.ORGS_DETAIL_DATA)
                .send()
            val organizationPermissionResponse = apiService.getOrgPermissions(
                accountManager.getBearerToken(),
                org.orgId,
                accountManager.getUserOrganizationInfo().userId
            )
            when (organizationPermissionResponse) {
                is ApiResponse.Success -> {
                    val isCooldown = organizationPermissionResponse.data.permissionData.isCooldown
                    val isFull = organizationPermissionResponse.data.permissionData.isFull
                    if (isCooldown || isFull) {
                        _selectOrgUiStateFlow.update {
                            SelectOrgUiState.GetOrgPermissionUnauthorized(
                                isCooldown = organizationPermissionResponse.data.permissionData.isCooldown,
                                isFull = organizationPermissionResponse.data.permissionData.isFull)
                        }
                    } else {
                        val isNeedToShowInAppTutorial = isNeedToShowInAppTutorial()
                        // Hide in-app tutorial entry point.
                        when (isNeedToShowInAppTutorial) {
                            true -> {
                                _selectOrgUiStateFlow.update { SelectOrgUiState.ShowInAppTutorial }
                            }
                            false -> {
                                _selectOrgUiStateFlow.update { SelectOrgUiState.GetOrgPermissionSuccessful }
                            }
                        }
                    }
                }
                else -> {
                    _enterOrgErrorFlow.emit(Unit)
                }
            }
        }
    }

    private suspend fun isNeedToShowInAppTutorial(): Boolean = withContext(Dispatchers.IO) {
        return@withContext when (val response = settingsApiService.getUserPreference(accountManager.getBearerToken(), accountManager.selectedOrg?.orgId ?: "")) {
            is ApiResponse.Success -> {
                if (isMyViewBoardBound()) {
                    false
                } else {
                    !response.data.userPreferenceData.tutorial.isShown
                }
            }
            else -> {
                false
            }
        }
    }

    sealed class SelectOrgUiState {
        data object Idle : SelectOrgUiState()
        data object ShowInAppTutorial : SelectOrgUiState()
        data object GetOrgPermissionSuccessful : SelectOrgUiState()
        data class GetOrgPermissionUnauthorized(val isCooldown: Boolean, val isFull: Boolean) : SelectOrgUiState()
    }
}
