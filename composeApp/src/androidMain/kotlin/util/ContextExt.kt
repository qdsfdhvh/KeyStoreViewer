package util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.content.pm.Signature
/**
 * 返回对应包的签名信息
 * @param packageName 包名
 * @return
 */
fun Context.getSignatures(packageName: String): Array<Signature> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo
            .apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            .signatures
    }
}