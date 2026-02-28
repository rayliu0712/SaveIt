package com.rayliu0712.saveit.service

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class Task(
  val id: Int
) {
  var filename by mutableStateOf<String?>(null)
  var fileSize by mutableLongStateOf(0L)
  var copiedLen by mutableLongStateOf(0L)

  var oUri: Uri? = null
  var mime: String? = null

  abstract fun cancel()
}

val taskCount = AtomicInteger(0)

val taskMap = ConcurrentHashMap<Int, Task>()

var cancelAllTasks: (() -> Unit)? = null
