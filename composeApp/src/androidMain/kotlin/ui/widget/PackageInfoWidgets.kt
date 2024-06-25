package ui.widget

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmapOrNull
import data.model.AppInfoEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.getPackageInfoCompat

@Composable
fun rememberAppName(
  packageInfo: PackageInfo,
  context: Context = LocalContext.current,
): String = rememberSaveable(packageInfo.packageName) {
  packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
}

@Composable
fun rememberPackageIcon(
  packageInfo: PackageInfo,
  context: Context = LocalContext.current,
): State<ImageBitmap?> = produceState<ImageBitmap?>(null, packageInfo, context) {
  value = withContext(Dispatchers.IO) {
    packageInfo.applicationInfo.loadIcon(context.packageManager)
      ?.toBitmapOrNull()
      ?.asImageBitmap()
  }
}

@Composable
fun rememberPackageIcon(
  appInfo: AppInfoEntry,
  context: Context = LocalContext.current,
): State<ImageBitmap?> = produceState<ImageBitmap?>(null, appInfo, context) {
  value = withContext(Dispatchers.IO) {
    context.packageManager.getPackageInfoCompat(appInfo.packageName, 0)
      ?.applicationInfo?.loadIcon(context.packageManager)
      ?.toBitmapOrNull()
      ?.asImageBitmap()
  }
}
