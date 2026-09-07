package signature

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Deterministic regression tests for the monitor LIFECYCLE ENGINE
 * ([MonitorScanEngine]) - the real serialization/collection/commit code, not
 * just the pure differ:
 *
 * - scans are rejected while the monitor is disabled (nothing collected,
 *   nothing stored);
 * - persistence failures propagate and schedule/notify never run;
 * - package-enumeration failures preserve the stored state;
 * - overlapping scans serialize and never double-commit or double-notify;
 * - a cancelled in-flight scan commits nothing and leaves the engine usable;
 * - a queued scan after disable is a no-op (no state resurrection);
 * - a commit-time revalidation against a disabled state discards the scan.
 */
class MonitorScanEngineTest {

  private class FakeStorage(
    initialState: MonitorState = MonitorState(),
  ) : MonitorStateStorage {
    var stored: MonitorState = initialState
    var failAllTransactions: Boolean = false
    var deleted = false
    val commitLog = mutableListOf<MonitorState>()

    override suspend fun load(): MonitorState = stored

    override suspend fun <T> transact(block: (MonitorState) -> Pair<MonitorState, T>): T {
      if (failAllTransactions) {
        throw FakeStorageException("disk full")
      }
      val (next, value) = block(stored)
      stored = next
      commitLog += next
      return value
    }

    override suspend fun deleteAll() {
      deleted = true
      stored = MonitorState()
    }
  }

  private class FakeStorageException(message: String) : Exception(message)

  private fun enabledStorage(): FakeStorage {
    val storage = FakeStorage()
    storage.stored = MonitorState(enabled = true)
    return storage
  }

  private fun MonitorState.snapshotMapsAreEmpty(): Boolean = baseline.isNullOrEmpty() && previous.isEmpty() && known.isEmpty()

  private val appA = SignerSnapshot("app.a", setOf("AA1"), 1)

  @Test
  fun scanIsRejectedWhileDisabledNothingCollectedOrStored() = runBlocking {
    val storage = FakeStorage() // enabled = false
    val engine = MonitorScanEngine { storage }
    var collected = false
    val outcome = engine.scan(
      collect = {
        collected = true
        mapOf("app.a" to appA)
      },
      scannedAtMillis = 1000,
      allowNotifications = true,
      notifyNewAlerts = { fail("must not notify while disabled") },
    )
    assertEquals(MonitorScanOutcome.MonitorDisabled, outcome)
    assertFalse("disabled scan must not collect", collected)
    assertTrue("disabled scan must not commit", storage.commitLog.isEmpty())
  }

  @Test
  fun collectorFailurePreservesStoredStateAndReportsScanFailed() = runBlocking {
    val storage = enabledStorage()
    val baseline = applyMonitorScan(storage.stored, mapOf("app.a" to appA), 500).newState
    storage.stored = baseline
    val engine = MonitorScanEngine { storage }
    val outcome = engine.scan(
      collect = { throw FakeStorageException("PackageManager exploded") },
      scannedAtMillis = 1000,
      allowNotifications = true,
      notifyNewAlerts = { fail("enumeration failure must not notify") },
    )
    assertTrue(outcome is MonitorScanOutcome.ScanFailed)
    assertEquals("PackageManager exploded", (outcome as MonitorScanOutcome.ScanFailed).reason)
    assertEquals("stored state preserved", baseline, storage.stored)
    assertTrue(storage.commitLog.isEmpty())
  }

  @Test
  fun persistenceFailurePropagatesAndNothingIsScheduled() = runBlocking {
    val storage = FakeStorage() // disabled
    val engine = MonitorScanEngine { storage }
    storage.failAllTransactions = true
    var scheduled = false
    try {
      engine.enable { scheduled = true }
      fail("persistence failure must propagate")
    } catch (expected: FakeStorageException) {
      assertEquals("disk full", expected.message)
    }
    assertFalse("work must not be scheduled when persisting enable failed", scheduled)
    // State was never changed by the failed enable.
    assertEquals(false, storage.stored.enabled)

    // Disable with a failing store must not claim completion either.
    storage.failAllTransactions = false
    storage.stored = MonitorState(enabled = true)
    storage.failAllTransactions = true
    var cancelled = false
    try {
      engine.disable { cancelled = true }
      fail("persistence failure must propagate")
    } catch (expected: FakeStorageException) {
      assertEquals("disk full", expected.message)
    }
    assertFalse("work must not be cancelled when persisting disable failed", cancelled)
    assertEquals("failed disable must not change stored state", true, storage.stored.enabled)
  }

  @Test
  fun enablePersistsBeforeSchedulingAndDisablePersistsBeforeCancelling() = runBlocking {
    val storage = FakeStorage()
    val engine = MonitorScanEngine { storage }
    val operations = mutableListOf<String>()
    engine.enable {
      operations += "schedule"
      assertEquals(true, storage.stored.enabled)
    }
    operations += "enabledCommitted"
    engine.disable {
      operations += "cancel"
      assertEquals(false, storage.stored.enabled)
      assertTrue(
        "disable must empty stored state before cancelling",
        storage.stored.snapshotMapsAreEmpty(),
      )
    }
    operations += "deleted"
    assertTrue(storage.deleted)
    assertEquals(
      listOf("schedule", "enabledCommitted", "cancel", "deleted"),
      operations,
    )
  }

