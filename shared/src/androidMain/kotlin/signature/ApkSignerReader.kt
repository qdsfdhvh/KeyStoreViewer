package signature

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import util.getPackageArchiveInfoCompat
import util.signaturesCompat
import util.versionCodeCompat
import java.io.File

/**
 * M4 dual-APK signer comparison: extract the complete current signer set of an
 * APK FILE via PackageManager's signing-aware archive APIs
 * ([android.content.pm.PackageManager.getPackageArchiveInfo] with
 * GET_SIGNING_CERTIFICATES, or GET_SIGNATURES below API 28).
 *
 * Result types are shared in [signature.ApkSignerRead]; only the Android
 * readers live here.
 */
suspend fun readApkSignerMeta(
  context: Context,
  filePath: String?,
): ApkSignerRead = withContext(Dispatchers.IO) {
  val file = filePath?.let { File(it) }
  if (file == null || !file.exists()) {
    return@withContext ApkSignerRead.Failure(ApkSignerReadError.MissingFile)
  }
  if (file.length() == 0L) {
    return@withContext ApkSignerRead.Failure(ApkSignerReadError.EmptyFile)
  }
  val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.absolutePath)
    ?: return@withContext ApkSignerRead.Failure(ApkSignerReadError.NotApkOrUnreadable)

  val signers = packageInfo.signaturesCompat
  if (signers.isEmpty()) {
    // Empty signature information is reported through the same honest
    // combined wording: this API path cannot prove "unsigned" separately.
    return@withContext ApkSignerRead.Failure(ApkSignerReadError.NotApkOrUnreadable)
  }
  ApkSignerRead.Success(
    ApkSignerMeta(
      packageName = packageInfo.packageName.orEmpty(),
      versionName = packageInfo.versionName.orEmpty(),
      versionCode = packageInfo.versionCodeCompat,
      signerSha256 = signers.mapTo(mutableSetOf()) { sha256UppercaseHex(it.toByteArray()) },
    ),
  )
}

/**
 * Copies the picked [uri] into a comparison-OWNED temporary file (bounded,
 * cancellation-aware), extracts its signer metadata, then deletes the copy in
 * a `finally` block - on success, on error, on replacement and on
 * cancellation. The caller only ever receives metadata, never a file path, so
 * no temporary APK can outlive this call.
 */
suspend fun readApkSignerMetaFromUri(
  context: Context,
  uri: Uri,
): ApkSignerRead = withContext(Dispatchers.IO) {
  try {
    withTemporaryApk(context.cacheDir) { tempFile ->
      val input = context.contentResolver.openInputStream(uri)
        ?: return@withTemporaryApk ApkSignerRead.Failure(ApkSignerReadError.MissingFile)
      // Captured outside the non-suspending chunk callback; checking the Job
      // still aborts the copy promptly on cancellation.
      val jobContext = currentCoroutineContext()
      input.use { stream ->
        stream.source().buffer().use { source ->
          tempFile.sink().buffer().use { sink ->
            copyBounded(source, sink) { jobContext.ensureActive() }
          }
        }
      }
      readApkSignerMeta(context, tempFile.absolutePath)
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: ApkTooLargeException) {
    ApkSignerRead.Failure(ApkSignerReadError.TooLarge(e.maxBytes))
  } catch (e: Exception) {
    // Copy failed (unreadable content/provider error); the partial file is
    // deleted below and the failure reported honestly.
    ApkSignerRead.Failure(ApkSignerReadError.NotApkOrUnreadable)
  }
}
