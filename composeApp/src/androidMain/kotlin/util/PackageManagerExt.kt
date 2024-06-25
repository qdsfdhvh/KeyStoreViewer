package util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo? {
  return runCatching {
    getPackageInfo(packageName, flags)
  }.onFailure {
    Log.w("PackageManagerExt", it)
  }.getOrNull()
}
