package ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Root availability is Android-only; the shared widget also supports native rendering tests.
enum class RootTab(val label: String, val icon: ImageVector) {
  Apps("Apps", Icons.Default.Apps),
  Tools("Tools", Icons.Default.Security),
  History("History", Icons.Default.History),
  Favorites("Favorites", Icons.Default.Star),
}

@Composable
fun RootTabBar(selected: RootTab, onSelect: (RootTab) -> Unit) {
  Surface(color = MaterialTheme.colorScheme.surface) {
    Column {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        Modifier.fillMaxWidth()
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
          .selectableGroup(),
      ) {
        RootTab.entries.forEach { tab ->
          val tint = if (tab == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          Column(
            modifier = Modifier.weight(1f)
              .selectable(selected = tab == selected, onClick = { onSelect(tab) }, role = Role.Tab)
              .heightIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
          ) {
            Icon(tab.icon, contentDescription = null, tint = tint)
            Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
          }
        }
      }
    }
  }
}
