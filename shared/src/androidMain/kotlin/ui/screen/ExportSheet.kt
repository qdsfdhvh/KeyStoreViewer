package ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import data.local.LocalExportQuota
import export.SignatureReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.ads.AdSlot
import platform.ads.LocalAdSlot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 批量导出签名报告的弹层。
 * play 变体:每天 2 次免费,看完激励广告 +2 次;foss 变体:UnlimitedExportQuota 完全免费。
 */
@Composable
fun ExportSheet(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  val quota = LocalExportQuota.current
  val adSlot = LocalAdSlot.current
  val scope = rememberCoroutineScope()
  val remaining by quota.remaining.collectAsState(null)
  var offerRewarded by remember { mutableStateOf(false) }

  val launcher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.CreateDocument("text/csv") },
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val csv = SignatureReportExporter.buildCsv(context)
        val ok = withContext(Dispatchers.IO) {
          SignatureReportExporter.write(context, uri, csv)
        }
        Toast.makeText(
          context,
          if (ok) "Report saved" else "Failed to save report",
          Toast.LENGTH_SHORT,
        ).show()
      }
    }
    onDismiss()
  }

  fun exportNow() = launcher.launch(
    "keystoreviewer-signatures-" + SimpleDateFormat(
      "yyyyMMdd-HHmm",
      Locale.US,
    ).format(Date()) + ".csv",
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier,
    title = {
      Text("Export signature report")
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          "Export MD5 / SHA1 / SHA256 of all installed apps as a CSV file.",
          style = MaterialTheme.typography.bodyMedium,
        )
        when (val left = remaining) {
          null -> CircularProgressIndicator()

          else -> Text(
            "Remaining exports today: $left",
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
      }
    },
    confirmButton = {
      Button(
        onClick = {
          scope.launch {
            if (quota.tryConsume()) {
              exportNow()
            } else {
              offerRewarded = true
            }
          }
        },
        enabled = remaining?.let { it > 0 || adSlot.canShowRewarded() } ?: false,
      ) {
        Text("Export CSV")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )

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
          onClick = {
            offerRewarded = false
            adSlot.showRewarded("export_report") { rewarded ->
              if (rewarded) {
                scope.launch {
                  quota.addBonus(AdSlot.REWARD_BONUS_COUNT)
                  if (quota.tryConsume()) {
                    exportNow()
                  }
                }
              } else {
                Toast.makeText(context, "Ad not finished", Toast.LENGTH_SHORT).show()
              }
            }
          },
        ) {
          Text("Watch ad")
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
