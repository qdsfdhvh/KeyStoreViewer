package data.model

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import util.versionCodeCompat

fun UiAppInfo.Companion.from(context: Context, info: PackageInfo): UiAppInfo = UiAppInfo(
  packageName = info.packageName,
  name = info.applicationInfo?.loadLabel(context.packageManager).toString(),
  versionCode = info.versionCodeCompat,
  versionName = info.versionName.orEmpty(),
  lastUpdateTime = info.lastUpdateTime,
  icon = info.applicationInfo?.loadIcon(context.packageManager)?.toBitmapOrNull()?.asImageBitmap(),
)
