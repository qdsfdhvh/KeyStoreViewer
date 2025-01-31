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
fun rememberIcShare(): ImageVector = remember {
  ImageVector.Builder(
    name = "share",
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
      moveTo(30.125f, 36.417f)
      quadToRelative(-1.958f, 0f, -3.354f, -1.375f)
      quadToRelative(-1.396f, -1.375f, -1.396f, -3.334f)
      quadToRelative(0f, -0.291f, 0.063f, -0.666f)
      quadToRelative(0.062f, -0.375f, 0.187f, -0.667f)
      lineToRelative(-12.333f, -7.167f)
      quadToRelative(-0.625f, 0.709f, -1.521f, 1.104f)
      quadToRelative(-0.896f, 0.396f, -1.813f, 0.396f)
      quadToRelative(-2f, 0f, -3.375f, -1.375f)
      reflectiveQuadTo(5.208f, 20f)
      quadToRelative(0f, -1.958f, 1.396f, -3.333f)
      quadTo(8f, 15.292f, 9.958f, 15.292f)
      quadToRelative(0.917f, 0f, 1.792f, 0.354f)
      reflectiveQuadToRelative(1.542f, 1.021f)
      lineToRelative(12.333f, -7.084f)
      quadToRelative(-0.125f, -0.291f, -0.187f, -0.645f)
      quadToRelative(-0.063f, -0.355f, -0.063f, -0.646f)
      quadToRelative(0f, -1.959f, 1.396f, -3.354f)
      quadToRelative(1.396f, -1.396f, 3.354f, -1.396f)
      quadToRelative(1.958f, 0f, 3.333f, 1.375f)
      quadToRelative(1.375f, 1.375f, 1.375f, 3.333f)
      quadToRelative(0f, 2f, -1.375f, 3.375f)
      reflectiveQuadTo(30.125f, 13f)
      quadToRelative(-0.958f, 0f, -1.833f, -0.312f)
      quadToRelative(-0.875f, -0.313f, -1.5f, -1.021f)
      lineToRelative(-12.334f, 6.916f)
      quadToRelative(0.084f, 0.292f, 0.146f, 0.709f)
      quadToRelative(0.063f, 0.416f, 0.063f, 0.708f)
      reflectiveQuadToRelative(-0.063f, 0.625f)
      quadToRelative(-0.062f, 0.333f, -0.146f, 0.625f)
      lineToRelative(12.334f, 7.042f)
      quadToRelative(0.625f, -0.625f, 1.458f, -0.98f)
      quadToRelative(0.833f, -0.354f, 1.875f, -0.354f)
      quadToRelative(1.958f, 0f, 3.333f, 1.396f)
      quadToRelative(1.375f, 1.396f, 1.375f, 3.354f)
      quadToRelative(0f, 1.959f, -1.375f, 3.334f)
      reflectiveQuadToRelative(-3.333f, 1.375f)
      close()
      moveToRelative(0f, -26.042f)
      quadToRelative(0.875f, 0f, 1.479f, -0.625f)
      quadToRelative(0.604f, -0.625f, 0.604f, -1.458f)
      quadToRelative(0f, -0.875f, -0.625f, -1.479f)
      quadToRelative(-0.625f, -0.605f, -1.458f, -0.605f)
      quadToRelative(-0.875f, 0f, -1.479f, 0.605f)
      quadToRelative(-0.604f, 0.604f, -0.604f, 1.479f)
      quadToRelative(0f, 0.875f, 0.604f, 1.479f)
      quadToRelative(0.604f, 0.604f, 1.479f, 0.604f)
      close()
      moveTo(9.958f, 22.083f)
      quadToRelative(0.875f, 0f, 1.48f, -0.604f)
      quadToRelative(0.604f, -0.604f, 0.604f, -1.479f)
      quadToRelative(0f, -0.875f, -0.625f, -1.479f)
      quadToRelative(-0.625f, -0.604f, -1.459f, -0.604f)
      quadToRelative(-0.875f, 0f, -1.479f, 0.604f)
      quadToRelative(-0.604f, 0.604f, -0.604f, 1.479f)
      quadToRelative(0f, 0.875f, 0.604f, 1.479f)
      quadToRelative(0.604f, 0.604f, 1.479f, 0.604f)
      close()
      moveToRelative(20.167f, 11.709f)
      quadToRelative(0.875f, 0f, 1.479f, -0.625f)
      quadToRelative(0.604f, -0.625f, 0.604f, -1.459f)
      quadToRelative(0f, -0.875f, -0.625f, -1.479f)
      quadToRelative(-0.625f, -0.604f, -1.458f, -0.604f)
      quadToRelative(-0.875f, 0f, -1.479f, 0.604f)
      quadToRelative(-0.604f, 0.604f, -0.604f, 1.479f)
      quadToRelative(0f, 0.875f, 0.604f, 1.48f)
      quadToRelative(0.604f, 0.604f, 1.479f, 0.604f)
      close()
      moveToRelative(0f, -25.5f)
      close()
      moveTo(9.958f, 20f)
      close()
      moveToRelative(20.167f, 11.708f)
      close()
    }
  }.build()
}
