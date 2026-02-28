package com.rayliu0712.saveit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import com.rayliu0712.saveit.R
import com.rayliu0712.saveit.compose.ProgressActivity

const val PROGRESS_CHANNEL_ID = "com.rayliu0712.saveit.PROGRESS_CHANNEL"
const val COMPLETION_CHANNEL_ID = "com.rayliu0712.saveit.COMPLETION_CHANNEL"
const val ACTION_CANCEL_ALL_JOBS =
  "com.rayliu0712.saveit.ACTION_CANCEL_ALL_JOBS"
const val ACTION_CANCEL_JOB = "com.rayliu0712.saveit.ACTION_CANCEL_JOB"
const val ID_NAME = "com.rayliu0712.saveit.ID"

private lateinit var notificationMan: NotificationManager

fun Context.initNotificationMan(
) {
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

fun Context.createCountNotification(
  remaining: Int,
): Notification {
  val serviceIntent = getFileCopyServicePendingIntent(1) {
    action = ACTION_CANCEL_ALL_JOBS
  }
  val action = Notification.Action.Builder(
    null, "Cancel All", serviceIntent
  ).build()

  val activityIntent = getProgressActivityPendingIntent(1) {
  }

  return Notification.Builder(this, PROGRESS_CHANNEL_ID)
    .apply {
      setSmallIcon(R.drawable.file_copy)
      setContentTitle("$remaining ${if (remaining == 1) "File" else "Files"} Remaining")
      setContentIntent(activityIntent)
      setActions(action)

      if (SDK_INT >= S) {
        setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
      }
    }
    .build()
}

fun Context.notifyCount(
  remaining: Int,
) {
  val notification = createCountNotification(remaining)
  notificationMan.notify(1, notification)
}

fun Context.notifyProgress(
  id: Int,
  filename: String,
  copiedLen: Long,
  fileSize: Long,
) {
  val progress = (copiedLen * 100 / fileSize).toInt()

  val serviceIntent = getFileCopyServicePendingIntent(id) {
    action = ACTION_CANCEL_JOB
    putExtra(ID_NAME, id)
  }
  val action = Notification.Action.Builder(
    null, "Cancel", serviceIntent
  ).build()

  val activityIntent = getProgressActivityPendingIntent(id) {
    putExtra(ID_NAME, id)
  }

  val notification = Notification.Builder(this, PROGRESS_CHANNEL_ID).apply {
    setSmallIcon(R.drawable.file_copy)
    setContentTitle(filename)
    setContentText("${copiedLen.toFileSizeFormat()} / ${fileSize.toFileSizeFormat()}")
    setProgress(100, progress, false)
    setContentIntent(activityIntent)
    setActions(action)

    if (SDK_INT >= S) {
      setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }
  }.build()

  notificationMan.notify(id, notification)
}

fun cancelProgressNotification(
  id: Int
) {
  notificationMan.cancel(id)
}

fun Context.notifyCompletion(
  oUri: Uri,
  filename: String,
  mime: String?
) {
  val id = oUri.hashCode()
  val intent = getOpenFilePendingIntent(id, oUri, mime)

  val notification = Notification.Builder(this, COMPLETION_CHANNEL_ID).apply {
    setSmallIcon(R.drawable.file_copy)
    setContentTitle("$filename Completed")
    setContentIntent(intent)
    setAutoCancel(true)

    if (SDK_INT >= S) {
      setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }
  }.build()

  notificationMan.notify(id, notification)
}

private fun Context.getFileCopyServicePendingIntent(
  id: Int,
  block: Intent.() -> Unit
): PendingIntent {
  val intent = Intent(this, FileCopyService::class.java).apply(block)

  return PendingIntent.getService(
    this,
    id,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
}

private fun Context.getProgressActivityPendingIntent(
  id: Int,
  block: Intent.() -> Unit
): PendingIntent {
  val intent = Intent(this, ProgressActivity::class.java)
    .apply {
      block()
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

  return PendingIntent.getActivity(
    this,
    id,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
}

private fun Context.getOpenFilePendingIntent(
  id: Int,
  uri: Uri,
  mime: String?
): PendingIntent {
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, mime)
    flags =
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
  }

  return PendingIntent.getActivity(
    this,
    id,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
}
