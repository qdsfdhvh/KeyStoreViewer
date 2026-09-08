package signature

import kotlinx.serialization.Serializable

/**
 * Pure state + diff logic for the opt-in installed-app signature-change
 * monitor (M4). All Android I/O lives behind thin adapters; this file is
 * unit-testable.
 *
 * Empty [SignerSnapshot.signerSha256] always means "unknown / unreadable
 * signers" and is handled as its own status; it is never compared as if it
 * were a real (empty) signer set.
 */
@Serializable
data class SignerSnapshot(
  val packageName: String,
  val signerSha256: Set<String>,
  val versionCode: Long,
) {
  val signersKnown: Boolean get() = signerSha256.isNotEmpty()
}

enum class PackageSignerStatus {
  UNCHANGED,
  SIGNATURE_CHANGED,
  SIGNERS_UNKNOWN,
  REINSTALLED,
  NEW_INSTALL,
  UNINSTALLED,
}

@Serializable
data class PackageAlert(
  val packageName: String,
  val status: PackageSignerStatus,
  val oldSigners: Set<String> = emptySet(),
  val newSigners: Set<String> = emptySet(),
  val oldVersionCode: Long = 0,
  val newVersionCode: Long = 0,
)

@Serializable
data class MonitorState(
  /** Snapshot of the first completed scan; null until then. Immutable. */
  val baseline: Map<String, SignerSnapshot>? = null,
  /** Snapshot of the most recent scan. */
  val previous: Map<String, SignerSnapshot> = emptyMap(),
  /** Every package ever observed, including packages first seen AFTER the
   *  baseline and packages currently uninstalled (tombstones). Reinstall
   *  detection compares against the LAST-SEEN snapshot from this map, never
   *  against the original baseline. */
  val known: Map<String, SignerSnapshot> = emptyMap(),
  /** Alerts currently valid for display (from the last diff). */
  val lastAlerts: List<PackageAlert> = emptyList(),
  /** Dedup fingerprints of alerts already notified, packageName -> fingerprint. */
  val notified: Map<String, String> = emptyMap(),
  val enabled: Boolean = false,
  val lastScanAtMillis: Long? = null,
  val lastBaselineSize: Int = 0,
) {
  companion object {
    fun disabledEmpty() = MonitorState(enabled = false)
  }
}

sealed interface MonitorScanOutcome {
  /** First scan only records a baseline, quietly. */
  data class BaselineCreated(val scanned: Int) : MonitorScanOutcome

  data class Updated(
    /** Alerts raised for the first time this scan (deduplicated for notify). */
    val newAlerts: List<PackageAlert>,
    /** All alerts valid for display after this scan. */
    val visibleAlerts: List<PackageAlert>,
    val scanned: Int,
  ) : MonitorScanOutcome

  /** The monitor is disabled: nothing was collected and nothing was stored. */
  data object MonitorDisabled : MonitorScanOutcome

  /** Package enumeration failed. Distinct from an empty scan: the previously
   *  stored state was preserved and nothing was committed. */
  data class ScanFailed(val reason: String) : MonitorScanOutcome
}

/** How a reinstalled package's certificates compare to its last-seen set. */
enum class ReinstallSignerComparison {
  /** Both sets known and equal. */
  SAME,

  /** Both sets known and different. */
  DIFFERENT,

  /** At least one set unreadable: never claim "same" or "different". */
  COMPARISON_UNAVAILABLE,
}

fun reinstallSignerComparison(
  oldSigners: Set<String>,
  newSigners: Set<String>,
): ReinstallSignerComparison = when {
  oldSigners.isEmpty() || newSigners.isEmpty() -> ReinstallSignerComparison.COMPARISON_UNAVAILABLE
  oldSigners == newSigners -> ReinstallSignerComparison.SAME
  else -> ReinstallSignerComparison.DIFFERENT
}

data class MonitorScanResult(
  val newState: MonitorState,
  val outcome: MonitorScanOutcome,
)

/**
 * Apply one scan of [current] snapshots against [state].
 *
 * Rules:
 * - baseline == null: first scan -> quiet baseline, no alerts.
 * - present in previous + current: UNCHANGED (known == known, equal sets),
 *   SIGNATURE_CHANGED (known sets differ) or SIGNERS_UNKNOWN (either side
 *   unreadable - never compared as "changed"/"equal").
 * - present in current, absent in previous: REINSTALLED if the package was
 *   ever seen before (last-seen tombstone from [MonitorState.known], whether
 *   from the original baseline or a later scan), NEW_INSTALL if it was never
 *   seen (quiet). The reinstall alert compares the new signers against that
 *   last-seen snapshot, not against the original baseline, so a legitimate
 *   rotation followed by a reinstall is never misdescribed as changed.
 * - present in previous, absent in current: UNINSTALLED.
 * - A returned notification is deduplicated per package via a fingerprint of
 *   status + signer sets + versions, so the same situation is not re-notified
 *   on every scan, while a genuinely new situation alerts again.
 */
