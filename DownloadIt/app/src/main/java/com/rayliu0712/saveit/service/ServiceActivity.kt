package com.rayliu0712.saveit.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

class ServiceActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val uri = when (intent.action) {
      Intent.ACTION_VIEW -> intent.data!!
      Intent.ACTION_SEND -> intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
      else -> error("Other actions")
    }
    val mime = intent.type

    val serviceIntent = Intent(this, FileCopyService::class.java).apply {
      setDataAndType(uri, mime)
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    startForegroundService(serviceIntent)
    finish()
  }
}