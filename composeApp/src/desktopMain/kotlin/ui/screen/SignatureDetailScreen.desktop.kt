package ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import data.model.AppSignature
import data.model.SignSource
import data.model.UiAppInfo

@Composable
actual fun ExtractSignatureInfo(
  signSource: SignSource,
  content: @Composable ExtractSignatureInfoScope.() -> Unit,
) {
  when (signSource) {
    is SignSource.PackageName -> {
      Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("Extracting signature info by package name is not supported on desktop")
      }
    }

    is SignSource.Apk -> ExtractSignatureInfoByFilePath()
  }
}

@Composable
private fun ExtractSignatureInfoByFilePath() {
  // TODO: Extract signature from apk file is not supported on desktop yet.
  Box(Modifier.fillMaxSize(), Alignment.Center) {
    Text("Extracting signature from apk file is not supported on desktop yet")
  }
}

@Stable
private class ExtractSignatureInfoScopeImpl(
  override val appInfo: UiAppInfo,
  override val signatures: List<AppSignature>,
) : ExtractSignatureInfoScope
