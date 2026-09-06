package util

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build

/**
 * 获取版本号
 */
val PackageInfo.versionCodeCompat: Long
  get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    longVersionCode
  } else {
    @Suppress("DEPRECATION")
    versionCode.toLong()
  }

/**
 * 获取签名信息
 */
val PackageInfo.signaturesCompat: Array<Signature>
  get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    signingInfo?.apkContentsSigners
  } else {
    @Suppress("DEPRECATION")
    signatures
  } ?: emptyArray()
