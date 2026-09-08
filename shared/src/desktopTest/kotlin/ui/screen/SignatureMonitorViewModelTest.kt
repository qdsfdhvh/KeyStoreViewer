package ui.screen

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import signature.MonitorScanOutcome
import signature.MonitorState
import signature.PackageAlert
import signature.PackageSignerStatus

class SignatureMonitorViewModelTest {

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private class FakeActions : SignatureMonitorActions {
    var storedState = MonitorState()
    var loadGate: CompletableDeferred<Unit>? = null
    var loadError: Exception? = null
    var enableError: Exception? = null
    var scanOutcome: MonitorScanOutcome = MonitorScanOutcome.BaselineCreated(7)
    var scanError: Exception? = null
    var scanGate: CompletableDeferred<Unit>? = null

    val loadCount get() = loads
    val enableCount get() = enables
    val disableCount get() = disables
    val scanCount get() = scans

    private var loads = 0
    private var enables = 0
    private var disables = 0
    private var scans = 0

    override suspend fun load(): MonitorState {
      loads++
      loadGate?.await()
      loadError?.let { throw it }
      return storedState
    }

    override suspend fun enable() {
      enables++
      enableError?.let { throw it }
      storedState = storedState.copy(enabled = true)
    }

    override suspend fun disable() {
      disables++
      storedState = MonitorState.disabledEmpty()
    }

    override suspend fun scanNow(): MonitorScanOutcome {
      scans++
      scanGate?.await()
      scanError?.let { throw it }
      return scanOutcome
    }
  }

  @Test
  fun initLoadsStoredStateAndClearsLoading() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply {
      storedState = MonitorState(enabled = true, lastScanAtMillis = 5L)
    }
    val viewModel = SignatureMonitorViewModel(actions)

