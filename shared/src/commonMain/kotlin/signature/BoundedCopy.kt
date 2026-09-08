package signature

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/** Safety bound for copying a picked APK for analysis (runaway streams). */
const val MAX_APK_COPY_BYTES: Long = 512L * 1024 * 1024

/** Thrown by [copyBounded] when the source exceeds the copy bound. */
class ApkTooLargeException(val maxBytes: Long) : Exception("Source exceeds the $maxBytes byte copy limit")

private const val COPY_CHUNK_BYTES = 64L * 1024

/**
 * Copy [source] into [sink] byte-for-byte, aborting with [ApkTooLargeException]
 * as soon as more than [maxBytes] would be written. [checkCancelled] runs
 * between chunks so coroutine cancellation stops the copy immediately; the
 * caller is responsible for deleting the partial output file.
 */
fun copyBounded(
  source: BufferedSource,
  sink: BufferedSink,
  maxBytes: Long = MAX_APK_COPY_BYTES,
  checkCancelled: () -> Unit = {},
) {
  val buffer = Buffer()
  var total = 0L
  while (true) {
    checkCancelled()
    val read = source.read(buffer, COPY_CHUNK_BYTES)
    if (read == -1L) {
      return
    }
    total += read
    if (total > maxBytes) {
      throw ApkTooLargeException(maxBytes)
    }
    sink.write(buffer, read)
  }
}
