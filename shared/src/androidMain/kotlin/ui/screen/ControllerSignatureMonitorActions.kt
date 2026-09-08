package ui.screen

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import signature.MonitorScanOutcome
import signature.MonitorState
import signature.MonitorStateStore
import signature.SignatureMonitorController

/**
 * Android [SignatureMonitorActions] backed by the WorkManager-based
 * controller and the no-backup state store. Holds the application context
 * only; never an Activity.
 */
class ControllerSignatureMonitorActions(
  private val applicationContext: Context,
) : SignatureMonitorActions {

  override suspend fun load(): MonitorState = withContext(Dispatchers.IO) {
    MonitorStateStore.get(applicationContext).load()
  }

  override suspend fun enable() {
    SignatureMonitorController.enable(applicationContext)
  }

  override suspend fun disable() {
    SignatureMonitorController.disable(applicationContext)
  }

  override suspend fun scanNow(): MonitorScanOutcome = SignatureMonitorController.scanNow(
    context = applicationContext,
    allowNotifications = false,
  )
}
