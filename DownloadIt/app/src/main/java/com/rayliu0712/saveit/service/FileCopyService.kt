package com.rayliu0712.saveit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rayliu0712.saveit.R
import com.rayliu0712.saveit.fuck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/// Service is singleton
class FileCopyService : LifecycleService() {
  private lateinit var notificationMan: NotificationManager
  private val counter = AtomicInteger(0)
  private val jobMap = mutableMapOf<Int, Job>()

  companion object {
    private const val PROGRESS_CHANNEL_ID = "PROGRESS_CHANNEL"
    private const val ACTION_CANCEL_JOB = "ACTION_CANCEL_JOB"
    private const val EXTRA_NAME = "NOTIFY_ID"
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    super.onStartCommand(intent, flags, startId)

    notificationMan = getSystemService(NotificationManager::class.java)
    createChannel()

    fuck("intent action", intent?.action)
    if (intent!!.action == ACTION_CANCEL_JOB) {
      fuck("enter ACTION_CANCEL_JOB")
      val notifyId = intent.getIntExtra(EXTRA_NAME, -1)
      jobMap[notifyId]?.cancel()
      return START_NOT_STICKY
    }

    lifecycleScope.launch {
      val context = this@FileCopyService

      // TODO: for multiple data (action send multiple)

      val notifyId = counter.incrementAndGet() + 1

      val notification = Notification.Builder(context, PROGRESS_CHANNEL_ID)
        .apply {
          setSmallIcon(R.drawable.file_copy)
          if (SDK_INT >= S) {
            setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
          }
        }
        .build()

      startForeground(
        notifyId,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )

      jobMap[notifyId] = launch(Dispatchers.IO) {
        startWork(notifyId, intent.data!!, intent.type!!)
      }
    }

    return START_NOT_STICKY
  }

  private fun CoroutineScope.startWork(
    notifyId: Int,
    iUri: Uri,
    mime: String,
  ) {
    var oUri: Uri? = null

    try {
      val (filename, fileSize) = getFilenameAndSize(iUri)
      notifyBeginning(notifyId, filename, fileSize)
      ensureActive()

      oUri = insertToDownload(filename, mime)
      ensureActive()

      val iStream = contentResolver.openInputStream(iUri)!!
      val oStream = contentResolver.openOutputStream(oUri)!!
      copyStream(iStream, oStream)
      iStream.close()
      oStream.close()
      ensureActive()

      markAsDone(oUri)
      notifyCompletion(notifyId, filename)
      //
    } catch (_: CancellationException) {
      //
      notificationMan.cancel(notifyId)
      if (oUri != null) {
        contentResolver.delete(oUri, null, null)
      }
    } finally {
      jobMap.remove(notifyId)
      if (counter.decrementAndGet() == 0) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }
  }

  private fun createChannel() {
    val channel = NotificationChannel(
      PROGRESS_CHANNEL_ID,
      "進度",
      IMPORTANCE_LOW
    )

    notificationMan.createNotificationChannel(channel)
  }

  private fun notifyBeginning(
    notifyId: Int,
    filename: String,
    fileSize: Long
  ) {
    fuck("notifyId", notifyId)

    val cancelIntent = Intent(this, FileCopyService::class.java).apply {
      action = ACTION_CANCEL_JOB
      putExtra(EXTRA_NAME, notifyId)
    }
    val pendingIntent = PendingIntent.getService(
      this,
      notifyId,
      cancelIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val action = Notification.Action.Builder(
      null, "取消", pendingIntent
    ).build()

    val notification = Notification.Builder(this, PROGRESS_CHANNEL_ID).apply {
      setSmallIcon(R.drawable.file_copy)
      setContentTitle(filename)
      setContentText(fileSize.toFileSizeFormat())
      setProgress(0, 0, true)
      setActions(action)
//      setOngoing(true)
//      setAutoCancel(true)

      if (SDK_INT >= S) {
        setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
      }
    }.build()

    notificationMan.notify(notifyId, notification)
  }

  private fun notifyCompletion(notifyId: Int, filename: String) {
    val notification = Notification.Builder(this, PROGRESS_CHANNEL_ID).apply {
      setSmallIcon(R.drawable.file_copy)
      setContentTitle("$filename 完成")
//      setOngoing(true)
//      setAutoCancel(true)

      if (SDK_INT >= S) {
        setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
      }
    }.build()

    notificationMan.notify(notifyId, notification)
  }
}