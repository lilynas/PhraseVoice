package com.phrasevoice.system

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.phrasevoice.MainActivity
import com.phrasevoice.R

object QuickReturnNotifier {
    const val ACTION_OPEN_COMMUNICATION = "com.phrasevoice.action.OPEN_COMMUNICATION"

    private const val CHANNEL_ID = "quick_return"
    private const val NOTIFICATION_ID = 4001
    private const val ANDROID_16_API = 36
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.quick_return_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.quick_return_notification_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(context: Context) {
        ensureChannel(context)
        if (!canPostNotifications(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_voice)
            .setContentTitle(context.getString(R.string.quick_return_notification_title))
            .setContentText(context.getString(R.string.quick_return_notification_text))
            .setContentIntent(openCommunicationPendingIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showSpeaking(context: Context, text: String) {
        ensureChannel(context)
        if (!canPostNotifications(context)) return

        val contentText = text
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { if (it.length > 96) it.take(96) + "..." else it }
            ?: context.getString(R.string.active_speech_notification_text)
        val notification = if (Build.VERSION.SDK_INT >= ANDROID_16_API) {
            buildAndroid16LiveUpdate(context, contentText)
        } else {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_voice)
                .setContentTitle(context.getString(R.string.active_speech_notification_title))
                .setContentText(contentText)
                .setContentIntent(openCommunicationPendingIntent(context))
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun isOpenCommunicationIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_OPEN_COMMUNICATION

    private fun openCommunicationPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_COMMUNICATION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildAndroid16LiveUpdate(
        context: Context,
        contentText: String,
    ): Notification =
        Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_voice)
            .setContentTitle(context.getString(R.string.active_speech_notification_title))
            .setContentText(contentText)
            .setStyle(Notification.BigTextStyle().bigText(contentText))
            .setShortCriticalText(context.getString(R.string.active_speech_notification_chip))
            .setContentIntent(openCommunicationPendingIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addExtras(
                Bundle().apply {
                    putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
                },
            )
            .build()
}
