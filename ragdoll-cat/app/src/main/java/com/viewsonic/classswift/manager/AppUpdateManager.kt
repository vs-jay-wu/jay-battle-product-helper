package com.viewsonic.classswift.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.api.OtaUpdateApiService
import com.viewsonic.classswift.api.VSApiGateway
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.constant.FirebaseAnalyticsConstant
import com.viewsonic.classswift.utils.OtaUpdateUtils
import com.viewsonic.classswift.utils.SystemInfoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume

class AppUpdateManager(
    private val applicationContext: Context,
    private val otaUpdateApiService: OtaUpdateApiService,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val vsApiGateway: VSApiGateway,
) {
    private val coroutineScope = CoroutineManager.getScope(this)
    private val defaultAppUpdateTimeoutInMillis: Long = 30000
    var isChecked = false
        private set

    private val _updateSharedFlow = MutableSharedFlow<UpdateState>()
    val updateSharedFlow: SharedFlow<UpdateState> = _updateSharedFlow.asSharedFlow()

    suspend fun checkAppAvailability(): AppAvailabilityResult = withContext(Dispatchers.IO) {
        when (val apiResponse = otaUpdateApiService.fetchVersionInfo("version.json")) {
            is ApiResponse.Success -> {
                Timber.d("[B][checkAppAvailability] : Success = ${apiResponse.data}")
                val vsModelName = SystemInfoUtils.getModelName()
                val modelName = Build.MODEL
                firebaseAnalytics.logEvent(FirebaseAnalyticsConstant.Name.CHECK_APP_AVAILABILITY) {
                    param(FirebaseAnalyticsConstant.Key.BUILD_MODEL_NAME, modelName)
                    param(FirebaseAnalyticsConstant.Key.VS_MODEL_NAME, vsModelName)
                }
                if (apiResponse.data.isBypassOta) {
                    return@withContext AppAvailabilityResult.Available
                }
                // Look up by VS model name first (precise), then by Build.MODEL.
                val supportedDeviceMap =
                    apiResponse.data.versionInfoList.associate { it.modelName to it.appVersion } +
                    apiResponse.data.versionInfoList.associate { it.vsModelName to it.appVersion }
                val appVersion = supportedDeviceMap[vsModelName]
                    ?: supportedDeviceMap[modelName]
                    ?: apiResponse.data.latestVersion.takeIf { it.isNotBlank() }
                appVersion?.let { latestVersion ->
                    val latestVersionCode = OtaUpdateUtils.releaseVersionNameToVersionCode(latestVersion)
                    val currentVersionCode = BuildConfig.VERSION_CODE
                    Timber.d("[B][checkAppAvailability] : latestVersionCode  = $latestVersionCode")
                    Timber.d("[B][checkAppAvailability] : currentVersionCode = $currentVersionCode")
                    if (latestVersionCode > currentVersionCode) {
                        return@withContext AppAvailabilityResult.NeedToUpdate(latestVersion)
                    }
                } ?: run {
                    return@withContext AppAvailabilityResult.NotAllowedDevice
                }
            }
            else -> return@withContext AppAvailabilityResult.NetworkError
        }
        return@withContext AppAvailabilityResult.Available
    }

    fun canRequestPackageInstalls(): Boolean {
        // Pre-flight for the PackageInstaller fallback path. VSApi install does
        // not require this permission, but we keep the conservative check so that
        // devices without VSApi can still emit a clean error rather than crashing
        // mid-install.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun emitUnknownSourceInstallDisallowed() {
        emitUpdateState(UpdateState.Error(UpdateError.UNKNOWN_SOURCE_INSTALLS_DISALLOWED))
    }

    fun setIsCheck() {
        isChecked = true
    }

    suspend fun executeApkUpdateFlow(appLatestVersion: String) = withContext(Dispatchers.IO) {
        val executionResult = withTimeoutOrNull(defaultAppUpdateTimeoutInMillis) {
            emitUpdateState(UpdateState.Downloading(progress = 0))
            val serverFileName = OtaUpdateUtils.getApkDownloadFileName(appLatestVersion)
            Timber.d("[B][executeApkUpdateFlow] : appLatestVersion = $appLatestVersion")
            Timber.d("[B][executeApkUpdateFlow] : serverFileName = $serverFileName")

            val response = otaUpdateApiService.downloadApkWithFileName(serverFileName)
            when (response) {
                is ApiResponse.Success -> {
                    val apkFilePath = saveApkToDownloadFolder(serverFileName, response.data) { progress ->
                        emitUpdateState(UpdateState.Downloading(progress))
                    }
                    if (apkFilePath.isEmpty()) {
                        emitUpdateState(UpdateState.Error(UpdateError.DOWNLOAD_APK_FAILED))
                        return@withTimeoutOrNull true
                    }

                    emitUpdateState(UpdateState.Done)

                    // Try VSApi.installApp first; fall back to PackageInstaller on null/false.
                    Timber.d("[B][executeApkUpdateFlow] : try VSApi.installApp at $apkFilePath")
                    val vsResult = vsApiGateway.installApp(apkFilePath)
                    if (vsResult == true) {
                        Timber.d("[B][executeApkUpdateFlow] : VSApi install succeeded")
                        return@withTimeoutOrNull true
                    }
                    Timber.d(
                        "[B][executeApkUpdateFlow] : VSApi install ${if (vsResult == false) "reported failure" else "unavailable"}, falling back to PackageInstaller"
                    )
                    val installResult = installByPackageInstaller(File(apkFilePath))
                    if (!installResult.isSuccess) {
                        Timber.w(
                            "[B][executeApkUpdateFlow] : PackageInstaller failed, status=${installResult.status} message=${installResult.statusMessage}"
                        )
                        emitUpdateState(UpdateState.Error(UpdateError.INSTALLATION_FAILED))
                    }
                }
                else -> {
                    emitUpdateState(UpdateState.Error(UpdateError.DOWNLOAD_APK_FAILED))
                }
            }
            return@withTimeoutOrNull true
        }
        if (executionResult == null) {
            emitUpdateState(UpdateState.Error(UpdateError.TIME_OUT))
        }
    }

    sealed class UpdateState {
        data class Downloading(val progress: Int) : UpdateState()
        data object Done : UpdateState()
        data class Error(val error: UpdateError) : UpdateState()
    }

    enum class UpdateError {
        DOWNLOAD_APK_FAILED,
        UNKNOWN_SOURCE_INSTALLS_DISALLOWED,
        INSTALLATION_FAILED,
        TIME_OUT
    }

    sealed class AppAvailabilityResult {
        data object NotAllowedDevice : AppAvailabilityResult()
        data object Available : AppAvailabilityResult()
        data object NetworkError : AppAvailabilityResult()
        class NeedToUpdate(
            val latestReleaseVersionName: String
        ) : AppAvailabilityResult()
    }

    private suspend fun saveApkToDownloadFolder(
        apkFileName: String,
        body: ResponseBody,
        onProgress: (Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val inputStream: InputStream = body.byteStream()
        var filePath = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val existingUri = findFileInDownloads(applicationContext, apkFileName)
            existingUri?.let {
                applicationContext.contentResolver.delete(it, null, null)
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, apkFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val contentResolver = applicationContext.contentResolver
            val savedUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            Timber.d("[B][saveApkToDownloadFolder] : savedUri = $savedUri")
            savedUri?.let { uri ->
                val openOutputStream = uri.let { contentResolver.openOutputStream(it) }
                openOutputStream?.let { outputStream ->
                    val result = downloadFileToOutputStream(inputStream, outputStream, body.contentLength(), onProgress)
                    Timber.d("[B][saveApkToDownloadFolder] : Android 10+ result = $result")

                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)

                    filePath = getRealPathFromUri(applicationContext, uri)
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, apkFileName)

            val result = downloadFileToOutputStream(
                inputStream,
                FileOutputStream(file),
                body.contentLength(),
                onProgress
            )
            Timber.d("[B][saveApkToDownloadFolder] : Android 9 result = $result")
            filePath = file.absolutePath
        }
        Timber.d("[B][saveApkToDownloadFolder] : filePath = $filePath")
        return@withContext filePath
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findFileInDownloads(context: Context, fileName: String): Uri? {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return null
    }

    private suspend fun downloadFileToOutputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        contentLength: Long,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val buffer = ByteArray(4096)
            var totalBytesRead = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0L) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }
            outputStream.flush()
        } catch (e: Exception) {
            Timber.e(e, "[B][downloadFileToOutputStream] : exception")
            return@withContext false
        } finally {
            inputStream.close()
            outputStream.close()
        }
        return@withContext true
    }

    private fun getRealPathFromUri(context: Context, uri: Uri): String {
        var filePath: String = ""
        val projection = arrayOf(MediaStore.Downloads.DATA)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATA)
                filePath = cursor.getString(columnIndex)
            }
        }
        return filePath
    }

    private suspend fun installByPackageInstaller(apkFile: File): InstallResult = withContext(Dispatchers.IO) {
        val packageInstaller = applicationContext.packageManager.packageInstaller
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)
        return@withContext try {
            apkFile.inputStream().use { inputStream ->
                session.openWrite("base.apk", 0, apkFile.length()).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }
            commitSessionAndAwaitResult(packageInstaller, sessionId, session)
        } finally {
            session.close()
        }
    }

    private suspend fun commitSessionAndAwaitResult(
        packageInstaller: PackageInstaller,
        sessionId: Int,
        session: PackageInstaller.Session
    ): InstallResult = suspendCancellableCoroutine { continuation ->
        val action = "${applicationContext.packageName}.PACKAGE_INSTALL_RESULT.$sessionId.${System.currentTimeMillis()}"
        val intentFilter = IntentFilter(action)
        var isCompleted = false

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    ?: PackageInstaller.STATUS_FAILURE
                val statusMessage = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                Timber.d("[B][PackageInstaller] : status = $status, message = $statusMessage")

                if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    val userActionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent?.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent?.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    if (userActionIntent != null) {
                        userActionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        applicationContext.startActivity(userActionIntent)
                        return
                    }
                }

                if (!isCompleted) {
                    isCompleted = true
                    runCatching { applicationContext.unregisterReceiver(this) }
                    continuation.resume(
                        InstallResult(
                            status = status,
                            statusMessage = statusMessage,
                            isSuccess = status == PackageInstaller.STATUS_SUCCESS
                        )
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            applicationContext.registerReceiver(receiver, intentFilter)
        }

        continuation.invokeOnCancellation {
            if (!isCompleted) {
                isCompleted = true
                runCatching { applicationContext.unregisterReceiver(receiver) }
                runCatching { packageInstaller.abandonSession(sessionId) }
            }
        }

        val callbackIntent = Intent(action).setPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            sessionId,
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        session.commit(pendingIntent.intentSender)
    }

    private fun emitUpdateState(updateState: UpdateState) {
        coroutineScope.launch {
            _updateSharedFlow.emit(updateState)
        }
    }

    private data class InstallResult(
        val status: Int,
        val statusMessage: String,
        val isSuccess: Boolean
    )
}