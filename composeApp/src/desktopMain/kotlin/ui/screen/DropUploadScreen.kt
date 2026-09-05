package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.model.SignSource
import kotlinx.coroutines.launch
import java.net.URI
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.toPath

object DropUploadScreen : Screen {

  private fun readResolve(): Any = DropUploadScreen

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    DropUploadContent(
      onNavigateToDetail = {
        navigator.push(
          SignatureDetailScreen(
            SignSource.Apk(it),
          ),
        )
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DropUploadContent(
  onNavigateToDetail: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var backgroundColor by remember { mutableStateOf(Color.Transparent) }

  val dragAndDropTarget = remember {
    object : DragAndDropTarget {
      override fun onStarted(event: DragAndDropEvent) {
        backgroundColor = Color.DarkGray.copy(alpha = 0.2f)
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

              println(path)
              println("size: ${path.fileSize()}")
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
        backgroundColor = Color.Transparent
      }
    }
  }

  Scaffold(
    snackbarHost = {
      SnackbarHost(snackbarHostState)
    },
  ) { innerPadding ->
    Box(Modifier.padding(innerPadding).fillMaxSize(), Alignment.Center) {
      Box(
        modifier =
        Modifier.fillMaxSize(0.5f)
          .dragAndDropTarget(
            shouldStartDragAndDrop = accept@{ _ ->
              true
            },
            target = dragAndDropTarget,
          )
          .background(backgroundColor)
          .border(
            width = 4.dp,
            color = Color.Black,
            shape = MaterialTheme.shapes.large,
          ),
      ) {
        Text(modifier = Modifier.align(Alignment.Center), text = "Drop anything here")
      }
    }
  }
}
