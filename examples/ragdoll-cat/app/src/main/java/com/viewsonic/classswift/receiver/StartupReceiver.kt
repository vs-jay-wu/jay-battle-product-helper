package com.viewsonic.classswift.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.ui.activity.LoginActivity
import timber.log.Timber

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("[onReceive] : intent action = ${intent.action}")
        Timber.d("[onReceive] : intent package = ${intent.data?.schemeSpecificPart}")

        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        // Launch LoginActivity directly via class reference. We deliberately do
        // NOT use getLaunchIntentForPackage() — Phase 7 strips the LAUNCHER
        // intent-filter, so that helper would return null after that point.
        val launchIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        runCatching {
            context.startActivity(launchIntent)
            Timber.d("[onReceive] : startActivity success")
        }.onFailure {
            Timber.w(it, "[onReceive] : startActivity failed, fallback to notification")
            showOpenAppNotification(context, launchIntent)
        }
    }

    private fun showOpenAppNotification(context: Context, launchIntent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isNotificationPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isNotificationPermissionGranted) {
                Timber.w("[showOpenAppNotification] : POST_NOTIFICATIONS not granted, skip notification")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Update",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.common_app_name))
            .setContentText("Update installed. Tap to open app.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }
}
