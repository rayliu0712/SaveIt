package com.rayliu0712.saveit.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.io.InputStream
import java.io.OutputStream

fun Context.getFilenameAndSize(uri: Uri): Pair<String, Long> {
  val projection = arrayOf(
    OpenableColumns.DISPLAY_NAME,
    OpenableColumns.SIZE
  )

  return contentResolver.query(uri, projection, null, null, null)!!
    .use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

      cursor.moveToFirst()
      val name = cursor.getString(nameIndex)
      val size = cursor.getLong(sizeIndex)
      name to size
    }
}

fun Context.saveToDownload(
  inputUri: Uri,
  filename: String,
  mime: String?,
  fileSize: Long,
): Flow<Long> {
  val iStream = contentResolver.openInputStream(inputUri)!!

  val values = ContentValues().apply {
    put(MediaStore.Downloads.DISPLAY_NAME, filename)
    put(MediaStore.Downloads.MIME_TYPE, mime)
    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    put(MediaStore.Downloads.IS_PENDING, true)
  }
  val oUri = contentResolver.insert(
    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
    values
  )!!
  val oStream = contentResolver.openOutputStream(oUri)!!

  return copyStream(iStream, oStream, fileSize)
    .onCompletion {
      val newValues = ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, false)
      }
      contentResolver.update(oUri, newValues, null, null)
    }
}

private fun copyStream(
  iStream: InputStream,
  oStream: OutputStream,
  fileSize: Long
) = flow {
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

fun Long.toFileSizeFormat(): String {
  val oneKiB = 1_024.0
  val oneMiB = 1_048_576.0
  val oneGiB = 1_073_741_824.0
  val oneTiB = 1_099_511_627_776.0

  return if (this < oneMiB)
    "%.1f KiB".format(this / oneKiB)
  else if (this < oneGiB)
    "%.1f MiB".format(this / oneMiB)
  else if (this < oneTiB)
    "%.1f GiB".format(this / oneGiB)
  else // size >= 1TiB
    "%.1f TiB".format(this / oneTiB)
}