package com.rayliu0712.saveit.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat

class ServiceActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
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
      putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
//      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startForegroundService(serviceIntent)
    finish()
  }
}
