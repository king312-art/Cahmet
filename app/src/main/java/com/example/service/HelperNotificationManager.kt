package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object HelperNotificationManager {
  private const val CHANNEL_ID = "chamet_customer_alerts_channel"
  private const val HELPER_NOTIFICATION_ID = 4401

  fun canPostNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          )
          .apply {
            description = context.getString(R.string.notif_channel_desc)
            enableVibration(false) // AlertSoundPlayer manages custom chime & pulse
            setSound(null, null)
          }
      manager?.createNotificationChannel(channel)
    }
  }

  fun showQuickCopyNotification(
    context: Context,
    senderTitle: String,
    customerSnippet: String,
    formattedReply: String,
    targetChametPackage: String,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannel(context)

    val copyIntent =
      Intent(context, CopyReplyBroadcastReceiver::class.java).apply {
        action = CopyReplyBroadcastReceiver.ACTION_COPY_SAVED_REPLY
        putExtra(CopyReplyBroadcastReceiver.EXTRA_REPLY_TEXT, formattedReply)
      }
    val copyPendingIntent =
      PendingIntent.getBroadcast(
        context,
        101,
        copyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    // Tap opens Chamet if installed, otherwise opens MEC Chamet Jarvis dashboard
    val launchChametIntent =
      context.packageManager.getLaunchIntentForPackage(targetChametPackage)
        ?: Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    val openPendingIntent =
      PendingIntent.getActivity(
        context,
        102,
        launchChametIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val safeSender = senderTitle.ifBlank { "Customer" }
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_diamond_monochrome)
        .setContentTitle(context.getString(R.string.notif_helper_title, safeSender))
        .setContentText(
          customerSnippet.ifBlank { "Tap 'Copy Saved Reply' to paste your diamond price list." }
        )
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(
              "${customerSnippet.ifBlank { "New customer inquiry detected." }}\n\nTap below to copy your full PKR price list & payment details."
            )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(openPendingIntent)
        .addAction(
          R.drawable.ic_diamond_monochrome,
          context.getString(R.string.notif_action_copy),
          copyPendingIntent,
        )
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(HELPER_NOTIFICATION_ID, notification)
    }
  }

  fun cancelHelperNotifications(context: Context) {
    runCatching {
      NotificationManagerCompat.from(context).cancel(HELPER_NOTIFICATION_ID)
    }
  }
}
