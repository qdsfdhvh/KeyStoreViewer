package ui.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SmallIcon(
  onClick: () -> Unit,
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = contentColorFor(color),
) {
  MediumIcon(
    onClick = onClick,
    imageVector = imageVector,
    contentDescription = contentDescription,
    modifier = modifier,
    color = color,
    contentColor = contentColor,
    padding = 6.dp,
    size = 16.dp,
  )
}

@Composable
fun MediumIcon(
  onClick: () -> Unit,
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = contentColorFor(color),
  padding: Dp = 6.dp,
  size: Dp = 24.dp,
) {
  Surface(
    onClick = onClick,
    color = color,
    contentColor = contentColor,
    shape = CircleShape,
    modifier = modifier,
  ) {
    Icon(
      imageVector,
      contentDescription = contentDescription,
      modifier = Modifier
        .padding(padding)
        .size(size),
    )
  }
}
