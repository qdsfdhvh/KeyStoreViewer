package util

import android.content.pm.PackageInfo


/**
 * 获取版本号
 */
val PackageInfo.versionCodeCompat: Long
  get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
    longVersionCode
  } else {
    @Suppress("DEPRECATION")
    versionCode.toLong()
  }
