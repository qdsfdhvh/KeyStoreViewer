package ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import signature.MonitorScanOutcome
import signature.MonitorState

/**
 * Storage and scheduler bridge for the signature-change monitor.
 * Implementations must be main-safe; [SignatureMonitorController]-backed on
 * Android. The persistence layer ([signature.MonitorScanEngine]) keeps its
 * own serialized enable/disable/scan semantics, including cancellation.
 */
interface SignatureMonitorActions {
  suspend fun load(): MonitorState

  suspend fun enable()

  suspend fun disable()

  suspend fun scanNow(): MonitorScanOutcome
}

/** One-shot requests from the ViewModel to the screen UI. */
sealed interface SignatureMonitorEvent {
  /**
   * The monitor was just enabled successfully; the screen should offer the
   * POST_NOTIFICATIONS permission when applicable (API 33+ and not granted).
   */
  data object RequestNotificationPermission : SignatureMonitorEvent
}

/** Everything the monitor screen renders; transient widget state stays local. */
data class SignatureMonitorUiState(
  val monitor: MonitorState = MonitorState(),
  val isLoading: Boolean = true,
  /** True when the stored state could not be read; the last state stays shown. */
  val loadFailed: Boolean = false,
  val isScanInFlight: Boolean = false,
  val scanMessage: String? = null,
  val actionError: String? = null,
)

/**
 * Lifecycle-owned state and actions of the opt-in signature-change monitor.
 *
 * "Scan now" is only available while enabled, and every state change is
 * serialized in [signature.MonitorScanEngine]: while the monitor is off,
 * nothing runs and nothing is stored; disabling invalidates in-flight scans,
 * so they can never resurrect stored state. Enable/disable/scan failures
 * (e.g. persistence) are shown honestly instead of reporting success. A
 * failed load keeps the last displayed state: it is not a disabled state.
 */
// Constructed by the Metro graph via SharedViewModelFactory: the WorkManager-
// backed actions adapter is an app-scoped binding, this instance stays owned
// by its Nav3 entry.
@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey(SignatureMonitorViewModel::class)
class SignatureMonitorViewModel(
  private val actions: SignatureMonitorActions,
) : ViewModel() {

  private val _state = MutableStateFlow(SignatureMonitorUiState())
  val state: StateFlow<SignatureMonitorUiState> = _state.asStateFlow()

  private val _events = Channel<SignatureMonitorEvent>(Channel.UNLIMITED)
  val events: Flow<SignatureMonitorEvent> = _events.receiveAsFlow()

  init {
    refresh()
  }

  /**
   * Monotonic guard so only the most recently requested load publishes: an
   * older in-flight refresh that completes after a newer one (overlapping
   * enable/scan/retry reloads) must never overwrite the newer state.
   */
  private var refreshGeneration = 0

  /** Reloads the stored monitor state; a failure keeps the displayed state. */
  fun refresh() {
    val generation = ++refreshGeneration
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true) }
      try {
        val loaded = actions.load()
        if (generation == refreshGeneration) {
          _state.update { it.copy(monitor = loaded, isLoading = false, loadFailed = false) }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        if (generation == refreshGeneration) {
          _state.update { it.copy(isLoading = false, loadFailed = true) }
        }
      }
    }
  }

  fun setEnabled(want: Boolean) {
    viewModelScope.launch {
      _state.update { it.copy(scanMessage = null, actionError = null) }
      try {
        if (want) {
          actions.enable()
          _events.send(SignatureMonitorEvent.RequestNotificationPermission)
        } else {
          actions.disable()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val prefix = if (want) "Could not turn on the monitor" else "Could not turn off the monitor"
        _state.update {
          it.copy(actionError = "$prefix: ${e.message ?: e::class.simpleName}. Please try again.")
        }
      }
      refresh()
    }
  }

  fun scan() {
    viewModelScope.launch {
      _state.update { it.copy(scanMessage = null, actionError = null, isScanInFlight = true) }
      try {
        when (val outcome = actions.scanNow()) {
          is MonitorScanOutcome.BaselineCreated ->
            _state.update {
              it.copy(
                scanMessage =
                "Baseline recorded for ${outcome.scanned} apps. No alerts for the first scan.",
              )
            }

          is MonitorScanOutcome.Updated ->
            _state.update {
              it.copy(
                scanMessage =
                "${outcome.newAlerts.size} new alert(s) out of ${outcome.scanned} apps scanned.",
              )
            }

          MonitorScanOutcome.MonitorDisabled ->
            _state.update { it.copy(scanMessage = "The monitor is off. Turn it on before scanning.") }

          is MonitorScanOutcome.ScanFailed ->
            _state.update {
              it.copy(
                actionError =
                "Scan failed: installed apps could not be read (${outcome.reason}). " +
                  "Previously stored data was kept.",
              )
            }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _state.update {
          it.copy(
            actionError =
            "Scan failed: monitor state could not be read or saved " +
              "(${e.message ?: e::class.simpleName}). Please try again.",
          )
        }
      } finally {
        _state.update { it.copy(isScanInFlight = false) }
      }
      refresh()
    }
  }
}
