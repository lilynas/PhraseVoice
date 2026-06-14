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
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.phrasevoice.MainActivity
import com.phrasevoice.R

object QuickReturnNotifier {
    const val ACTION_OPEN_COMMUNICATION = "com.phrasevoice.action.OPEN_COMMUNICATION"
    const val ACTION_REPLAY_LAST = "com.phrasevoice.action.REPLAY_LAST"
    const val ACTION_STOP_SPEECH = "com.phrasevoice.action.STOP_SPEECH"
    const val EXTRA_SPEECH_TEXT = "com.phrasevoice.extra.SPEECH_TEXT"

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
    fun show(context: Context, lastText: String? = null) {
        ensureChannel(context)
        if (!canPostNotifications(context)) return

        val speechText = lastText?.trim().orEmpty()
        val contentText = speechText
            .takeIf { it.isNotEmpty() }
            ?.let { if (it.length > 96) it.take(96) + "..." else it }
            ?: context.getString(R.string.quick_return_notification_text)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_voice)
            .setContentTitle(context.getString(R.string.quick_return_notification_title))
            .setContentText(contentText)
            .setContentIntent(openCommunicationPendingIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (speechText.isNotEmpty()) {
            builder
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .addAction(
                    R.drawable.ic_notification_voice,
                    context.getString(R.string.quick_return_notification_action_replay),
                    commandPendingIntent(
                        context = context,
                        action = ACTION_REPLAY_LAST,
                        requestCode = 1,
                        text = speechText,
                    ),
                )
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showSpeaking(context: Context, text: String) {
        ensureChannel(context)
        if (!canPostNotifications(context)) return

        val speechText = text.trim()
        val contentText = speechText
            .takeIf { it.isNotEmpty() }
            ?.let { if (it.length > 96) it.take(96) + "..." else it }
            ?: context.getString(R.string.active_speech_notification_text)
        val notification = if (Build.VERSION.SDK_INT >= ANDROID_16_API) {
            buildAndroid16LiveUpdate(context, contentText, speechText)
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
                .addAction(
                    R.drawable.ic_notification_voice,
                    context.getString(R.string.quick_return_notification_action_replay),
                    commandPendingIntent(
                        context = context,
                        action = ACTION_REPLAY_LAST,
                        requestCode = 1,
                        text = speechText,
                    ),
                )
                .addAction(
                    R.drawable.ic_notification_voice,
                    context.getString(R.string.quick_return_notification_action_stop),
                    commandPendingIntent(
                        context = context,
                        action = ACTION_STOP_SPEECH,
                        requestCode = 2,
                    ),
                )
                .build()
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun isOpenCommunicationIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_OPEN_COMMUNICATION

    fun isReplayIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_REPLAY_LAST

    fun isStopIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_STOP_SPEECH

    fun replayText(intent: Intent?): String =
        intent?.getStringExtra(EXTRA_SPEECH_TEXT).orEmpty()

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

    private fun commandPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        text: String? = null,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            if (!text.isNullOrBlank()) {
                putExtra(EXTRA_SPEECH_TEXT, text)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildAndroid16LiveUpdate(
        context: Context,
        contentText: String,
        speechText: String,
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
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification_voice),
                    context.getString(R.string.quick_return_notification_action_replay),
                    commandPendingIntent(
                        context = context,
                        action = ACTION_REPLAY_LAST,
                        requestCode = 1,
                        text = speechText,
                    ),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification_voice),
                    context.getString(R.string.quick_return_notification_action_stop),
                    commandPendingIntent(
                        context = context,
                        action = ACTION_STOP_SPEECH,
                        requestCode = 2,
                    ),
                ).build(),
            )
            .addExtras(
                Bundle().apply {
                    putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
                },
            )
            .build()
}
