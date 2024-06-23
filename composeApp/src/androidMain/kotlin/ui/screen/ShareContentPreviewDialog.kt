package ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ui.theme.AppTheme

@Composable
fun ShareContentPreviewDialog(
  onDismissRequest: () -> Unit,
  onShareClick: () -> Unit,
  content: String,
) {
  Dialog(onDismissRequest) {
    Surface(
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp),
      ) {
        Text(
          "Preview",
          style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
          Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 500.dp),
          ) {
            Text(
              content,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Button(onShareClick) {
          Text("Share")
        }
      }
    }
  }
}

@Preview
@Composable
private fun ShareContentPreviewDialogPreview() {
  AppTheme {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize(),
    ) {
      ShareContentPreviewDialog(
        onDismissRequest = {},
        onShareClick = {},
        content = "Hello, World!",
      )
    }
  }
}
