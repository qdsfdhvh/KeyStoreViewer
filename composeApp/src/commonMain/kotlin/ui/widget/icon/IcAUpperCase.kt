package ui.widget.icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Composable
fun rememberIcAUpperCase(): ImageVector = remember {
  ImageVector.Builder(
    name = "IcAUpperCase",
    defaultWidth = 40.dp,
    defaultHeight = 40.dp,
    viewportWidth = 1024f,
    viewportHeight = 1024f,
  ).apply {
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 1.0f,
      stroke = null,
      strokeAlpha = 1.0f,
      strokeLineWidth = 1.0f,
      strokeLineCap = StrokeCap.Butt,
      strokeLineJoin = StrokeJoin.Miter,
      strokeLineMiter = 1.0f,
      pathFillType = PathFillType.NonZero,
    ) {
      moveTo(696.64f, 769.6f)
      horizontalLineTo(325.12f)
      lineToRelative(-72f, 182.4f)
      horizontalLineToRelative(-144f)
      lineToRelative(334.4f, -880f)
      horizontalLineToRelative(136.32f)
      lineToRelative(335.36f, 880f)
      horizontalLineToRelative(-145.28f)
      lineToRelative(-73.28f, -182.4f)
      close()
      moveToRelative(-49.6f, -123.84f)
      lineTo(510.72f, 303.68f)
      lineTo(374.4f, 645.76f)
      horizontalLineToRelative(272.64f)
      close()
    }
  }.build()
}
