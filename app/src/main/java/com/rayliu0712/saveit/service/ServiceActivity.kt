package com.rayliu0712.saveit.service

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat

class ServiceActivity : ComponentActivity() {
  override fun onCreate(
    savedInstanceState: Bundle?
  ) {
    super.onCreate(savedInstanceState)

    val uriList = when (intent.action) {
      Intent.ACTION_SEND -> arrayListOf(
        IntentCompat.getParcelableExtra(
          intent,
          Intent.EXTRA_STREAM,
          Uri::class.java
        )!!
      )

      Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(
        intent,
        Intent.EXTRA_STREAM,
        Uri::class.java
      )!!

      else -> error("Other actions")
    }

    val serviceIntent = Intent(this, FileCopyService::class.java).apply {
      val myClipData = ClipData.newRawUri("files", uriList[0])
      for (i in 1 until uriList.size) {
        val item = ClipData.Item(uriList[i])
        myClipData.addItem(item)
      }

      clipData = myClipData
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startForegroundService(serviceIntent)
    finish()
  }
}
