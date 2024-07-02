package ui.screen

import android.content.pm.PackageInfo
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import ui.widget.AppListItem
import ui.widget.HexText
import ui.widget.MediumIcon
import ui.widget.icon.rememberIcALowerCase
import ui.widget.icon.rememberIcAUpperCase
import ui.widget.icon.rememberIcShare
import ui.widget.rememberAppName
import ui.widget.rememberPackageIcon
import util.copyContent
import util.createShareTempFile
import util.getPackageInfoCompat
import util.getSignatures
import util.shareFile
import util.versionCodeCompat
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey

data class SignatureDetailScreen(
  private val packageName: String,
) : Screen {
  @OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalStdlibApi::class,
  )
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    Scaffold(
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(onClick = {
              navigator.pop()
            }) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "back",
              )
            }
          },
          title = {
            Text("详情")
          },
        )
      },
    ) { innerPadding ->

      val context = LocalContext.current
      val packageInfoNullable by produceState<PackageInfo?>(null) {
        value = withContext(Dispatchers.IO) {
          context.packageManager.getPackageInfoCompat(packageName)
        }
      }

      packageInfoNullable?.let { packageInfo ->
        Column(
          modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        ) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp),
          ) {
            AppListItem(
              leadingContent = {
                val icon by rememberPackageIcon(packageInfo, context)
                icon?.let {
                  Image(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                  )
                } ?: run {
                  Spacer(Modifier.size(40.dp))
                }
              },
              headlineContent = {
                Text(rememberAppName(packageInfo))
              },
              supportingContent = {
                Text(packageInfo.packageName)
              },
              trailingContent = {
                Column(Modifier) {
                  Text(
                    "version name:",
                    style = MaterialTheme.typography.labelSmall,
                  )
                  Text(
                    remember {
                      packageInfo.versionName ?: ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                  )
                  Spacer(Modifier.height(6.dp))
                  Text(
                    "version code:",
                    style = MaterialTheme.typography.labelSmall,
                  )
                  Text(
                    remember {
                      packageInfo.versionCodeCompat.toString()
                    },
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              },
              modifier = Modifier.padding(8.dp),
            )
          }

          val signatures by produceState(emptyArray()) {
            value = withContext(Dispatchers.IO) {
              context.getSignatures(packageName)
            }
          }

          val pagerState = rememberPagerState(0) { signatures.size }

          HorizontalPager(
            pagerState,
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
          ) { page ->
            val signature = signatures[page]

            val signatureByteString by produceState(ByteString.EMPTY) {
              value = withContext(Dispatchers.IO) {
                ByteString.of(*signature.toByteArray())
              }
            }

            var isMd5Upper by rememberSaveable { mutableStateOf(false) }
            var isMd5ColonSplit by rememberSaveable { mutableStateOf(true) }

            val md5 by remember {
              derivedStateOf {
                if (signatureByteString == ByteString.EMPTY) {
                  ""
                } else {
                  signatureByteString.md5().toByteArray().toHexString(
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
            }

            var isSha1Upper by rememberSaveable { mutableStateOf(false) }
            var isSha1ColonSplit by rememberSaveable { mutableStateOf(true) }
            val sha1 by remember {
              derivedStateOf {
                if (signatureByteString == ByteString.EMPTY) {
                  ""
                } else {
                  signatureByteString.sha1().toByteArray().toHexString(
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
            }

            var isSha256Upper by rememberSaveable { mutableStateOf(false) }
            var isSha256ColonSplit by rememberSaveable { mutableStateOf(true) }
            val sha256 by remember {
              derivedStateOf {
                if (signatureByteString == ByteString.EMPTY) {
                  ""
                } else {
                  signatureByteString.sha256().toByteArray().toHexString(
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
            }

            val modulus by produceState(BigInteger.ZERO) {
              snapshotFlow { signatureByteString }.collect {
                value = withContext(Dispatchers.IO) {
                  runCatching {
                    val certFactory = CertificateFactory.getInstance("X.509")
                    val cert =
                      certFactory.generateCertificate(ByteArrayInputStream(signatureByteString.toByteArray()))

                    when (val algorithm = cert.publicKey.algorithm) {
                      "RSA" -> (cert.publicKey as RSAPublicKey).modulus
                      else -> throw NotImplementedError("$algorithm public key not supported")
                    }
                  }.getOrElse {
                    Log.d("SignatureDetailScreen", "public key error:", it)
                    BigInteger.ZERO
                  }
                }
              }
            }

            val modulusHex by remember {
              derivedStateOf { modulus.toString(16) }
            }

            val modulusString by remember {
              derivedStateOf { modulus.toString() }
            }

            LazyColumn(
              contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              stickyHeader {
                Row(
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                ) {
                  var isShowShareContentDialog by remember { mutableStateOf(false) }
                  val isAllUpper by remember {
                    derivedStateOf {
                      isMd5Upper && isSha1Upper && isSha256Upper
                    }
                  }

                  Spacer(Modifier.weight(1f))
                  MediumIcon(
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
                    imageVector = if (isAllUpper) {
                      rememberIcAUpperCase()
                    } else {
                      rememberIcALowerCase()
                    },
                    contentDescription = "toggle all upper or lower case",
                    color = if (isAllUpper) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.secondaryContainer
                    },
                  )
                  MediumIcon(
                    onClick = { isShowShareContentDialog = true },
                    imageVector = rememberIcShare(),
                    contentDescription = "share",
                  )

                  if (isShowShareContentDialog) {
                    val content by produceState("") {
                      value = withContext(Dispatchers.IO) {
                        """
                        |package: $packageName
                        |name: ${packageInfo.applicationInfo.loadLabel(context.packageManager)}
                        |version name: ${packageInfo.versionName}
                        |version code: ${packageInfo.versionCodeCompat}
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
                    }
                    if (content.isNotEmpty()) {
                      ShareContentPreviewDialog(
                        onDismissRequest = { isShowShareContentDialog = false },
                        onShareClick = {
                          isShowShareContentDialog = false
                          val tempFile = context.createShareTempFile(content)
                          context.shareFile(tempFile)
                        },
                        content = content,
                      )
                    }
                  }
                }
              }
              item {
                HexText(
                  title = "MD5",
                  text = md5,
                  onCopyContentClick = {
                    context.copyContent(md5)
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
                    context.copyContent(sha1)
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
                  title = "SHA256",
                  text = sha256,
                  onCopyContentClick = {
                    context.copyContent(sha256)
                  },
                  isShowToggleUpperOrLowCase = true,
                  isUpperCase = isSha256Upper,
                  onToggleUpperOrLowCaseClick = {
                    isSha256Upper = !isSha256Upper
                  },
                  isShowColonButton = true,
                  isColonSplit = isSha256ColonSplit,
                  onToggleColonSplitClick = {
                    isSha256ColonSplit = !isSha256ColonSplit
                  },
                )
              }
              item {
                HexText(
                  title = "Public Key (16)",
                  text = modulusHex,
                  onCopyContentClick = {
                    context.copyContent(modulusHex)
                  },
                )
              }
              item {
                HexText(
                  title = "Public Key",
                  text = modulusString,
                  onCopyContentClick = {
                    context.copyContent(modulusString)
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
}

@OptIn(ExperimentalStdlibApi::class)
private val hexFormat = HexFormat {
  bytes.bytesPerGroup = 1
  bytes.groupSeparator = ":"
}

private class SignatureData(
  val md5: String,
  val sha1: String,
  val sha256: String,
  val rsaModuleHex: String,
  val rsaModule: String,
)
