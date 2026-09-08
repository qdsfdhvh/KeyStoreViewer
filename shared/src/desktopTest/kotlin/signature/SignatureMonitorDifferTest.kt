package signature

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignerSetComparerTest {

  @Test
  fun identicalSetsCompareIdentical() {
    val a = setOf("AA", "BB")
    val b = setOf("BB", "AA")
    val comparison = compareSignerSets(a, b)
    assertEquals(SignerSetComparisonOutcome.IDENTICAL, comparison.outcome)
  }

  @Test
  fun orderDoesNotMatter() {
    // Same members in a different insertion order must still match: the
    // comparison is over sets, not lists.
    val a = buildSet {
      add("AA")
      add("BB")
      add("CC")
    }
    val b = linkedSetOf("CC", "AA", "BB")
    assertEquals(SignerSetComparisonOutcome.IDENTICAL, compareSignerSets(a, b).outcome)
  }

  @Test
  fun differentSetsReportOnlyInLeftAndRight() {
    val a = setOf("AA", "BB", "CC")
    val b = setOf("BB", "DD")
    val comparison = compareSignerSets(a, b)
    assertEquals(SignerSetComparisonOutcome.DIFFERENT, comparison.outcome)
    assertEquals(setOf("AA", "CC"), comparison.onlyInLeft)
    assertEquals(setOf("DD"), comparison.onlyInRight)
  }

  @Test
  fun twoEmptySetsAreNeverEqual() {
    val comparison = compareSignerSets(emptySet(), emptySet())
    assertEquals(SignerSetComparisonOutcome.BOTH_EMPTY, comparison.outcome)
  }

  @Test
  fun oneEmptySetIsAnErrorOnThatSide() {
    assertEquals(
      SignerSetComparisonOutcome.LEFT_EMPTY,
      compareSignerSets(emptySet(), setOf("AA")).outcome,
    )
    assertEquals(
      SignerSetComparisonOutcome.RIGHT_EMPTY,
      compareSignerSets(setOf("AA"), emptySet()).outcome,
    )
  }
}

class SignatureMonitorDifferTest {

  private val appA = SignerSnapshot("app.a", setOf("AA1", "AA2"), 1)
  private val appB = SignerSnapshot("app.b", setOf("BB1"), 2)

  @Test
  fun firstScanCreatesQuietBaseline() {
    val result = applyMonitorScan(
      state = MonitorState(),
      current = mapOf("app.a" to appA, "app.b" to appB),
      scannedAtMillis = 1000,
    )
    val outcome = result.outcome as MonitorScanOutcome.BaselineCreated
    assertEquals(2, outcome.scanned)
    assertEquals(mapOf("app.a" to appA, "app.b" to appB), result.newState.baseline)
    assertTrue(result.newState.lastAlerts.isEmpty())
  }

