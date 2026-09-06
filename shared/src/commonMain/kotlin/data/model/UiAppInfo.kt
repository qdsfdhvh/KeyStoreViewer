package data.model

import androidx.compose.ui.graphics.ImageBitmap

data class UiAppInfo(
  val packageName: String,
  val name: String,
  val versionCode: Long,
  val versionName: String,
  val lastUpdateTime: Long,
  val icon: ImageBitmap?,
) {
  companion object
}
