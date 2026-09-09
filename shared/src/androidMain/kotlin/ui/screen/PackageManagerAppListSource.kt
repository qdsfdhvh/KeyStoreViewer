package ui.screen

import android.content.Context
import data.model.UiAppInfo
import data.model.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.getSystemAppInfos
import util.getUserInstalledAppInfos

/** Real device app list; runs PackageManager work (labels/icons) off the main thread. */
class PackageManagerAppListSource(
  private val applicationContext: Context,
) : AppListSource {

  override suspend fun load(type: AppType): List<UiAppInfo> = withContext(Dispatchers.IO) {
    when (type) {
      AppType.User -> applicationContext.getUserInstalledAppInfos()
      AppType.System -> applicationContext.getSystemAppInfos()
    }.map {
      UiAppInfo.from(applicationContext, it)
    }
  }
}
