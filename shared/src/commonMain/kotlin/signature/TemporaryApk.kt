package signature

import java.io.File
import java.io.IOException

/** Owns a single picked APK copy; callers retain metadata, never this file. */
suspend fun <T> withTemporaryApk(directory: File, read: suspend (File) -> T): T {
  val file = File.createTempFile("apk_compare_", ".apk", directory)
  var failure: Throwable? = null
  try {
    return read(file)
  } catch (e: Throwable) {
    failure = e
    throw e
  } finally {
    try {
      if (!file.delete() && file.exists()) {
        throw IOException("Could not remove temporary APK")
      }
    } catch (cleanup: Exception) {
      // In particular, never replace cancellation with a cleanup exception.
      if (failure != null) failure.addSuppressed(cleanup) else throw cleanup
    }
  }
}
