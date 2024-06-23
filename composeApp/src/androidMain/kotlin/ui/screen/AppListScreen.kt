package ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.component.navigator.LocalNavigator
import ui.component.navigator.Screen
import ui.widget.AppListItem
import ui.widget.rememberAppName
import ui.widget.rememberPackageIcon
import util.getAppInfos
import util.startSystemSetting

object AppListScreen : Screen {
  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.current

    val context = LocalContext.current

    Scaffold { innerPadding ->

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val permissionState = rememberPermissionState(Manifest.permission.QUERY_ALL_PACKAGES)
        when (val status = permissionState.status) {
          PermissionStatus.Granted -> {
            AppListContent(
              innerPadding = innerPadding,
              context = context,
              onItemClick = { packageName ->
                navigator.push(SignatureDetailScreen(packageName))
              },
            )
          }
          is PermissionStatus.Denied -> {
            LaunchedEffect(Unit) {
              permissionState.launchPermissionRequest()
            }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            ) {
              Text("应用需要读取程序列表，请授予读取应用程序列表权限。")
              Button(
                onClick = {
                  if (status.shouldShowRationale) {
                    context.startSystemSetting()
                  } else {
                    permissionState.launchPermissionRequest()
                  }
                },
              ) {
                Text("请求权限")
              }
            }
          }
        }
      } else {
        AppListContent(
          innerPadding = innerPadding,
          context = context,
          onItemClick = { packageName ->
            navigator.push(SignatureDetailScreen(packageName))
          },
        )
      }
    }
  }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListContent(
  innerPadding: PaddingValues,
  onItemClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  context: Context = LocalContext.current,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val packages by produceState(emptyList()) {
    value = withContext(Dispatchers.IO) {
      context.getAppInfos()
    }
  }
  val displayPackages by remember {
    derivedStateOf {
      if (query.isNotEmpty()) {
        packages.filter {
          it.packageName.contains(query, ignoreCase = true) ||
            it.applicationInfo.loadLabel(context.packageManager).contains(query, ignoreCase = true)
        }
      } else {
        packages
      }
    }
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    stickyHeader {
      var changeQuery by remember { mutableStateOf(TextFieldValue(query)) }
      OutlinedTextField(
        value = changeQuery,
        onValueChange = { changeQuery = it },
        label = { Text("Search") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.background,
          unfocusedContainerColor = MaterialTheme.colorScheme.background,
        ),
        keyboardOptions = KeyboardOptions(
          imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
          onSearch = {
            query = changeQuery.text
          },
          onDone = {
            query = changeQuery.text
          },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = innerPadding.calculateTopPadding()),
      )
    }
    item {
      Spacer(Modifier.height(8.dp))
    }
    items(
      displayPackages,
      key = { it.packageName },
    ) { appInfo ->
      AppItem(
        item = appInfo,
        onClick = {
          onItemClick(appInfo.packageName)
        },
        context = context,
      )
    }
  }
}

@Composable
private fun AppItem(
  item: PackageInfo,
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
        Text(rememberAppName(item))
      },
      supportingContent = {
        Text(item.packageName)
      },
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
  }
}
