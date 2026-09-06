package data.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface SignSource {
  @Serializable
  data class PackageName(
    val packageName: String,
  ) : SignSource

  @Serializable
  data class Apk(
    val filePath: String,
  ) : SignSource
}
