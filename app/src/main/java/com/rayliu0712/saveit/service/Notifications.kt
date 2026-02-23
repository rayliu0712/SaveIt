package com.rayliu0712.saveit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import com.rayliu0712.saveit.R

const val PROGRESS_CHANNEL_ID = "com.rayliu0712.saveit.PROGRESS_CHANNEL"
const val COMPLETION_CHANNEL_ID = "com.rayliu0712.saveit.COMPLETION_CHANNEL"
const val ACTION_CANCEL_ALL_JOBS =
  "com.rayliu0712.saveit.ACTION_CANCEL_ALL_JOBS"
const val ACTION_CANCEL_JOB = "com.rayliu0712.saveit.ACTION_CANCEL_JOB"
const val PROGRESS_ID_NAME = "com.rayliu0712.saveit.PROGRESS_ID"

private lateinit var notificationMan: NotificationManager

fun Context.initNotificationMan() {
  notificationMan = getSystemService(NotificationManager::class.java).apply {
    val channels = listOf(
      NotificationChannel(
        PROGRESS_CHANNEL_ID,
        "Progress",
        IMPORTANCE_LOW
      ),
      NotificationChannel(
        COMPLETION_CHANNEL_ID,
        "Completion",
        IMPORTANCE_LOW
      )
    )

    createNotificationChannels(channels)
  }
}

fun Context.createCountNotification(remaining: Int): Notification {
  val cancelIntent = Intent(this, FileCopyService::class.java).apply {
    action = ACTION_CANCEL_ALL_JOBS
  }
  val pendingIntent = PendingIntent.getService(
    this,
    1,
    cancelIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
  val action = Notification.Action.Builder(
    null, "Cancel All", pendingIntent
  ).build()

  return Notification.Builder(this, PROGRESS_CHANNEL_ID)
    .apply {
      setSmallIcon(R.drawable.file_copy)
      setContentTitle("remains $remaining files")
      setActions(action)

      if (SDK_INT >= S) {
        setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
      }
    }
    .build()
}

fun Context.notifyCount(remaining: Int) {
  val notification = createCountNotification(remaining)
  notificationMan.notify(1, notification)
}

fun Context.notifyProgress(
  progressId: Int,
  filename: String,
  copiedLen: Long,
  fileSize: Long,
) {
  val cancelIntent = Intent(this, FileCopyService::class.java).apply {
    action = ACTION_CANCEL_JOB
    putExtra(PROGRESS_ID_NAME, progressId)
  }
  val pendingIntent = PendingIntent.getService(
    this,
    progressId,
    cancelIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
  val action = Notification.Action.Builder(
    null, "Cancel", pendingIntent
  ).build()

  val progress = (copiedLen * 100 / fileSize).toInt()

  val notification = Notification.Builder(this, PROGRESS_CHANNEL_ID).apply {
    setSmallIcon(R.drawable.file_copy)
    setContentTitle(filename)
    setContentText(fileSize.toFileSizeFormat())
    setProgress(100, progress, false)
    setActions(action)

    if (SDK_INT >= S) {
      setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }
  }.build()

  notificationMan.notify(progressId, notification)
}

fun cancelProgressNotification(progressId: Int) {
  notificationMan.cancel(progressId)
}

fun Context.notifyCompletion(filename: String) {
  val notification = Notification.Builder(this, COMPLETION_CHANNEL_ID).apply {
    setSmallIcon(R.drawable.file_copy)
    setContentTitle("$filename Completed")

    if (SDK_INT >= S) {
      setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }
  }.build()

  val id = System.currentTimeMillis().hashCode()
  notificationMan.notify(id, notification)
}
