package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ActivityLogEntity
import com.example.data.JarvisRepository
import com.example.data.JarvisSettings
import com.example.data.PricePackageEntity
import com.example.data.ReplyFormatter
import com.example.service.AlertSoundPlayer
import com.example.service.ChametNotificationListenerService
import com.example.service.HelperNotificationManager
import com.example.service.IncomingNotificationCandidate
import com.example.service.NotificationDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class JarvisTab {
  DASHBOARD,
  REPLY_PRICES,
  CHAMET_SETUP,
}

data class PermissionStatus(
  val isNotificationListenerEnabled: Boolean = false,
  val canPostHelperNotifications: Boolean = false,
  val isChametAppInstalled: Boolean = false,
)

data class JarvisUiState(
  val selectedTab: JarvisTab = JarvisTab.DASHBOARD,
  val settings: JarvisSettings = JarvisSettings(),
  val pricePackages: List<PricePackageEntity> = PricePackageEntity.defaultPackages(),
  val formattedReply: String =
    ReplyFormatter.buildCustomerReply(
      greeting = ReplyFormatter.DEFAULT_GREETING,
      packages = PricePackageEntity.defaultPackages(),
      paymentNumbers = ReplyFormatter.DEFAULT_PAYMENT_NUMBERS,
      closing = ReplyFormatter.DEFAULT_CLOSING,
    ),
  val recentLogs: List<ActivityLogEntity> = emptyList(),
  val isAlertSoundPlaying: Boolean = false,
  val showPreviewSheet: Boolean = false,
  val permissions: PermissionStatus = PermissionStatus(),
  val snackbarMessage: String? = null,
)

private data class LocalTransientState(
  val selectedTab: JarvisTab = JarvisTab.DASHBOARD,
  val showPreviewSheet: Boolean = false,
  val permissions: PermissionStatus = PermissionStatus(),
  val snackbarMessage: String? = null,
)

class JarvisViewModel(private val repository: JarvisRepository) : ViewModel() {

  private val transientState = MutableStateFlow(LocalTransientState())

  init {
    viewModelScope.launch {
      repository.ensureDefaultPricesSeeded()
      val current = repository.getCurrentSettings()
      ChametNotificationListenerService.isRunningFastFlag = current.isAssistantRunning
    }
  }

