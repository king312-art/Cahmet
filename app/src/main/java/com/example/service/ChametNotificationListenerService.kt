package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.JarvisRepository
import com.example.data.JarvisSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChametNotificationListenerService : NotificationListenerService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Volatile private var cachedSettings: JarvisSettings = JarvisSettings(isAssistantRunning = false)

  override fun onCreate() {
    super.onCreate()
    val repo = JarvisRepository.getInstance(applicationContext)
    serviceScope.launch {
      repo.settingsFlow.collectLatest { updated ->
        cachedSettings = updated
        isRunningFastFlag = updated.isAssistantRunning
      }
    }
  }

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    // Immediate zero-overhead exit if the assistant is OFF
    if (!isRunningFastFlag || sbn == null) return

    val currentSettings = cachedSettings
    if (!currentSettings.isAssistantRunning) return

    val pkg = sbn.packageName ?: return
    if (
      pkg.equals(packageName, ignoreCase = true) ||
        !pkg.equals(currentSettings.targetPackageName.trim(), ignoreCase = true)
    ) {
      return
    }

    val notification = sbn.notification ?: return
    val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
    val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    val extras = notification.extras
    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    val text =
      extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

    val inlineReplyPair = findDirectReplyAction(notification)
    val chatKey = sbn.tag ?: sbn.groupKey ?: sbn.key ?: title

    val candidate =
      IncomingNotificationCandidate(
        packageName = pkg,
        chatKey = chatKey,
        senderTitle = title,
        messageText = text,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        hasInlineReplyAction = inlineReplyPair != null,
      )

    val decision = processor.evaluate(candidate, currentSettings, packageName)
    when (decision) {
      is NotificationDecision.SendDirectAutoReply -> {
        serviceScope.launch {
          val repo = JarvisRepository.getInstance(applicationContext)
          val replyMessage = repo.getFormattedReplySnapshot()
          if (decision.shouldPlayAlert) {
            AlertSoundPlayer.playForEvent(decision.eventKey, applicationContext)
          }
          val sentDirect =
            inlineReplyPair?.let { (action, remoteInputs) ->
              sendInlineReply(applicationContext, action, remoteInputs, replyMessage)
            } ?: false

          if (sentDirect) {
            repo.appendActivityLog(
              senderTitle = title.ifBlank { "Chamet Chat" },
              messageSnippet = text.ifBlank { "New Chamet notification" },
              statusBadge = "Auto-Replied Directly",
            )
          } else {
            // Honest fallback if PendingIntent failed
            HelperNotificationManager.showQuickCopyNotification(
              context = applicationContext,
              senderTitle = title,
              customerSnippet = text,
              formattedReply = replyMessage,
              targetChametPackage = currentSettings.targetPackageName,
            )
            repo.appendActivityLog(
              senderTitle = title.ifBlank { "Chamet Chat" },
              messageSnippet = text.ifBlank { "New Chamet notification" },
              statusBadge = "Fallback 1-Tap Copy Ready",
            )
          }
        }
      }

      is NotificationDecision.TriggerFallbackCopyAlert -> {
        serviceScope.launch {
          val repo = JarvisRepository.getInstance(applicationContext)
          val replyMessage = repo.getFormattedReplySnapshot()
          if (decision.shouldPlayAlert) {
            AlertSoundPlayer.playForEvent(decision.eventKey, applicationContext)
          }
          HelperNotificationManager.showQuickCopyNotification(
            context = applicationContext,
            senderTitle = title,
            customerSnippet = text,
            formattedReply = replyMessage,
            targetChametPackage = currentSettings.targetPackageName,
          )
          repo.appendActivityLog(
            senderTitle = title.ifBlank { "Chamet Chat" },
            messageSnippet = text.ifBlank { "New Chamet notification" },
            statusBadge = decision.statusBadge,
          )
        }
      }

      NotificationDecision.IgnoredAssistantOff,
      NotificationDecision.IgnoredUnrelatedApp,
      NotificationDecision.IgnoredSystemOrEmpty,
      NotificationDecision.IgnoredDuplicateEvent,
      NotificationDecision.IgnoredChatReplyCooldown -> Unit
    }
  }

  private fun findDirectReplyAction(
    notification: Notification
  ): Pair<Notification.Action, Array<RemoteInput>>? {
    val actions = notification.actions ?: return null
    for (action in actions) {
      val remoteInputs = action.remoteInputs
      if (!remoteInputs.isNullOrEmpty() && action.actionIntent != null) {
        return action to remoteInputs
      }
    }
    return null
  }

  private fun sendInlineReply(
    context: Context,
    action: Notification.Action,
    remoteInputs: Array<RemoteInput>,
    replyText: String,
  ): Boolean {
    return try {
      val fillInIntent = Intent()
      val resultsBundle = Bundle()
      for (input in remoteInputs) {
        resultsBundle.putCharSequence(input.resultKey, replyText)
      }
      RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, resultsBundle)
      action.actionIntent.send(context, 0, fillInIntent)
      true
    } catch (_: PendingIntent.CanceledException) {
      false
    } catch (_: Exception) {
      false
    }
  }

  companion object {
    @Volatile var isRunningFastFlag: Boolean = false

    val processor = ChametNotificationProcessor()

    fun stopAllBackgroundWorkImmediately(context: Context) {
      isRunningFastFlag = false
      processor.resetState()
      AlertSoundPlayer.stopImmediately(context)
      HelperNotificationManager.cancelHelperNotifications(context)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
      val expectedComponent =
        ComponentName(context, ChametNotificationListenerService::class.java).flattenToString()
      val enabledListeners =
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
          .orEmpty()
      return enabledListeners.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }
  }
}
