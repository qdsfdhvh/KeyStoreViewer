package data.model

import android.content.Context
import android.content.pm.PackageInfo

fun AppInfoEntry.Companion.from(info: PackageInfo, context: Context): AppInfoEntry = AppInfoEntry(
  packageName = info.packageName,
  name = info.applicationInfo.loadLabel(context.packageManager).toString(),
  lastUpdateTime = info.lastUpdateTime,
)
