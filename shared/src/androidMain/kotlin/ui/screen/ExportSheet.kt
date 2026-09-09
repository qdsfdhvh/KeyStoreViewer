package ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import data.local.UnlimitedExportQuota
import dev.zacsweers.metrox.viewmodel.metroViewModel
import platform.ads.AdSlot
import platform.ads.LocalAdSlot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ui.widget.PrimaryButton as Button

/**
 * 批量导出签名报告的弹层。
 * play 变体:每天 2 次免费,看完激励广告 +2 次;foss 变体:UnlimitedExportQuota 完全免费。
 *
 * 配额消耗、激励奖励与报告写入属于业务动作,由 entry 级 [ExportViewModel]
 * 持有(写入与奖励用 NonCancellable 保证完成);isExporting / 激励弹窗属于
 * 瞬时 UI 状态,保留在本组合内。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  // Scoped to the hosting Nav3 entry (retained across Activity recreation):
  // quota and report writer come from the app graph.
  val viewModel = metroViewModel<ExportViewModel>()
  val quota = viewModel.quota
  val adSlot = LocalAdSlot.current
  val remaining by quota.remaining.collectAsState(null)
  val rewardedReady by adSlot.isRewardedReady.collectAsState()
  var offerRewarded by remember { mutableStateOf(false) }
  var isExporting by remember { mutableStateOf(false) }

  val launcher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.CreateDocument("text/csv") },
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.writeReport(uri.toString())
    } else {
      isExporting = false
      onDismiss()
    }
  }

  LaunchedEffect(viewModel) {
    // Sheet session start: this instance supersedes older ones, so queued and
    // in-flight producers from detached sessions can no longer drive this
    // sheet (stale toasts, instant dismiss, an unexpected document-creator
    // launch). Re-reading the date-sensitive quota un-sticks Export after a
    // day rollover on a retained quota instance.
    val session = viewModel.beginSession()
    try {
      viewModel.refreshQuota()
      viewModel.events.collect { event ->
        when (event) {
          ExportEvent.CreateDocument -> launcher.launch(defaultCsvName())

          ExportEvent.OutOfQuota -> {
            isExporting = false
            offerRewarded = true
          }

          is ExportEvent.WriteFinished -> {
            Toast.makeText(
              context,
              if (event.ok) "Report saved" else "Failed to save report",
              Toast.LENGTH_SHORT,
            ).show()
            isExporting = false
            onDismiss()
          }

          ExportEvent.BonusWithoutSlot -> isExporting = false
        }
      }
    } finally {
      // Invalidate callbacks even if no replacement sheet ever opens. An old
      // collector's cleanup must not invalidate a newer session.
      viewModel.endSession(session)
    }
  }

  ModalBottomSheet(
    onDismissRequest = { if (!isExporting) onDismiss() },
    modifier = modifier,
    sheetState = rememberModalBottomSheetState(
      skipPartiallyExpanded = true,
      confirmValueChange = { !isExporting || it != SheetValue.Hidden },
    ),
    containerColor = MaterialTheme.colorScheme.background,
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
      Text("Export signature report", style = MaterialTheme.typography.headlineSmall)
      Text(
        "Export MD5 / SHA1 / SHA256 of user apps as a CSV file, one row per signing certificate.",
        style = MaterialTheme.typography.bodyMedium,
      )
      when (val left = remaining) {
        null -> CircularProgressIndicator()

        else -> Text(
          if (quota === UnlimitedExportQuota) "Unlimited exports · No ads" else "Remaining exports today: $left",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      if (adSlot.canShowRewarded()) {
        Text(
          "Watch a short ad to get +${AdSlot.REWARD_BONUS_COUNT} exports.",
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Button(
        onClick = {
          if (!isExporting) {
            isExporting = true
            viewModel.exportClicked()
          }
        },
        enabled = !isExporting && (remaining?.let { it > 0 || (adSlot.canShowRewarded() && rewardedReady) } ?: false),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (isExporting) "Exporting…" else "Export CSV")
      }
      TextButton(onClick = onDismiss, enabled = !isExporting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Cancel")
      }
    }
  }

  if (offerRewarded) {
    AlertDialog(
      onDismissRequest = { offerRewarded = false },
      title = {
        Text("Out of free exports")
      },
      text = {
        Text(
          "You have used up today's free exports. Watch a short ad to get " +
            "+${AdSlot.REWARD_BONUS_COUNT} more exports.",
        )
      },
      confirmButton = {
        TextButton(
          enabled = rewardedReady,
          onClick = {
            offerRewarded = false
            isExporting = true
            // Capture this session before the ad shows: the callback is
            // delivered later and must not be routed to a newer session.
            val originSession = viewModel.session
            adSlot.showRewarded("export_report") { rewarded ->
              if (rewarded) {
                // Crediting starts only if this VM is still alive when the
                // ad ends (a cleared entry cannot credit a pending reward);
                // once started it completes even if the entry is torn down
                // mid-credit, and only the originating session may consume
                // the earned slot.
                viewModel.rewardEarned(originSession)
              } else {
                isExporting = false
                Toast.makeText(context, "Ad not finished", Toast.LENGTH_SHORT).show()
              }
            }
          },
        ) {
          Text(if (rewardedReady) "Watch ad" else "Loading ad…")
        }
      },
      dismissButton = {
        TextButton(onClick = { offerRewarded = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

private fun defaultCsvName(): String = "keystoreviewer-signatures-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".csv"
