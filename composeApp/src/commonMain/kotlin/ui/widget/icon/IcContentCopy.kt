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
fun rememberIcContentCopy(): ImageVector = remember {
  ImageVector.Builder(
    name = "IcContentCopy",
    defaultWidth = 40.0.dp,
    defaultHeight = 40.0.dp,
    viewportWidth = 40.0f,
    viewportHeight = 40.0f,
  ).apply {
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 1f,
      stroke = null,
      strokeAlpha = 1f,
      strokeLineWidth = 1.0f,
      strokeLineCap = StrokeCap.Butt,
      strokeLineJoin = StrokeJoin.Miter,
      strokeLineMiter = 1f,
      pathFillType = PathFillType.NonZero,
    ) {
      moveTo(13.292f, 30.958f)
      quadToRelative(-1.084f, 0f, -1.875f, -0.77f)
      quadToRelative(-0.792f, -0.771f, -0.792f, -1.855f)
      verticalLineToRelative(-22f)
      quadToRelative(0f, -1.083f, 0.792f, -1.854f)
      quadToRelative(0.791f, -0.771f, 1.875f, -0.771f)
      horizontalLineToRelative(17.083f)
      quadToRelative(1.083f, 0f, 1.854f, 0.771f)
      quadTo(33f, 5.25f, 33f, 6.333f)
      verticalLineToRelative(22f)
      quadToRelative(0f, 1.084f, -0.771f, 1.855f)
      quadToRelative(-0.771f, 0.77f, -1.854f, 0.77f)
      close()
      moveToRelative(0f, -2.625f)
      horizontalLineToRelative(17.083f)
      verticalLineToRelative(-22f)
      horizontalLineTo(13.292f)
      verticalLineToRelative(22f)
      close()
      moveTo(8f, 36.25f)
      quadToRelative(-1.083f, 0f, -1.854f, -0.771f)
      quadToRelative(-0.771f, -0.771f, -0.771f, -1.854f)
      verticalLineTo(10.792f)
      quadToRelative(0f, -0.542f, 0.375f, -0.938f)
      quadToRelative(0.375f, -0.396f, 0.917f, -0.396f)
      quadToRelative(0.583f, 0f, 0.958f, 0.396f)
      reflectiveQuadToRelative(0.375f, 0.938f)
      verticalLineToRelative(22.833f)
      horizontalLineToRelative(17.625f)
      quadToRelative(0.5f, 0f, 0.896f, 0.375f)
      reflectiveQuadToRelative(0.396f, 0.917f)
      quadToRelative(0f, 0.583f, -0.396f, 0.958f)
      reflectiveQuadToRelative(-0.896f, 0.375f)
      close()
      moveToRelative(5.292f, -29.917f)
      verticalLineToRelative(22f)
      verticalLineToRelative(-22f)
      close()
    }
  }.build()
}
