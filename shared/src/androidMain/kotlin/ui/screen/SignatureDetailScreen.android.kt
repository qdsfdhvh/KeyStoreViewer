package ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import data.model.AppSignature
import data.model.SignSource
import data.model.UiAppInfo
import data.model.from
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.toByteString
import util.getPackageArchiveInfoCompat
import util.getPackageInfoCompat
import util.signaturesCompat
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey

@Composable
actual fun ExtractSignatureInfo(
  signSource: SignSource,
  content: @Composable ExtractSignatureInfoScope.() -> Unit,
) {
  // The loader is an app-graph binding; the runtime SignSource rides through
  // the manual assisted factory. Scoped to this Nav3 entry's ViewModelStore.
  val viewModel = assistedMetroViewModel<SignatureDetailViewModel, SignatureDetailViewModel.Factory> {
    create(signSource)
  }
  val data by viewModel.data.collectAsState()
  data?.let {
    ExtractSignatureInfoScopeImpl(
      appInfo = it.appInfo,
      signatures = it.signatures,
    ).content()
  }
}

/** Loads package info, signatures, and certificate modulus for the detail screen. */
class PackageManagerSignatureDetailLoader(
  private val applicationContext: Context,
) : SignatureDetailLoader {

  override suspend fun load(signSource: SignSource): SignatureDetailData? = withContext(Dispatchers.IO) {
    val packageInfo = when (signSource) {
      is SignSource.PackageName -> applicationContext.packageManager.getPackageInfoCompat(signSource.packageName)
      is SignSource.Apk -> applicationContext.packageManager.getPackageArchiveInfoCompat(signSource.filePath)
    } ?: return@withContext null
    SignatureDetailData(
      appInfo = UiAppInfo.from(applicationContext, packageInfo),
      signatures = packageInfo.signaturesCompat.map { signature ->
        val bytes = AppSignature.from(signature).byteArray.toByteString()
        val modulus = certificateModulus(bytes)
        SignatureDetailCertificate(
          bytes = bytes,
          modulusHex = modulus.toString(16),
          modulusString = modulus.toString(),
        )
      },
    )
  }
}

private fun certificateModulus(bytes: ByteString): BigInteger = runCatching {
  val cert = CertificateFactory.getInstance("X.509")
    .generateCertificate(ByteArrayInputStream(bytes.toByteArray()))
  when (val algorithm = cert.publicKey.algorithm) {
    "RSA" -> (cert.publicKey as RSAPublicKey).modulus
    else -> throw NotImplementedError("$algorithm public key not supported")
  }
}.getOrElse {
  BigInteger.ZERO
}

@Stable
private class ExtractSignatureInfoScopeImpl(
  override val appInfo: UiAppInfo,
  override val signatures: List<SignatureDetailCertificate>,
) : ExtractSignatureInfoScope
