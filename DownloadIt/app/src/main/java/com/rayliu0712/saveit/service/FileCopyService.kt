package com.rayliu0712.saveit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rayliu0712.saveit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/// Service is singleton
class FileCopyService : LifecycleService() {
  private lateinit var notificationMan: NotificationManager

  private val counter = AtomicInteger(0)

  companion object {
    private const val PROGRESS_CHANNEL_ID = "PROGRESS_CHANNEL"
    private const val COMPLETION_CHANNEL_ID = "COMPLETION_CHANNEL"
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    counter.incrementAndGet()
    notificationMan = getSystemService(NotificationManager::class.java)
    createChannels()

    val notifyId = System.currentTimeMillis().hashCode()

    val builder = Notification.Builder(this, PROGRESS_CHANNEL_ID)
      .apply {
        setSmallIcon(R.drawable.file_copy)
        setProgress(100, 0, false)
        setOnlyAlertOnce(true)
        // setOnGoing(true) is not working

        if (SDK_INT >= S) {
          setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
      }

    startForeground(
      notifyId,
      builder.build(),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    )

    lifecycleScope.launch {
      val uri = intent!!.data!!
      val mime = intent.type!!

      val (filename, fileSize) = withContext(Dispatchers.IO) {
        getFilenameAndSize(uri)
      }

      saveToDownload(uri, filename, mime, fileSize)
        .flowOn(Dispatchers.IO)
        .collect { value ->
          val notification = builder
            .setContentTitle(filename)
            .setContentText("${value.toFileSizeFormat()} / ${fileSize.toFileSizeFormat()}")
            .setProgress(100, (value * 100 / fileSize).toInt(), false)
            .build()

          notificationMan.notify(notifyId, notification)
        }

      notificationMan.cancel(notifyId)
      notifyCompletion(filename)

      // stop foreground & service
      if (counter.decrementAndGet() == 0) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }

    // return START_REDELIVER_INTENT
    return START_NOT_STICKY
  }

  private fun createChannels() {
    notificationMan.createNotificationChannels(
      listOf(
        NotificationChannel(
          PROGRESS_CHANNEL_ID,
          "Progress",
          NotificationManager.IMPORTANCE_LOW
        ),
        NotificationChannel(
          COMPLETION_CHANNEL_ID,
          "Completion",
          NotificationManager.IMPORTANCE_DEFAULT
        )
      )
    )
  }

  private fun notifyCompletion(filename: String) {
    val notifyId = System.currentTimeMillis().hashCode()

    val notification = Notification.Builder(this, COMPLETION_CHANNEL_ID)
      .apply {
        setSmallIcon(R.drawable.done)
        setContentTitle("\"$filename\" completed")

        if (SDK_INT >= S) {
          setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
      }
      .build()

    notificationMan.notify(notifyId, notification)
  }
}