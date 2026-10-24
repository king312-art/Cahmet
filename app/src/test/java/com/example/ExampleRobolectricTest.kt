package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.JarvisSettings
import com.example.service.AlertSoundPlayer
import com.example.ui.DashboardScreen
import com.example.ui.JarvisUiState
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun appName_matchesMecChametJarvis() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MEC Chamet Jarvis", appName)
  }

  @Test
  fun dashboardControls_startStopAlertAndCopyWorkCorrectly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var isRunning by mutableStateOf(false)
    var autoReply by mutableStateOf(false)
    var copyTriggered = false

    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(
          uiState =
            JarvisUiState(
              settings =
                JarvisSettings(
                  isAssistantRunning = isRunning,
                  isAutoReplyEnabled = autoReply,
                )
            ),
          onStartClick = { isRunning = true },
          onStopClick = {
            isRunning = false
            AlertSoundPlayer.stopImmediately(context)
          },
          onAutoReplyToggle = { autoReply = it },
          onAlertSoundToggle = {},
          onTestAlertClick = { AlertSoundPlayer.playTestAlert(context) },
          onPreviewReplyClick = {},
          onCopyReplyClick = {
            copyTriggered = true
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(
              ClipData.newPlainText(
                "Reply",
                "Assalam-o-Alaikum 😊 PKR 750 — 15,000 diamonds",
              )
            )
          },
          onDismissPreviewSheet = {},
          onNavigateToReplyEditor = {},
          onNavigateToSetup = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("MEC Chamet Jarvis").assertIsDisplayed()
    composeTestRule.onNodeWithText("ASSISTANT OFF").assertIsDisplayed()

    // 1. Test START button turns status indicator to ASSISTANT ON
    composeTestRule
      .onNodeWithTag("dashboard_list")
      .performScrollToNode(hasTestTag("start_assistant_button"))
    composeTestRule.onNodeWithTag("start_assistant_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(isRunning)
    composeTestRule
      .onNodeWithTag("dashboard_list")
      .performScrollToNode(hasTestTag("assistant_status_card"))
    composeTestRule.onNodeWithText("ASSISTANT ON").assertIsDisplayed()

    // 2. Test Alert Sound & immediate STOP halting
    composeTestRule
      .onNodeWithTag("dashboard_list")
      .performScrollToNode(hasTestTag("test_alert_button"))
    composeTestRule.onNodeWithTag("test_alert_button").performClick()

    composeTestRule
      .onNodeWithTag("dashboard_list")
      .performScrollToNode(hasTestTag("stop_assistant_button"))
    composeTestRule.onNodeWithTag("stop_assistant_button").performClick()
    composeTestRule.waitForIdle()
    assertFalse(isRunning)
    assertFalse(AlertSoundPlayer.isPlaying.value)

    // 3. Test Copy Reply button
    composeTestRule
      .onNodeWithTag("dashboard_list")
      .performScrollToNode(hasTestTag("copy_reply_button"))
    composeTestRule.onNodeWithTag("copy_reply_button").performClick()
    assertTrue(copyTriggered)
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copied = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
    assertTrue(copied.contains("PKR 750 — 15,000 diamonds"))
  }
}
