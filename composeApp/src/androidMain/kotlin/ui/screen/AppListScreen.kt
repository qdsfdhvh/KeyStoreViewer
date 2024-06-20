package ui.screen

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.component.navigator.LocalNavigator
import ui.component.navigator.Screen
import ui.widget.rememberPackageIcon
import ui.widget.rememberPackageName
import util.getAppInfos

object AppListScreen : Screen {
    // private fun readResolve(): Any = AppListScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        val context = LocalContext.current
        val packages by produceState(emptyList()) {
            value = withContext(Dispatchers.IO) {
                context.getAppInfos()
            }
        }

        Scaffold { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                items(packages) { appInfo ->
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
            tonalElevation = 1.dp,
            modifier = modifier,
        ) {
            ListItem(
                leadingContent = {
                    val icon by rememberPackageIcon(item, context)
                    icon?.let {
                        Image(
                            it,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    } ?: run {
                        Spacer(Modifier.size(40.dp))
                    }
                },
                headlineContent = {
                    Text(rememberPackageName(item))
                },
                supportingContent = {
                    Text(item.packageName)
                },
            )
        }
    }
}
