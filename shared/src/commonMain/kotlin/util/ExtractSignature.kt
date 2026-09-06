package util

import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.openZip
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.io.path.toPath

fun extractSignature(path: String): ByteArray {
  val path = URI.create(path).toPath().toOkioPath()
  FileSystem.SYSTEM.openZip(path)
    .use { zipFile ->
      val entry = zipFile.listRecursively("/".toPath())
        .firstOrNull { it.name == "META-INF/CERT.RSA" }

      if (entry != null) {
        zipFile.read(entry) {
          val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(inputStream())
            as X509Certificate
          return certificate.signature
        }
      }
    }
  throw IllegalArgumentException("No signature found in $path")
}
