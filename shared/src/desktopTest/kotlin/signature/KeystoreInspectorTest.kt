package signature

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the M4 read-only keystore file parser, using SYNTHETIC fixtures
 * (throwaway keys generated with keytool, password "ksv-test-pass") checked
 * into desktopTest resources. No real signing material is used anywhere.
 *
 * Provider paths covered:
 * - null provider = JVM default resolution (desktop JVMs ship SUN JKS/PKCS12).
 * - [KeystoreCrypto.providerFor] = the BouncyCastle path used on Android.
 *
 * Empirically verified limitation of the Android path: BouncyCastle 1.82's
 * JKS reader is read-only and only supports certificate entries.
 */
class KeystoreInspectorTest {

  private fun resource(name: String): ByteArray = javaClass.classLoader
    ?.getResourceAsStream("signature/$name")
    ?.use { it.readBytes() }
    ?: error("Missing test resource signature/$name")

  private fun fixtureJks(): ByteArray = resource("fixture.jks")
  private fun fixtureP12(): ByteArray = resource("fixture.p12")
  private fun emptyJks(): ByteArray = resource("empty.jks")
  private fun certsOnlyJks(): ByteArray = resource("certsonly.jks")

  @Test
  fun parsesFullJksWithDefaultProvider() {
    val result = KeystoreInspector.parse(
      bytes = fixtureJks(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = null,
    )
    val success = result as KeystoreInspection.Success
    assertEquals(KeystoreFileFormat.JKS, success.format)
    // key1 (key entry), key2 (key entry), trusted1 (trusted certificate)
    assertEquals(setOf("key1", "key2", "trusted1"), success.aliases.map { it.alias }.toSet())

    val key1 = success.aliases.first { it.alias == "key1" }
    assertTrue(key1.isKeyEntry)
    assertTrue(key1.certificates.isNotEmpty())
    val cert1 = key1.certificates.first()
    assertTrue(cert1.subjectDn.contains("CN=KSV Synthetic One"))
    assertTrue(cert1.issuerDn.contains("CN=KSV Synthetic One"))
    assertTrue(cert1.notAfterMillis > cert1.notBeforeMillis)
    // Pin the fixture: SHA-256 as printed by keytool for key1.
    assertEquals(
      "8F5A79ED0FEE314FB2E1842FFC802ABC0EC9FEF1F2245CE1C01A9E3E644D60F4",
      cert1.sha256Fingerprint,
    )

    val trusted1 = success.aliases.first { it.alias == "trusted1" }
    assertFalse(trusted1.isKeyEntry)
    // Imported from key1, so it must carry the same certificate.
    assertEquals(
      key1.certificates.first().sha256Fingerprint,
      trusted1.certificates.first().sha256Fingerprint,
    )
  }

  @Test
  fun parsesPkcs12WithBouncyCastleProvider() {
    val result = KeystoreInspector.parse(
      bytes = fixtureP12(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    val success = result as KeystoreInspection.Success
    assertEquals(KeystoreFileFormat.PKCS12, success.format)
    val key1 = success.aliases.first { it.alias == "key1" }
    assertTrue(key1.isKeyEntry)
    assertTrue(key1.certificates.first().subjectDn.contains("CN=KSV Synthetic P12"))
    val p12Cert = key1.certificates.first()
    assertTrue(p12Cert.notAfterMillis > p12Cert.notBeforeMillis)
  }

  @Test
  fun parsesPkcs12WithDefaultProvider() {
    val result = KeystoreInspector.parse(
      bytes = fixtureP12(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = null,
    )
    val success = result as KeystoreInspection.Success
    assertEquals(KeystoreFileFormat.PKCS12, success.format)
    assertEquals(setOf("key1"), success.aliases.map { it.alias }.toSet())
  }

  @Test
  fun jksWithCertificateEntriesOpensOnAndroidPath() {
    // BouncyCastle reads JKS files that contain only certificate entries.
    val result = KeystoreInspector.parse(
      bytes = certsOnlyJks(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    val success = result as KeystoreInspection.Success
    assertEquals(KeystoreFileFormat.JKS, success.format)
    assertEquals(setOf("trusted1"), success.aliases.map { it.alias }.toSet())
    val trusted = success.aliases.single()
    assertFalse(trusted.isKeyEntry)
    assertTrue(trusted.certificates.first().subjectDn.contains("CN=T"))
  }

  @Test
  fun jksWithKeyEntriesReportsSpecificAndroidLimitation() {
    // Empirical BouncyCastle behaviour: stores with private key entries are
    // rejected read-only; the parser must surface this as a specific error.
    val result = KeystoreInspector.parse(
      bytes = fixtureJks(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    assertEquals(
      KeystoreError.JksKeyEntriesUnsupported,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun wrongPasswordOnJksIsReported() {
    val result = KeystoreInspector.parse(
      bytes = fixtureJks(),
      password = "not-the-password".toCharArray(),
      providerFor = null,
    )
    assertEquals(
      KeystoreError.WrongPasswordOrCorrupt,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun wrongPasswordOnJksCertificateStoreWithBouncyCastleIsReported() {
    val result = KeystoreInspector.parse(
      bytes = certsOnlyJks(),
      password = "not-the-password".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    assertEquals(
      KeystoreError.WrongPasswordOrCorrupt,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun wrongPasswordOnPkcs12IsReported() {
    val result = KeystoreInspector.parse(
      bytes = fixtureP12(),
      password = "not-the-password".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    assertEquals(
      KeystoreError.WrongPasswordOrCorrupt,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun emptyFileIsRejected() {
    val result = KeystoreInspector.parse(
      bytes = ByteArray(0),
      password = "x".toCharArray(),
      providerFor = null,
    )
    assertEquals(
      KeystoreError.EmptyFile,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun emptyJksKeystoreReportsNoEntries() {
    val result = KeystoreInspector.parse(
      bytes = emptyJks(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    assertEquals(
      KeystoreError.EmptyKeystore,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun garbageBytesAreUnsupported() {
    val garbage = "this is definitely not a keystore file at all".toByteArray()
    val result = KeystoreInspector.parse(
      bytes = garbage,
      password = "x".toCharArray(),
      providerFor = null,
    )
    assertEquals(
      KeystoreError.UnsupportedFormat,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun oversizedInputIsRejectedBeforeParsing() {
    val oversized = ByteArray(KeystoreInspector.MAX_FILE_BYTES + 1)
    val result = KeystoreInspector.parse(
      bytes = oversized,
      password = "x".toCharArray(),
      providerFor = null,
    )
    assertTrue((result as KeystoreInspection.Failure).error is KeystoreError.TooLarge)
  }

  @Test
  fun formatDetectionUsesMagicNotExtension() {
    // ".keystore" files may hold either format; detection must use the magic.
    assertEquals(KeystoreFileFormat.JKS, KeystoreInspector.detectFormat(fixtureJks()))
    assertEquals(KeystoreFileFormat.PKCS12, KeystoreInspector.detectFormat(fixtureP12()))
    // DER SEQUENCE header (0x30 0x82 ...) but bogus content: format detected,
    // parse fails downstream.
    assertEquals(
      KeystoreFileFormat.PKCS12,
      KeystoreInspector.detectFormat(
        byteArrayOf(0x30.toByte(), 0x82.toByte(), 0x01.toByte(), 0x02.toByte()),
      ),
    )
    assertEquals(null, KeystoreInspector.detectFormat(byteArrayOf(1, 2, 3, 4)))
    assertEquals(null, KeystoreInspector.detectFormat(byteArrayOf(0x30)))
  }

  /** Builds a BC PKCS12 keystore in memory (the exact producer of real
   *  user files under test) and returns the encoded bytes. */
  private fun bcPkcs12Bytes(
    configure: (java.security.KeyStore) -> Unit,
  ): ByteArray {
    val provider = BouncyCastleProvider()
    val keyStore = java.security.KeyStore.getInstance("PKCS12", provider)
    keyStore.load(null, "builder-pass".toCharArray())
    configure(keyStore)
    val output = java.io.ByteArrayOutputStream()
    keyStore.store(output, "builder-pass".toCharArray())
    return output.toByteArray()
  }

  @Test
  fun bcGeneratedEmptyBerPkcs12RoundTripsToEmptyKeystore() {
    // Supervisor repro (BcRoundTripProbe): BC itself emits BER indefinite-
    // length sequences (0x30 0x80 ...). A legal, BC-generated empty store
    // must be detected as PKCS12 and parse as EmptyKeystore - never as
    // UnsupportedFormat.
    val bytes = bcPkcs12Bytes { }
    assertEquals(0x30, bytes[0].toInt() and 0xFF)
    assertEquals(0x80, bytes[1].toInt() and 0xFF) // BER indefinite length
    assertEquals(KeystoreFileFormat.PKCS12, KeystoreInspector.detectFormat(bytes))
    val result = KeystoreInspector.parse(
      bytes = bytes,
      password = "builder-pass".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    assertEquals(
      KeystoreError.EmptyKeystore,
      (result as KeystoreInspection.Failure).error,
    )
  }

  @Test
  fun bcGeneratedPkcs12WithCertificateRoundTripsToSuccess() {
    val provider = BouncyCastleProvider()
    // Take a real certificate from the synthetic PKCS12 fixture...
    val source = java.security.KeyStore.getInstance("PKCS12", provider).apply {
      java.io.ByteArrayInputStream(fixtureP12()).use {
        load(it, "ksv-test-pass".toCharArray())
      }
    }
    val alias = source.aliases().nextElement()
    val certificate = source.getCertificate(alias)
    // ...and build a BC PKCS12 store containing it as a trusted entry.
    val bytes = bcPkcs12Bytes { builder -> builder.setCertificateEntry("synthetic-cert", certificate) }
    assertEquals(KeystoreFileFormat.PKCS12, KeystoreInspector.detectFormat(bytes))
    val result = KeystoreInspector.parse(
      bytes = bytes,
      password = "builder-pass".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    val success = result as KeystoreInspection.Success
    assertEquals(KeystoreFileFormat.PKCS12, success.format)
    val entry = success.aliases.single { it.alias == "synthetic-cert" }
    assertFalse(entry.isKeyEntry)
    assertEquals(
      sha256UppercaseHex(certificate.encoded),
      entry.certificates.single().sha256Fingerprint,
    )
  }

  @Test
  fun malformedBerSequenceFailsAtTheProviderNotAtDetection() {
    // Detection only checks the ASN.1 SEQUENCE tag; structure validation is
    // delegated to the vetted provider, so malformed 0x30 0x80 data must NOT
    // be reported as UnsupportedFormat.
    val malformed = byteArrayOf(
      0x30.toByte(),
      0x80.toByte(),
      0x02,
      0x01,
      0x7F,
      0x00,
    )
    assertEquals(KeystoreFileFormat.PKCS12, KeystoreInspector.detectFormat(malformed))
    val result = KeystoreInspector.parse(
      bytes = malformed,
      password = "whatever".toCharArray(),
      providerFor = KeystoreCrypto::providerFor,
    )
    val error = (result as KeystoreInspection.Failure).error
    assertTrue(
      "expected a provider-side parse error, got $error",
      error is KeystoreError.ParseFailed || error is KeystoreError.WrongPasswordOrCorrupt,
    )
  }

  @Test
  fun keyEntriesExposeCertificateChainOnly() {
    // The M4 contract: private keys are never read. The result model only
    // carries certificate metadata, so key entries must still surface their
    // public certificate chain and nothing else.
    val result = KeystoreInspector.parse(
      bytes = fixtureJks(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = null,
    )
    val success = result as KeystoreInspection.Success
    success.aliases.forEach { alias ->
      assertTrue(
        alias.certificates.isNotEmpty(),
      )
    }
    val key1 = success.aliases.first { it.alias == "key1" }
    assertTrue(key1.isKeyEntry)
    assertEquals(1, key1.certificates.size)
  }

  @Test
  fun bouncyCastleProviderInstanceIsUsableForPkcs12() {
    // Guards the exact mechanism used on Android: an unregistered provider
    // instance passed explicitly to KeyStore.getInstance.
    val provider: java.security.Provider = BouncyCastleProvider()
    val result = KeystoreInspector.parse(
      bytes = fixtureP12(),
      password = "ksv-test-pass".toCharArray(),
      providerFor = { format ->
        when (format) {
          KeystoreFileFormat.PKCS12 -> provider
          KeystoreFileFormat.JKS -> KeystoreCrypto.providerFor(format)
        }
      },
    )
    assertTrue(result is KeystoreInspection.Success)
  }
}
