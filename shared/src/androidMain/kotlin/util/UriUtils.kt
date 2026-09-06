package util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import okio.buffer
import okio.sink
import okio.source
import java.io.File

fun getFilePathFromUri(context: Context, uri: Uri): String? {
  // 如果是 file:// 类型的 Uri
  if (ContentResolver.SCHEME_FILE == uri.scheme) {
    return uri.path
  }

  // 如果是 content:// 类型的 Uri
  if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
    context.contentResolver.openInputStream(uri)?.source()?.use { source ->
      val tempFile = File.createTempFile("temp_apk", ".apk", context.cacheDir)
      tempFile.sink().buffer().use { sink ->
        sink.writeAll(source)
      }
      return tempFile.absolutePath
    }
  }

  return null
}
