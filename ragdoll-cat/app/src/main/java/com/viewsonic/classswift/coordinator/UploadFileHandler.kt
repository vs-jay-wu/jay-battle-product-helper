package com.viewsonic.classswift.coordinator

import android.content.Context
import android.net.Uri
import com.viewsonic.classswift.api.UploadFileApiService
import com.viewsonic.classswift.api.amazon.S3UploadService
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.uncategorized.AwsPreSignedUrl
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.CoroutineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File
import androidx.core.net.toUri

class UploadFileHandler(
    private val applicationContext: Context,
    private val uploadApiService: UploadFileApiService,
    private val accountManager: AccountManager,
    private val amazonClient: Retrofit
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var fetchPreSignedUrlJob: Job? = null
    private var uploadImageStatusJob: Job? = null

    val awsPreSignedUrl: AwsPreSignedUrl = AwsPreSignedUrl(s3PutUrl = "", s3GetUrl = "")

    private val _uploadImageSharedFlow = MutableSharedFlow<Boolean>(replay = 1)
    val uploadImageSharedFlow = _uploadImageSharedFlow.asSharedFlow()

    /**
     * get preSignedUrl and start ImageUploading process
     */
    fun fetchPreSignedUrl(lessonId: String, imageUrl: String) {
        fetchPreSignedUrlJob?.cancel()
        fetchPreSignedUrlJob = coroutineScope.launch(Dispatchers.IO) {
            val response = uploadApiService.getPreSignedUrl(
                token = accountManager.getBearerToken(),
                lessonId = lessonId
            )

            when (response) {
                is ApiResponse.NetworkDisconnected -> {
                    emitImageUploadResultEvent(isSuccess = false)
                }

                is ApiResponse.Success -> {
                    Timber.d(
                        "fetchPreSignedUrl: Success, putUrl: " +
                                "${response.data.put}, getUrl: ${response.data.get}"
                    )
                    updateS3Urls(response.data.put, response.data.get)
                    startUploadingProcess(localScreenshotUri = imageUrl)
                }

                else -> {
                    emitImageUploadResultEvent(isSuccess = false)
                }
            }
        }
    }

    fun uploadImageToS3UsingContentUri(lessonId: String, imageUrl: String) {
        fetchPreSignedUrlJob?.cancel()
        fetchPreSignedUrlJob = coroutineScope.launch(Dispatchers.IO) {

            val response = uploadApiService.getPreSignedUrl(
                token = accountManager.getBearerToken(),
                lessonId = lessonId
            )

            when (response) {
                is ApiResponse.Success -> {
                    Timber.d(
                        "fetchPreSignedUrl: Success, putUrl: " +
                                "${response.data.put}, getUrl: ${response.data.get}"
                    )
                    updateS3Urls(response.data.put, response.data.get)
                    startUploadingProcessByContentUri(localContentUri = imageUrl)
                }

                else -> {
                    removeUploadFileInstance(applicationContext, imageUrl.toUri())
                    emitImageUploadResultEvent(isSuccess = false)
                }
            }
        }
    }

    private suspend fun startUploadingProcess(localScreenshotUri: String) {
        Timber.d("Local Screenshot Uri: $localScreenshotUri")

        val imageFile = File(localScreenshotUri)
        val isSuccessful = uploadImageToS3(imageFile)

        Timber.d("uploadImage result: $isSuccessful")
        emitImageUploadResultEvent(isSuccess = isSuccessful)
    }

    /**
     * Upload local screen shot image to AWS S3 using content uri
     */
    private suspend fun uploadImageToS3(
        imageFile: File,
        preSignedUrl: String = awsPreSignedUrl.s3PutUrl
    ): Boolean = withContext(Dispatchers.IO) {

        if (preSignedUrl.isEmpty()) {
            return@withContext false
        }

        val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val service = amazonClient.create(S3UploadService::class.java)

        try {
            val response = service.uploadImage(preSignedUrl, requestBody).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Timber.e("uploadImage error: ${e.message}")
            return@withContext false
        }
    }

    private suspend fun startUploadingProcessByContentUri(localContentUri: String) {
        Timber.d("Local Screenshot Uri: $localContentUri")
        val uri = localContentUri.toUri()
        val isSuccessful = uploadImageToS3ByContentUri(applicationContext, uri)
        Timber.d("uploadImage result: $isSuccessful")
        emitImageUploadResultEvent(isSuccess = isSuccessful)
    }

    /**
     * Upload local screen shot image to AWS S3 using content uri
     */
    private suspend fun uploadImageToS3ByContentUri(
        context: Context,
        uri: Uri,
        preSignedUrl: String = awsPreSignedUrl.s3PutUrl
    ): Boolean = withContext(Dispatchers.IO) {
        if (preSignedUrl.isEmpty()) {
            return@withContext false
        }

        val service = amazonClient.create(S3UploadService::class.java)

        return@withContext try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val requestBody = inputStream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
                val response = service.uploadImage(preSignedUrl, requestBody).execute()
                //Remove local image file
                removeUploadFileInstance(context, uri)
                response.isSuccessful
            } ?: false
        } catch (e: Exception) {
            Timber.e("uploadImage error: ${e.message}")
            false
        }
    }

    private fun removeUploadFileInstance(context: Context, uri: Uri) {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            Timber.d("Deleted $rows rows for uri: $uri")
        } catch (ex: Exception) {
            Timber.w("Failed to delete contentUri: $uri, error=${ex.message}")
        }
    }

    private fun updateS3Urls(putUrl: String, getUrl: String) {
        awsPreSignedUrl.apply {
            s3PutUrl = putUrl
            s3GetUrl = getUrl
        }
    }

    private fun emitImageUploadResultEvent(isSuccess: Boolean) {
        uploadImageStatusJob?.cancel()
        uploadImageStatusJob = coroutineScope.launch(Dispatchers.IO) {
            _uploadImageSharedFlow.emit(isSuccess)
        }
    }
}