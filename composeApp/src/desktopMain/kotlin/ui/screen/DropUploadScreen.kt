package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

object DropUploadScreen : Screen {

  private fun readResolve(): Any = DropUploadScreen

  @Composable
  override fun Content() {
    DropUploadContent()
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DropUploadContent() {
  val validMimeTypePrefixes = remember {
    setOf(
      "image/",
      "text/",
      "video/",
      "audio/",
    )
  }
  var backgroundColor by remember { mutableStateOf(Color.Transparent) }
  val dragAndDropTarget = remember {
    object : DragAndDropTarget {
      override fun onStarted(event: DragAndDropEvent) {
        backgroundColor = Color.DarkGray.copy(alpha = 0.2f)
      }

      override fun onDrop(event: DragAndDropEvent): Boolean {
        val data = event.dragData()
        if (data is DragData.FilesList) {
          println(data.readFiles().joinToString { it })
        }
        return true
      }

      override fun onEnded(event: DragAndDropEvent) {
        backgroundColor = Color.Transparent
      }
    }
  }
  Box(
    modifier =
    Modifier.fillMaxSize()
      .dragAndDropTarget(
        shouldStartDragAndDrop = accept@{ _ ->
          true
        },
        target = dragAndDropTarget,
      )
      .background(backgroundColor)
      .border(width = 4.dp, color = Color.Magenta, shape = RoundedCornerShape(16.dp)),
  ) {
    Text(modifier = Modifier.align(Alignment.Center), text = "Drop anything here")
  }
}
