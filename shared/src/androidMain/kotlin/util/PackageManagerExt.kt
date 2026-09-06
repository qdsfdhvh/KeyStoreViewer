package util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

fun PackageManager.getPackageInfoCompat(
  packageName: String,
  flags: Int = signaturesFlagCompat,
): PackageInfo? = runCatching {
  getPackageInfo(packageName, flags)
}.onFailure {
  Log.w("PackageManagerExt", it)
}.getOrNull()

fun PackageManager.getPackageArchiveInfoCompat(
  archiveFilePath: String,
  flags: Int = signaturesFlagCompat,
): PackageInfo? = runCatching {
  getPackageArchiveInfo(archiveFilePath, flags)
}.onFailure {
  Log.w("PackageManagerExt", it)
}.getOrNull()

@Suppress("DEPRECATION")
private val signaturesFlagCompat: Int
  get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    PackageManager.GET_SIGNING_CERTIFICATES
  } else {
    0
  } or PackageManager.GET_SIGNATURES
