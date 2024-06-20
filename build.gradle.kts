plugins {
  // this is necessary to avoid the plugins to be loaded multiple times
  // in each subproject's classloader
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.jetbrainsCompose) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.spotless)
}

spotless {
  kotlin {
    target("*.kt", "**/*.kt")
    targetExclude("build/", "**/build/")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(editorConfigOverride)
  }
  kotlinGradle {
    target("*.gradle.kts", "**/*.gradle.kts")
    targetExclude("build/", "**/build/")
    ktlint(libs.versions.ktlint.get())
      .editorConfigOverride(editorConfigOverride)
  }
}

val editorConfigOverride = mapOf(
  "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
)
