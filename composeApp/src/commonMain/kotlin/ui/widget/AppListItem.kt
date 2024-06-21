package ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppListItem(
  modifier: Modifier = Modifier,
  leadingContent: @Composable (() -> Unit)? = null,
  supportingContent: @Composable (() -> Unit)? = null,
  overLineText: @Composable (() -> Unit)? = null,
  trailingContent: @Composable (RowScope.() -> Unit)? = null,
  verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
  iconEndPadding: Dp = 16.dp,
  headlineContent: @Composable () -> Unit,
) {
  val typography = MaterialTheme.typography
  val styledText = applyTextStyle(typography.titleMedium, headlineContent) ?: headlineContent
  val styledSecondaryText = applyTextStyle(typography.bodyMedium, supportingContent)
  val styledOverLineText = applyTextStyle(typography.bodyMedium, overLineText)
  val styledTrailing = applyTextStyle(typography.labelMedium, trailingContent)
  Row(
    modifier = modifier,
    verticalAlignment = verticalAlignment,
  ) {
    if (leadingContent != null) {
      leadingContent()
      Spacer(Modifier.width(iconEndPadding))
    }
    when {
      styledSecondaryText != null -> {
        Column(Modifier.weight(1f)) {
          styledText.invoke()
          Spacer(Modifier.height(4.dp))
          CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
          ) {
            styledSecondaryText()
          }
        }
      }
      styledOverLineText != null -> {
        Column(Modifier.weight(1f)) {
          styledOverLineText()
          Spacer(Modifier.height(4.dp))
          styledText.invoke()
        }
      }
      else -> {
        Box(Modifier.weight(1f)) {
          styledText.invoke()
        }
      }
    }
    if (styledTrailing != null) {
      Spacer(Modifier.width(8.dp))
      CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
      ) {
        styledTrailing()
      }
    }
  }
}

private fun applyTextStyle(
  textStyle: TextStyle,
  content: @Composable (() -> Unit)?,
): @Composable (() -> Unit)? {
  if (content == null) return null
  return {
    CompositionLocalProvider(
      LocalTextStyle provides textStyle,
      content = content,
    )
  }
}

private fun <T> applyTextStyle(
  textStyle: TextStyle,
  content: @Composable (T.() -> Unit)?,
): @Composable (T.() -> Unit)? {
  if (content == null) return null
  return {
    CompositionLocalProvider(
      LocalTextStyle provides textStyle,
      content = {
        content.invoke(this)
      },
    )
  }
}
