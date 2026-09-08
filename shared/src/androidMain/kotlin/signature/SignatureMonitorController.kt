package signature

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Controller for the explicitly opt-in installed-app signature-change monitor.
 *
 * - Opt-in: nothing is scheduled or stored until the user enables it.
 * - Unique periodic WorkManager work; disabling first stores a durable
 *   disabled state (so no late scan can resurrect data), then cancels the
 *   work and deletes the state file (no-backup storage only).
 * - All enable/disable/scan cycles are serialized by [MonitorScanEngine];
 *   scans collect and commit only while enabled, persistence failures
 *   propagate, and package-enumeration failures never masquerade as empty
 *   scans.
 * - The first scan after enabling only records a quiet baseline.
 * - Scans compare complete order-independent signer SHA-256 sets; empty
 *   (unreadable) sets are their own status. Uninstall and reinstall are
 *   reported explicitly and never conflated with in-place signer changes.
 * - A reported change is a factual diff of signing certificates, never a
 *   claim that anything malicious happened.
 */
object SignatureMonitorController {

  private const val TAG = "SignatureMonitor"
  const val UNIQUE_WORK_NAME = "signature-change-monitor"
  const val NOTIFICATION_CHANNEL_ID = "signature_monitor"
  const val PERIODIC_INTERVAL_HOURS = 6L

  private val engineLock = Any()

  @Volatile
  private var engineInstance: MonitorScanEngine? = null

  /** One engine (and therefore one serialization mutex) per process. */
  private fun engine(context: Context): MonitorScanEngine = engineInstance ?: synchronized(engineLock) {
    engineInstance ?: MonitorScanEngine { MonitorStateStore.get(context.applicationContext) }
      .also { engineInstance = it }
  }

  /** Durably enable, then schedule the unique periodic work. */
  suspend fun enable(context: Context): Unit = withContext(Dispatchers.IO) {
    ensureNotificationChannel(context)
    engine(context).enable {
      val request = PeriodicWorkRequestBuilder<SignatureMonitorWorker>(
        PERIODIC_INTERVAL_HOURS,
        TimeUnit.HOURS,
      ).build()
      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
      )
    }
  }

  /**
   * Persist the durable disabled state first, then cancel the unique work and
   * delete the state file. Work cancellation is best effort: the durable
   * disabled state already turns any late scan into a no-op.
   */
  suspend fun disable(context: Context): Unit = withContext(Dispatchers.IO) {
    engine(context).disable {
      runCatching {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
      }.onFailure {
        Log.w(TAG, "Could not cancel signature monitor work", it)
      }
    }
  }

  /**
   * Manual or background scan. Returns [MonitorScanOutcome.MonitorDisabled]
   * when the monitor is off (nothing collected, nothing stored) and
   * [MonitorScanOutcome.ScanFailed] when package enumeration failed (stored
   * state preserved). Persistence failures throw.
   */
  suspend fun scanNow(
    context: Context,
    allowNotifications: Boolean,
  ): MonitorScanOutcome = withContext(Dispatchers.IO) {
    engine(context).scan(
      collect = { collectInstalledSignerSnapshots(context) },
      scannedAtMillis = System.currentTimeMillis(),
      allowNotifications = allowNotifications,
      notifyNewAlerts = { alerts -> notifyNewAlerts(context, alerts) },
    )
  }

  private fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return // channels exist only on API 26+; older devices need none
    }
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
      return
    }
    val channel = NotificationChannel(
      NOTIFICATION_CHANNEL_ID,
      "Signature change monitor",
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = "Alerts when an installed app changes its signing certificate set"
    }
    manager.createNotificationChannel(channel)
  }

  // areNotificationsEnabled() covers the API 33+ POST_NOTIFICATIONS runtime
  // permission: when it is denied we simply skip notifying (no crash, in-app
  // status still shows the alerts).
  @SuppressLint("MissingPermission")
  private fun notifyNewAlerts(
    context: Context,
    newAlerts: List<PackageAlert>,
  ) {
    if (newAlerts.isEmpty()) {
      return
    }
    val notificationManager = NotificationManagerCompat.from(context)
    if (!notificationManager.areNotificationsEnabled()) {
      Log.i(TAG, "Notifications disabled by user; alerts available in-app only")
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      ensureNotificationChannel(context)
    }
    for (alert in newAlerts) {
      val notification = buildAlertNotification(context, alert) ?: continue
      runCatching {
        notificationManager.notify(alert.packageName.hashCode(), notification)
      }.onFailure {
        Log.w(TAG, "Could not post alert for ${alert.packageName}", it)
      }
    }
  }

  private fun buildAlertNotification(
    context: Context,
    alert: PackageAlert,
  ): Notification? {
    val icon = context.applicationInfo.icon ?: return null
    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
      .setSmallIcon(icon)
      .setAutoCancel(true)
    when (alert.status) {
      PackageSignerStatus.SIGNATURE_CHANGED ->
        builder
          .setContentTitle("Signature change detected")
          .setContentText(
            "${alert.packageName} now uses different signing certificate(s). " +
              "Verify where this update came from.",
          )

      PackageSignerStatus.REINSTALLED ->
        builder
          .setContentTitle("App reinstalled")
          .setContentText(
            "${alert.packageName} was installed again" +
              when (reinstallSignerComparison(alert.oldSigners, alert.newSigners)) {
                ReinstallSignerComparison.DIFFERENT -> " with different signing certificate(s)."

                ReinstallSignerComparison.SAME -> " with the same signing certificate set."

                // Never claim sameness when a signer set is unreadable.
                ReinstallSignerComparison.COMPARISON_UNAVAILABLE -> "."
              },
          )

      PackageSignerStatus.UNINSTALLED ->
        builder
          .setContentTitle("App uninstalled")
          .setContentText("${alert.packageName} is no longer installed.")

      PackageSignerStatus.SIGNERS_UNKNOWN ->
        builder
          .setContentTitle("Signatures unreadable")
          .setContentText(
            "The signing certificate(s) of ${alert.packageName} could not be read.",
          )

      PackageSignerStatus.UNCHANGED, PackageSignerStatus.NEW_INSTALL -> return null
    }
    return builder.build()
  }
}
