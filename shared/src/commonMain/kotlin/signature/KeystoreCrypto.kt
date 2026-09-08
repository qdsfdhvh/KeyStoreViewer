package signature

import org.bouncycastle.jcajce.provider.keystore.util.JKSKeyStoreSpi
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Provider

/**
 * Supplies vetted FOSS keystore providers for the M4 browser WITHOUT touching
 * the global [java.security.Security] registration (important on Android,
 * where a built-in provider named "BC" already exists).
 *
 * Verified against bcprov-jdk18on 1.82:
 * - PKCS12: fully supported via [BouncyCastleProvider].
 * - JKS: [JKSKeyStoreSpi] is READ-ONLY and only supports certificate entries;
 *   JKS files containing private key entries are rejected with a specific,
 *   user-facing error instead of a generic failure.
 */
object KeystoreCrypto {

  /** Local (never globally registered) provider resolution per format. */
  fun providerFor(format: KeystoreFileFormat): Provider = when (format) {
    KeystoreFileFormat.JKS -> BcJksProvider.INSTANCE
    KeystoreFileFormat.PKCS12 -> BouncyCastleHolder.INSTANCE
  }

  private object BouncyCastleHolder {
    val INSTANCE: Provider = BouncyCastleProvider()
  }

  /**
   * BouncyCastle has no public no-arg constructor path for "JKS" (and does
   * not register the type at all), so expose its SPI through a Service whose
   * [Service.newInstance] builds a fresh SPI backed by the BouncyCastle
   * provider instance.
   */
  private class BcJksProvider private constructor() :
    Provider(
      "KSV-BC-JKS",
      1.0,
      "Read-only BouncyCastle JKS keystore access (locally scoped, unregistered)",
    ) {
    init {
      val bc = BouncyCastleHolder.INSTANCE
      putService(
        object : Service(
          this,
          "KeyStore",
          "JKS",
          JKSKeyStoreSpi::class.java.name,
          null,
          null,
        ) {
          override fun newInstance(constructorParameter: Any?): Any = JKSKeyStoreSpi(ProviderJcaJceHelper(bc))
        },
      )
    }

    companion object {
      val INSTANCE: Provider = BcJksProvider()
    }
  }
}
