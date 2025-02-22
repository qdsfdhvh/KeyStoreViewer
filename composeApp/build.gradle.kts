import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.util.Properties

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  androidTarget()
  jvm("desktop")

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyTemplate {
    common {
      withJvm()
      withAndroidTarget()
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.ui)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.materialIconsExtended)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.bundles.voyager)
      implementation(libs.molecule.runtime)
      implementation(libs.okio)
      implementation(libs.material.kolor)
    }
    androidMain.dependencies {
      implementation(compose.preview)
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.activity.compose)
      implementation(libs.accompanist.permissions)
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

android {
  namespace = "com.seiko.keystoreviewer"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  sourceSets["main"].res.srcDirs("src/androidMain/res")
  sourceSets["main"].resources.srcDirs("src/commonMain/resources")

  defaultConfig {
    applicationId = "com.seiko.keystoreviewer"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 3
    versionName = "1.0.0"
    ndk {
      abiFilters.addAll(listOf("arm64-v8a")) // "armeabi-v7a", "x86", "x86_64"
    }
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  val debugSignFile = file("debug-signing.properties")
  val hasDebugSigningProps = debugSignFile.exists()
  signingConfigs {
    if (hasDebugSigningProps) {
      create("debugSign") {
        val signingProp = Properties()
        signingProp.load(debugSignFile.inputStream())
        storeFile = project.file(signingProp.getProperty("storeFile"))
        storePassword = signingProp.getProperty("storePassword")
        keyAlias = signingProp.getProperty("keyAlias")
        keyPassword = signingProp.getProperty("keyPassword")
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
      }
    }
  }

  val releaseSignFile = file("release-signing.properties")
  val hasReleaseSigningProps = releaseSignFile.exists()
  signingConfigs {
    if (hasReleaseSigningProps) {
      create("releaseSign") {
        val signingProp = Properties()
        signingProp.load(releaseSignFile.inputStream())
        storeFile = project.file(signingProp.getProperty("storeFile"))
        storePassword = signingProp.getProperty("storePassword")
        keyAlias = signingProp.getProperty("keyAlias")
        keyPassword = signingProp.getProperty("keyPassword")
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
      }
    }
  }

  buildTypes {
    getByName("debug") {
      isDebuggable = true
      isMinifyEnabled = false
      if (hasDebugSigningProps) {
        signingConfig = signingConfigs.getByName("debugSign")
      }
    }
    getByName("release") {
      isDebuggable = false
      isMinifyEnabled = true
      isShrinkResources = true
      if (hasReleaseSigningProps) {
        signingConfig = signingConfigs.getByName("releaseSign")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
  dependencies {
    debugImplementation(compose.uiTooling)
  }
  applicationVariants.all {
    outputs.all {
      this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
      outputFileName = "keystoreviewer-$name-$versionName-${gitCommit()}.apk"
    }
  }
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

private fun Project.gitCommit(): String {
  val isGitDir = rootProject.file(".git").exists()
  return if (isGitDir) {
    @Suppress("UnstableApiUsage")
    providers.exec {
      commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
  } else {
    ""
  }
}
