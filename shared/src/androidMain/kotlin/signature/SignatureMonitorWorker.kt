package signature

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

/**
 * Periodic scan for the opt-in signature-change monitor (M4).
 *
 * Runs roughly every [SignatureMonitorController.PERIODIC_INTERVAL_HOURS]
 * hours; Android may defer it (no instant guarantee).
 *
 * Retry policy: a failed package enumeration ([MonitorScanOutcome.ScanFailed])
 * or a persistence failure retries with WorkManager backoff instead of
 * reporting success without a durable state change. A disabled monitor is a
 * successful no-op. Cancellation is never swallowed.
 */
class SignatureMonitorWorker(
  context: Context,
  parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

  override suspend fun doWork(): Result = try {
    when (
      SignatureMonitorController.scanNow(
        context = applicationContext,
        allowNotifications = true,
      )
    ) {
      is MonitorScanOutcome.ScanFailed -> Result.retry()
      else -> Result.success()
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.w(TAG, "Signature monitor scan failed", e)
    Result.retry()
  }

  private companion object {
    const val TAG = "SignatureMonitorWorker"
  }
}
