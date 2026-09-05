package data.model

sealed interface SignSource {
  data class PackageName(
    val packageName: String,
  ) : SignSource

  data class Apk(
    val filePath: String,
  ) : SignSource
}
