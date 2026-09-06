import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  id("com.android.kotlin.multiplatform.library")
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.compose.compiler)
  id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
  jvm("desktop")

  androidLibrary {
    namespace = "com.seiko.keystoreviewer.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
  }

  sourceSets {
    commonMain.dependencies {
      api(compose.runtime)
      api(compose.ui)
      api(compose.foundation)
      api(compose.material3)
      implementation(compose.materialIconsExtended)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.kotlinx.io)
      implementation(libs.okio)
      implementation(libs.material.kolor)
    }
    androidMain.dependencies {
      implementation(compose.preview)
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.activity.compose)
      implementation(libs.accompanist.permissions)
      implementation(libs.molecule.runtime)
      implementation(libs.androidx.navigationevent.compose)
    }
    val desktopMain by getting
    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
    }
  }

  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }

  jvmToolchain(17)
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.seiko.keystoreviewer"
      packageVersion = "1.0.0"
    }
  }
}