  val uiState: StateFlow<JarvisUiState> =
    combine(
        repository.settingsFlow,
        repository.pricePackagesFlow,
        repository.formattedReplyFlow,
        repository.recentLogsFlow,
        combine(AlertSoundPlayer.isPlaying, transientState) { playing, local -> playing to local },
      ) { settings, packages, reply, logs, (isPlaying, local) ->
        JarvisUiState(
          selectedTab = local.selectedTab,
          settings = settings,
          pricePackages = packages,
          formattedReply = reply,
          recentLogs = logs,
          isAlertSoundPlaying = isPlaying,
          showPreviewSheet = local.showPreviewSheet,
          permissions = local.permissions,
          snackbarMessage = local.snackbarMessage,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JarvisUiState(),
      )

  fun selectTab(tab: JarvisTab) {
    transientState.update { it.copy(selectedTab = tab) }
  }

  fun refreshSystemStatus(context: Context) {
    val targetPkg = uiState.value.settings.targetPackageName
    val listenerEnabled = ChametNotificationListenerService.isNotificationListenerEnabled(context)
    val postEnabled = HelperNotificationManager.canPostNotifications(context)
    val chametInstalled = isPackageInstalled(context, targetPkg)
    transientState.update {
      it.copy(
        permissions =
          PermissionStatus(
            isNotificationListenerEnabled = listenerEnabled,
            canPostHelperNotifications = postEnabled,
            isChametAppInstalled = chametInstalled,
          )
      )
    }
  }

  fun startAssistant(context: Context) {
    ChametNotificationListenerService.isRunningFastFlag = true
    viewModelScope.launch {
      repository.setAssistantRunning(true)
      postSnackbar(context.getString(R.string.msg_assistant_started))
    }
  }

  fun stopAssistant(context: Context) {
    // Immediately stop all sound, monitoring, and helper notifications synchronously
    ChametNotificationListenerService.stopAllBackgroundWorkImmediately(context)
    viewModelScope.launch {
      repository.setAssistantRunning(false)
      postSnackbar(context.getString(R.string.msg_assistant_stopped))
    }
  }

  fun setAutoReplyEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setAutoReplyEnabled(enabled)
    }
  }

  fun setAlertSoundEnabled(context: Context, enabled: Boolean) {
    if (!enabled) {
      AlertSoundPlayer.stopImmediately(context)
    }
    viewModelScope.launch {
      repository.setAlertSoundEnabled(enabled)
    }
  }

  fun triggerTestAlertSound(context: Context) {
    AlertSoundPlayer.playTestAlert(context)
    if (!uiState.value.settings.isAlertSoundEnabled) {
      postSnackbar("Playing test chime (Note: Alert Sound switch is currently OFF for incoming chats).")
    }
  }

  fun setPreviewSheetVisible(visible: Boolean) {
    transientState.update { it.copy(showPreviewSheet = visible) }
  }

  fun copyFormattedReply(context: Context) {
    val text = uiState.value.formattedReply
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("MEC Chamet Reply", text))
    postSnackbar(context.getString(R.string.msg_reply_copied))
  }

  fun saveReplyTemplate(
    context: Context,
    greeting: String,
    paymentNumbers: String,
    closing: String,
  ) {
    viewModelScope.launch {
      repository.saveReplyTemplate(greeting, paymentNumbers, closing)
      postSnackbar(context.getString(R.string.msg_template_saved))
    }
  }

  fun savePricePackage(
    context: Context,
    id: Int,
    priceText: String,
    diamondsText: String,
    sortOrder: Int,
  ) {
    viewModelScope.launch {
      repository.addOrUpdatePackage(
        id = id,
        priceText = priceText,
        diamondsText = diamondsText,
        sortOrder = sortOrder,
      )
      postSnackbar(context.getString(R.string.msg_price_saved))
    }
  }

  fun deletePricePackage(id: Int) {
    viewModelScope.launch {
      repository.deletePackage(id)
    }
  }

  fun resetDefaultPrices(context: Context) {
    viewModelScope.launch {
      repository.resetDefaultPrices()
      postSnackbar(context.getString(R.string.msg_prices_reset))
    }
  }

  fun saveTargetPackage(context: Context, packageName: String) {
    viewModelScope.launch {
      repository.saveTargetPackageName(packageName)
      refreshSystemStatus(context)
      postSnackbar("Chamet package filter updated.")
    }
  }

  fun simulateChametIncomingMessage(context: Context) {
    viewModelScope.launch {
      val settings = repository.getCurrentSettings()
      val candidate =
        IncomingNotificationCandidate(
          packageName = settings.targetPackageName,
          chatKey = "sim_customer_${System.currentTimeMillis() / 15_000L}",
          senderTitle = "Customer Inquiry (Test)",
          messageText = "Salam! Need 30,000 Chamet diamonds, price list send karein.",
          isOngoing = false,
          isGroupSummary = false,
          hasInlineReplyAction = false,
        )

      val decision =
        ChametNotificationListenerService.processor.evaluate(
          candidate = candidate,
          settings = settings,
          selfPackageName = context.packageName,
        )

      when (decision) {
        NotificationDecision.IgnoredAssistantOff -> {
          postSnackbar("Assistant is OFF — Ignored! Press START on Dashboard to activate monitoring.")
        }

        NotificationDecision.IgnoredDuplicateEvent -> {
          postSnackbar("Duplicate message suppressed (anti-spam protection working!).")
        }

        is NotificationDecision.TriggerFallbackCopyAlert,
        is NotificationDecision.SendDirectAutoReply -> {
          val reply = repository.getFormattedReplySnapshot()
          if (settings.isAlertSoundEnabled) {
            AlertSoundPlayer.playForEvent("sim_${System.currentTimeMillis()}", context)
          }
          HelperNotificationManager.showQuickCopyNotification(
            context = context,
            senderTitle = candidate.senderTitle,
            customerSnippet = candidate.messageText,
            formattedReply = reply,
            targetChametPackage = settings.targetPackageName,
          )
          repository.appendActivityLog(
            senderTitle = candidate.senderTitle,
            messageSnippet = candidate.messageText,
            statusBadge = "Simulated Test — Alert & 1-Tap Copy Ready",
          )
          postSnackbar("Chamet test detected! Alert triggered & reply ready.")
        }

        else -> {
          postSnackbar("Filtered out by safety rules.")
        }
      }
    }
  }

  fun clearActivityLogs() {
    viewModelScope.launch {
      repository.clearActivityLogs()
    }
  }

  fun consumeSnackbar() {
    transientState.update { it.copy(snackbarMessage = null) }
  }

  private fun postSnackbar(message: String) {
    transientState.update { it.copy(snackbarMessage = message) }
  }

  private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    if (packageName.isBlank()) return false
    return try {
      context.packageManager.getPackageInfo(packageName.trim(), 0)
      true
    } catch (_: PackageManager.NameNotFoundException) {
      false
    } catch (_: Exception) {
      false
    }
  }

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return JarvisViewModel(JarvisRepository.getInstance(context.applicationContext)) as T
    }
  }
}
