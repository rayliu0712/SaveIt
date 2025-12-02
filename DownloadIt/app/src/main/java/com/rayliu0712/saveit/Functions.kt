package com.rayliu0712.saveit

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow

/**
 * IO
 */
fun Context.getFilenameAndSize(uri: Uri): Pair<String, Long> {
  val projection = arrayOf(
    OpenableColumns.DISPLAY_NAME,
    OpenableColumns.SIZE
  )

  contentResolver.query(uri, projection, null, null, null)!!.use { cursor ->
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

    cursor.moveToFirst()
    val name = cursor.getString(nameIndex)
    val size = cursor.getLong(sizeIndex)
    return name to size
  }
}

/**
 * IO
 */
fun Context.saveToDownload(
  inputUri: Uri,
  filename: String,
  mime: String?,
  fileSize: Long
): Flow<Long> {
  val iStream = contentResolver.openInputStream(inputUri)!!

  val values = ContentValues().apply {
    put(MediaStore.Downloads.DISPLAY_NAME, filename)
    put(MediaStore.Downloads.MIME_TYPE, mime)
    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
  }
  val oUri = contentResolver.insert(
    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
    values
  )!!
  val oStream = contentResolver.openOutputStream(oUri)!!

  return copyStream(iStream, oStream, fileSize)
}

/**
 * IO
 */
private fun copyStream(
  iStream: InputStream,
  oStream: OutputStream,
  fileSize: Long
): Flow<Long> {
  return flow {
    val buffer = ByteArray(1_048_576)  // 1 MB
    var total = 0L
    var lastPercent = 0L

    while (true) {
      val len = iStream.read(buffer)
      if (len == -1)
        break

      oStream.write(buffer, 0, len)
      total += len

      val currentPercent = (total * 100) / fileSize
      if (currentPercent > lastPercent) {
        emit(total)
        lastPercent = currentPercent
      }
    }

    iStream.close()
    oStream.close()
  }
}

val Long.fileSizeFormat: String
  get() {
    val oneKiB = 1024.0.pow(1)
    val oneMiB = 1024.0.pow(2)
    val oneGiB = 1024.0.pow(3)
    val oneTiB = 1024.0.pow(4)

    return if (this < oneMiB)
      "%.1f KiB".format(this / oneKiB)
    else if (this < oneGiB)
      "%.1f MiB".format(this / oneMiB)
    else if (this < oneTiB)
      "%.1f GiB".format(this / oneGiB)
    else // size >= 1TiB
      "%.1f TiB".format(this / oneTiB)
  }

fun Context.finishApp() {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) {
      break
    }
    context = context.baseContext
  }

  (context as Activity).finishAndRemoveTask()
}
