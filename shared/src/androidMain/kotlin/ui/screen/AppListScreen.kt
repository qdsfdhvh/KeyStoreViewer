package ui.screen

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import data.local.LocalFavoritesRepository
import data.local.LocalHistoryRepository
import data.model.SignSource
import data.model.UiAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rememberApkDocument
import ui.component.state.UiState
import ui.theme.AppTheme
import ui.widget.AppListItem
import ui.widget.LargeTitle
import ui.widget.PermissionRequestContent
import ui.widget.groupedRowShape
import util.getFilePathFromUri

@Composable
fun AppListScreen(onItemClick: (SignSource) -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val history = LocalHistoryRepository.current
  val favorites = LocalFavoritesRepository.current
  var showExportSheet by remember { mutableStateOf(false) }
  val launcher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.GetContent() },
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val path = withContext(Dispatchers.IO) { getFilePathFromUri(context, uri) }
        if (path != null) onItemClick(SignSource.Apk(path))
      }
    }
  }
  if (showExportSheet) ExportSheet(onDismiss = { showExportSheet = false })
  Scaffold { innerPadding ->
    Box(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).fillMaxSize()) {
      PermissionRequestContent(
        permissions = remember {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            listOf(Manifest.permission.QUERY_ALL_PACKAGES)
          } else {
            emptyList()
          }
        },
        modifier = Modifier.fillMaxSize(),
        label = "Allow access to the application list to view installed app signatures.",
      ) {
        val viewModel = viewModel {
          AppListViewModel(
            source = PackageManagerAppListSource(context.applicationContext),
            historyRepository = history,
            favoritesRepository = favorites,
          )
        }
        val state by viewModel.state.collectAsState()
        AppListContent(
          state = state,
          onEvent = state.eventSink,
          onOpenApk = { launcher.launch("application/vnd.android.package-archive") },
          onExport = { showExportSheet = true },
          onOpenApp = { app ->
            viewModel.recordViewed(app)
            onItemClick(SignSource.PackageName(app.packageName))
          },
          onFavorite = { app ->
            viewModel.toggleFavorite(app)
            Toast.makeText(context, "Favorites updated", Toast.LENGTH_SHORT).show()
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListContent(
  state: AppListScreenState,
  onEvent: (AppListScreenEvent) -> Unit,
  onOpenApk: () -> Unit,
  onExport: () -> Unit,
  onOpenApp: (UiAppInfo) -> Unit,
  onFavorite: (UiAppInfo) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
  ) {
    item {
      LargeTitle("Apps") {
        IconButton(onClick = onExport) {
          Icon(Icons.Default.FileDownload, "Export report", tint = MaterialTheme.colorScheme.primary)
        }
      }
      TextField(
        value = state.query,
        onValueChange = { onEvent(AppListScreenEvent.OnQueryChanged(it)) },
        placeholder = { Text("Search apps or packages") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (state.query.text.isNotEmpty()) {
            IconButton(onClick = { onEvent(AppListScreenEvent.OnQueryChanged(TextFieldValue())) }) {
              Icon(Icons.Default.Close, "Clear search")
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          focusedIndicatorColor = MaterialTheme.colorScheme.primary,
          unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
      )
      AppTypeFilter(state.appType) { onEvent(AppListScreenEvent.OnAppTypeChanged(it)) }
      Surface(
        onClick = onOpenApk,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      ) {
        AppListItem(
          leadingContent = {
            Icon(rememberApkDocument(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
          },
          headlineContent = { Text("Open APK file", color = MaterialTheme.colorScheme.primary) },
          supportingContent = { Text("View the file's signing certificates") },
          trailingContent = { Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, null) },
          modifier = Modifier.padding(16.dp),
        )
      }
      Text(
        "Files are read on this device, never uploaded.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          if (state.appType == AppType.User) "User apps" else "System apps",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onEvent(AppListScreenEvent.Refresh) }) { Text("Refresh") }
      }
    }
    when (val packages = state.displayPackages) {
      UiState.Loading -> item {
        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }

      is UiState.Loaded -> {
        if (packages.data.isEmpty()) {
          item {
            Text(
              "No apps found. Try another name or switch the app category.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(24.dp),
            )
          }
        }
        itemsIndexed(packages.data, key = { _, app -> app.packageName }) { index, app ->
          Surface(
            shape = groupedRowShape(index == 0, index == packages.data.lastIndex),
            modifier = Modifier.fillMaxWidth().combinedClickable(
              onClick = { onOpenApp(app) },
              onLongClick = { onFavorite(app) },
              onLongClickLabel = "Toggle favorite",
              role = Role.Button,
            ),
          ) {
            Column {
              AppListItem(
                leadingContent = {
                  app.icon?.let { Image(it, null, Modifier.size(40.dp)) }
                    ?: Icon(rememberApkDocument(), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                },
                headlineContent = { Text(app.name) },
                supportingContent = { Text(app.packageName) },
                trailingContent = { Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
              )
              if (index != packages.data.lastIndex) {
                HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AppTypeFilter(selected: AppType, onSelect: (AppType) -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.padding(vertical = 12.dp),
  ) {
    Row(Modifier.fillMaxWidth().padding(3.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      AppType.entries.forEach { type ->
        Surface(
          color = if (selected == type) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
          shape = MaterialTheme.shapes.extraSmall,
          shadowElevation = if (selected == type) 1.dp else 0.dp,
          modifier = Modifier.weight(1f).selectable(
            selected = selected == type,
            onClick = { onSelect(type) },
            role = Role.Tab,
          ),
        ) {
          Box(Modifier.heightIn(min = 48.dp).padding(8.dp), contentAlignment = Alignment.Center) {
            Text(if (type == AppType.User) "User apps" else "System apps", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun AppListContentPreview() {
  AppTheme {
    Scaffold { padding ->
      Box(Modifier.padding(padding)) {
        AppListContent(
          state = AppListScreenState(TextFieldValue(), AppType.User, UiState.Loaded(emptyList())) {},
          onEvent = {},
          onOpenApk = {},
          onExport = {},
          onOpenApp = {},
          onFavorite = {},
        )
      }
    }
  }
}