fun applyMonitorScan(
  state: MonitorState,
  current: Map<String, SignerSnapshot>,
  scannedAtMillis: Long,
): MonitorScanResult {
  val baseline = state.baseline
  if (baseline == null) {
    return MonitorScanResult(
      newState = state.copy(
        baseline = current,
        previous = current,
        known = current,
        lastAlerts = emptyList(),
        lastScanAtMillis = scannedAtMillis,
        lastBaselineSize = current.size,
      ),
      outcome = MonitorScanOutcome.BaselineCreated(current.size),
    )
  }

  // Last-seen snapshot per package (tombstones included). States persisted
  // before reinstall tracking existed have an empty `known`; reconstruct it
  // from the baseline and the previous scan so old files keep working.
  val lastSeen: Map<String, SignerSnapshot> = state.known.ifEmpty { baseline + state.previous }

  val alerts = mutableListOf<PackageAlert>()
  // Fingerprint dedup: a package that settles back to a normal tracked state
  // gets its entry cleared, so a genuinely new (or recurring) situation
  // alerts again instead of being suppressed forever.
  val notifiedNext = mutableMapOf<String, String>()
  notifiedNext.putAll(state.notified)
  for (packageName in (state.previous.keys + current.keys).sorted()) {
    val before = state.previous[packageName]
    val after = current[packageName]
    when {
      before != null && after != null -> {
        when {
          !before.signersKnown && !after.signersKnown -> {
            // Still unreadable; previously alerted or still unknown - no new alert.
          }

          before.signersKnown && !after.signersKnown -> alerts += PackageAlert(
            packageName = packageName,
            status = PackageSignerStatus.SIGNERS_UNKNOWN,
            oldSigners = before.signerSha256,
            newVersionCode = after.versionCode,
          )

          !before.signersKnown && after.signersKnown -> {
            // Recovered readability; treat as fresh comparison point, no alert.
            notifiedNext.remove(packageName)
          }

          before.signerSha256 == after.signerSha256 -> {
            // UNCHANGED: no alert; drop any stale dedup entry.
            notifiedNext.remove(packageName)
          }

          else -> alerts += PackageAlert(
            packageName = packageName,
            status = PackageSignerStatus.SIGNATURE_CHANGED,
            oldSigners = before.signerSha256,
            newSigners = after.signerSha256,
            oldVersionCode = before.versionCode,
            newVersionCode = after.versionCode,
          )
        }
      }

      before == null && after != null -> {
        val lastSeenSnapshot = lastSeen[packageName]
        if (lastSeenSnapshot != null) {
          alerts += PackageAlert(
            packageName = packageName,
            status = PackageSignerStatus.REINSTALLED,
            oldSigners = lastSeenSnapshot.signerSha256,
            newSigners = after.signerSha256,
            oldVersionCode = lastSeenSnapshot.versionCode,
            newVersionCode = after.versionCode,
          )
        } else {
          // NEW_INSTALL - quietly tracked, no alert.
          notifiedNext.remove(packageName)
        }
      }

      before != null && after == null -> alerts += PackageAlert(
        packageName = packageName,
        status = PackageSignerStatus.UNINSTALLED,
        oldSigners = before.signerSha256,
        oldVersionCode = before.versionCode,
      )

      else -> {
        // Absent from both scans: nothing to report (e.g. still uninstalled).
      }
    }
  }

  val newAlerts = alerts.filter { alert ->
    val fingerprint = alertFingerprint(alert)
    if (notifiedNext[alert.packageName] == fingerprint) {
      false
    } else {
      notifiedNext[alert.packageName] = fingerprint
      true
    }
  }

  return MonitorScanResult(
    newState = state.copy(
      previous = current,
      known = lastSeen + current,
      lastAlerts = alerts,
      notified = notifiedNext,
      lastScanAtMillis = scannedAtMillis,
    ),
    outcome = MonitorScanOutcome.Updated(
      newAlerts = newAlerts,
      visibleAlerts = alerts,
      scanned = current.size,
    ),
  )
}

/** Never equate two unknown (empty) signer sets: fingerprint includes the sets. */
internal fun alertFingerprint(alert: PackageAlert): String = buildString {
  append(alert.status.name)
  append("|old=")
  append(alert.oldSigners.sorted().joinToString(","))
  append("|new=")
  append(alert.newSigners.sorted().joinToString(","))
  append("|v=")
  append(alert.oldVersionCode)
  append("->")
  append(alert.newVersionCode)
}
