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

/// Service is singleton
class FileCopyService : LifecycleService() {
  private val parentJob = SupervisorJob()
  private val parentScope = lifecycleScope + parentJob

  override fun onCreate() {
    super.onCreate()
    cancelAllTasks = { parentJob.cancelChildren() }
    initNotificationMan()
  }

  override fun onDestroy() {
    cancelAllTasks = null
    super.onDestroy()
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    super.onStartCommand(intent, flags, startId)

    // 取消全部
    if (intent!!.action == ACTION_CANCEL_ALL_JOBS) {
      cancelAllTasks?.invoke()
    }

    // 取消單一
    else if (intent.action == ACTION_CANCEL_JOB) {
      val id = intent.getIntExtra(ID_NAME, -1)
      taskMap[id]?.cancel()
    }

    // 啟動 & 儲存
    else {
      val clipData = intent.clipData!!
      val uriCount = clipData.itemCount

      val lastCount = taskCount.getAndAdd(uriCount)
      if (lastCount == 0)
        startForeground(
          1,  // id = 1
          createCountNotification(uriCount),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      else
        notifyCount(lastCount + uriCount)

      for (i in 0 until uriCount) {
        val uri = clipData.getItemAt(i).uri

        // id: 2, 4, 6, 8, ...
        val id = (lastCount + i + 1) * 2

        startSaveTask(id, uri)
      }
    }

    return START_NOT_STICKY
  }

  private fun startSaveTask(
    id: Int,
    iUri: Uri
  ) {
    var job: Job? = null

    // [+] add to futureMap
    val task = object : Task(id) {
      override fun cancel() {
        job?.cancel()
      }
    }
    taskMap[id] = task

    job = parentScope.launch(Dispatchers.Main) {
      try {
        val (filename, fileSize) = withContext(Dispatchers.IO) {
          contentResolver.getFilenameAndSize(iUri)
        }
        task.filename = filename
        task.fileSize = fileSize
        notifyProgress(id, filename, 0, fileSize)

        withContext(Dispatchers.IO) {
          task.mime = contentResolver.getType(iUri)
          task.oUri = contentResolver.insertToDownload(filename, task.mime)
          saveUriToDownload(iUri, task)
        }
      } finally {
        // [-] remove from futureMap
        taskMap.remove(id)

        val count = taskCount.decrementAndGet()
        notifyCount(count)
        if (count == 0) {
          stopForeground(STOP_FOREGROUND_REMOVE)
          stopSelf()
        }
      }
    }
  }

  private suspend fun CoroutineScope.saveUriToDownload(
    iUri: Uri,
    task: Task
  ) {
    val oUri = task.oUri!!
    val id = task.id
    val filename = task.filename!!
    val fileSize = task.fileSize

    try {
      contentResolver.openInputStream(iUri)!!.use { iStream ->
        contentResolver.openOutputStream(oUri)!!.use { oStream ->
          copyStream(iStream, oStream)
            .collect { copiedLen ->
              // 在 Main Dispatcher 更新，保證 Compose 能監測到
              withContext(Dispatchers.Main) {
                task.copiedLen = copiedLen
              }
              notifyProgress(id, filename, copiedLen, fileSize)
            }
        }
      }

      cancelProgressNotification(id)
      contentResolver.markAsDone(oUri)
      notifyCompletion(oUri, filename, task.mime)

    } catch (e: CancellationException) {
      cancelProgressNotification(id)
      contentResolver.delete(oUri, null, null)
      throw e
    }
  }
}
