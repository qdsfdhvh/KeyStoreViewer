package ui.screen

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.component.navigator.LocalNavigator
import ui.component.navigator.Screen
import ui.widget.AppListItem
import ui.widget.rememberAppName
import ui.widget.rememberPackageIcon
import util.getAppInfos
import util.plus

object AppListScreen : Screen {
  // private fun readResolve(): Any = AppListScreen

  @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.current

    val context = LocalContext.current

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

    Scaffold { innerPadding ->
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
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
            modifier = Modifier.fillMaxWidth()
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
              navigator.push(SignatureDetailScreen(appInfo.packageName))
            },
            context = context,
          )
        }
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
}
