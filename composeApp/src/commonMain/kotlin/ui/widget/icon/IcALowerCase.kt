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
fun rememberIcALowerCase(): ImageVector = remember {
  ImageVector.Builder(
    name = "IcALowerCase",
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
      moveTo(740.8f, 724.48f)
      lineToRelative(6.72f, 70.08f)
      horizontalLineToRelative(-114.24f)
      lineToRelative(-9.6f, -54.72f)
      curveToRelative(-12.8f, 16.64f, -31.68f, 31.68f, -56.64f, 44.48f)
      curveToRelative(-24.96f, 13.12f, -55.36f, 19.84f, -91.2f, 19.84f)
      curveToRelative(-38.4f, 0f, -72.96f, -7.04f, -103.04f, -21.44f)
      curveToRelative(-30.4f, -14.4f, -54.08f, -34.56f, -71.04f, -60.48f)
      curveToRelative(-16.96f, -25.92f, -25.28f, -55.04f, -25.28f, -87.68f)
      curveToRelative(0f, -40.96f, 10.88f, -74.56f, 32.96f, -101.12f)
      curveToRelative(22.08f, -26.56f, 48.96f, -45.76f, 80.96f, -58.24f)
      curveToRelative(32f, -12.16f, 63.36f, -18.24f, 94.08f, -18.24f)
      curveToRelative(50.56f, 0f, 85.76f, 4.8f, 105.92f, 13.76f)
      curveToRelative(20.16f, 9.28f, 31.04f, 14.4f, 32.96f, 15.04f)
      verticalLineToRelative(-39.36f)
      curveToRelative(0f, -30.08f, -10.24f, -54.72f, -30.4f, -73.92f)
      curveToRelative(-20.16f, -19.2f, -48.32f, -28.8f, -83.84f, -28.8f)
      curveToRelative(-32f, 0f, -55.68f, 6.4f, -71.36f, 18.56f)
      curveToRelative(-15.68f, 12.48f, -29.12f, 27.52f, -39.68f, 45.44f)
      lineTo(288f, 370.56f)
      curveToRelative(10.88f, -21.12f, 23.04f, -40f, 36.48f, -56.96f)
      curveToRelative(13.44f, -16.96f, 35.52f, -33.28f, 66.88f, -48.96f)
      curveToRelative(31.04f, -15.68f, 70.4f, -23.36f, 118.4f, -23.36f)
      curveToRelative(50.56f, 0f, 93.12f, 8.64f, 127.68f, 25.92f)
      reflectiveCurveToRelative(60.16f, 40.64f, 77.12f, 69.44f)
      curveToRelative(16.96f, 29.12f, 25.6f, 61.76f, 25.6f, 98.24f)
      lineToRelative(0.64f, 289.6f)
      close()
      moveToRelative(-117.12f, -139.2f)
      curveToRelative(-14.08f, -9.6f, -32f, -17.28f, -53.76f, -23.36f)
      curveToRelative(-21.76f, -6.08f, -42.24f, -8.96f, -61.44f, -8.96f)
      curveToRelative(-33.92f, 0f, -61.76f, 7.36f, -83.52f, 21.76f)
      curveToRelative(-21.76f, 14.4f, -32.64f, 34.24f, -32.64f, 59.84f)
      reflectiveCurveToRelative(10.24f, 45.12f, 30.08f, 58.24f)
      curveToRelative(20.16f, 13.12f, 41.6f, 19.52f, 64.96f, 19.52f)
      curveToRelative(33.28f, 0f, 64.32f, -8.96f, 93.12f, -27.2f)
      curveToRelative(28.8f, -18.24f, 43.2f, -49.92f, 43.2f, -95.36f)
      verticalLineToRelative(-4.48f)
      close()
    }
  }.build()
}
