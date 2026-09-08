import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(projects.shared)
  implementation(compose.desktop.currentOs)
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.seiko.keystoreviewer"
      packageVersion = "1.1.0"
      // Product branding generated from the same master as the Android
      // launcher icons (implementation/tools/generate_icons.py, ignored).
      linux {
        iconFile.set(rootProject.file("desktopApp/icons/keystoreviewer-linux.png"))
      }
      windows {
        iconFile.set(rootProject.file("desktopApp/icons/keystoreviewer.ico"))
      }
      macOS {
        iconFile.set(rootProject.file("desktopApp/icons/keystoreviewer.icns"))
      }
    }
  }
}
