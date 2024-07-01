package ui.screen

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.model.AppInfoEntry
import ui.component.state.UiState
import ui.theme.AppTheme
import ui.widget.AppListItem
import ui.widget.PermissionRequestContent
import ui.widget.rememberPackageIcon

object AppListScreen : Screen {
  private fun readResolve(): Any = AppListScreen

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    Scaffold { innerPadding ->
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
        label = "应用需要读取程序列表，请授予读取应用程序列表权限。",
      ) {
        val state by rememberScreenModel {
          AppListScreenModel(context.applicationContext)
        }.state.collectAsState()
        AppListContent(
          innerPadding = innerPadding,
          state = state,
          context = context,
          onEvent = { event ->
            when (event) {
              is AppListScreenEvent.OnItemClick -> {
                navigator.push(SignatureDetailScreen(event.packageName))
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
}

@Composable
private fun AppListContent(
  innerPadding: PaddingValues,
  state: AppListScreenState,
  onEvent: (AppListScreenEvent) -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
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
    ) {
      SelectButton(
        onClick = { onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.User)) },
        selected = state.appType == AppType.User,
        text = "User",
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(16.dp))
      SelectButton(
        onClick = { onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.System)) },
        selected = state.appType == AppType.System,
        text = "System",
        modifier = Modifier.weight(1f),
      )
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
          items(
            uiState.data,
            key = { it.packageName },
          ) { appInfo ->
            AppItem(
              item = appInfo,
              onClick = {
                onEvent(AppListScreenEvent.OnItemClick(appInfo.packageName))
              },
              context = context,
            )
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
      ButtonDefaults.outlinedButtonBorder
    },
  ) {
    // Checkbox(
    //   checked = selected,
    //   onCheckedChange = null,
    // )
    // Spacer(Modifier.width(8.dp))
    Text(text)
  }
}

@Composable
private fun AppItem(
  item: AppInfoEntry,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    tonalElevation = 1.dp,
    modifier = modifier,
  ) {
    AppListItem(
      leadingContent = {
        val icon by rememberPackageIcon(item, context)
        icon?.let {
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
              AppInfoEntry(
                name = "App1",
                packageName = "com.example.app1",
                lastUpdateTime = 0,
              ),
              AppInfoEntry(
                name = "App2",
                packageName = "com.example.app2",
                lastUpdateTime = 0,
              ),
            ),
          ),
          eventSink = {},
        ),
        onEvent = {},
      )
    }
  }
}
