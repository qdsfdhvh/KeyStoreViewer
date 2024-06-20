package ui.screen

import android.content.pm.PackageInfo
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import ui.widget.rememberPackageIcon
import ui.widget.rememberPackageName
import util.getAppInfo
import util.getSignatures
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey

class SignatureDetailScreen(
    private val packageName: String,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            }
        ) { innerPadding ->

            val context = LocalContext.current
            val packageInfoNullable by produceState<PackageInfo?>(null) {
                value = withContext(Dispatchers.IO) {
                    context.getAppInfo(packageName)
                }
            }

            packageInfoNullable?.let { packageInfo ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    Surface {
                        ListItem(
                            leadingContent = {
                                val icon by rememberPackageIcon(packageInfo, context)
                                icon?.let {
                                    Image(
                                        it,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } ?: run {
                                    Spacer(Modifier.size(40.dp))
                                }
                            },
                            headlineContent = {
                                Text(rememberPackageName(packageInfo))
                            },
                            supportingContent = {
                                Text(packageInfo.packageName)
                            },
                            trailingContent = {
                                Text(
                                    packageInfo.versionName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
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
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) { page ->
                        val signature = signatures[page]

                        val signatureDataNullable by produceState<SignatureData?>(null, signature) {
                            value = withContext(Dispatchers.IO) {
                                val bytes = signature.toByteArray()
                                val byteString = ByteString.of(*bytes)

                                val (rsaModuleHex, rsaModule) = runCatching {
                                    val certFactory = CertificateFactory.getInstance("X.509")
                                    val cert = certFactory.generateCertificate(ByteArrayInputStream(bytes))

                                    val modulus = when (val algorithm = cert.publicKey.algorithm) {
                                        "RSA" -> (cert.publicKey as RSAPublicKey).modulus
                                        else -> throw NotImplementedError("$algorithm public key not supported")
                                    }
                                    modulus.toString(16) to modulus.toString()
                                }.onFailure {
                                    Log.d("AAA", "public key error:", it)
                                }.getOrElse { "" to "" }

                                SignatureData(
                                    md5 = byteString.md5().hex(),
                                    sha1 = byteString.sha1().hex(),
                                    sha256 = byteString.sha256().hex(),
                                    rsaModuleHex = rsaModuleHex,
                                    rsaModule = rsaModule,
                                )
                            }
                        }

                        signatureDataNullable?.let { signatureData ->
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                item {
                                    Text("MD5")
                                }
                                item {
                                    Text(signatureData.md5)
                                }
                                item {
                                    Spacer(Modifier.height(32.dp))
                                }
                                item {
                                    Text("SHA1")
                                }
                                item {
                                    Text(signatureData.sha1)
                                }
                                item {
                                    Spacer(Modifier.height(32.dp))
                                }
                                item {
                                    Text("SHA256")
                                }
                                item {
                                    Text(signatureData.sha256)
                                }
                                item {
                                    Spacer(Modifier.height(32.dp))
                                }
                                item {
                                    Text("公钥 16进制")
                                }
                                item {
                                    Text(signatureData.rsaModuleHex)
                                }
                                item {
                                    Spacer(Modifier.height(32.dp))
                                }
                                item {
                                    Text("公钥 10进制")
                                }
                                item {
                                    Text(signatureData.rsaModule)
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
}

private class SignatureData(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val rsaModuleHex: String,
    val rsaModule: String,
)
