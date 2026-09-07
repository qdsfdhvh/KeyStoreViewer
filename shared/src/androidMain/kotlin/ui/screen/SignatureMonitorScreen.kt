package ui.screen

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import signature.MonitorScanOutcome
import signature.MonitorState
import signature.MonitorStateStore
import signature.PackageSignerStatus
import signature.ReinstallSignerComparison
import signature.SignatureMonitorController
import signature.colonSeparatedHex
import signature.reinstallSignerComparison
import java.text.DateFormat
import java.util.Date

/**
 * M4 opt-in installed-app signature-change monitor.
 *
 * - Explicit opt-in; the first scan only records a quiet baseline.
 * - "Scan now" is only available while enabled, and every state change is
 *   serialized in [signature.MonitorScanEngine]: while the monitor is off,
 *   nothing runs and nothing is stored; disabling invalidates in-flight
 *   scans, so they can never resurrect stored state.
 * - Enable/disable/scan failures (e.g. persistence) are shown honestly
 *   instead of reporting success.
 * - The POST_NOTIFICATIONS request (API 33+) never crashes on denial: the
 *   monitor keeps working and alerts stay visible in-app only.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SignatureMonitorScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  val scope = rememberCoroutineScope()
  var refreshKey by remember { mutableIntStateOf(0) }
  var scanMessage by remember { mutableStateOf<String?>(null) }
  var actionError by remember { mutableStateOf<String?>(null) }
  var isScanInFlight by remember { mutableStateOf(false) }
  var loadError by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  val state by produceState(MonitorState(), refreshKey) {
    isLoading = true
    try {
      value = withContext(Dispatchers.IO) { MonitorStateStore.get(context).load() }
      loadError = null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // Keep the last displayed state; a failed read is not a disabled state.
      loadError = "Could not load monitor state. Please retry."
    } finally {
      isLoading = false
    }
  }

  val notificationPermission = if (Build.VERSION.SDK_INT >= 33) {
    rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
  } else {
    null
  }

  fun refresh() {
    refreshKey++
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "back",
            )
          }
        },
        title = {
          Text("Signature monitor")
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize(),
    ) {
      item {
        Card {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Monitor installed apps", style = MaterialTheme.typography.titleMedium)
                Text(
                  "Checks signing certificates of installed apps in the " +
                    "background. Off = nothing runs, nothing is stored.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(Modifier.width(8.dp))
              Switch(
                checked = state.enabled,
                enabled = !isLoading && loadError == null,
                onCheckedChange = { wantEnabled ->
                  scope.launch {
                    actionError = null
                    scanMessage = null
                    try {
                      if (wantEnabled) {
                        SignatureMonitorController.enable(context)
                        if (Build.VERSION.SDK_INT >= 33 &&
                          notificationPermission != null &&
                          !notificationPermission.status.isGranted
                        ) {
                          // Denied permission never disables monitoring; the
                          // user can also grant it later in system settings.
                          notificationPermission.launchPermissionRequest()
                        }
                      } else {
                        SignatureMonitorController.disable(context)
                      }
                    } catch (e: CancellationException) {
                      throw e
                    } catch (e: Exception) {
                      actionError =
                        (if (wantEnabled) "Could not turn on the monitor" else "Could not turn off the monitor") +
                        ": ${e.message ?: e::class.simpleName}. Please try again."
                    }
                    refresh()
                  }
                },
              )
            }
            MonitorStatusText(state)
            if (state.enabled &&
              Build.VERSION.SDK_INT >= 33 &&
              notificationPermission != null &&
              !notificationPermission.status.isGranted
            ) {
              Text(
                "Notifications are not permitted, so background changes only " +
                  "appear here in the app, never as system notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
            Button(
              onClick = {
                scope.launch {
                  scanMessage = null
                  actionError = null
                  isScanInFlight = true
                  try {
                    val outcome = SignatureMonitorController.scanNow(
                      context = context,
                      allowNotifications = false,
                    )
                    scanMessage = when (outcome) {
                      is MonitorScanOutcome.BaselineCreated ->
                        "Baseline recorded for ${outcome.scanned} apps. " +
                          "No alerts for the first scan."

                      is MonitorScanOutcome.Updated ->
                        "${outcome.newAlerts.size} new alert(s) out of " +
                          "${outcome.scanned} apps scanned."

                      MonitorScanOutcome.MonitorDisabled ->
                        "The monitor is off. Turn it on before scanning."

                      is MonitorScanOutcome.ScanFailed -> {
                        actionError =
                          "Scan failed: installed apps could not be read " +
                          "(${outcome.reason}). Previously stored data was kept."
                        null
                      }
                    }
                  } catch (e: CancellationException) {
                    throw e
                  } catch (e: Exception) {
                    actionError =
                      "Scan failed: monitor state could not be read or saved " +
                      "(${e.message ?: e::class.simpleName}). Please try again."
                  } finally {
                    isScanInFlight = false
                  }
                  refresh()
                }
              },
              enabled = state.enabled && !isScanInFlight && !isLoading && loadError == null,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                when {
                  isScanInFlight -> "Scanning…"
                  state.enabled -> "Scan now"
                  else -> "Scan now (monitor is off)"
                },
              )
            }
            scanMessage?.let {
              Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            loadError?.let {
              Text(it, color = MaterialTheme.colorScheme.error)
              Button(onClick = ::refresh) { Text("Retry loading") }
            }
            actionError?.let {
              Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      if (state.lastAlerts.isNotEmpty()) {
        item {
          Text(
            "Alerts from the last scan",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        items(state.lastAlerts, key = { alert -> alert.packageName + alert.status.name }) { alert ->
          AlertCard(alert)
        }
      }

      item {
        Text(
          "How this works:\n" +
            "• The first scan records a baseline and raises no alerts.\n" +
            "• Background checks run roughly every " +
            "${SignatureMonitorController.PERIODIC_INTERVAL_HOURS} hours. " +
            "Android may delay them; there is no instant guarantee. Use " +
            "\"Scan now\" for an immediate check.\n" +
            "• Android already rejects installing most updates whose signing " +
            "certificates are incompatible with the installed app, so an " +
            "in-place signature change is rare.\n" +
            "• A reported change means the signing certificate set differs " +
            "from the earlier snapshot. It is not a claim that anything " +
            "malicious happened - legitimate key rotations by developers are " +
            "one common cause.\n" +
            "• An app that disappears is reported as uninstalled; if it " +
            "appears again later it is reported as reinstalled, compared " +
            "against what was last seen for it - including whether its " +
            "certificates changed. When certificates could not be read, no " +
            "same/different claim is made.\n" +
            "• Everything stays on this device: snapshots and status are kept " +
            "in app-private storage that Android never includes in backups. " +
            "Turning the monitor off deletes them.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun MonitorStatusText(state: MonitorState) {
  val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
  val lastScan = state.lastScanAtMillis?.let { dateFormat.format(Date(it)) } ?: "never"
  val baseline = state.baseline?.let { "${it.size} apps" } ?: "none yet"
  Text(
    "Last scan: $lastScan · Baseline: $baseline",
    style = MaterialTheme.typography.bodyMedium,
  )
}

@Composable
private fun AlertCard(alert: signature.PackageAlert) {
  Card {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(alert.packageName, style = MaterialTheme.typography.titleMedium)
      Text(
        when (alert.status) {
          PackageSignerStatus.SIGNATURE_CHANGED ->
            "Signing certificate set changed (versions " +
              "${alert.oldVersionCode} → ${alert.newVersionCode})"

          PackageSignerStatus.REINSTALLED ->
            when (reinstallSignerComparison(alert.oldSigners, alert.newSigners)) {
              ReinstallSignerComparison.DIFFERENT ->
                "Reinstalled with different signing certificate(s)"

              ReinstallSignerComparison.SAME ->
                "Reinstalled (same signing certificate set)"

              // Empty signer set(s) are unreadable, never a proof of sameness.
              ReinstallSignerComparison.COMPARISON_UNAVAILABLE ->
                "Reinstalled (certificate comparison unavailable)"
            }

          PackageSignerStatus.UNINSTALLED -> "No longer installed"

          PackageSignerStatus.SIGNERS_UNKNOWN -> "Signing certificates unreadable"

          PackageSignerStatus.UNCHANGED -> "Unchanged"

          PackageSignerStatus.NEW_INSTALL -> "Newly installed"
        },
        color = if (alert.status == PackageSignerStatus.SIGNATURE_CHANGED ||
          alert.status == PackageSignerStatus.SIGNERS_UNKNOWN
        ) {
          MaterialTheme.colorScheme.error
        } else {
          MaterialTheme.colorScheme.onSurface
        },
        style = MaterialTheme.typography.bodyMedium,
      )
      if (alert.status == PackageSignerStatus.SIGNATURE_CHANGED) {
        Text("Before:", style = MaterialTheme.typography.labelSmall)
        alert.oldSigners.sorted().forEach {
          Text(colonSeparatedHex(it), style = MaterialTheme.typography.bodySmall)
        }
        Text("After:", style = MaterialTheme.typography.labelSmall)
        alert.newSigners.sorted().forEach {
          Text(colonSeparatedHex(it), style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}
