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
fun rememberIcColon(): ImageVector = remember {
  ImageVector.Builder(
    name = "冒号 (2)",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
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
      moveTo(507.84f, 387.712f)
      curveToRelative(-26.048f, 0f, -48.48f, -9.6f, -67.264f, -28.736f)
      arcToRelative(95.04f, 95.04f, 0f, isMoreThanHalf = false, isPositiveArc = true, -28.16f, -68.992f)
      curveToRelative(0f, -26.816f, 9.376f, -49.824f, 28.16f, -68.992f)
      curveToRelative(18.784f, -19.168f, 41.216f, -28.736f, 67.264f, -28.736f)
      curveToRelative(26.816f, 0f, 49.6f, 9.6f, 68.416f, 28.736f)
      arcToRelative(95.04f, 95.04f, 0f, isMoreThanHalf = false, isPositiveArc = true, 28.16f, 68.992f)
      arcToRelative(95.04f, 95.04f, 0f, isMoreThanHalf = false, isPositiveArc = true, -28.16f, 68.992f)
      curveToRelative(-18.784f, 19.136f, -41.6f, 28.736f, -68.416f, 28.736f)
      close()
      moveToRelative(0f, 428.832f)
      curveToRelative(-26.048f, 0f, -48.48f, -9.6f, -67.264f, -28.736f)
      arcToRelative(95.04f, 95.04f, 0f, isMoreThanHalf = false, isPositiveArc = true, -28.16f, -68.992f)
      curveToRelative(0f, -26.816f, 9.376f, -49.824f, 28.16f, -68.992f)
      curveToRelative(18.784f, -19.136f, 41.216f, -28.736f, 67.264f, -28.736f)
      curveToRelative(26.048f, 0f, 48.672f, 9.6f, 67.84f, 28.736f)
      arcToRelative(94.08f, 94.08f, 0f, isMoreThanHalf = false, isPositiveArc = true, 28.736f, 68.992f)
      arcToRelative(95.04f, 95.04f, 0f, isMoreThanHalf = false, isPositiveArc = true, -28.16f, 68.992f)
      curveToRelative(-18.784f, 19.168f, -41.6f, 28.736f, -68.416f, 28.736f)
      close()
    }
  }.build()
}
