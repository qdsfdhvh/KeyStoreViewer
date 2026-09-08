package ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun HexText(
  title: String,
  text: String,
  onCopyContentClick: () -> Unit,
  modifier: Modifier = Modifier,
  isShowToggleUpperOrLowCase: Boolean = false,
  isUpperCase: Boolean = false,
  onToggleUpperOrLowCaseClick: () -> Unit = {},
  isShowColonButton: Boolean = false,
  isColonSplit: Boolean = false,
  onToggleColonSplitClick: () -> Unit = {},
) {
  Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        TextButton(
          onClick = onCopyContentClick,
          modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Copy $title" },
        ) {
          Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Copy")
        }
      }
      SelectionContainer {
        Text(
          text,
          style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
      }
      if (isShowToggleUpperOrLowCase || isShowColonButton) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isShowToggleUpperOrLowCase) {
            TextButton(
              onClick = onToggleUpperOrLowCaseClick,
              modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Change $title letter case"
                stateDescription = if (isUpperCase) "Uppercase" else "Lowercase"
              },
            ) { Text(if (isUpperCase) "ABC · Uppercase" else "abc · Lowercase") }
          }
          if (isShowColonButton) {
            TextButton(
              onClick = onToggleColonSplitClick,
              modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Change $title separator"
                stateDescription = if (isColonSplit) "Colons" else "No separator"
              },
            ) { Text(if (isColonSplit) "AA:BB · Colons" else "AABB · No separator") }
          }
        }
      }
    }
  }
}
