package ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import util.startSystemSetting

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestContent(
  permissions: List<String>,
  label: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  if (permissions.isEmpty()) {
    content()
    return
  }

  val context = LocalContext.current
  val permissionState = rememberMultiplePermissionsState(permissions)
  if (permissionState.allPermissionsGranted) {
    content()
  } else {
    LaunchedEffect(Unit) {
      permissionState.launchMultiplePermissionRequest()
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = modifier,
    ) {
      Text(label)
      Button(
        onClick = {
          if (permissionState.shouldShowRationale) {
            context.startSystemSetting()
          } else {
            permissionState.launchMultiplePermissionRequest()
          }
        },
      ) {
        Text("Request")
      }
    }
  }
}
