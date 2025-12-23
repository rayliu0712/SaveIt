package com.rayliu0712.saveit.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

class ServiceActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    when (intent.action) {
      Intent.ACTION_VIEW -> {
        val uri = intent.data!!
        startServiceAndFinish(uri)
      }

      Intent.ACTION_SEND -> {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        startServiceAndFinish(uri!!)
      }
    }
  }

  private fun startServiceAndFinish(uri: Uri) {
    val i = Intent(this, FileCopyService::class.java)
    i.setDataAndType(uri, intent.type)
    startForegroundService(i)
    finish()
  }
}