  @Test
  fun repeatedUnchangedScanRaisesNoAlert() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val result = applyMonitorScan(baseline, mapOf("app.a" to appA), 2000)
    val outcome = result.outcome as MonitorScanOutcome.Updated
    assertTrue(outcome.newAlerts.isEmpty())
    assertTrue(outcome.visibleAlerts.isEmpty())
  }

  @Test
  fun versionOnlyUpdateIsUnchanged() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val updated = appA.copy(versionCode = 2)
    val result = applyMonitorScan(baseline, mapOf("app.a" to updated), 2000)
    val outcome = result.outcome as MonitorScanOutcome.Updated
    assertTrue("version bump without signer change must not alert", outcome.newAlerts.isEmpty())
  }

  @Test
  fun signatureChangeAlertsOnceWithDetails() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val rotated = appA.copy(signerSha256 = setOf("ZZ9"), versionCode = 3)
    val changed = applyMonitorScan(baseline, mapOf("app.a" to rotated), 2000)
    val outcome = changed.outcome as MonitorScanOutcome.Updated
    assertEquals(1, outcome.newAlerts.size)
    val alert = outcome.newAlerts.single()
    assertEquals(PackageSignerStatus.SIGNATURE_CHANGED, alert.status)
    assertEquals(setOf("AA1", "AA2"), alert.oldSigners)
    assertEquals(setOf("ZZ9"), alert.newSigners)

    // Same situation next scan: no new alerts, and nothing to re-notify.
    val repeated = applyMonitorScan(changed.newState, mapOf("app.a" to rotated), 3000)
    val repeatedOutcome = repeated.outcome as MonitorScanOutcome.Updated
    assertTrue(repeatedOutcome.newAlerts.isEmpty())
  }

  @Test
  fun unknownSignersAreDistinctFromChange() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    // Unknown signers (empty set) must never count as "no signers" or equal
    // another unknown set.
    val unknown = appA.copy(signerSha256 = emptySet())
    val first = applyMonitorScan(baseline, mapOf("app.a" to unknown), 2000)
    val firstOutcome = first.outcome as MonitorScanOutcome.Updated
    assertEquals(PackageSignerStatus.SIGNERS_UNKNOWN, firstOutcome.newAlerts.single().status)

    // Still unknown: no new alert.
    val still = applyMonitorScan(first.newState, mapOf("app.a" to unknown), 3000)
    assertTrue((still.outcome as MonitorScanOutcome.Updated).newAlerts.isEmpty())

    // Recovered readability: quiet, then a later regression alerts again.
    val recovered = applyMonitorScan(still.newState, mapOf("app.a" to appA), 4000)
    assertTrue((recovered.outcome as MonitorScanOutcome.Updated).newAlerts.isEmpty())
    val regressed = applyMonitorScan(recovered.newState, mapOf("app.a" to unknown), 5000)
    assertEquals(
      PackageSignerStatus.SIGNERS_UNKNOWN,
      (regressed.outcome as MonitorScanOutcome.Updated).newAlerts.single().status,
    )
  }

  @Test
  fun uninstallIsReportedExplicitly() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val result = applyMonitorScan(baseline, emptyMap(), 2000)
    val outcome = result.outcome as MonitorScanOutcome.Updated
    val alert = outcome.newAlerts.single()
    assertEquals(PackageSignerStatus.UNINSTALLED, alert.status)
    assertEquals("app.a", alert.packageName)
  }

  @Test
  fun reinstalledAppIsReportedIncludingSignatureChange() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val uninstalled = applyMonitorScan(baseline, emptyMap(), 2000).newState

    // Comes back with the SAME signer set.
    val reinstall = applyMonitorScan(
      uninstalled,
      mapOf("app.a" to appA.copy(versionCode = 5)),
      3000,
    )
    val reinstallAlert = (reinstall.outcome as MonitorScanOutcome.Updated).newAlerts.single()
    assertEquals(PackageSignerStatus.REINSTALLED, reinstallAlert.status)
    assertEquals(setOf("AA1", "AA2"), reinstallAlert.newSigners)

    // Uninstalled again, then comes back with a DIFFERENT signer set.
    val uninstalledAgain = applyMonitorScan(
      reinstall.newState,
      emptyMap(),
      4000,
    )
    assertEquals(
      PackageSignerStatus.UNINSTALLED,
      (uninstalledAgain.outcome as MonitorScanOutcome.Updated).newAlerts.single().status,
    )
    val badReinstall = applyMonitorScan(
      uninstalledAgain.newState,
      mapOf("app.a" to appA.copy(signerSha256 = setOf("EVIL1"), versionCode = 6)),
      5000,
    )
    val badAlert = (badReinstall.outcome as MonitorScanOutcome.Updated).newAlerts.single()
    assertEquals(PackageSignerStatus.REINSTALLED, badAlert.status)
    assertEquals(setOf("EVIL1"), badAlert.newSigners)
  }

  @Test
  fun reinstalledAppSeenOnlyAfterBaselineIsStillReported() {
    // app.b is installed AFTER the baseline. It must still be tracked as
    // known, so uninstalling and reinstalling it is a REINSTALLED alert (not
    // a quiet NEW_INSTALL), compared against its last-seen signers.
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val seenNew = applyMonitorScan(
      baseline,
      mapOf("app.a" to appA, "app.b" to appB),
      2000,
    )
    assertTrue((seenNew.outcome as MonitorScanOutcome.Updated).newAlerts.isEmpty())

    val uninstalled = applyMonitorScan(seenNew.newState, mapOf("app.a" to appA), 3000)
    assertEquals(
      PackageSignerStatus.UNINSTALLED,
      (uninstalled.outcome as MonitorScanOutcome.Updated).newAlerts.single().status,
    )

    val reinstalledEvil = applyMonitorScan(
      uninstalled.newState,
      mapOf("app.a" to appA, "app.b" to appB.copy(signerSha256 = setOf("EVIL"), versionCode = 9)),
      4000,
    )
    val alert = (reinstalledEvil.outcome as MonitorScanOutcome.Updated).newAlerts.single()
    assertEquals("app.b", alert.packageName)
    assertEquals(PackageSignerStatus.REINSTALLED, alert.status)
    assertEquals("compared against last-seen app.b, not the baseline", setOf("BB1"), alert.oldSigners)
    assertEquals(setOf("EVIL"), alert.newSigners)
  }

  @Test
  fun reinstallAfterRotationComparesAgainstLastSeenNotOriginalBaseline() {
    // Baseline signers A -> legitimate rotation to B -> uninstall -> reinstall
    // with B. The comparison must use the last-seen snapshot (B), so this is
    // "reinstalled, same certificates" - never "reinstalled with different
    // certificates" against the stale original baseline.
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val rotated = appA.copy(signerSha256 = setOf("BB1"), versionCode = 2)
    val changed = applyMonitorScan(baseline, mapOf("app.a" to rotated), 2000)
    assertEquals(
      PackageSignerStatus.SIGNATURE_CHANGED,
      (changed.outcome as MonitorScanOutcome.Updated).newAlerts.single().status,
    )
    val uninstalled = applyMonitorScan(changed.newState, emptyMap(), 3000).newState
    val reinstalled = applyMonitorScan(
      uninstalled,
      mapOf("app.a" to rotated.copy(versionCode = 3)),
      4000,
    )
    val alert = (reinstalled.outcome as MonitorScanOutcome.Updated).newAlerts.single()
    assertEquals(PackageSignerStatus.REINSTALLED, alert.status)
    assertEquals("last-seen rotated set, not the original baseline", setOf("BB1"), alert.oldSigners)
    assertEquals(setOf("BB1"), alert.newSigners)
  }

  @Test
  fun reinstallTrackingSurvivesLegacyStateWithoutKnownMap() {
    // States persisted before the `known` map existed decode with an empty
    // `known`; reinstall detection must fall back to baseline + previous.
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val legacyUninstalled = MonitorState(
      baseline = baseline.baseline,
      previous = emptyMap(),
      lastAlerts = listOf(
        PackageAlert("app.a", PackageSignerStatus.UNINSTALLED, oldSigners = appA.signerSha256),
      ),
      enabled = true,
    )
    val result = applyMonitorScan(
      legacyUninstalled,
      mapOf("app.a" to appA.copy(versionCode = 4)),
      2000,
    )
    val alert = (result.outcome as MonitorScanOutcome.Updated).newAlerts.single()
    assertEquals(PackageSignerStatus.REINSTALLED, alert.status)
    assertEquals(setOf("AA1", "AA2"), alert.oldSigners)
  }

  @Test
  fun reinstallSignerComparisonDistinguishesUnknownFromEqual() {
    val signers = setOf("AA1")
    assertEquals(
      ReinstallSignerComparison.SAME,
      reinstallSignerComparison(signers, setOf("AA1")),
    )
    assertEquals(
      ReinstallSignerComparison.DIFFERENT,
      reinstallSignerComparison(signers, setOf("BB1")),
    )
    // Any empty (unreadable) set must render as unavailable, never "same".
    assertEquals(
      ReinstallSignerComparison.COMPARISON_UNAVAILABLE,
      reinstallSignerComparison(emptySet(), signers),
    )
    assertEquals(
      ReinstallSignerComparison.COMPARISON_UNAVAILABLE,
      reinstallSignerComparison(signers, emptySet()),
    )
    assertEquals(
      ReinstallSignerComparison.COMPARISON_UNAVAILABLE,
      reinstallSignerComparison(emptySet(), emptySet()),
    )
  }

  @Test
  fun newlyInstalledAppsAreQuiet() {
    val baseline = applyMonitorScan(MonitorState(), mapOf("app.a" to appA), 1000).newState
    val newApp = SignerSnapshot("app.new", setOf("NN1"), 1)
    val result = applyMonitorScan(baseline, mapOf("app.a" to appA, "app.new" to newApp), 2000)
    val outcome = result.outcome as MonitorScanOutcome.Updated
    assertTrue("first sight of a new app must not alert", outcome.newAlerts.isEmpty())
    // It is now tracked, so the next scan sees it unchanged.
    val next = applyMonitorScan(result.newState, mapOf("app.a" to appA, "app.new" to newApp), 3000)
    assertTrue((next.outcome as MonitorScanOutcome.Updated).newAlerts.isEmpty())
  }

  @Test
  fun monitorStateSurvivesJsonRoundTrip() {
    val baseline = applyMonitorScan(
      MonitorState(enabled = true),
      mapOf("app.a" to appA),
      1000,
    ).newState
    val rotated = appA.copy(signerSha256 = setOf("ZZ9"), versionCode = 3)
    val changed = applyMonitorScan(baseline, mapOf("app.a" to rotated), 2000).newState

    val json = Json { encodeDefaults = true }
    val encoded = json.encodeToString(MonitorState.serializer(), changed)
    val decoded = json.decodeFromString(MonitorState.serializer(), encoded)

    assertEquals(changed, decoded)
    assertNotNull(decoded.baseline)
  }
}
