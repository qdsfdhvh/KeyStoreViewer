package util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo? = runCatching {
  getPackageInfo(packageName, flags)
}.onFailure {
  Log.w("PackageManagerExt", it)
}.getOrNull()
