package com.example.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.R
import com.example.data.JarvisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CopyReplyBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val providedReply = intent.getStringExtra(EXTRA_REPLY_TEXT)
    if (!providedReply.isNullOrBlank()) {
      copyToClipboard(context, providedReply)
      HelperNotificationManager.cancelHelperNotifications(context)
      return
    }

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.Main).launch {
      try {
        val reply = JarvisRepository.getInstance(context).getFormattedReplySnapshot()
        copyToClipboard(context, reply)
        HelperNotificationManager.cancelHelperNotifications(context)
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Chamet Customer Reply", text))
    Toast.makeText(context, context.getString(R.string.msg_reply_copied), Toast.LENGTH_SHORT).show()
  }

  companion object {
    const val ACTION_COPY_SAVED_REPLY = "com.example.action.COPY_SAVED_REPLY"
    const val EXTRA_REPLY_TEXT = "extra_reply_text"
  }
}
