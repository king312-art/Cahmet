package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AlertSoundPlayer {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val lock = Any()

  private var activeTrack: AudioTrack? = null
  private var playbackJob: Job? = null

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private const val DEDUP_WINDOW_MS = 20_000L
  private val recentEventTimestamps = LinkedHashMap<String, Long>()

  /**
   * Plays the distinctive Diamond Chime immediately for the Dashboard "Test Alert Sound" button.
   */
  fun playTestAlert(context: Context? = null) {
    startChimeInternal(context)
  }

  /**
   * Plays the alert sound for an incoming Chamet event, avoiding duplicate alerts for the same event.
   * Returns true if the alert played, or false if suppressed as a duplicate.
   */
  fun playForEvent(
    eventKey: String,
    context: Context? = null,
    nowMs: Long = System.currentTimeMillis(),
  ): Boolean {
    synchronized(lock) {
      val iterator = recentEventTimestamps.entries.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        if (nowMs - entry.value > DEDUP_WINDOW_MS) {
          iterator.remove()
        }
      }
      val previous = recentEventTimestamps[eventKey]
      if (previous != null && nowMs - previous <= DEDUP_WINDOW_MS) {
        return false
      }
      recentEventTimestamps[eventKey] = nowMs
    }
    startChimeInternal(context)
    return true
  }

  /**
   * Immediately stops any playing alert sound and vibration. Called when STOP is pressed.
   */
  fun stopImmediately(context: Context? = null) {
    synchronized(lock) {
      playbackJob?.cancel()
      playbackJob = null
      releaseActiveTrackLocked()
      _isPlaying.value = false
    }
    if (context != null) {
      runCatching { getVibrator(context)?.cancel() }
    }
  }

  fun clearDeduplicationCacheForTesting() {
    synchronized(lock) {
      recentEventTimestamps.clear()
    }
  }

  private fun startChimeInternal(context: Context?) {
    stopImmediately(context)

    if (context != null) {
      triggerSubtleVibration(context)
    }

    synchronized(lock) {
      _isPlaying.value = true
      playbackJob =
        scope.launch {
          try {
            val sampleRate = 24_000
            val pcmSamples = synthesizeDiamondChimePcm(sampleRate)
            val durationMs = (pcmSamples.size * 1000L) / sampleRate

            val trackCreated =
              runCatching {
                  val track =
                    AudioTrack.Builder()
                      .setAudioAttributes(
                        AudioAttributes.Builder()
                          .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                          .build()
                      )
                      .setAudioFormat(
                        AudioFormat.Builder()
                          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                          .setSampleRate(sampleRate)
                          .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                          .build()
                      )
                      .setBufferSizeInBytes(pcmSamples.size * 2)
                      .setTransferMode(AudioTrack.MODE_STATIC)
                      .build()

                  track.write(pcmSamples, 0, pcmSamples.size)
                  synchronized(lock) {
                    if (!_isPlaying.value) {
                      track.release()
                      return@launch
                    }
                    activeTrack = track
                    track.play()
                  }
                  true
                }
                .getOrDefault(false)

            // Wait for chime completion while remaining immediately cancellable via stopImmediately()
            delay(if (trackCreated) durationMs + 80L else 1_800L)
          } finally {
            synchronized(lock) {
              releaseActiveTrackLocked()
              _isPlaying.value = false
            }
          }
        }
    }
  }

  private fun releaseActiveTrackLocked() {
    val track = activeTrack
    activeTrack = null
    if (track != null) {
      runCatching {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.pause()
          track.flush()
          track.stop()
        }
      }
      runCatching { track.release() }
    }
  }

  /**
   * Synthesizes a distinctive two-phrase crystalline diamond bell arpeggio (C6 - E6 - G6 - C7)
   * with harmonic overtones so the seller immediately recognizes a Chamet customer inquiry.
   */
  internal fun synthesizeDiamondChimePcm(sampleRate: Int = 24_000): ShortArray {
    data class Note(val freqHz: Double, val startSec: Double, val durationSec: Double)

    val sequence =
      listOf(
        // First bright diamond arpeggio
        Note(1046.50, 0.00, 0.28), // C6
        Note(1318.51, 0.16, 0.28), // E6
        Note(1567.98, 0.32, 0.30), // G6
        Note(2093.00, 0.48, 0.65), // C7
        // Second attention-grabbing echo chime
        Note(1318.51, 1.28, 0.25), // E6
        Note(1567.98, 1.44, 0.25), // G6
        Note(2093.00, 1.60, 0.30), // C7
        Note(2637.02, 1.78, 0.78), // E7 sparkling peak
      )

    val totalSeconds = 2.65
    val totalSamples = (sampleRate * totalSeconds).toInt()
    val mixBuffer = FloatArray(totalSamples)

    for (note in sequence) {
      val startIdx = (note.startSec * sampleRate).toInt().coerceAtLeast(0)
      val noteSamples = (note.durationSec * sampleRate).toInt()
      for (i in 0 until noteSamples) {
        val sampleIdx = startIdx + i
        if (sampleIdx >= totalSamples) break
        val t = i.toDouble() / sampleRate
        val attack = (t / 0.008).coerceAtMost(1.0)
        val decay = exp(-4.8 * t / note.durationSec)
        val envelope = (attack * decay).toFloat()

        // Fundamental + crystalline octave & fifth bell overtones
        val wave =
          0.62 * sin(2.0 * PI * note.freqHz * t) +
            0.26 * sin(2.0 * PI * (note.freqHz * 2.0) * t) +
            0.12 * sin(2.0 * PI * (note.freqHz * 3.0) * t)

        mixBuffer[sampleIdx] += (wave.toFloat() * envelope * 0.48f)
      }
    }

    val pcm = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val clamped = mixBuffer[i].coerceIn(-0.95f, 0.95f)
      pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
    }
    return pcm
  }

  private fun triggerSubtleVibration(context: Context) {
    runCatching {
      val vibrator = getVibrator(context) ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
          VibrationEffect.createWaveform(longArrayOf(0L, 120L, 90L, 180L), -1)
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(220L)
      }
    }
  }

  private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }
}
