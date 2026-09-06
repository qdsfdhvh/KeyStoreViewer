package ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.local.HistoryEntry
import data.local.LocalHistoryRepository
import kotlinx.coroutines.launch
import ui.widget.AppListItem
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  onBack: () -> Unit,
  onOpen: (packageName: String) -> Unit,
) {
  val repository = LocalHistoryRepository.current
  val entries by repository.entries.collectAsState(emptyList())
  val scope = rememberCoroutineScope()

  Scaffold(
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
          Text("History")
        },
        actions = {
          if (entries.isNotEmpty()) {
            IconButton(
              onClick = { scope.launch { repository.clear() } },
            ) {
              Icon(
                Icons.Filled.DeleteSweep,
                contentDescription = "clear history",
              )
            }
          }
        },
      )
    },
  ) { innerPadding ->
    if (entries.isEmpty()) {
      Box(
        modifier = Modifier
          .padding(innerPadding)
          .fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          "No history yet",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .padding(innerPadding)
          .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
      ) {
        items(
          entries,
          key = { it.packageName },
        ) { entry ->
          HistoryItem(
            entry = entry,
            onClick = { onOpen(entry.packageName) },
            onRemove = { scope.launch { repository.remove(entry.packageName) } },
          )
        }
      }
    }
  }
}

@Composable
private fun HistoryItem(
  entry: HistoryEntry,
  onClick: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    tonalElevation = 1.dp,
    modifier = modifier,
  ) {
    AppListItem(
      headlineContent = {
        Text(entry.displayName)
      },
      supportingContent = {
        Text(entry.packageName)
      },
      trailingContent = {
        Text(
          DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(entry.viewedAtMillis)),
          style = MaterialTheme.typography.labelSmall,
        )
      },
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
  }
}
