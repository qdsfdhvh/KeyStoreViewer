package ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * M4 entry point for the three signature tools. All tools are free on both
 * the foss and play flavors; none of them touch the ad stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureToolsScreen(
  onBack: () -> Unit,
  onOpenKeystoreBrowser: () -> Unit,
  onOpenApkCompare: () -> Unit,
  onOpenSignatureMonitor: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
          Text("Signature tools")
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
        ToolCard(
          icon = Icons.Filled.Key,
          title = "KeyStore browser",
          description = "Open a PKCS12 (.p12/.pfx) or certificate-entry JKS " +
            "keystore file read-only and inspect its certificates. The " +
            "password stays on this device.",
          onClick = onOpenKeystoreBrowser,
        )
      }
      item {
        ToolCard(
          icon = Icons.Filled.Compare,
          title = "Compare APK signatures",
          description = "Compare the signing certificate sets of two APK " +
            "files, with package and version info.",
          onClick = onOpenApkCompare,
        )
      }
      item {
        ToolCard(
          icon = Icons.Filled.NotificationsActive,
          title = "Signature monitor",
          description = "Optionally watch installed apps and get an alert " +
            "when one comes back with different signing certificates. " +
            "Off by default.",
          onClick = onOpenSignatureMonitor,
        )
      }
    }
  }
}

@Composable
private fun ToolCard(
  icon: ImageVector,
  title: String,
  description: String,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Icon(icon, contentDescription = null)
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
