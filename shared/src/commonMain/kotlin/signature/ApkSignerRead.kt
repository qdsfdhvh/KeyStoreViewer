package signature

/**
 * M4 dual-APK signer comparison result types.
 *
 * This extracts certificate metadata only; it performs NO full APK integrity
 * verification (zip entry digest checking is the OS installer's job), and the
 * UI must say so. The readers live in the Android source set; these shared
 * types let the comparison ViewModel own result state on every target.
 */
sealed interface ApkSignerRead {
  data class Success(val meta: ApkSignerMeta) : ApkSignerRead

  data class Failure(val error: ApkSignerReadError) : ApkSignerRead
}

sealed interface ApkSignerReadError {
  data object MissingFile : ApkSignerReadError

  data object EmptyFile : ApkSignerReadError

  data class TooLarge(val maxBytes: Long) : ApkSignerReadError

  /**
   * The platform archive API returned no usable result. It cannot distinguish
   * "unsigned APK" from "corrupted/not an APK" (a null result covers both),
   * so the wording must not claim a distinct unsigned diagnosis.
   */
  data object NotApkOrUnreadable : ApkSignerReadError
}

data class ApkSignerMeta(
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val signerSha256: Set<String>,
)
