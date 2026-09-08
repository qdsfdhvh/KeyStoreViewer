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
      // Runtime for the M4 @Serializable monitor state (store lives in androidMain).
      implementation(libs.kotlinx.serialization.json)
      // M4: vetted FOSS BouncyCastle for JKS/PKCS12 keystore parsing
      // (Android built-ins have no JKS provider). Both targets are JVM.
      implementation(libs.bouncycastle.bcprov)
    }
    androidMain.dependencies {
      implementation(compose.preview)
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.activity.compose)
      implementation(libs.accompanist.permissions)
      implementation(libs.molecule.runtime)
      implementation(libs.androidx.navigationevent.compose)
      // M4 signature tools: WorkManager-based monitor.
      implementation(libs.work.runtime.ktx)
    }
    val desktopTest by getting
    desktopTest.dependencies {
      implementation(libs.junit)
    }
  }

  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }

  jvmToolchain(17)
}
