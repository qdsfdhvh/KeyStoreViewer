package ui.widget

import androidx.compose.runtime.Composable
import ui.component.preview.ThemePreviews
import ui.theme.AppTheme
import ui.widget.icon.rememberIcShare

@ThemePreviews
@Composable
private fun SmallIconPreview() {
  AppTheme {
    SmallIcon(
      onClick = {},
      imageVector = rememberIcShare(),
      contentDescription = null,
    )
  }
}

@ThemePreviews
@Composable
private fun MediumIconPreview() {
  AppTheme {
    MediumIcon(
      onClick = {},
      imageVector = rememberIcShare(),
      contentDescription = null,
    )
  }
}
