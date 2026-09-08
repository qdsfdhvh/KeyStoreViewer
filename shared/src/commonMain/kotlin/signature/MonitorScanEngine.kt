package signature

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialized lifecycle engine of the opt-in signature-change monitor.
 *
 * Every state change runs under one mutex, and every commit re-validates the
 * stored state, so:
 * - While the monitor is disabled, no scan collects or persists anything and
 *   manual scans are rejected ([MonitorScanOutcome.MonitorDisabled]).
 * - Disabling is durable before work cancellation/file deletion is attempted,
 *   and an in-flight scan can never resurrect state: a scan only commits if
 *   the committed state is still enabled. Cancellation of a scan aborts it
 *   before collection or commit.
 * - Overlapping scans are serialized: each observes the previous scan's
 *   committed state, so snapshots never overwrite each other and alert
 *   deduplication (via persisted fingerprints) prevents duplicate commits
 *   and duplicate notifications.
 * - Package-enumeration failures abort the scan BEFORE any commit; the
 *   previously stored state is preserved and reported as
 *   [MonitorScanOutcome.ScanFailed] (never as an empty scan).
 * - Persistence failures propagate to the caller: nothing reports success
 *   unless the state change was durably stored.
 * - [CancellationException] is always rethrown, never converted to a result.
 *
 * The engine is storage/scheduler agnostic; the Android controller injects
 * the real [MonitorStateStorage], WorkManager scheduling and notifications.
 */
class MonitorScanEngine(
  private val storageProvider: () -> MonitorStateStorage,
) {

  private val mutex = Mutex()
  private val storage: MonitorStateStorage get() = storageProvider()

  /**
   * Durably enable the monitor, then run [scheduleWork]. If persisting fails,
   * [scheduleWork] is never invoked and the failure propagates.
   */
  suspend fun enable(scheduleWork: suspend () -> Unit): Unit = mutex.withLock {
    storage.transact { current -> current.copy(enabled = true) to Unit }
    scheduleWork()
  }

  /**
   * Durably disable the monitor (an enabled=false, emptied state), then cancel
   * the background work and delete the state file. Because the persisted state
   * is disabled first, a failure of [cancelWork] or deletion can never re-enable
   * collection: any late scan observes the disabled state and becomes a no-op.
   */
  suspend fun disable(cancelWork: suspend () -> Unit): Unit = mutex.withLock {
    storage.transact { MonitorState.disabledEmpty() to Unit }
    cancelWork()
    storage.deleteAll()
  }

  /**
   * Run one scan. [collect] enumerates installed packages (it must throw on
   * failure rather than returning an empty map); [notifyNewAlerts] is invoked
   * for freshly raised alerts only when [allowNotifications] is true, after
   * the scan result was durably committed.
   */
  suspend fun scan(
    collect: suspend () -> Map<String, SignerSnapshot>,
    scannedAtMillis: Long,
    allowNotifications: Boolean,
    notifyNewAlerts: suspend (List<PackageAlert>) -> Unit,
  ): MonitorScanOutcome = mutex.withLock {
    currentCoroutineContext().ensureActive()
    val start = storage.load()
    if (!start.enabled) {
      // Off = nothing runs, nothing is stored.
      return@withLock MonitorScanOutcome.MonitorDisabled
    }
    currentCoroutineContext().ensureActive()
    val current = try {
      collect()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // Enumeration failure is NOT an empty scan: keep the stored state so a
      // transient failure cannot fabricate a mass uninstall / mass reinstall.
      return@withLock MonitorScanOutcome.ScanFailed(
        reason = e.message ?: e::class.simpleName ?: "installed packages could not be read",
      )
    }
    // Cancelled during collection: abort before any commit.
    currentCoroutineContext().ensureActive()
    val scanResult = applyMonitorScan(start, current, scannedAtMillis)
    val outcome = storage.transact { existing ->
      if (existing.enabled) {
        scanResult.newState to scanResult.outcome
      } else {
        // The monitor was disabled while this scan was in flight; never
        // resurrect snapshots or the enabled flag.
        existing to MonitorScanOutcome.MonitorDisabled
      }
    }
    val newAlerts = (outcome as? MonitorScanOutcome.Updated)?.newAlerts.orEmpty()
    if (allowNotifications && newAlerts.isNotEmpty()) {
      notifyNewAlerts(newAlerts)
    }
    outcome
  }
}
