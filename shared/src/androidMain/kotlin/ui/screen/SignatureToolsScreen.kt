package ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.widget.AppListItem
import ui.widget.LargeTitle
import ui.widget.groupedRowShape

@Composable
fun SignatureToolsScreen(
  onOpenKeystoreBrowser: () -> Unit,
  onOpenApkCompare: () -> Unit,
  onOpenSignatureMonitor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(modifier = modifier) { padding ->
    LazyColumn(
      modifier = Modifier.padding(padding).fillMaxSize(),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
      item {
        LargeTitle("Tools")
        Text(
          "Signatures & certificates",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        )
        ToolRow(
          Icons.Default.Key,
          "KeyStore browser",
          "Read certificates from PKCS12 and certificate-entry JKS files.",
          onOpenKeystoreBrowser,
          first = true,
        )
        ToolRow(
          Icons.Default.Compare,
          "Compare APK signatures",
          "Compare the signing certificate sets of two APK files.",
          onOpenApkCompare,
        )
        ToolRow(
          Icons.Default.NotificationsActive,
          "Signature monitor",
          "Watch for certificate changes on this device. Off by default.",
          onOpenSignatureMonitor,
          last = true,
        )
        Text(
          "All tools are free. Files and certificate snapshots stay on this device.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun ToolRow(
  icon: ImageVector,
  title: String,
  description: String,
  onClick: () -> Unit,
  first: Boolean = false,
  last: Boolean = false,
) {
  Surface(
    onClick = onClick,
    shape = groupedRowShape(first, last),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      AppListItem(
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        modifier = Modifier.padding(16.dp),
      )
      if (!last) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}
