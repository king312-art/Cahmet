package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class JarvisRepository(
  private val dao: JarvisDao,
  private val prefs: JarvisPreferencesRepository,
) {
  val settingsFlow: Flow<JarvisSettings> = prefs.settingsFlow
  val pricePackagesFlow: Flow<List<PricePackageEntity>> = dao.observePricePackages()
  val recentLogsFlow: Flow<List<ActivityLogEntity>> = dao.observeRecentLogs()

  val formattedReplyFlow: Flow<String> =
    combine(settingsFlow, pricePackagesFlow) { settings, packages ->
      ReplyFormatter.buildCustomerReply(
        greeting = settings.greeting,
        packages = packages,
        paymentNumbers = settings.paymentNumbers,
        closing = settings.closing,
      )
    }

  suspend fun ensureDefaultPricesSeeded() {
    val settings = prefs.getSnapshot()
    if (!settings.hasSeededDefaultPrices && dao.countPricePackages() == 0) {
      dao.insertAllPricePackages(PricePackageEntity.defaultPackages())
      prefs.markPricesSeeded()
    }
  }

  suspend fun getCurrentSettings(): JarvisSettings = prefs.getSnapshot()

  suspend fun getFormattedReplySnapshot(): String {
    ensureDefaultPricesSeeded()
    val settings = prefs.getSnapshot()
    val packages = dao.getPricePackagesSnapshot()
    return ReplyFormatter.buildCustomerReply(
      greeting = settings.greeting,
      packages = packages,
      paymentNumbers = settings.paymentNumbers,
      closing = settings.closing,
    )
  }

  suspend fun setAssistantRunning(running: Boolean) = prefs.setAssistantRunning(running)

  suspend fun setAutoReplyEnabled(enabled: Boolean) = prefs.setAutoReplyEnabled(enabled)

  suspend fun setAlertSoundEnabled(enabled: Boolean) = prefs.setAlertSoundEnabled(enabled)

  suspend fun saveReplyTemplate(greeting: String, paymentNumbers: String, closing: String) {
    prefs.saveReplyTemplate(greeting, paymentNumbers, closing)
  }

  suspend fun saveTargetPackageName(packageName: String) {
    prefs.saveTargetPackageName(packageName)
  }

  suspend fun addOrUpdatePackage(id: Int = 0, priceText: String, diamondsText: String, sortOrder: Int) {
    val cleanPrice = priceText.trim()
    val cleanDiamonds = diamondsText.trim()
    if (cleanPrice.isEmpty() || cleanDiamonds.isEmpty()) return
    val entity = PricePackageEntity(
      id = id,
      priceText = cleanPrice,
      diamondsText = cleanDiamonds,
      sortOrder = sortOrder,
    )
    if (id == 0) {
      dao.insertPricePackage(entity)
    } else {
      dao.updatePricePackage(entity)
    }
  }

  suspend fun deletePackage(id: Int) {
    dao.deletePricePackageById(id)
  }

  suspend fun resetDefaultPrices() {
    dao.deleteAllPricePackages()
    dao.insertAllPricePackages(PricePackageEntity.defaultPackages())
    prefs.markPricesSeeded()
  }

  suspend fun appendActivityLog(senderTitle: String, messageSnippet: String, statusBadge: String) {
    dao.insertLog(
      ActivityLogEntity(
        senderTitle = senderTitle,
        messageSnippet = messageSnippet,
        statusBadge = statusBadge,
      )
    )
  }

  suspend fun clearActivityLogs() {
    dao.clearAllLogs()
  }

  companion object {
    @Volatile private var INSTANCE: JarvisRepository? = null

    fun getInstance(context: Context): JarvisRepository {
      return INSTANCE
        ?: synchronized(this) {
          val appCtx = context.applicationContext
          INSTANCE
            ?: JarvisRepository(
                dao = JarvisDatabase.getInstance(appCtx).jarvisDao(),
                prefs = JarvisPreferencesRepository(appCtx),
              )
              .also { INSTANCE = it }
        }
    }
  }
}