  @Test
  fun overlappingScansSerializeWithoutDuplicateCommitsOrNotifications() = runBlocking {
    val storage = enabledStorage()
    val engine = MonitorScanEngine { storage }
    val started = AtomicInteger(0)
    val order = mutableListOf<Int>()
    val gate = CompletableDeferred<Unit>()

    suspend fun collect(): Map<String, SignerSnapshot> {
      val index = started.incrementAndGet()
      synchronized(order) { order += index }
      if (index == 1) {
        gate.await() // hold the first scan mid-collection
      }
      return if (index == 1) {
        mapOf("app.a" to appA)
      } else {
        // The second scan observes a rotated signer set.
        mapOf("app.a" to appA.copy(signerSha256 = setOf("ZZ1"), versionCode = 2))
      }
    }

    val notified = mutableListOf<PackageAlert>()
    val job1 = launch { engine.scan(::collect, 1000, true) { notified += it } }
    yield() // let scan 1 start and block inside collect
    val job2 = launch { engine.scan(::collect, 2000, true) { notified += it } }
    yield() // let scan 2 queue on the engine mutex (it must NOT collect yet)
    assertEquals("second scan must wait for the first", 1, started.get())
    gate.complete(Unit)
    job1.join()
    job2.join()
    assertEquals("collections serialized", listOf(1, 2), order)
    // The second scan sees the first scan's committed state, so the baseline
    // is created exactly once and the change alerts exactly once.
    assertTrue(storage.stored.baseline != null)
    assertEquals("only the second scan's change alerts", 1, notified.size)
    assertTrue(
      "fingerprint persisted for dedup",
      storage.stored.notified["app.a"]?.contains("SIGNATURE_CHANGED") == true,
    )
  }

  @Test
  fun cancelledInFlightScanCommitsNothingAndEngineStaysUsable() = runBlocking {
    val storage = enabledStorage()
    val engine = MonitorScanEngine { storage }
    val gate = CompletableDeferred<Unit>()
    val job = launch {
      try {
        engine.scan(
          collect = {
            gate.await()
            emptyMap<String, SignerSnapshot>()
          },
          scannedAtMillis = 1000,
          allowNotifications = true,
          notifyNewAlerts = { fail("cancelled scan must not notify") },
        )
        fail("scan must surface cancellation")
      } catch (expected: CancellationException) {
        // expected: cancellation must surface, never become a result
      }
    }
    yield()
    job.cancel()
    gate.complete(Unit)
    job.join()
    assertEquals("nothing committed", true, storage.stored.enabled)
    assertTrue(storage.commitLog.isEmpty())

    // The engine still works for a fresh scan afterwards.
    val outcome = engine.scan(
      collect = { mapOf("app.a" to appA) },
      scannedAtMillis = 2000,
      allowNotifications = false,
      notifyNewAlerts = { fail("must not notify") },
    )
    assertTrue(outcome is MonitorScanOutcome.BaselineCreated)
  }

  @Test
  fun queuedScanAfterDisableIsANoOp() = runBlocking {
    val storage = enabledStorage()
    val engine = MonitorScanEngine { storage }
    val gate = CompletableDeferred<Unit>()
    var queuedScanCollected = false

    val scan1 = async {
      engine.scan(
        collect = {
          gate.await()
          mapOf("app.a" to appA)
        },
        scannedAtMillis = 1000,
        allowNotifications = false,
        notifyNewAlerts = {},
      )
    }
    yield()
    // Disable queues while scan 1 is in flight; scan 2 queues behind it.
    val disableJob = launch { engine.disable { } }
    yield()
    val scan2 = async {
      engine.scan(
        collect = {
          queuedScanCollected = true
          mapOf("app.a" to appA)
        },
        scannedAtMillis = 3000,
        allowNotifications = false,
        notifyNewAlerts = {},
      )
    }
    yield()
    gate.complete(Unit)
    val scan1Outcome = scan1.await()
    disableJob.join()
    val scan2Outcome = scan2.await()

    assertTrue(
      "scan 1 completed while still enabled",
      scan1Outcome is MonitorScanOutcome.BaselineCreated,
    )
    assertFalse("scan after disable must not collect", queuedScanCollected)
    assertTrue(scan2Outcome is MonitorScanOutcome.MonitorDisabled)
    assertFalse("disable must invalidate queued scans", storage.stored.enabled)
    assertTrue(storage.deleted)
  }

  @Test
  fun scanCommitRevalidatesAgainstDisabledState() = runBlocking {
    // If the committed state is disabled when the scan tries to commit (e.g.
    // the store was changed by a writer outside the engine), the scan must
    // discard its result instead of resurrecting the enabled flag.
    val storage = enabledStorage()
    val engine = MonitorScanEngine { storage }
    val gate = CompletableDeferred<Unit>()
    val outcomeDeferred = CompletableDeferred<MonitorScanOutcome>()
    val job = launch {
      val outcome = engine.scan(
        collect = {
          gate.await()
          mapOf("app.a" to appA)
        },
        scannedAtMillis = 1000,
        allowNotifications = true,
        notifyNewAlerts = { fail("must not notify a discarded scan") },
      )
      outcomeDeferred.complete(outcome)
    }
    yield()
    // Simulate an external writer flipping the monitor off mid-scan.
    storage.stored = storage.stored.copy(enabled = false)
    gate.complete(Unit)
    job.join()
    assertEquals(
      MonitorScanOutcome.MonitorDisabled,
      outcomeDeferred.await(),
    )
    assertFalse(storage.stored.enabled)
    assertTrue(storage.stored.snapshotMapsAreEmpty())
  }
}
