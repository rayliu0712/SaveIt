package com.rayliu0712.saveit.service

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.OutputStream

fun ContentResolver.getFilenameAndSize(uri: Uri): Pair<String, Long> {
  val projection = arrayOf(
    OpenableColumns.DISPLAY_NAME,
    OpenableColumns.SIZE
  )

  return query(uri, projection, null, null, null)!!
    .use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

      cursor.moveToFirst()
      val name = cursor.getString(nameIndex)
      val size = cursor.getLong(sizeIndex)
      name to size
    }
}

fun ContentResolver.insertToDownload(
  filename: String,
  mime: String?,
): Uri {
  val values = ContentValues().apply {
    put(MediaStore.Downloads.DISPLAY_NAME, filename)
    put(MediaStore.Downloads.MIME_TYPE, mime)
    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    // todo: put(MediaStore.Downloads.IS_PENDING, true)
  }

  return insert(
    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
    values
  )!!
}

fun ContentResolver.markAsDone(uri: Uri) {
  val values = ContentValues().apply {
    put(MediaStore.Downloads.IS_PENDING, false)
  }

  update(uri, values, null, null)
}

fun CoroutineScope.copyStream(
  iStream: InputStream,
  oStream: OutputStream,
  onProgress: (Long) -> Unit
) {
  val buffer = ByteArray(1_048_576)  // 1 MB
  var copiedLen = 0L
  var lastTime = System.currentTimeMillis()

  while (true) {
    val len = iStream.read(buffer)
    if (len == -1) break
    ensureActive()

    oStream.write(buffer, 0, len)
    ensureActive()

    copiedLen += len
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastTime >= 1000) {
      onProgress(copiedLen)
      lastTime = System.currentTimeMillis()
    }
  }

  onProgress(copiedLen)
}


fun Long.toFileSizeFormat(): String {
  val oneKiB = 1_024.0
  val oneMiB = 1_048_576.0
  val oneGiB = 1_073_741_824.0
  val oneTiB = 1_099_511_627_776.0

  return if (this < oneMiB)
    "%.1f KB".format(this / oneKiB)
  else if (this < oneGiB)
    "%.1f MB".format(this / oneMiB)
  else if (this < oneTiB)
    "%.1f GB".format(this / oneGiB)
  else // size >= 1TiB
    "%.1f TB".format(this / oneTiB)
}
