package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "mec_chamet_jarvis_prefs")

data class JarvisSettings(
  val isAssistantRunning: Boolean = false,
  val isAutoReplyEnabled: Boolean = false,
  val isAlertSoundEnabled: Boolean = true,
  val greeting: String = ReplyFormatter.DEFAULT_GREETING,
  val paymentNumbers: String = ReplyFormatter.DEFAULT_PAYMENT_NUMBERS,
  val closing: String = ReplyFormatter.DEFAULT_CLOSING,
  val targetPackageName: String = ReplyFormatter.DEFAULT_CHAMET_PACKAGE,
  val hasSeededDefaultPrices: Boolean = false,
)

class JarvisPreferencesRepository(private val dataStore: DataStore<Preferences>) {

  constructor(context: Context) : this(context.applicationContext.jarvisDataStore)

  private object Keys {
    val IS_RUNNING = booleanPreferencesKey("is_assistant_running")
    val AUTO_REPLY = booleanPreferencesKey("is_auto_reply_enabled")
    val ALERT_SOUND = booleanPreferencesKey("is_alert_sound_enabled")
    val GREETING = stringPreferencesKey("reply_greeting")
    val PAYMENT_NUMBERS = stringPreferencesKey("reply_payment_numbers")
    val CLOSING = stringPreferencesKey("reply_closing")
    val TARGET_PACKAGE = stringPreferencesKey("target_chamet_package")
    val SEEDED_PRICES = booleanPreferencesKey("has_seeded_default_prices")
  }

  val settingsFlow: Flow<JarvisSettings> =
    dataStore.data.map { prefs ->
      JarvisSettings(
        isAssistantRunning = prefs[Keys.IS_RUNNING] ?: false,
        isAutoReplyEnabled = prefs[Keys.AUTO_REPLY] ?: false,
        isAlertSoundEnabled = prefs[Keys.ALERT_SOUND] ?: true,
        greeting = prefs[Keys.GREETING] ?: ReplyFormatter.DEFAULT_GREETING,
        paymentNumbers = prefs[Keys.PAYMENT_NUMBERS] ?: ReplyFormatter.DEFAULT_PAYMENT_NUMBERS,
        closing = prefs[Keys.CLOSING] ?: ReplyFormatter.DEFAULT_CLOSING,
        targetPackageName = prefs[Keys.TARGET_PACKAGE] ?: ReplyFormatter.DEFAULT_CHAMET_PACKAGE,
        hasSeededDefaultPrices = prefs[Keys.SEEDED_PRICES] ?: false,
      )
    }

  suspend fun getSnapshot(): JarvisSettings = settingsFlow.first()

  suspend fun setAssistantRunning(running: Boolean) {
    dataStore.edit { it[Keys.IS_RUNNING] = running }
  }

  suspend fun setAutoReplyEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.AUTO_REPLY] = enabled }
  }

  suspend fun setAlertSoundEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.ALERT_SOUND] = enabled }
  }

  suspend fun saveReplyTemplate(greeting: String, paymentNumbers: String, closing: String) {
    dataStore.edit { prefs ->
      prefs[Keys.GREETING] = greeting.trim().ifEmpty { ReplyFormatter.DEFAULT_GREETING }
      prefs[Keys.PAYMENT_NUMBERS] =
        paymentNumbers.trim().ifEmpty { ReplyFormatter.DEFAULT_PAYMENT_NUMBERS }
      prefs[Keys.CLOSING] = closing.trim().ifEmpty { ReplyFormatter.DEFAULT_CLOSING }
    }
  }

  suspend fun saveTargetPackageName(packageName: String) {
    val clean = packageName.trim().ifEmpty { ReplyFormatter.DEFAULT_CHAMET_PACKAGE }
    dataStore.edit { it[Keys.TARGET_PACKAGE] = clean }
  }

  suspend fun markPricesSeeded() {
    dataStore.edit { it[Keys.SEEDED_PRICES] = true }
  }
}
