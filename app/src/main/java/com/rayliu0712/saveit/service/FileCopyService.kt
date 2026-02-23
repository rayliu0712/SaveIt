package com.rayliu0712.saveit.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/// Service is singleton
class FileCopyService : LifecycleService() {
  private val parentJob = SupervisorJob()
  private val parentScope = lifecycleScope + parentJob
  private val counter = AtomicInteger(0)
  private val jobMap = ConcurrentHashMap<Int, Job>()

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    super.onStartCommand(intent, flags, startId)

    if (intent!!.action == ACTION_CANCEL_ALL_JOBS) {
      parentJob.cancelChildren()

    } else if (intent.action == ACTION_CANCEL_JOB) {
      val progressId = intent.getIntExtra(ID_NAME, -1)
      jobMap[progressId]?.cancel()

    } else {
      val clipData = intent.clipData!!
      val uriCount = clipData.itemCount

      val lastCount = counter.getAndAdd(uriCount)
      if (lastCount == 0) {
        initNotificationMan()

        startForeground(
          1,  // id = 1
          createCountNotification(uriCount),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      } else {
        notifyCount(lastCount + uriCount)
      }

      for (i in 0 until uriCount) {
        val uri = clipData.getItemAt(i).uri

        // progressId: 2, 4, 6, 8, ...
        val progressId = (lastCount + i + 1) * 2

        jobMap[progressId] = parentScope.launch(Dispatchers.Main) {
          try {
            withContext(Dispatchers.IO) {
              startWork(progressId, uri)
            }
          } finally {
            jobMap.remove(progressId)
            val count = counter.decrementAndGet()
            notifyCount(count)
            if (count == 0) {
              stopForeground(STOP_FOREGROUND_REMOVE)
              stopSelf()
            }
          }
        }
      }
    }

    return START_NOT_STICKY
  }

  private fun CoroutineScope.startWork(progressId: Int, iUri: Uri) {
    val (filename, fileSize) = contentResolver.getFilenameAndSize(iUri)
    notifyProgress(progressId, filename, 0, fileSize)

    val mime = contentResolver.getType(iUri)
    val oUri = contentResolver.insertToDownload(filename, mime)

    try {
      contentResolver.openInputStream(iUri)!!.use { iStream ->
        contentResolver.openOutputStream(oUri)!!.use { oStream ->
          copyStream(iStream, oStream) { copiedLen ->
            notifyProgress(progressId, filename, copiedLen, fileSize)
          }
        }
      }

      cancelProgressNotification(progressId)
      contentResolver.markAsDone(oUri)
      notifyCompletion(filename)
    } catch (e: CancellationException) {
      cancelProgressNotification(progressId)
      contentResolver.delete(oUri, null, null)
      throw e
    }
  }
}
