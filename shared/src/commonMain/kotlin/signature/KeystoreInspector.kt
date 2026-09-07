package signature

import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.Provider
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * Read-only inspection of keystore FILES for M4.
 *
 * - Supports the two formats Android users actually meet: JKS (the classic
 *   `keytool` format) and PKCS12. Note that ".keystore" is only a file
 *   extension, not a format; the real format is detected from the file magic.
 * - Android's built-in security providers have no JKS implementation, so on
 *   Android a vetted FOSS provider (BouncyCastle) is injected via [provider].
 *   Passing `null` uses the JVM default provider resolution (desktop/tests).
 * - Passwords are ephemeral caller-supplied char arrays; nothing is persisted
 *   or logged here.
 * - Private keys are NEVER accessed or exported: only
 *   [KeyStore.getCertificate] / [KeyStore.getCertificateChain] are read.
 */
object KeystoreInspector {

  /** Keystores are tiny; anything beyond this bound is rejected before work. */
  const val MAX_FILE_BYTES: Int = 32 * 1024 * 1024

  /** Parse a keystore file held fully in memory.
   *
   * [providerFor] resolves the JCA provider per detected format. Pass `null`
   * to use the JVM's default provider resolution (desktop JVMs ship JKS);
   * Android callers must pass [KeystoreCrypto.providerFor] because Android
   * ships no JKS provider.
   */
  fun parse(
    bytes: ByteArray,
    password: CharArray,
    providerFor: ((KeystoreFileFormat) -> Provider)? = null,
  ): KeystoreInspection {
    if (bytes.isEmpty()) {
      return KeystoreInspection.Failure(KeystoreError.EmptyFile)
    }
    if (bytes.size > MAX_FILE_BYTES) {
      return KeystoreInspection.Failure(KeystoreError.TooLarge(MAX_FILE_BYTES.toLong()))
    }
    val format = detectFormat(bytes)
      ?: return KeystoreInspection.Failure(KeystoreError.UnsupportedFormat)

    val keyStore = if (providerFor != null) {
      KeyStore.getInstance(format.storeType, providerFor(format))
    } else {
      KeyStore.getInstance(format.storeType)
    }
    try {
      ByteArrayInputStream(bytes).use { input ->
        keyStore.load(input, password)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return KeystoreInspection.Failure(classifyLoadFailure(e))
    }

    val aliases = keyStore.aliases().toList()
    if (aliases.isEmpty()) {
      return KeystoreInspection.Failure(KeystoreError.EmptyKeystore)
    }

    val entries = aliases.map { alias ->
      val chain: List<Certificate> = keyStore.getCertificateChain(alias)?.toList()
        ?: keyStore.getCertificate(alias)?.let { listOf(it) }
        ?: emptyList()
      KeystoreAliasInfo(
        alias = alias,
        isKeyEntry = keyStore.isKeyEntry(alias),
        certificates = chain.mapNotNull { it.toCertificateMeta() },
      )
    }
    return KeystoreInspection.Success(format, entries)
  }

  /**
   * ".keystore" is an extension, not a format: detect the real format from
   * magic bytes instead of the file name.
   *
   * PKCS12 is an ASN.1 SEQUENCE: tag byte 0x30 followed by a length byte.
   * Lengths may be short-form (0x00–0x7F), long-form DER (0x81–0xFF) or the
   * BER indefinite form (0x80) that BouncyCastle itself emits for PKCS12
   * stores. Any second byte is therefore a plausible length encoding here;
   * whether the structure really is a PKCS12 store is validated by the vetted
   * keystore provider during [parse], not by this cheap pre-check.
   */
  fun detectFormat(bytes: ByteArray): KeystoreFileFormat? {
    if (bytes.size < 4) {
      return null
    }
    val isJks = bytes[0] == 0xFE.toByte() && bytes[1] == 0xED.toByte() &&
      bytes[2] == 0xFE.toByte() && bytes[3] == 0xED.toByte()
    if (isJks) {
      return KeystoreFileFormat.JKS
    }
    if (bytes[0] == 0x30.toByte()) {
      return KeystoreFileFormat.PKCS12
    }
    return null
  }

  /**
   * JKS and PKCS12 both signal "wrong password" through integrity/MAC or
   * padding failures, which are hard to separate from actual corruption, so
   * both are reported honestly as one user-facing case.
   */
  private fun classifyLoadFailure(e: Exception): KeystoreError {
    val message = e.message.orEmpty().lowercase()
    if (message.contains("read-only") && message.contains("certificate entries")) {
      // BouncyCastle's JKS reader refuses stores with private key entries.
      return KeystoreError.JksKeyEntriesUnsupported
    }
    val passwordRelated = listOf(
      "password",
      "tampered",
      "integrity check",
      "mac check",
      "padded",
      "bad decrypt",
    ).any { message.contains(it) }
    return if (passwordRelated) {
      KeystoreError.WrongPasswordOrCorrupt
    } else {
      KeystoreError.ParseFailed(e.message ?: e::class.simpleName ?: "unknown error")
    }
  }

  private fun Certificate.toCertificateMeta(): CertificateMeta? {
    val x509 = this as? X509Certificate ?: return null
    return CertificateMeta(
      subjectDn = x509.subjectX500Principal.name,
      issuerDn = x509.issuerX500Principal.name,
      notBeforeMillis = x509.notBefore.time,
      notAfterMillis = x509.notAfter.time,
      sha256Fingerprint = sha256UppercaseHex(x509.encoded),
    )
  }
}

enum class KeystoreFileFormat(val storeType: String) {
  JKS("JKS"),
  PKCS12("PKCS12"),
}

sealed interface KeystoreInspection {
  data class Success(
    val format: KeystoreFileFormat,
    val aliases: List<KeystoreAliasInfo>,
  ) : KeystoreInspection

  data class Failure(val error: KeystoreError) : KeystoreInspection
}

sealed interface KeystoreError {
  data object EmptyFile : KeystoreError
  data class TooLarge(val maxBytes: Long) : KeystoreError
  data object UnsupportedFormat : KeystoreError
  data object WrongPasswordOrCorrupt : KeystoreError
  data object EmptyKeystore : KeystoreError

  /** JKS file with private key entries: the BouncyCastle JKS reader used
   *  here reads certificate entries only (empirically verified for
   *  bcprov-jdk18on 1.82). */
  data object JksKeyEntriesUnsupported : KeystoreError

  data class ParseFailed(val message: String) : KeystoreError
}

data class KeystoreAliasInfo(
  val alias: String,
  val isKeyEntry: Boolean,
  val certificates: List<CertificateMeta>,
)

data class CertificateMeta(
  val subjectDn: String,
  val issuerDn: String,
  val notBeforeMillis: Long,
  val notAfterMillis: Long,
  val sha256Fingerprint: String,
)
