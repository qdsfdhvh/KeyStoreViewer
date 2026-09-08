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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import signature.CertificateMeta
import signature.KeystoreAliasInfo
import signature.KeystoreBrowserState
import signature.KeystoreError
import signature.KeystoreFileFormat
import signature.KeystoreInspection
import signature.KeystoreInspector
import signature.colonSeparatedHex
import ui.widget.DetailTopBar
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date
import ui.widget.GroupedCard as Card
import ui.widget.PrimaryButton as Button
import ui.widget.SecondaryButton as OutlinedButton

/**
 * M4 read-only KeyStore file browser.
 *
 * - Opens keystore FILES through the system document picker (SAF, read only).
 * - The format is detected from file content because ".keystore" is an
 *   extension, not a format.
 * - Fully supported: PKCS12 (.p12/.pfx) keystores and JKS files containing
 *   only certificate entries (via the vetted FOSS BouncyCastle provider,
 *   the only JKS reader wired in here). JKS files with private key entries
 *   cannot be fully read on Android and get a specific, honest error.
 * - The password is masked, kept only in memory, never logged or persisted,
 *   and cleared on exit.
 * - Private keys are never accessed or exported; only certificates are shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreBrowserScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  val scope = rememberCoroutineScope()
  val browser = remember { KeystoreBrowserState() }

  DisposableEffect(Unit) {
    onDispose { browser.clear() }
  }

  val launcher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.OpenDocument() },
  ) { uri: Uri? ->
    if (uri == null) {
      return@rememberLauncherForActivityResult
    }
    val token = browser.select()
    scope.launch {
      browser.read(token, uri.lastPathSegment?.substringAfterLast('/')) {
        withContext(Dispatchers.IO) { readKeystoreFile(context, uri) }
      }
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      DetailTopBar("KeyStore browser", onBack)
    },
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .padding(innerPadding)
        .imePadding()
        .fillMaxSize(),
    ) {
      item {
        OutlinedButton(
          onClick = { launcher.launch(arrayOf("*/*")) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(browser.fileName?.let { "Selected: $it" } ?: "Choose a .jks / .keystore / .p12 file")
        }
      }

      if (browser.isReading) {
        item { Text("Reading selected file…") }
      }

      browser.error?.let { error ->
        item {
          ErrorCard(error)
        }
      }

      if (browser.canParse) {
        item {
          Column {
            OutlinedTextField(
              value = browser.password,
              onValueChange = { browser.password = it },
              label = { Text("Keystore password") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
              onClick = {
                scope.launch { browser.parse() }
              },
              enabled = browser.canParse && !browser.isReading && !browser.isParsing,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(if (browser.isParsing) "Reading…" else "Open keystore")
            }
          }
        }
      }

      when (val result = browser.inspection) {
        is KeystoreInspection.Failure -> item {
          ErrorCard(describeKeystoreError(result.error))
        }

        is KeystoreInspection.Success -> {
          item {
            FormatAndCountHeader(
              format = result.format,
              aliasCount = result.aliases.size,
            )
          }
          items(result.aliases, key = { it.alias }) { aliasInfo ->
            KeystoreAliasCard(aliasInfo)
          }
        }

        null -> Unit
      }

      item {
        Text(
          "Certificates are shown read-only. Private keys are never read or " +
            "exported. The password is kept in memory only and cleared when " +
            "you leave this screen. Fully supported: PKCS12 (.p12/.pfx) " +
            "keystores and JKS files containing only certificate entries. " +
            "JKS files with private key entries cannot be fully read on " +
            "Android.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun FormatAndCountHeader(
  format: KeystoreFileFormat,
  aliasCount: Int,
) {
  Card {
    Column(Modifier.padding(12.dp)) {
      Text("Format detected: ${format.storeType}")
      Text(
        "$aliasCount ${if (aliasCount == 1) "entry" else "entries"}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun KeystoreAliasCard(aliasInfo: KeystoreAliasInfo) {
  Card {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(aliasInfo.alias, style = MaterialTheme.typography.titleMedium)
      Text(
        if (aliasInfo.isKeyEntry) {
          "Key entry (private key not displayed)"
        } else {
          "Trusted certificate entry"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      val leaf = aliasInfo.certificates.firstOrNull()
      if (leaf == null) {
        Text("No certificate available", style = MaterialTheme.typography.bodyMedium)
      } else {
        CertificateFields(leaf)
      }
      if (aliasInfo.certificates.size > 1) {
        Text(
          "Certificate chain length: ${aliasInfo.certificates.size}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CertificateFields(certificate: CertificateMeta) {
  val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    CertField("Subject", certificate.subjectDn)
    CertField("Issuer", certificate.issuerDn)
    CertField("Valid from", dateFormat.format(Date(certificate.notBeforeMillis)))
    CertField("Valid until", dateFormat.format(Date(certificate.notAfterMillis)))
    CertField(
      "SHA-256",
      colonSeparatedHex(certificate.sha256Fingerprint),
    )
  }
}

@Composable
private fun CertField(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelSmall)
    Text(value, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun ErrorCard(message: String) {
  Card {
    Text(
      message,
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier
        .padding(12.dp)
        .fillMaxWidth(),
    )
  }
}

private fun describeKeystoreError(error: KeystoreError): String = when (error) {
  is KeystoreError.EmptyFile -> "The selected file is empty."

  is KeystoreError.TooLarge ->
    "The file is larger than ${error.maxBytes / (1024 * 1024)} MB and was not read."

  KeystoreError.UnsupportedFormat ->
    "Unsupported keystore format. Only JKS and PKCS12 keystores are supported " +
      "(.keystore files can contain either format)."

  KeystoreError.WrongPasswordOrCorrupt ->
    "Wrong password, or the file is corrupted."

  KeystoreError.EmptyKeystore ->
    "The keystore opened successfully but contains no entries."

  KeystoreError.JksKeyEntriesUnsupported ->
    "This JKS keystore contains private key entries, which cannot be read " +
      "on Android: only JKS certificate entries are supported here. " +
      "PKCS12 (.p12/.pfx) keystores are fully supported. To list this file " +
      "on a desktop computer, use a standard Java tool such as " +
      "\"keytool -list\"."

  is KeystoreError.ParseFailed -> "Could not read the keystore: ${error.message}"
}

/** Bounded, in-memory read; never writes the keystore file to disk. */
private suspend fun readKeystoreFile(
  context: Context,
  uri: Uri,
): ByteArray {
  val stream = context.contentResolver.openInputStream(uri)
    ?: error("Could not open the selected file")
  return stream.use { input ->
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0
    while (true) {
      currentCoroutineContext().ensureActive()
      val read = input.read(buffer)
      if (read < 0) {
        break
      }
      total += read
      if (total > KeystoreInspector.MAX_FILE_BYTES) {
        error("The file is larger than ${KeystoreInspector.MAX_FILE_BYTES / (1024 * 1024)} MB")
      }
      output.write(buffer, 0, read)
    }
    output.toByteArray()
  }
}
