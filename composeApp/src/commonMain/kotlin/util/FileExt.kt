package util

import java.io.File
import java.security.cert.Certificate
import java.util.jar.JarFile

fun File.getApkSignatures(): Array<Certificate> {
  val jarFile = JarFile(this)
  return jarFile.getJarEntry("AndroidManifest.xml")?.let { manifest ->
    jarFile.getInputStream(manifest).use {
      manifest.certificates
    }
  } ?: emptyArray()
}
