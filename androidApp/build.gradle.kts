@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
}

android {
  namespace = "com.seiko.keystoreviewer"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.seiko.keystoreviewer"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 6
    versionName = "1.1.0"
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
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(projects.shared)
  implementation(libs.androidx.activity.compose)
  implementation(libs.navigation3.runtime)
  implementation(libs.navigation3.ui)
  implementation(libs.androidx.navigationevent.compose)
  debugImplementation(libs.compose.ui.tooling)
}

val gitCommit: String =
  if (rootProject.file(".git").exists()) {
    providers.exec {
      commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
  } else {
    ""
  }

fun renameApkTask(variant: String) = tasks.register<Copy>("rename${variant.replaceFirstChar { it.uppercase() }}Apk") {
  dependsOn("assemble${variant.replaceFirstChar { it.uppercase() }}")
  from(layout.buildDirectory.dir("outputs/apk/${variant.lowercase()}"))
  include("*.apk")
  into(layout.buildDirectory.dir("outputs/apk-named/${variant.lowercase()}"))
  rename { "keystoreviewer-${variant.lowercase()}-${android.defaultConfig.versionName}-$gitCommit.apk" }
}

renameApkTask("debug")
renameApkTask("release")
