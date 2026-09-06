package data.model

import android.content.pm.Signature

fun AppSignature.Companion.from(signature: Signature): AppSignature = AndroidAppSignature(
  signature = signature,
)

private class AndroidAppSignature(
  private val signature: Signature,
) : AppSignature {
  override val byteArray: ByteArray by lazy {
    signature.toByteArray()
  }
}