    assertTrue(viewModel.state.value.isLoading.not())
    assertFalse(viewModel.state.value.loadFailed)
    assertTrue(viewModel.state.value.monitor.enabled)
    assertEquals(5L, viewModel.state.value.monitor.lastScanAtMillis)
    assertEquals(1, actions.loadCount)
  }

  @Test
  fun loadFailureKeepsLastDisplayedStateAndRetryReloads() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply {
      storedState = MonitorState(enabled = true)
    }
    val viewModel = SignatureMonitorViewModel(actions)
    assertEquals(1, actions.loadCount)

    actions.loadError = IllegalStateException("disk broken")
    viewModel.refresh()
    advanceUntilIdle()
    assertTrue(viewModel.state.value.loadFailed)
    assertTrue(viewModel.state.value.monitor.enabled) // a failed read is not a disabled state
    assertTrue(viewModel.state.value.isLoading.not())

    actions.loadError = null
    viewModel.refresh()
    advanceUntilIdle()
    assertFalse(viewModel.state.value.loadFailed)
  }

  @Test
  fun enablingSucceedsRequestsNotificationPermissionAndReloads() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions()
    val viewModel = SignatureMonitorViewModel(actions)
    val events = mutableListOf<SignatureMonitorEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.events.collect { events += it }
    }

    viewModel.setEnabled(true)
    advanceUntilIdle()

    assertEquals(1, actions.enableCount)
    assertEquals(listOf(SignatureMonitorEvent.RequestNotificationPermission), events)
    assertTrue(viewModel.state.value.monitor.enabled)
    assertNull(viewModel.state.value.actionError)
    assertEquals(2, actions.loadCount) // the action result is re-loaded
  }

  @Test
  fun disablingNeverRequestsNotificationPermission() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply { storedState = MonitorState(enabled = true) }
    val viewModel = SignatureMonitorViewModel(actions)
    val events = mutableListOf<SignatureMonitorEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.events.collect { events += it }
    }

    viewModel.setEnabled(false)
    advanceUntilIdle()

    assertEquals(1, actions.disableCount)
    assertEquals(emptyList<SignatureMonitorEvent>(), events)
    assertFalse(viewModel.state.value.monitor.enabled)
  }

  @Test
  fun enableFailureReportsAnHonestActionErrorAndStillReloads() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply { enableError = IllegalStateException("prefs locked") }
    val viewModel = SignatureMonitorViewModel(actions)
    val events = mutableListOf<SignatureMonitorEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.events.collect { events += it }
    }

    viewModel.setEnabled(true)
    advanceUntilIdle()

    assertEquals(
      "Could not turn on the monitor: prefs locked. Please try again.",
      viewModel.state.value.actionError,
    )
    assertEquals(emptyList<SignatureMonitorEvent>(), events) // no permission request on failure
    assertEquals(2, actions.loadCount)
  }

  @Test
  fun scanOutcomesMapToMessagesOrErrorsWhileInFlightIsReported() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply { storedState = MonitorState(enabled = true) }
    val viewModel = SignatureMonitorViewModel(actions)

    // Gated scan: in-flight flag is observable while the scan runs.
    actions.scanGate = CompletableDeferred()
    viewModel.scan()
    assertTrue(viewModel.state.value.isScanInFlight)
    actions.scanGate?.complete(Unit)
    advanceUntilIdle()
    assertFalse(viewModel.state.value.isScanInFlight)
    assertEquals(
      "Baseline recorded for 7 apps. No alerts for the first scan.",
      viewModel.state.value.scanMessage,
    )

    actions.scanOutcome = MonitorScanOutcome.Updated(
      newAlerts = listOf(PackageAlert(packageName = "com.x", status = PackageSignerStatus.UNINSTALLED)),
      visibleAlerts = emptyList(),
      scanned = 9,
    )
    viewModel.scan()
    advanceUntilIdle()
    assertEquals("1 new alert(s) out of 9 apps scanned.", viewModel.state.value.scanMessage)

    actions.scanOutcome = MonitorScanOutcome.MonitorDisabled
    viewModel.scan()
    advanceUntilIdle()
    assertEquals("The monitor is off. Turn it on before scanning.", viewModel.state.value.scanMessage)

    actions.scanOutcome = MonitorScanOutcome.ScanFailed(reason = "pm busy")
    viewModel.scan()
    advanceUntilIdle()
    assertNull(viewModel.state.value.scanMessage)
    assertEquals(
      "Scan failed: installed apps could not be read (pm busy). Previously stored data was kept.",
      viewModel.state.value.actionError,
    )
  }

  @Test
  fun scanPersistenceFailureReportsAnHonestError() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply { scanError = IllegalStateException("commit failed") }
    val viewModel = SignatureMonitorViewModel(actions)

    viewModel.scan()
    advanceUntilIdle()

    assertFalse(viewModel.state.value.isScanInFlight)
    assertEquals(
      "Scan failed: monitor state could not be read or saved (commit failed). Please try again.",
      viewModel.state.value.actionError,
    )
  }

  @Test
  fun staleRefreshNeverPublishesOverANewerOne() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val staleLoadGate = CompletableDeferred<Unit>() // gates only the first (stale) load
    val actions = FakeActions().apply {
      storedState = MonitorState(enabled = true, lastScanAtMillis = 1L)
      loadGate = staleLoadGate
    }
    val viewModel = SignatureMonitorViewModel(actions)
    assertTrue(viewModel.state.value.isLoading)
    assertEquals(1, actions.loadCount)

    // A newer refresh starts, sees fresher stored data, and completes first.
    actions.storedState = MonitorState(enabled = false, lastScanAtMillis = 2L)
    actions.loadGate = null
    viewModel.refresh()
    advanceUntilIdle()
    assertFalse(viewModel.state.value.monitor.enabled)
    assertEquals(2L, viewModel.state.value.monitor.lastScanAtMillis)
    assertFalse(viewModel.state.value.isLoading)

    // The stale first load finishes last with even different data and must
    // never overwrite the newer published state.
    actions.storedState = MonitorState(enabled = true, lastScanAtMillis = 3L)
    staleLoadGate.complete(Unit)
    advanceUntilIdle()
    assertFalse(viewModel.state.value.monitor.enabled)
    assertEquals(2L, viewModel.state.value.monitor.lastScanAtMillis)
    assertFalse(viewModel.state.value.isLoading)
    assertFalse(viewModel.state.value.loadFailed)
  }

  @Test
  fun newActionClearsPreviousMessagesAndErrors() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val actions = FakeActions().apply { scanOutcome = MonitorScanOutcome.ScanFailed(reason = "x") }
    val viewModel = SignatureMonitorViewModel(actions)

    viewModel.scan()
    advanceUntilIdle()
    assertTrue(viewModel.state.value.actionError != null)

    actions.scanOutcome = MonitorScanOutcome.BaselineCreated(3)
    viewModel.scan()
    advanceUntilIdle()
    assertNull(viewModel.state.value.actionError)
    assertEquals("Baseline recorded for 3 apps. No alerts for the first scan.", viewModel.state.value.scanMessage)
  }
}
