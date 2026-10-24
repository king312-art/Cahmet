package com.example.service

import com.example.data.JarvisSettings

data class IncomingNotificationCandidate(
  val packageName: String,
  val chatKey: String,
  val senderTitle: String,
  val messageText: String,
  val isOngoing: Boolean,
  val isGroupSummary: Boolean,
  val hasInlineReplyAction: Boolean,
  val timestampMs: Long = System.currentTimeMillis(),
)

sealed class NotificationDecision {
  data object IgnoredAssistantOff : NotificationDecision()
  data object IgnoredUnrelatedApp : NotificationDecision()
  data object IgnoredSystemOrEmpty : NotificationDecision()
  data object IgnoredDuplicateEvent : NotificationDecision()
  data object IgnoredChatReplyCooldown : NotificationDecision()

  data class SendDirectAutoReply(
    val eventKey: String,
    val shouldPlayAlert: Boolean,
  ) : NotificationDecision()

  data class TriggerFallbackCopyAlert(
    val eventKey: String,
    val shouldPlayAlert: Boolean,
    val statusBadge: String,
  ) : NotificationDecision()
}

/**
 * Pure, testable rules engine enforcing strict Chamet-only filtering, STOP/OFF silence,
 * duplicate notification suppression, and per-chat auto-reply cooldowns.
 */
class ChametNotificationProcessor(
  private val eventDedupWindowMs: Long = 30_000L,
  private val autoReplyCooldownPerChatMs: Long = 120_000L,
) {
  private val lock = Any()
  private val seenEvents = LinkedHashMap<String, Long>()
  private val recentAutoRepliedChats = LinkedHashMap<String, Long>()

  fun evaluate(
    candidate: IncomingNotificationCandidate,
    settings: JarvisSettings,
    selfPackageName: String,
  ): NotificationDecision {
    // 1. Strict STOP / OFF enforcement: zero background action when Assistant is OFF
    if (!settings.isAssistantRunning) {
      return NotificationDecision.IgnoredAssistantOff
    }

    // 2. Never touch our own notifications or unrelated apps (WhatsApp, SMS, Instagram, etc.)
    val allowedPkg = settings.targetPackageName.trim()
    if (
      allowedPkg.isEmpty() ||
        candidate.packageName.equals(selfPackageName, ignoreCase = true) ||
        !candidate.packageName.equals(allowedPkg, ignoreCase = true)
    ) {
      return NotificationDecision.IgnoredUnrelatedApp
    }

    // 3. Ignore ongoing background service banners, group summaries, or blank notifications
    val cleanTitle = candidate.senderTitle.trim()
    val cleanText = candidate.messageText.trim()
    if (candidate.isOngoing || candidate.isGroupSummary || (cleanTitle.isEmpty() && cleanText.isEmpty())) {
      return NotificationDecision.IgnoredSystemOrEmpty
    }

    val normalizedChatKey = candidate.chatKey.trim().ifEmpty { cleanTitle.ifEmpty { "default_chat" } }
    val eventKey = "${candidate.packageName}|$normalizedChatKey|$cleanTitle|$cleanText"

    synchronized(lock) {
      pruneExpiredLocked(candidate.timestampMs)

      // 4. Suppress duplicate notification updates for the exact same message
      val prevEventTime = seenEvents[eventKey]
      if (prevEventTime != null && candidate.timestampMs - prevEventTime <= eventDedupWindowMs) {
        return NotificationDecision.IgnoredDuplicateEvent
      }
      seenEvents[eventKey] = candidate.timestampMs

      // 5. Decide between Direct Inline Reply vs Safe 1-Tap Fallback
      if (settings.isAutoReplyEnabled && candidate.hasInlineReplyAction) {
        val lastRepliedTime = recentAutoRepliedChats[normalizedChatKey]
        if (
          lastRepliedTime != null &&
            candidate.timestampMs - lastRepliedTime <= autoReplyCooldownPerChatMs
        ) {
          return NotificationDecision.IgnoredChatReplyCooldown
        }
        recentAutoRepliedChats[normalizedChatKey] = candidate.timestampMs
        return NotificationDecision.SendDirectAutoReply(
          eventKey = eventKey,
          shouldPlayAlert = settings.isAlertSoundEnabled,
        )
      }

      val badge =
        if (!settings.isAutoReplyEnabled) {
          "Alert + 1-Tap Copy (Auto Reply OFF)"
        } else {
          "Alert + 1-Tap Copy (No Inline Reply)"
        }

      return NotificationDecision.TriggerFallbackCopyAlert(
        eventKey = eventKey,
        shouldPlayAlert = settings.isAlertSoundEnabled,
        statusBadge = badge,
      )
    }
  }

  fun resetState() {
    synchronized(lock) {
      seenEvents.clear()
      recentAutoRepliedChats.clear()
    }
  }

  private fun pruneExpiredLocked(nowMs: Long) {
    val eventIter = seenEvents.entries.iterator()
    while (eventIter.hasNext()) {
      if (nowMs - eventIter.next().value > eventDedupWindowMs) {
        eventIter.remove()
      }
    }
    val replyIter = recentAutoRepliedChats.entries.iterator()
    while (replyIter.hasNext()) {
      if (nowMs - replyIter.next().value > autoReplyCooldownPerChatMs) {
        replyIter.remove()
      }
    }
  }
}
