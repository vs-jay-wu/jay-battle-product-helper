package com.viewsonic.classswift.ui.widgetmodel.quizcollection

import com.viewsonic.classswift.api.QuizCollectionApiService
import com.viewsonic.classswift.api.body.CreateQuizCollectionFolderBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.manager.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CSCreateQuizCollectionFolderWidgetModel(
    private val accountManager: AccountManager,
    private val quizCollectionApiService: QuizCollectionApiService
) {
    // Sample server error for a reserved folder name => This folder name %s is reserved for default folder
    private val reservedFolderErrorMessage = "is reserved for default folder"

    suspend fun createFolder(folderName: String): CreateFolderResult = withContext(Dispatchers.IO) {
        accountManager.selectedOrg?.orgId?.let { orgId ->
            val response = quizCollectionApiService.createQuizCollectionFolder(accountManager.getBearerToken(),
                CreateQuizCollectionFolderBody(orgId, folderName)
            )
            when (response) {
                is ApiResponse.ExceptionFailure,
                is ApiResponse.HttpFailure,
                is ApiResponse.NetworkDisconnected -> return@withContext CreateFolderResult.Error
                is ApiResponse.Rfc7807Failure -> {
                    return@withContext if (response.error.detail.contains(reservedFolderErrorMessage)) {
                        CreateFolderResult.IsReservedFolderError
                    } else {
                        CreateFolderResult.Error
                    }
                }
                is ApiResponse.Success -> {
                    return@withContext CreateFolderResult.Success(response.data.id)
                }
            }
        }
        return@withContext CreateFolderResult.Error
    }

    sealed class CreateFolderResult {
        data class Success(val folderId: String) : CreateFolderResult()
        data object Error : CreateFolderResult()
        data object IsReservedFolderError : CreateFolderResult()
    }
}