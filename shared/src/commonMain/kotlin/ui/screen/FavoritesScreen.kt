package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import data.local.LocalFavoritesRepository
import kotlinx.coroutines.launch
import ui.widget.AppListItem
import ui.widget.LargeTitle
import ui.widget.groupedRowShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(onOpen: (packageName: String) -> Unit) {
  val repository = LocalFavoritesRepository.current
  val entries by repository.entries.collectAsState(emptyList())
  val scope = rememberCoroutineScope()
  Scaffold { padding ->
    LazyColumn(
      modifier = Modifier.padding(padding).fillMaxSize(),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
      item {
        LargeTitle("Favorites")
        Text(
          if (entries.isEmpty()) "Long press an app in the list to add it to favorites." else "Your frequently viewed signatures",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        )
      }
      itemsIndexed(entries, key = { _, entry -> entry.packageName }) { index, entry ->
        val remove = {
          scope.launch { repository.remove(entry.packageName) }
          Unit
        }
        Surface(
          shape = groupedRowShape(index == 0, index == entries.lastIndex),
          modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { onOpen(entry.packageName) },
            onLongClick = remove,
            onLongClickLabel = "Remove favorite",
            role = Role.Button,
          ),
        ) {
          Column {
            AppListItem(
              headlineContent = { Text(entry.displayName) },
              supportingContent = { Text(entry.packageName) },
              trailingContent = {
                IconButton(onClick = remove) {
                  Icon(Icons.Default.Star, "Remove ${entry.displayName} from favorites", tint = MaterialTheme.colorScheme.primary)
                }
              },
              modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            )
            if (index != entries.lastIndex) {
              HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
          }
        }
      }
    }
  }
}
