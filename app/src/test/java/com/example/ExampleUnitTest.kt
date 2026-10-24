package com.example

import com.example.data.JarvisSettings
import com.example.data.PricePackageEntity
import com.example.data.ReplyFormatter
import com.example.service.AlertSoundPlayer
import com.example.service.ChametNotificationProcessor
import com.example.service.IncomingNotificationCandidate
import com.example.service.NotificationDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

  @Before
  fun setUp() {
    AlertSoundPlayer.stopImmediately()
    AlertSoundPlayer.clearDeduplicationCacheForTesting()
  }

  @Test
  fun replyFormatter_buildsExactDefaultTemplateAndEditablePrices() {
    val defaultReply =
      ReplyFormatter.buildCustomerReply(
        greeting = ReplyFormatter.DEFAULT_GREETING,
        packages = PricePackageEntity.defaultPackages(),
        paymentNumbers = "JazzCash: 0301-1234567",
        closing = ReplyFormatter.DEFAULT_CLOSING,
      )

    assertTrue(defaultReply.contains("Assalam-o-Alaikum 😊"))
    assertTrue(defaultReply.contains("Welcome! Chamet Diamonds available hain 💎"))
    assertTrue(defaultReply.contains("- PKR 750 — 15,000 diamonds"))
    assertTrue(defaultReply.contains("- PKR 990 — 20,000 diamonds"))
    assertTrue(defaultReply.contains("- PKR 1,480 — 30,000 diamonds"))
    assertTrue(defaultReply.contains("- PKR 2,480 — 50,000 diamonds"))
    assertTrue(defaultReply.contains("- PKR 4,850 — 100,000 diamonds"))
    assertTrue(defaultReply.contains("JazzCash: 0301-1234567"))
    assertTrue(defaultReply.contains("Shukriya ❤️"))
  }

  @Test
  fun notificationProcessor_ignoresAllEventsWhenAssistantIsOff() {
    val processor = ChametNotificationProcessor()
    val candidate =
      IncomingNotificationCandidate(
        packageName = "com.hkfuliao.chamet",
        chatKey = "chat_101",
        senderTitle = "Ali",
        messageText = "Diamonds rate?",
        isOngoing = false,
        isGroupSummary = false,
        hasInlineReplyAction = true,
        timestampMs = 1_000L,
      )

    val decision =
      processor.evaluate(
        candidate = candidate,
        settings = JarvisSettings(isAssistantRunning = false, isAutoReplyEnabled = true),
        selfPackageName = "com.aistudio.chametjarvis.vxpqkz",
      )

    assertEquals(NotificationDecision.IgnoredAssistantOff, decision)
  }

  @Test
  fun notificationProcessor_neverRepliesToUnrelatedApps() {
    val processor = ChametNotificationProcessor()
    val whatsappCandidate =
      IncomingNotificationCandidate(
        packageName = "com.whatsapp",
        chatKey = "chat_wa",
        senderTitle = "Family Group",
        messageText = "Hello",
        isOngoing = false,
        isGroupSummary = false,
        hasInlineReplyAction = true,
        timestampMs = 2_000L,
      )

    val decision =
      processor.evaluate(
        candidate = whatsappCandidate,
        settings = JarvisSettings(isAssistantRunning = true, isAutoReplyEnabled = true),
        selfPackageName = "com.aistudio.chametjarvis.vxpqkz",
      )

    assertEquals(NotificationDecision.IgnoredUnrelatedApp, decision)
  }

  @Test
  fun notificationProcessor_preventsDuplicateRepliesAndSupportsFallback() {
    val processor = ChametNotificationProcessor()
    val activeSettings =
      JarvisSettings(
        isAssistantRunning = true,
        isAutoReplyEnabled = true,
        isAlertSoundEnabled = true,
      )

    // 1. Chamet notification WITH inline reply -> Direct Auto Reply
    val directCandidate =
      IncomingNotificationCandidate(
        packageName = "com.hkfuliao.chamet",
        chatKey = "customer_42",
        senderTitle = "Zain",
        messageText = "Need 50k diamonds",
        isOngoing = false,
        isGroupSummary = false,
        hasInlineReplyAction = true,
        timestampMs = 10_000L,
      )
    val firstDecision =
      processor.evaluate(directCandidate, activeSettings, "com.aistudio.chametjarvis.vxpqkz")
    assertTrue(firstDecision is NotificationDecision.SendDirectAutoReply)

    // 2. Exact same notification reposted 2 seconds later -> Suppressed as Duplicate
    val dupDecision =
      processor.evaluate(
        directCandidate.copy(timestampMs = 12_000L),
        activeSettings,
        "com.aistudio.chametjarvis.vxpqkz",
      )
    assertEquals(NotificationDecision.IgnoredDuplicateEvent, dupDecision)

    // 3. Same customer sends a follow-up within 2-minute cooldown -> Cooldown prevents double spam
    val followUpDecision =
      processor.evaluate(
        directCandidate.copy(messageText = "Tell me fast", timestampMs = 25_000L),
        activeSettings,
        "com.aistudio.chametjarvis.vxpqkz",
      )
    assertEquals(NotificationDecision.IgnoredChatReplyCooldown, followUpDecision)

    // 4. Chamet notification WITHOUT inline reply -> Honest 1-Tap Copy Fallback
    val tapOnlyCandidate =
      IncomingNotificationCandidate(
        packageName = "com.hkfuliao.chamet",
        chatKey = "customer_99",
        senderTitle = "Bilal",
        messageText = "Assalam o alaikum",
        isOngoing = false,
        isGroupSummary = false,
        hasInlineReplyAction = false,
        timestampMs = 30_000L,
      )
    val fallbackDecision =
      processor.evaluate(tapOnlyCandidate, activeSettings, "com.aistudio.chametjarvis.vxpqkz")
    assertTrue(fallbackDecision is NotificationDecision.TriggerFallbackCopyAlert)
  }

  @Test
  fun alertSoundPlayer_synthesizesNonSilentPcmAndDeduplicatesEvents() {
    val pcm = AlertSoundPlayer.synthesizeDiamondChimePcm(24_000)
    assertTrue(pcm.size > 24_000)
    assertTrue(pcm.any { it.toInt() != 0 })

    val firstPlayed = AlertSoundPlayer.playForEvent("event_abc", context = null, nowMs = 5_000L)
    val duplicatePlayed = AlertSoundPlayer.playForEvent("event_abc", context = null, nowMs = 8_000L)
    AlertSoundPlayer.stopImmediately()

    assertTrue(firstPlayed)
    assertFalse(duplicatePlayed)
    assertFalse(AlertSoundPlayer.isPlaying.value)
  }
}
