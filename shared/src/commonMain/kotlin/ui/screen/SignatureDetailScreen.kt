package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.SignSource
import data.model.UiAppInfo
import kotlinx.coroutines.launch
import platform.LocalContentHandler
import ui.widget.DetailTopBar
import ui.widget.HexText

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalFoundationApi::class,
  ExperimentalStdlibApi::class,
)
@Composable
fun SignatureDetailScreen(
  signSource: SignSource,
  onBack: () -> Unit,
) {
  val contentHandler = LocalContentHandler.current
  SignatureDetailScreen(
    onBack = onBack,
    signSource = signSource,
    onShareContentClick = { content ->
      contentHandler.shareContent(content)
    },
    onCopyContentClick = { content, label ->
      contentHandler.copyToClipboard(content, label)
    },
  )
}

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalFoundationApi::class,
  ExperimentalStdlibApi::class,
)
@Composable
private fun SignatureDetailScreen(
  onBack: () -> Unit,
  signSource: SignSource,
  onShareContentClick: (content: String) -> Unit,
  onCopyContentClick: (content: String, label: String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  Scaffold(
    topBar = {
      DetailTopBar("Signature details", onBack)
    },
    snackbarHost = {
      SnackbarHost(snackbarHostState)
    },
  ) { innerPadding ->

    ExtractSignatureInfo(signSource) {
      Column(
        modifier = Modifier
          .padding(innerPadding)
          .fillMaxSize(),
      ) {
        val pagerState = rememberPagerState(0) { signatures.size }

        HorizontalPager(
          pagerState,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        ) { page ->
          val signature = signatures[page]

          var isMd5Upper by rememberSaveable { mutableStateOf(false) }
          var isMd5ColonSplit by rememberSaveable { mutableStateOf(true) }

          val md5 by remember {
            derivedStateOf {
              signature.bytes.md5().toByteArray().toHexString(
                HexFormat {
                  upperCase = isMd5Upper
                  if (isMd5ColonSplit) {
                    bytes.bytesPerGroup = 1
                    bytes.groupSeparator = ":"
                  }
                },
              )
            }
          }

          var isSha1Upper by rememberSaveable { mutableStateOf(false) }
          var isSha1ColonSplit by rememberSaveable { mutableStateOf(true) }
          val sha1 by remember {
            derivedStateOf {
              signature.bytes.sha1().toByteArray().toHexString(
                HexFormat {
                  upperCase = isSha1Upper
                  if (isSha1ColonSplit) {
                    bytes.bytesPerGroup = 1
                    bytes.groupSeparator = ":"
                  }
                },
              )
            }
          }

          var isSha256Upper by rememberSaveable { mutableStateOf(false) }
          var isSha256ColonSplit by rememberSaveable { mutableStateOf(true) }
          val sha256 by remember {
            derivedStateOf {
              signature.bytes.sha256().toByteArray().toHexString(
                HexFormat {
                  upperCase = isSha256Upper
                  if (isSha256ColonSplit) {
                    bytes.bytesPerGroup = 1
                    bytes.groupSeparator = ":"
                  }
                },
              )
            }
          }

          val modulusHex = signature.modulusHex
          val modulusString = signature.modulusString

          LazyColumn(
            contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            item {
              SignatureIdentity(appInfo)
            }
            stickyHeader {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.background)
                  .fillMaxWidth(),
              ) {
                var isShowShareContentDialog by remember { mutableStateOf(false) }
                val isAllUpper by remember {
                  derivedStateOf {
                    isMd5Upper && isSha1Upper && isSha256Upper
                  }
                }

                Text(
                  "Certificate ${page + 1} of ${signatures.size}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = 16.dp),
                )
                TextButton(
                  onClick = {
                    if (isAllUpper) {
                      isMd5Upper = false
                      isSha1Upper = false
                      isSha256Upper = false
                    } else {
                      isMd5Upper = true
                      isSha1Upper = true
                      isSha256Upper = true
                    }
                  },
                  modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(if (isAllUpper) "Lowercase all" else "Uppercase all") }
                TextButton(
                  onClick = { isShowShareContentDialog = true },
                  modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Share") }

                if (isShowShareContentDialog) {
                  val content = remember(md5, sha1, sha256, modulusHex, modulusString) {
                    """
                      |package: ${appInfo.packageName}
                      |name: ${appInfo.name}
                      |version name: ${appInfo.versionName}
                      |version code: ${appInfo.versionCode}
                      |
                      |MD5:
                      |$md5
                      |
                      |SHA1:
                      |$sha1
                      |
                      |SHA256:
                      |$sha256
                      |
                      |Public Key (16):
                      |$modulusHex
                      |
                      |Public Key:
                      |$modulusString
                    """.trimMargin()
                  }
                  if (content.isNotEmpty()) {
                    ShareContentPreviewDialog(
                      onDismissRequest = { isShowShareContentDialog = false },
                      onShareClick = {
                        isShowShareContentDialog = false
                        onShareContentClick(content)
                      },
                      content = content,
                    )
                  }
                }
              }
            }
            item {
              HexText(
                title = "SHA-256",
                text = sha256,
                onCopyContentClick = {
                  onCopyContentClick(sha256, "sha256")
                  scope.launch { snackbarHostState.showSnackbar("sha256 copied") }
                },
                isShowToggleUpperOrLowCase = true,
                isUpperCase = isSha256Upper,
                onToggleUpperOrLowCaseClick = { isSha256Upper = !isSha256Upper },
                isShowColonButton = true,
                isColonSplit = isSha256ColonSplit,
                onToggleColonSplitClick = { isSha256ColonSplit = !isSha256ColonSplit },
              )
            }
            item {
              HexText(
                title = "MD5",
                text = md5,
                onCopyContentClick = {
                  onCopyContentClick(md5, "md5")
                  scope.launch {
                    snackbarHostState.showSnackbar("md5 copied")
                  }
                },
                isShowToggleUpperOrLowCase = true,
                isUpperCase = isMd5Upper,
                onToggleUpperOrLowCaseClick = {
                  isMd5Upper = !isMd5Upper
                },
                isShowColonButton = true,
                isColonSplit = isMd5ColonSplit,
                onToggleColonSplitClick = {
                  isMd5ColonSplit = !isMd5ColonSplit
                },
              )
            }
            item {
              HexText(
                title = "SHA1",
                text = sha1,
                onCopyContentClick = {
                  onCopyContentClick(sha1, "sha1")
                  scope.launch {
                    snackbarHostState.showSnackbar("sha1 copied")
                  }
                },
                isShowToggleUpperOrLowCase = true,
                isUpperCase = isSha1Upper,
                onToggleUpperOrLowCaseClick = {
                  isSha1Upper = !isSha1Upper
                },
                isShowColonButton = true,
                isColonSplit = isSha1ColonSplit,
                onToggleColonSplitClick = {
                  isSha1ColonSplit = !isSha1ColonSplit
                },
              )
            }
            item {
              HexText(
                title = "Public Key (16)",
                text = modulusHex,
                onCopyContentClick = {
                  onCopyContentClick(modulusHex, "public key (16)")
                  scope.launch {
                    snackbarHostState.showSnackbar("public key (16) copied")
                  }
                },
              )
            }
            item {
              HexText(
                title = "Public Key",
                text = modulusString,
                onCopyContentClick = {
                  onCopyContentClick(modulusString, "public key")
                  scope.launch {
                    snackbarHostState.showSnackbar("public key copied")
                  }
                },
              )
            }
            item {
              Spacer(Modifier.height(32.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SignatureIdentity(appInfo: UiAppInfo) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    appInfo.icon?.let { Image(it, null, Modifier.size(64.dp)) }
      ?: Icon(Icons.Default.Key, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
    Text(appInfo.name, style = MaterialTheme.typography.headlineSmall)
    Text(appInfo.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      "Version ${appInfo.versionName} · ${appInfo.versionCode}",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
expect fun ExtractSignatureInfo(
  signSource: SignSource,
  content: @Composable ExtractSignatureInfoScope.() -> Unit,
)

interface ExtractSignatureInfoScope {
  val appInfo: UiAppInfo
  val signatures: List<SignatureDetailCertificate>
}
