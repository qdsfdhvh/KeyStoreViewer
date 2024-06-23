package util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * 获取已安装的应用
 */
fun Context.getAppInfos(
  flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES,
): List<PackageInfo> = packageManager.getInstalledPackages(flags)

/**
 * 获取已安装的应用信息
 */
fun Context.getAppInfoCompat(packageName: String): PackageInfo =
  packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)

/**
 * 返回对应包的签名信息
 * @param packageName 包名
 * @return
 */
fun Context.getSignatures(packageName: String): Array<Signature> =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
      .signingInfo
      .apkContentsSigners
  } else {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
      .signatures
  }

/**
 * 复制内容到剪贴板
 * @param content 内容
 */
fun Context.copyContent(content: String) {
  ContextCompat.getSystemService(this, android.content.ClipboardManager::class.java)
    ?.setPrimaryClip(android.content.ClipData.newPlainText(null, content))
}

/**
 * 分享文件
 * @param file 文件
 */
fun Context.shareFile(file: File) {
  val intent = Intent()
  intent.setAction(Intent.ACTION_SEND)
  intent.putExtra(Intent.EXTRA_STREAM, getUriForFile(file))
  intent.setType("*/*")
  startActivity(Intent.createChooser(intent, "分享文件"))
}

/**
 * 创建分享的临时文件
 * @param content 内容
 */
fun Context.createShareTempFile(content: String): File {
  val parentDir = File(cacheDir, "key_store_infos")
  if (!parentDir.exists()) {
    parentDir.mkdirs()
  }
  return File.createTempFile("share", ".txt", parentDir).apply {
    writeText(content)
  }
}

/**
 * 将本地file转换成用于分享的 uri
 * @param file 文件
 */
fun Context.getUriForFile(file: File): Uri = FileProvider.getUriForFile(
  this,
  "${applicationInfo.packageName}.appfileprovider",
  file,
)

/**
 * 是否有查询所有包名的权限
 */
fun Context.hasQueryAllPackagesPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
  ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.QUERY_ALL_PACKAGES,
  ) == PackageManager.PERMISSION_GRANTED
} else {
  true
}

/**
 * 打开系统设置
 */
fun Context.startSystemSetting() {
  val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  val uri = Uri.fromParts("package", packageName, null)
  intent.data = uri
  startActivity(intent)
}
