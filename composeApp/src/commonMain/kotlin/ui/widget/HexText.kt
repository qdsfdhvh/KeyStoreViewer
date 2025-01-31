package ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.widget.icon.rememberIcALowerCase
import ui.widget.icon.rememberIcAUpperCase
import ui.widget.icon.rememberIcColon
import ui.widget.icon.rememberIcContentCopy

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
  SelectionContainer {
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
      Column(
        modifier = Modifier
          .padding(start = 8.dp, bottom = 8.dp)
          .fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              title,
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Spacer(Modifier.weight(1f))

          if (isShowColonButton) {
            SmallIcon(
              onClick = onToggleColonSplitClick,
              imageVector = rememberIcColon(),
              contentDescription = if (isColonSplit) {
                "hide colon $title"
              } else {
                "show colon $title"
              },
              color = if (isColonSplit) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
            )
          }

          if (isShowToggleUpperOrLowCase) {
            SmallIcon(
              onClick = onToggleUpperOrLowCaseClick,
              imageVector = if (isUpperCase) {
                rememberIcAUpperCase()
              } else {
                rememberIcALowerCase()
              },
              contentDescription = if (isUpperCase) {
                "show $title lower case"
              } else {
                "show $title upper case"
              },
              color = if (isUpperCase) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
            )
          }

          SmallIcon(
            onClick = onCopyContentClick,
            imageVector = rememberIcContentCopy(),
            contentDescription = "copy $title content",
          )
        }
        Text(
          text,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(end = 8.dp),
        )
      }
    }
  }
}
