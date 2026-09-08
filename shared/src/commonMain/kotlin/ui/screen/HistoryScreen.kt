package ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import data.local.LocalHistoryRepository
import ui.widget.AppListItem
import ui.widget.LargeTitle
import ui.widget.groupedRowShape
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(onOpen: (packageName: String) -> Unit) {
  val repository = LocalHistoryRepository.current
  val viewModel = viewModel { HistoryViewModel(repository) }
  val entries by viewModel.entries.collectAsState()
  Scaffold { padding ->
    LazyColumn(
      modifier = Modifier.padding(padding).fillMaxSize(),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
      item {
        LargeTitle("History") {
          if (entries.isNotEmpty()) {
            TextButton(onClick = viewModel::clear) { Text("Clear") }
          }
        }
        Text(
          if (entries.isEmpty()) "No history yet. Open an app to view its signatures." else "Recently viewed apps",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        )
      }
      itemsIndexed(entries, key = { _, entry -> entry.packageName }) { index, entry ->
        Surface(
          onClick = { onOpen(entry.packageName) },
          shape = groupedRowShape(index == 0, index == entries.lastIndex),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            AppListItem(
              leadingContent = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary) },
              headlineContent = { Text(entry.displayName) },
              supportingContent = {
                Column {
                  Text(entry.packageName)
                  Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.viewedAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
              },
              trailingContent = {
                IconButton(onClick = { viewModel.remove(entry.packageName) }) {
                  Icon(Icons.Default.Close, "Remove ${entry.displayName} from history")
                }
              },
              modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            )
            if (index != entries.lastIndex) {
              HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
          }
        }
      }
    }
  }
}
