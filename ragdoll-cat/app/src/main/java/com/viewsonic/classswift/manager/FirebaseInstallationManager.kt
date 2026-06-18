package com.viewsonic.classswift.manager

import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class FirebaseInstallationManager{
    private var installationId: String = ""

    fun getInstallationId() = installationId

    suspend fun fetchInstallationId(): String {
        if (installationId.isEmpty()) {
            initInstallationId()
        }
        return installationId
    }

    private suspend fun initInstallationId(): Boolean = suspendCancellableCoroutine { continuation ->
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task: Task<String?> ->
            if (task.isSuccessful) {
                task.result?.let { firebaseInstallationId ->
                    installationId = firebaseInstallationId
                    Timber.d("[B][initInstallationId] : installationId = $installationId")
                    continuation.resume(true)
                } ?: continuation.resume(false)
            }
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }
}