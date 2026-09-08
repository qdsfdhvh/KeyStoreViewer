package ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rememberApkDocument
import ui.widget.LargeTitle
import java.net.URI
import kotlin.io.path.exists
import kotlin.io.path.toPath

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DropUploadContent(
  onNavigateToDetail: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var isDragging by remember { mutableStateOf(false) }

  val dragAndDropTarget = remember {
    object : DragAndDropTarget {
      override fun onStarted(event: DragAndDropEvent) {
        isDragging = true
      }

      override fun onDrop(event: DragAndDropEvent): Boolean {
        val data = event.dragData()

        var success = false
        if (data is DragData.FilesList) {
          val fileString = data.readFiles().firstOrNull()
          if (fileString != null && fileString.endsWith(".apk")) {
            val path = URI.create(fileString).toPath()
            if (path.exists()) {
              success = true

              onNavigateToDetail(path.toString())
            }
          }
        }
        if (!success) {
          scope.launch {
            snackbarHostState.showSnackbar("Only APK files are supported or file not found")
          }
        }
        return true
      }

      override fun onEnded(event: DragAndDropEvent) {
        isDragging = false
      }
    }
  }

  Scaffold(
    snackbarHost = {
      SnackbarHost(snackbarHostState)
    },
  ) { innerPadding ->
    Box(Modifier.padding(innerPadding).fillMaxSize().padding(24.dp), Alignment.Center) {
      Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
        LargeTitle("Inspect a signature")
        Surface(
          shape = MaterialTheme.shapes.large,
          color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = dragAndDropTarget,
          ),
        ) {
          Column(
            Modifier.heightIn(min = 280.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
          ) {
            Icon(rememberApkDocument(), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (isDragging) "Release to inspect" else "Drop an APK here", style = MaterialTheme.typography.headlineSmall)
            Text(
              "View signing fingerprints and public key information. Files are read locally, never uploaded.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Text(
          "Only APK files are supported.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
    }
  }
}
