package signature

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.signaturesCompat
import util.versionCodeCompat

/**
 * Collects the current signer sets of all visible installed packages for the
 * M4 signature-change monitor.
 *
 * Packages whose signing information cannot be read get an EMPTY signer set,
 * which the diff layer treats as its own "unknown" status - never as "no
 * signers" and never compared against another unknown set.
 *
 * A failure of the package enumeration itself is NOT mapped to an empty map:
 * it is rethrown so the monitor engine can abort the scan while preserving
 * the stored state ([MonitorScanOutcome.ScanFailed]) instead of fabricating a
 * mass uninstall followed by a mass false reinstall.
 */
suspend fun collectInstalledSignerSnapshots(
  context: Context,
): Map<String, SignerSnapshot> = withContext(Dispatchers.IO) {
  val packageManager = context.packageManager
  val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    PackageManager.GET_SIGNING_CERTIFICATES
  } else {
    @Suppress("DEPRECATION")
    PackageManager.GET_SIGNATURES
  }
  val installed = packageManager.getInstalledPackages(flags)

  installed.associate { info ->
    val signers = info.signaturesCompat.mapTo(mutableSetOf()) {
      sha256UppercaseHex(it.toByteArray())
    }
    val snapshot = SignerSnapshot(
      packageName = info.packageName,
      signerSha256 = signers,
      versionCode = info.versionCodeCompat,
    )
    snapshot.packageName to snapshot
  }
}
