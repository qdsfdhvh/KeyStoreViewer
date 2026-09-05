package ui.screen

import android.content.pm.PackageInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import data.model.AppSignature
import data.model.SignSource
import data.model.UiAppInfo
import data.model.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.getPackageArchiveInfoCompat
import util.getPackageInfoCompat
import util.signaturesCompat

@Composable
actual fun ExtractSignatureInfo(
  signSource: SignSource,
  content: @Composable ExtractSignatureInfoScope.() -> Unit,
) {
  when (signSource) {
    is SignSource.PackageName -> ExtractSignatureInfoByPackageName(
      packageName = signSource.packageName,
      content = content,
    )

    is SignSource.Apk -> ExtractSignatureInfoByFilePath(
      filePath = signSource.filePath,
      content = content,
    )
  }
}

@Composable
private fun ExtractSignatureInfoByPackageName(
  packageName: String,
  content: @Composable ExtractSignatureInfoScope.() -> Unit,
) {
  val context = LocalContext.current
  val packageInfoNullable by produceState<PackageInfo?>(null) {
    value = withContext(Dispatchers.IO) {
      context.packageManager.getPackageInfoCompat(packageName)
    }
  }
  packageInfoNullable?.let { packageInfo ->
    val appInfo: UiAppInfo = remember { UiAppInfo.from(context, packageInfo) }
    val signatures: List<AppSignature> by produceState(emptyList()) {
      value = withContext(Dispatchers.IO) {
        packageInfo.signaturesCompat.map {
          AppSignature.from(it)
        }
      }
    }
    ExtractSignatureInfoScopeImpl(
      appInfo = appInfo,
      signatures = signatures,
    ).content()
  }
}

@Composable
private fun ExtractSignatureInfoByFilePath(
  filePath: String,
  content: @Composable ExtractSignatureInfoScope.() -> Unit,
) {
  val context = LocalContext.current
  val packageInfoNullable by produceState<PackageInfo?>(null) {
    value = withContext(Dispatchers.IO) {
      context.packageManager.getPackageArchiveInfoCompat(filePath)
    }
  }
  packageInfoNullable?.let { packageInfo ->
    val appInfo: UiAppInfo = remember { UiAppInfo.from(context, packageInfo) }
    val signatures: List<AppSignature> by produceState(emptyList()) {
      value = withContext(Dispatchers.IO) {
        packageInfo.signaturesCompat.map {
          AppSignature.from(it)
        }
      }
    }
    ExtractSignatureInfoScopeImpl(
      appInfo = appInfo,
      signatures = signatures,
    ).content()
  }
}

@Stable
private class ExtractSignatureInfoScopeImpl(
  override val appInfo: UiAppInfo,
  override val signatures: List<AppSignature>,
) : ExtractSignatureInfoScope
