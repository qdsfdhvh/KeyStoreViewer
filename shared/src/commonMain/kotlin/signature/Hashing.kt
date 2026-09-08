package signature

import java.security.MessageDigest

/**
 * M4 helpers for stable signer identifiers.
 *
 * Signer identity everywhere in M4 is the SHA-256 of the DER-encoded
 * certificate, uppercase hex without separators. The set is order-independent
 * (a [Set]), and an empty set always means "unknown / unreadable", never
 * "no signers" - two empty sets must never compare equal.
 */
fun sha256UppercaseHex(bytes: ByteArray): String {
  val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
  return buildString(digest.size * 2) {
    for (byte in digest) {
      val value = byte.toInt() and 0xFF
      append("0123456789ABCDEF"[value ushr 4])
      append("0123456789ABCDEF"[value and 0x0F])
    }
  }
}

/** Uppercase hex with colon separators, the conventional fingerprint display. */
fun colonSeparatedHex(hex: String): String = hex.chunked(2).joinToString(":")
