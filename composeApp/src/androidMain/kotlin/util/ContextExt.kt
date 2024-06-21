package util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

/**
 * 获取已安装的应用
 */
fun Context.getAppInfos(
  flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES,
): List<PackageInfo> = packageManager.getInstalledPackages(flags)

/**
 * 获取已安装的应用
 */
fun Context.getAppInfo(packageName: String): PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)

/**
 * 返回对应包的签名信息
 * @param packageName 包名
 * @return
 */
fun Context.getSignatures(packageName: String): Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
  packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    .signingInfo
    .apkContentsSigners
} else {
  @Suppress("DEPRECATION")
  packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    .signatures
}
