package data.model

data class AppInfoEntry(
  val packageName: String,
  val name: String,
  val lastUpdateTime: Long,
) {
  companion object
}
