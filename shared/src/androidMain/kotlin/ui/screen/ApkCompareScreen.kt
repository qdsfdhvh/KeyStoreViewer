package ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import signature.ApkSignerMeta
import signature.ApkSignerRead
import signature.ApkSignerReadError
import signature.SelectionGeneration
import signature.SignerSetComparisonOutcome
import signature.colonSeparatedHex
import signature.compareSignerSets
import signature.readApkSignerMetaFromUri
import ui.widget.DetailTopBar
import ui.widget.GroupedCard as Card
import ui.widget.SecondaryButton as OutlinedButton

/**
 * M4 dual-APK signer comparison.
 *
 * Compares the complete, order-independent CURRENT signing certificate sets
 * (SHA-256 of each certificate) of two APK files using PackageManager's
 * signing-aware archive APIs.
 *
 * Temporary copies of the picked APKs are lifecycle-owned by
 * [signature.readApkSignerMetaFromUri]: bounded, cancellation-aware and
 * deleted in a finally block. Screen state only ever holds metadata, so
 * replacing a selection or leaving the screen cannot leak a temporary file.
 *
 * Honest framing (also shown in the UI):
 * - this is certificate metadata extraction, NOT full APK integrity
 *   verification (zip digest checking happens in the OS installer);
 * - equal signers do not prove an APK is authentic, unmodified or upgradable;
 * - different signers alone do not prove an APK is malicious.
 */
private data class ApkCompareSide(
  val result: ApkSignerRead?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkCompareScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  val scope = rememberCoroutineScope()
  var leftSide by remember { mutableStateOf(ApkCompareSide(null)) }
  var rightSide by remember { mutableStateOf(ApkCompareSide(null)) }
  val leftSelection = remember { SelectionGeneration() }
  val rightSelection = remember { SelectionGeneration() }

  fun onPicked(
    selection: SelectionGeneration,
    setSide: (ApkCompareSide) -> Unit,
    uri: Uri?,
  ) {
    if (uri == null) {
      return
    }
    scope.launch {
      val token = selection.invalidate()
      setSide(ApkCompareSide(null))
      val read = try {
        readApkSignerMetaFromUri(context, uri)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        ApkSignerRead.Failure(ApkSignerReadError.NotApkOrUnreadable)
      }
      // A newer selection for this side supersedes this result.
      if (selection.isValid(token)) {
        setSide(ApkCompareSide(read))
      }
    }
  }

  val leftLauncher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.OpenDocument() },
  ) { uri -> onPicked(leftSelection, { leftSide = it }, uri) }
  val rightLauncher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.OpenDocument() },
  ) { uri -> onPicked(rightSelection, { rightSide = it }, uri) }

  Scaffold(
    modifier = modifier,
    topBar = {
      DetailTopBar("Compare APK signatures", onBack)
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
        SideCard(
          label = "APK A",
          side = leftSide,
          onPick = { leftLauncher.launch(arrayOf(APK_MIME)) },
        )
      }
      item {
        SideCard(
          label = "APK B",
          side = rightSide,
          onPick = { rightLauncher.launch(arrayOf(APK_MIME)) },
        )
      }
      item {
        ComparisonResultCard(
          left = (leftSide.result as? ApkSignerRead.Success)?.meta,
          right = (rightSide.result as? ApkSignerRead.Success)?.meta,
        )
      }
      item {
        Text(
          "What this does and does not tell you:\n" +
            "• It compares the current signing certificate sets extracted from " +
            "each APK file. It does NOT verify the integrity of the APK " +
            "contents; that is done by the operating system when installing.\n" +
            "• Equal signers do not prove an APK is authentic, unmodified, or " +
            "that it can be installed as an upgrade.\n" +
            "• Different signers alone do not prove an APK is malicious; " +
            "developers can legitimately rotate keys.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private const val APK_MIME = "application/vnd.android.package-archive"

@Composable
private fun SideCard(
  label: String,
  side: ApkCompareSide,
  onPick: () -> Unit,
) {
  Card {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(label, style = MaterialTheme.typography.titleMedium)
      OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text("Choose APK")
      }
      when (val result = side.result) {
        is ApkSignerRead.Success -> {
          Text(result.meta.packageName, style = MaterialTheme.typography.bodyMedium)
          Text(
            "version ${result.meta.versionName} (${result.meta.versionCode})",
            style = MaterialTheme.typography.bodyMedium,
          )
          Text("Current signers:", style = MaterialTheme.typography.labelSmall)
          result.meta.signerSha256.sorted().forEach { sha ->
            Text(
              colonSeparatedHex(sha),
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        is ApkSignerRead.Failure -> Text(
          when (val error = result.error) {
            ApkSignerReadError.MissingFile ->
              "The file could not be accessed any more. Please choose it again."

            ApkSignerReadError.EmptyFile ->
              "The selected file is empty."

            is ApkSignerReadError.TooLarge ->
              "The file is larger than the ${error.maxBytes / (1024 * 1024)} MB comparison limit."

            ApkSignerReadError.NotApkOrUnreadable ->
              "APK/signature information could not be read. The file may be " +
                "damaged, may not be an APK, or may carry no signing " +
                "certificate information."
          },
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
        )

        null -> Unit
      }
    }
  }
}

@Composable
private fun ComparisonResultCard(
  left: ApkSignerMeta?,
  right: ApkSignerMeta?,
) {
  if (left == null || right == null) {
    return
  }
  val comparison = compareSignerSets(left.signerSha256, right.signerSha256)
  Card {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      when (comparison.outcome) {
        SignerSetComparisonOutcome.IDENTICAL -> Text(
          "Same current signing certificate set",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.titleSmall,
        )

        SignerSetComparisonOutcome.DIFFERENT -> {
          Text(
            "Different signing certificates",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleSmall,
          )
          if (comparison.onlyInLeft.isNotEmpty()) {
            Text("Only in APK A:", style = MaterialTheme.typography.labelSmall)
            comparison.onlyInLeft.sorted().forEach {
              Text(colonSeparatedHex(it), style = MaterialTheme.typography.bodySmall)
            }
          }
          if (comparison.onlyInRight.isNotEmpty()) {
            Text("Only in APK B:", style = MaterialTheme.typography.labelSmall)
            comparison.onlyInRight.sorted().forEach {
              Text(colonSeparatedHex(it), style = MaterialTheme.typography.bodySmall)
            }
          }
        }

        SignerSetComparisonOutcome.LEFT_EMPTY -> Text(
          "Signer information for APK A is unavailable; cannot compare.",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.titleSmall,
        )

        SignerSetComparisonOutcome.RIGHT_EMPTY -> Text(
          "Signer information for APK B is unavailable; cannot compare.",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.titleSmall,
        )

        SignerSetComparisonOutcome.BOTH_EMPTY -> Text(
          "Neither file exposes signing certificate information; cannot compare.",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.titleSmall,
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        "Comparison is order-independent over the complete current signer set. " +
          "It is not an authenticity or upgrade-compatibility verdict.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
