package ui.screen

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import signature.MonitorState
import signature.PackageSignerStatus
import signature.ReinstallSignerComparison
import signature.SignatureMonitorController
import signature.colonSeparatedHex
import signature.reinstallSignerComparison
import ui.widget.DetailTopBar
import java.text.DateFormat
import java.util.Date
import ui.widget.GroupedCard as Card
import ui.widget.PrimaryButton as Button

/**
 * M4 opt-in installed-app signature-change monitor.
 *
 * - Explicit opt-in; the first scan only records a quiet baseline.
 * - Load state, scan-in-flight and message/error reporting are owned by the
 *   entry-scoped [SignatureMonitorViewModel]; the notification-permission
 *   request stays screen-local transient UI.
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
) {
  // Scoped to this Nav3 entry's ViewModelStore; the WorkManager-backed actions
  // adapter is an app-graph binding.
  val viewModel = metroViewModel<SignatureMonitorViewModel>()
  val state by viewModel.state.collectAsState()

  val notificationPermission = if (Build.VERSION.SDK_INT >= 33) {
    rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
  } else {
    null
  }
  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        SignatureMonitorEvent.RequestNotificationPermission -> {
          if (
            Build.VERSION.SDK_INT >= 33 &&
            notificationPermission != null &&
            !notificationPermission.status.isGranted
          ) {
            // Denied permission never disables monitoring; the user can also
            // grant it later in system settings.
            notificationPermission.launchPermissionRequest()
          }
        }
      }
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      DetailTopBar("Signature monitor", onBack)
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
                checked = state.monitor.enabled,
                enabled = !state.isLoading && !state.loadFailed,
                onCheckedChange = viewModel::setEnabled,
              )
            }
            MonitorStatusText(state.monitor)
            if (
              state.monitor.enabled &&
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
              onClick = viewModel::scan,
              enabled = state.monitor.enabled && !state.isScanInFlight && !state.isLoading && !state.loadFailed,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                when {
                  state.isScanInFlight -> "Scanning…"
                  state.monitor.enabled -> "Scan now"
                  else -> "Scan now (monitor is off)"
                },
              )
            }
            state.scanMessage?.let {
              Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (state.loadFailed) {
              Text("Could not load monitor state. Please retry.", color = MaterialTheme.colorScheme.error)
              Button(onClick = viewModel::refresh) { Text("Retry loading") }
            }
            state.actionError?.let {
              Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      if (state.monitor.lastAlerts.isNotEmpty()) {
        item {
          Text(
            "Alerts from the last scan",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        items(state.monitor.lastAlerts, key = { alert -> alert.packageName + alert.status.name }) { alert ->
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
