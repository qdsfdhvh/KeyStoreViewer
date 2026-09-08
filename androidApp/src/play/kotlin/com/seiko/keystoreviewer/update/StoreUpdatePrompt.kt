package com.seiko.keystoreviewer.update

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A non-modal prompt: neither startup nor returning from an ad/SAF launches Play consent. */
@Composable
fun StoreUpdatePrompt() {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var resultController by remember { mutableStateOf<FlexibleUpdateController?>(null) }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
    resultController?.onConsentResult(
      accepted = it.resultCode == Activity.RESULT_OK,
      failed = it.resultCode != Activity.RESULT_OK && it.resultCode != Activity.RESULT_CANCELED,
    )
  }
  val controller = remember(context, launcher) {
    FlexibleUpdateController(GooglePlayUpdateClient(context.applicationContext, launcher))
  }
  DisposableEffect(controller, lifecycle) {
    resultController = controller
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> controller.resume()
        Lifecycle.Event.ON_PAUSE -> controller.pause()
        else -> Unit
      }
    }
    lifecycle.addObserver(observer)
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) controller.resume()
    onDispose {
      lifecycle.removeObserver(observer)
      controller.pause()
      resultController = null
    }
  }
  val state by controller.state.collectAsState()
  if (state == UpdateUi.Hidden) return
  Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
    Column(
      Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
      Text(
        when (val value = state) {
          UpdateUi.Available -> "An update is available on Google Play."
          UpdateUi.Checking -> "Checking for an update…"
          UpdateUi.Ready -> "Update downloaded. Restart to install when you're ready."
          UpdateUi.Installing -> "Restarting to install the update…"
          is UpdateUi.Error -> if (value.completing) "Couldn't install the update. Please try again." else "Couldn't update right now. Please try again later."
          UpdateUi.Hidden -> ""
        },
        style = MaterialTheme.typography.bodyMedium,
      )
      if (state != UpdateUi.Checking && state != UpdateUi.Installing) {
        FlowRow {
          TextButton(
            onClick = {
              when (state) {
                UpdateUi.Ready -> controller.restart()
                is UpdateUi.Error -> controller.retry()
                else -> controller.requestUpdate()
              }
            },
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(
              when (state) {
                UpdateUi.Ready -> "Restart"
                is UpdateUi.Error -> "Retry"
                else -> "Update"
              },
            )
          }
          TextButton(onClick = controller::dismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Later") }
        }
      }
    }
  }
}
