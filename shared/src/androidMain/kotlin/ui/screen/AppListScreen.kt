package ui.screen

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import data.local.LocalExportQuota
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
import ui.widget.PermissionRequestContent
import util.getFilePathFromUri

@Composable
fun AppListScreen(
  onItemClick: (SignSource) -> Unit,
  onOpenHistory: () -> Unit = {},
  onOpenFavorites: () -> Unit = {},
) {
  val context = LocalContext.current

  val scope = rememberCoroutineScope()

  val launcher = rememberLauncherForActivityResult(
    remember { ActivityResultContracts.GetContent() },
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val filePath = withContext(Dispatchers.IO) {
          getFilePathFromUri(context, uri)
        }
        if (filePath != null) {
          onItemClick(SignSource.Apk(filePath))
        }
      }
    }
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          // *.apk
          launcher.launch("application/vnd.android.package-archive")
        },
      ) {
        Icon(
          rememberApkDocument(),
          contentDescription = null,
        )
      }
    },
  ) { innerPadding ->
    var showExportSheet by remember { mutableStateOf(false) }
    if (showExportSheet) {
      ExportSheet(onDismiss = { showExportSheet = false })
    }
    PermissionRequestContent(
      permissions = remember {
        buildList {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(Manifest.permission.QUERY_ALL_PACKAGES)
          }
        }
      },
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize(),
      label = "The application needs to access the program list, please grant permission to read the application list.",
    ) {
      val state by remember {
        AppListScreenModel(context.applicationContext)
      }.state.collectAsState()
      AppListContent(
        innerPadding = innerPadding,
        state = state,
        context = context,
        onOpenHistory = onOpenHistory,
        onOpenFavorites = onOpenFavorites,
        onExportClick = { showExportSheet = true },
        onEvent = { event ->
          when (event) {
            is AppListScreenEvent.OnItemClick -> {
              onItemClick(SignSource.PackageName(event.packageName))
            }

            else -> {
              state.eventSink(event)
            }
          }
        },
      )
    }
  }
}

@Composable
private fun AppListContent(
  innerPadding: PaddingValues,
  state: AppListScreenState,
  onEvent: (AppListScreenEvent) -> Unit,
  onOpenHistory: () -> Unit,
  onOpenFavorites: () -> Unit,
  onExportClick: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  val scope = rememberCoroutineScope()
  val historyRepository = LocalHistoryRepository.current
  val favoritesRepository = LocalFavoritesRepository.current
  Column(
    modifier = modifier,
  ) {
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
      value = state.query,
      onValueChange = {
        onEvent(AppListScreenEvent.OnQueryChanged(it))
      },
      label = { Text("Search") },
      singleLine = true,
      shape = MaterialTheme.shapes.medium,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.background,
        unfocusedContainerColor = MaterialTheme.colorScheme.background,
      ),
      keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Done,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(top = innerPadding.calculateTopPadding()),
    )

    Spacer(Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SelectButton(
        onClick = { onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.User)) },
        selected = state.appType == AppType.User,
        text = "User",
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(12.dp))
      SelectButton(
        onClick = { onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.System)) },
        selected = state.appType == AppType.System,
        text = "System",
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(12.dp))
      IconButton(onClick = onExportClick) {
        Icon(
          Icons.Filled.FileDownload,
          contentDescription = "export report",
        )
      }
      IconButton(onClick = onOpenHistory) {
        Icon(
          Icons.Filled.History,
          contentDescription = "history",
        )
      }
      IconButton(onClick = onOpenFavorites) {
        Icon(
          Icons.Filled.Star,
          contentDescription = "favorites",
        )
      }
    }

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
    ) {
      when (val uiState = state.displayPackages) {
        UiState.Loading -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }

        is UiState.Loaded -> {
          if (uiState.data.isNotEmpty()) {
            items(
              uiState.data,
              key = { it.packageName },
            ) { appInfo ->
              AppItem(
                item = appInfo,
                onClick = {
                  scope.launch {
                    historyRepository.record(appInfo.packageName, appInfo.name)
                  }
                  onEvent(AppListScreenEvent.OnItemClick(appInfo.packageName))
                },
                onLongClick = {
                  scope.launch {
                    favoritesRepository.toggle(appInfo.packageName, appInfo.name)
                  }
                  Toast.makeText(context, "Favorites updated", Toast.LENGTH_SHORT).show()
                },
                context = context,
              )
            }
          } else {
            item {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(500.dp),
              ) {
                Text("No apps found")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onEvent(AppListScreenEvent.Refresh) }) {
                  Text("Refresh")
                }
              }
            }
          }
        }
      }
      item {
        Spacer(Modifier.height(32.dp))
      }
    }
  }
}

@Composable
private fun SelectButton(
  onClick: () -> Unit,
  selected: Boolean,
  text: String,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    modifier = modifier,
    colors = if (selected) {
      ButtonDefaults.buttonColors()
    } else {
      ButtonDefaults.outlinedButtonColors()
    },
    border = if (selected) {
      null
    } else {
      ButtonDefaults.outlinedButtonBorder(true)
    },
  ) {
    Text(text)
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
  item: UiAppInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
  onLongClick: () -> Unit = {},
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    tonalElevation = 1.dp,
    modifier = modifier.combinedClickable(
      onClick = onClick,
      onLongClick = onLongClick,
    ),
  ) {
    AppListItem(
      leadingContent = {
        item.icon?.let {
          Image(
            it,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
          )
        } ?: run {
          Spacer(Modifier.size(32.dp))
        }
      },
      headlineContent = {
        Text(item.name)
      },
      supportingContent = {
        Text(item.packageName)
      },
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
  }
}

@Preview
@Composable
private fun AppListContentPreview() {
  AppTheme {
    Scaffold { innerPadding ->
      AppListContent(
        innerPadding = innerPadding,
        state = AppListScreenState(
          query = TextFieldValue("A"),
          appType = AppType.User,
          displayPackages = UiState.Loaded(
            data = listOf(
              UiAppInfo(
                name = "App1",
                packageName = "com.example.app1",
                versionCode = 0,
                versionName = "",
                lastUpdateTime = 0,
                icon = null,
              ),
              UiAppInfo(
                name = "App2",
                packageName = "com.example.app2",
                versionCode = 0,
                versionName = "",
                lastUpdateTime = 0,
                icon = null,
              ),
            ),
          ),
          eventSink = {},
        ),
        onEvent = {},
        onOpenHistory = {},
        onOpenFavorites = {},
        onExportClick = {},
      )
    }
  }
}
